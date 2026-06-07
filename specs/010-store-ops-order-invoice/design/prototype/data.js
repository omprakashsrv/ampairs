/* Sample data — realistic Indian retail. window.AppData */
(function () {
  const seller = {
    name: 'Shree Balaji Traders',
    gstin: '27ABCDE1234F1Z5',
    stateCode: '27',
    state: 'Maharashtra',
    address: 'Shop 14, APMC Market, Vashi, Navi Mumbai 400703',
  };

  // gstRate as a fraction. units: first is base (multiplier 1).
  const catalog = [
    {
      id: 'PRD-CEM', name: 'UltraTech PPC Cement 50kg', hsn: '2523', gstRate: 0.28,
      price: 385, units: [
        { id: 'BAG', name: 'BAG', multiplier: 1, decimals: 0 },
        { id: 'PALLET', name: 'PALLET', multiplier: 30, decimals: 0 },
      ], variants: null,
    },
    {
      id: 'PRD-OIL', name: 'Fortune Sunflower Oil', hsn: '1512', gstRate: 0.05,
      price: 145, units: [
        { id: 'PCS', name: 'PCS', multiplier: 1, decimals: 0 },
        { id: 'CARTON', name: 'CARTON', multiplier: 12, decimals: 0 },
      ],
      variants: [
        { sku: 'OIL-1L', label: '1 L pouch', price: 145 },
        { sku: 'OIL-5L', label: '5 L jar', price: 690 },
      ],
    },
    {
      id: 'PRD-TMT', name: 'TMT Steel Bar 12mm', hsn: '7214', gstRate: 0.18,
      price: 62.5, units: [
        { id: 'KG', name: 'KG', multiplier: 1, decimals: 3 },
        { id: 'BUNDLE', name: 'BUNDLE', multiplier: 60, decimals: 0 },
      ], variants: null,
    },
    {
      id: 'PRD-BIS', name: 'Parle-G Biscuits 800g', hsn: '1905', gstRate: 0.18,
      price: 90, units: [
        { id: 'PCS', name: 'PCS', multiplier: 1, decimals: 0 },
        { id: 'BOX', name: 'BOX', multiplier: 24, decimals: 0 },
      ], variants: null,
    },
    {
      id: 'PRD-COL', name: 'Colgate MaxFresh', hsn: '3306', gstRate: 0.18,
      price: 98, units: [
        { id: 'PCS', name: 'PCS', multiplier: 1, decimals: 0 },
        { id: 'BOX', name: 'BOX', multiplier: 36, decimals: 0 },
      ],
      variants: [
        { sku: 'COL-75', label: '75 g', price: 54 },
        { sku: 'COL-150', label: '150 g', price: 98 },
      ],
    },
    {
      id: 'PRD-NOTE', name: 'Classmate Notebook 200pg', hsn: '4820', gstRate: 0.12,
      price: 55, units: [
        { id: 'PCS', name: 'PCS', multiplier: 1, decimals: 0 },
        { id: 'PACK', name: 'PACK', multiplier: 6, decimals: 0 },
      ], variants: null,
    },
  ];

  const customers = [
    {
      id: 'CUS-1', name: 'Rajesh Kumar Hardware', phone: '+91 98200 11223',
      gstin: '27PQRST5678U1Z3', stateCode: '27', state: 'Maharashtra', initials: 'RK',
    },
    {
      id: 'CUS-2', name: 'Karnataka Build Mart', phone: '+91 99000 44556',
      gstin: '29WXYZ9012A1Z7', stateCode: '29', state: 'Karnataka', initials: 'KB',
    },
    {
      id: 'CUS-3', name: 'Deepa Stores', phone: '+91 91234 77889',
      gstin: '', stateCode: '27', state: 'Maharashtra', initials: 'DS',
    },
  ];

  const settings = {
    defaultPriceMode: 'TAX_EXCLUSIVE',
    defaultOverallDiscountMode: 'POST_TAX_REDUCTION',
    invoiceSeriesPrefix: 'MH27',
    financialYear: '26-27',
    nextSequence: 43,
    counterName: 'Counter 1 · Vashi',
  };

  // helpers
  function unitPriceFor(product, unit, variant) {
    const base = variant ? variant.price : product.price;
    return Calc.r2(base * unit.multiplier);
  }
  function newLine(product, opts) {
    opts = opts || {};
    const variant = opts.variant || (product.variants ? product.variants[product.variants.length - 1] : null);
    const unit = opts.unit || product.units[0];
    const qty = opts.quantity != null ? opts.quantity : 1;
    return {
      id: 'L' + Math.random().toString(36).slice(2, 8),
      productId: product.id,
      name: product.name,
      hsn: product.hsn,
      gstRate: product.gstRate,
      product: product,
      variant: variant,
      unitId: unit.id,
      unitName: unit.name,
      multiplier: unit.multiplier,
      decimals: unit.decimals,
      quantity: qty,
      unitPrice: unitPriceFor(product, unit, variant),
      priceOverridden: false,
      lineDiscount: opts.lineDiscount || { kind: 'PERCENT', amount: 0 },
    };
  }

  // opening invoice that reconciles to ₹9,207.10 (matches the wireframe)
  function seedLines() {
    const cement = catalog.find((p) => p.id === 'PRD-CEM');
    const oil = catalog.find((p) => p.id === 'PRD-OIL');
    return [
      newLine(cement, { unit: cement.units[0], quantity: 2 }),
      newLine(oil, { unit: oil.units[1], quantity: 5, variant: oil.variants[0], lineDiscount: { kind: 'PERCENT', amount: 10 } }),
    ];
  }

  window.AppData = { seller, catalog, customers, settings, unitPriceFor, newLine, seedLines };
})();
