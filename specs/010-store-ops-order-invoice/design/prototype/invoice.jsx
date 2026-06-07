/* InvoiceView (printable tax invoice) + ConfirmConvert → window */

function InvoiceView({ doc, result, customer, invoiceNo, sync, onBack, onPrint }) {
  const intra = result.scenario === 'INTRA';
  const seller = AppData.seller;
  // group lines by rate for the summary
  const byRate = {};
  result.lines.forEach((l) => {
    if (l.exempt) return;
    const k = (l.gstRate * 100).toFixed(0);
    if (!byRate[k]) byRate[k] = { rate: l.gstRate, taxable: 0, tax: 0 };
    byRate[k].taxable = Calc.r2(byRate[k].taxable + l.taxable);
    byRate[k].tax = Calc.r2(byRate[k].tax + l.totalTax);
  });
  return (
    <div className="screen">
      <div className="appbar tint">
        <button className="iconbtn" onClick={onBack}><Icon n="arrow_back" /></button>
        <div className="title">{doc.type === 'invoice' ? 'Tax Invoice' : 'Order'} <small className="mono">{invoiceNo}</small></div>
        <SyncChip state={sync} />
        <button className="iconbtn"><Icon n="share" /></button>
        <button className="btn sm" onClick={onPrint}><Icon n="print" s={18} /> Print</button>
      </div>
      <div className="invoicewrap">
        <div className="paper">
          <div className="ihead">
            <div>
              <div className="tt">{doc.type === 'invoice' ? 'TAX INVOICE' : 'SALES ORDER'}</div>
              <div className="seller"><b>{seller.name}</b><br />{seller.address}<br />GSTIN {seller.gstin} · {seller.state}</div>
            </div>
            <img src="ampairs-mark.png" alt="" style={{ width: 44, height: 44 }} />
          </div>

          <div className="billrow">
            <div>
              <div className="lbl">Bill to</div>
              <div style={{ font: 'var(--title-small)' }}>{customer.name}</div>
              <div style={{ font: 'var(--body-small)', opacity: .8 }}>
                {customer.phone}<br />
                {customer.gstin ? 'GSTIN ' + customer.gstin : 'Unregistered'} · {customer.state} ({customer.stateCode})
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div className="lbl">{doc.type === 'invoice' ? 'Invoice no.' : 'Order no.'}</div>
              <div className="invno">{invoiceNo}</div>
              <div style={{ font: 'var(--body-small)', opacity: .7, marginTop: 4 }}>06 Jun 2026</div>
              <div style={{ marginTop: 4 }}><span className="chip" style={{ fontSize: 10 }}>{doc.priceMode === 'TAX_INCLUSIVE' ? 'Tax-inclusive' : 'Tax-exclusive'}</span></div>
            </div>
          </div>

          <table className="itable">
            <thead><tr>
              <th className="desc">Item / HSN</th><th className="num">Qty</th><th className="num">Rate</th><th className="num">Taxable</th>
              {intra ? <React.Fragment><th className="num">CGST</th><th className="num">SGST</th></React.Fragment> : <th className="num">IGST</th>}
              <th className="num">Amount</th>
            </tr></thead>
            <tbody>
              {result.lines.map((l) => {
                const half = Calc.r2(l.totalTax / 2);
                return (
                  <tr key={l.id}>
                    <td className="desc">{l.name}{l.variant ? ' · ' + l.variant.label : ''} <span style={{ opacity: .5 }}>{l.hsn}</span></td>
                    <td className="num">{l.quantity} {l.unitName}</td>
                    <td className="num">{inr(l.unitPrice, 2)}</td>
                    <td className="num">{inr(l.taxable)}</td>
                    {intra
                      ? <React.Fragment><td className="num">{inr(half)}</td><td className="num">{inr(Calc.r2(l.totalTax - half))}</td></React.Fragment>
                      : <td className="num">{inr(l.totalTax)}</td>}
                    <td className="num">{inr(l.lineTotal)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          <div className="isum">
            <div style={{ flex: 1 }}>
              <div className="lbl" style={{ marginTop: 4 }}>Tax summary (by rate)</div>
              <table className="itable" style={{ marginTop: 4 }}>
                <tbody>
                  {Object.entries(byRate).map(([k, g]) => (
                    <tr key={k}>
                      <td className="desc">{intra ? `CGST ${(g.rate * 50).toFixed(g.rate * 50 % 1 ? 1 : 0)}% + SGST ${(g.rate * 50).toFixed(g.rate * 50 % 1 ? 1 : 0)}%` : `IGST ${(g.rate * 100).toFixed(0)}%`} on {inr(g.taxable)}</td>
                      <td className="num">{inr(g.tax)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="words" style={{ marginTop: 12 }}>{Calc.inWords(result.grandTotal)}</div>
            </div>
            <div className="tot">
              <div className="r"><span>Taxable</span><span>{inr(result.basePrice)}</span></div>
              {result.overallDiscount > 0 && <div className="r"><span>Discount</span><span>− {inr(result.overallDiscount)}</span></div>}
              {result.postTaxDiscount > 0 && <div className="r"><span>Discount</span><span>− {inr(result.postTaxDiscount)}</span></div>}
              <div className="r"><span>Total GST</span><span>{inr(result.totalTax)}</span></div>
              <div className="r g"><span>Grand total</span><span>{inr(result.grandTotal)}</span></div>
            </div>
          </div>
          <div style={{ marginTop: 22, display: 'flex', justifyContent: 'space-between', font: 'var(--body-small)', opacity: .7 }}>
            <span>Computer-generated tax invoice.</span>
            <span>For {seller.name}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

function ConfirmConvert({ orderNo, onConfirm, onCancel }) {
  const items = [
    ['shopping_bag', 'Copies all lines · units · prices'],
    ['receipt_long', 'Copies tax breakdown & discounts'],
    ['link', 'Cross-links order ⇄ invoice'],
    ['tag', 'Assigns a GST invoice number on save'],
  ];
  return (
    <Dialog onClose={onCancel}>
      <h3>Create invoice from this order?</h3>
      <p style={{ color: 'var(--on-surface-variant)', marginBottom: 14 }}>{orderNo} → a new GST tax invoice.</p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {items.map(([ic, t]) => (
          <div key={t} style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <span className="listitem" style={{ padding: 0 }}><span className="lead primary" style={{ width: 34, height: 34 }}><Icon n={ic} s={18} /></span></span>
            <span style={{ font: 'var(--body-medium)' }}>{t}</span>
          </div>
        ))}
      </div>
      <div className="hint amber" style={{ marginTop: 16 }}><Icon n="info" s={18} />Re-converting opens the existing invoice — idempotent, no duplicate.</div>
      <div className="actions">
        <button className="btn text" onClick={onCancel}>Cancel</button>
        <button className="btn" onClick={onConfirm}>Create invoice</button>
      </div>
    </Dialog>
  );
}

Object.assign(window, { InvoiceView, ConfirmConvert });
