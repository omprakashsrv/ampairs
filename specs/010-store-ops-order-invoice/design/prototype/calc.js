/* ──────────────────────────────────────────────────────────────────────
   calc-core — pure GST / unit / discount math.  Mirrors spec 010
   (FR-003/004/014/015/016, C1/C2). No UI, no framework. window.Calc.
   Ordering ALWAYS: line discount → overall-discount apportionment → tax.
   ────────────────────────────────────────────────────────────────────── */
(function () {
  // round half-up to 2 decimals
  function r2(x) {
    return Math.round((x + Number.EPSILON) * 100) / 100;
  }
  function inr(x, dp) {
    if (dp === undefined) dp = 2;
    const n = (x < 0 ? -x : x).toFixed(dp);
    // Indian digit grouping
    const parts = n.split('.');
    let s = parts[0];
    let last3 = s.length > 3 ? s.slice(-3) : s;
    const rest = s.length > 3 ? s.slice(0, -3) : '';
    const grouped = rest ? rest.replace(/\B(?=(\d{2})+(?!\d))/g, ',') + ',' + last3 : last3;
    return (x < 0 ? '-' : '') + '₹' + grouped + (parts[1] !== undefined ? '.' + parts[1] : '');
  }

  // place-of-supply: same state code => INTRA (CGST+SGST), else INTER (IGST)
  function scenario(buyerStateCode, sellerStateCode) {
    if (!buyerStateCode) return 'INTRA'; // missing buyer GSTIN → assume intra vs seller
    return String(buyerStateCode) === String(sellerStateCode) ? 'INTRA' : 'INTER';
  }

  // split a total GST rate into its components for a scenario
  function components(rate, sc) {
    if (rate <= 0) return [];
    if (sc === 'INTER') return [{ name: 'IGST', rate: rate }];
    return [
      { name: 'CGST', rate: rate / 2 },
      { name: 'SGST', rate: rate / 2 },
    ];
  }

  // discount resolver → resolved value against a base
  function resolveDiscount(d, base) {
    if (!d || !d.amount) return 0;
    if (d.kind === 'PERCENT') return r2(base * (d.amount / 100));
    return r2(Math.min(d.amount, base));
  }

  /* Compute the whole document.
     doc = {
       priceMode: 'TAX_EXCLUSIVE' | 'TAX_INCLUSIVE',
       overallDiscountMode: 'PRE_TAX_APPORTIONED' | 'POST_TAX_REDUCTION',
       overallDiscount: { kind, amount } | null,
       scenario: 'INTRA' | 'INTER',
       lines: [{ id, name, hsn, gstRate, unitName, multiplier, decimals,
                 quantity, unitPrice, lineDiscount:{kind,amount}, variant }]
     }
  */
  function computeDocument(doc) {
    const sc = doc.scenario;
    const incl = doc.priceMode === 'TAX_INCLUSIVE';

    // pass 1 — per-line gross, line discount, base after line discount
    const work = doc.lines.map((ln) => {
      const gross = r2((ln.unitPrice || 0) * (ln.quantity || 0));
      const baseQuantity = r2((ln.quantity || 0) * (ln.multiplier || 1));
      const lineDiscVal = resolveDiscount(ln.lineDiscount, gross);
      const afterLine = Math.max(0, r2(gross - lineDiscVal));
      return { ln, gross, baseQuantity, lineDiscVal, afterLine, apportioned: 0 };
    });

    const sumAfterLine = r2(work.reduce((a, w) => a + w.afterLine, 0));

    // pass 2 — overall discount
    let overallDiscVal = 0;
    let postTaxDiscount = 0;
    if (doc.overallDiscount && doc.overallDiscount.amount) {
      if (doc.overallDiscountMode === 'PRE_TAX_APPORTIONED') {
        overallDiscVal = resolveDiscount(doc.overallDiscount, sumAfterLine);
        // apportion proportional to afterLine; remainder to the largest line
        let assigned = 0;
        let largest = 0;
        work.forEach((w, i) => {
          if (sumAfterLine > 0) {
            w.apportioned = r2((w.afterLine / sumAfterLine) * overallDiscVal);
          }
          assigned = r2(assigned + w.apportioned);
          if (w.afterLine > work[largest].afterLine) largest = i;
        });
        const remainder = r2(overallDiscVal - assigned);
        if (work.length) work[largest].apportioned = r2(work[largest].apportioned + remainder);
      }
      // POST_TAX_REDUCTION resolved after totals (needs grand incl tax)
    }

    // pass 3 — tax per line
    let basePrice = 0;
    let totalTax = 0;
    const taxGroupMap = {}; // key name|rate
    const lines = work.map((w) => {
      const net = Math.max(0, r2(w.afterLine - w.apportioned)); // taxable base (excl) OR inclusive amount
      const rate = w.ln.gstRate || 0;
      const comps = components(rate, sc);
      let taxable, compVals, lineTax;

      if (rate <= 0) {
        taxable = net;
        compVals = [];
        lineTax = 0;
      } else if (!incl) {
        taxable = net;
        compVals = comps.map((c) => ({ name: c.name, rate: c.rate, value: r2(taxable * c.rate) }));
        lineTax = r2(compVals.reduce((a, c) => a + c.value, 0));
      } else {
        const inclAmt = net;
        taxable = r2(inclAmt / (1 + rate));
        lineTax = r2(inclAmt - taxable);
        if (sc === 'INTER') {
          compVals = [{ name: 'IGST', rate: rate, value: lineTax }];
        } else {
          const cgst = r2(lineTax / 2);
          compVals = [
            { name: 'CGST', rate: rate / 2, value: cgst },
            { name: 'SGST', rate: rate / 2, value: r2(lineTax - cgst) },
          ];
        }
      }
      const lineTotal = r2(taxable + lineTax);
      basePrice = r2(basePrice + taxable);
      totalTax = r2(totalTax + lineTax);
      compVals.forEach((c) => {
        const k = c.name + '|' + c.rate;
        if (!taxGroupMap[k]) taxGroupMap[k] = { name: c.name, rate: c.rate, value: 0 };
        taxGroupMap[k].value = r2(taxGroupMap[k].value + c.value);
      });
      return {
        ...w.ln,
        gross: w.gross,
        baseQuantity: w.baseQuantity,
        lineDiscVal: w.lineDiscVal,
        apportioned: w.apportioned,
        taxable,
        taxInfos: compVals,
        totalTax: lineTax,
        lineTotal,
        exempt: rate <= 0,
        floored: w.afterLine - w.apportioned <= 0 && w.gross > 0,
      };
    });

    let grand = r2(basePrice + totalTax);
    if (doc.overallDiscount && doc.overallDiscount.amount && doc.overallDiscountMode === 'POST_TAX_REDUCTION') {
      postTaxDiscount = resolveDiscount(doc.overallDiscount, grand);
      grand = Math.max(0, r2(grand - postTaxDiscount));
    }

    // tax groups sorted: CGST, SGST, IGST then by rate
    const order = { CGST: 0, SGST: 1, IGST: 2 };
    const taxGroups = Object.values(taxGroupMap).sort(
      (a, b) => (order[a.name] - order[b.name]) || (a.rate - b.rate)
    );

    const subtotalGross = r2(lines.reduce((a, l) => a + l.gross, 0));
    const totalLineDiscount = r2(lines.reduce((a, l) => a + l.lineDiscVal, 0));

    return {
      lines,
      scenario: sc,
      priceMode: doc.priceMode,
      subtotalGross,
      totalLineDiscount,
      overallDiscount: overallDiscVal,
      overallDiscountMode: doc.overallDiscountMode,
      postTaxDiscount,
      basePrice,      // total taxable
      taxGroups,      // [{name,rate,value}]
      totalTax,
      grandTotal: grand,
      totalQuantity: r2(lines.reduce((a, l) => a + (l.quantity || 0), 0)),
      totalItems: lines.length,
    };
  }

  // amount in words (Indian) — for invoice
  function inWords(num) {
    const a = ['', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine', 'ten',
      'eleven', 'twelve', 'thirteen', 'fourteen', 'fifteen', 'sixteen', 'seventeen', 'eighteen', 'nineteen'];
    const b = ['', '', 'twenty', 'thirty', 'forty', 'fifty', 'sixty', 'seventy', 'eighty', 'ninety'];
    function two(n) { return n < 20 ? a[n] : b[Math.floor(n / 10)] + (n % 10 ? ' ' + a[n % 10] : ''); }
    function three(n) { return (Math.floor(n / 100) ? a[Math.floor(n / 100)] + ' hundred' + (n % 100 ? ' ' : '') : '') + (n % 100 ? two(n % 100) : ''); }
    const rupees = Math.floor(num);
    const paise = Math.round((num - rupees) * 100);
    if (rupees === 0 && paise === 0) return 'Zero rupees only';
    let n = rupees, out = '';
    const crore = Math.floor(n / 10000000); n %= 10000000;
    const lakh = Math.floor(n / 100000); n %= 100000;
    const thou = Math.floor(n / 1000); n %= 1000;
    if (crore) out += three(crore) + ' crore ';
    if (lakh) out += three(lakh) + ' lakh ';
    if (thou) out += three(thou) + ' thousand ';
    if (n) out += three(n);
    out = out.trim();
    let res = 'Rupees ' + (out ? out.charAt(0).toUpperCase() + out.slice(1) : 'Zero');
    if (paise) res += ' and ' + two(paise) + ' paise';
    return res + ' only';
  }

  window.Calc = { r2, inr, scenario, components, computeDocument, inWords };
})();
