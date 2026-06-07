/* Pickers: ProductPicker, UnitMenu, VariantMenu, CreateProductForm → window */

function ProductResults({ query, setQuery, onPick, onCreate, autoFocus }) {
  const list = AppData.catalog.filter((p) => {
    const q = query.trim().toLowerCase();
    if (!q) return true;
    return p.name.toLowerCase().includes(q) || p.hsn.includes(q);
  });
  const gstChip = (r) => Math.round(r * 100) + '%';
  return (
    <React.Fragment>
      <div className="field tint" style={{ margin: '2px 0 4px' }}>
        <div className="fakeinput" style={{ padding: 0, border: 'none', background: 'transparent', height: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, width: '100%', height: 48, padding: '0 14px', background: 'var(--surface-container-high)', borderRadius: 'var(--corner-sm)' }}>
            <Icon n="search" c="var(--on-surface-variant)" />
            <input autoFocus={autoFocus} value={query} placeholder="Search product or HSN…"
              onChange={(e) => setQuery(e.target.value)}
              style={{ border: 'none', background: 'transparent', outline: 'none', flex: 1, font: 'var(--body-large)', color: 'var(--on-surface)', height: 44, padding: 0 }} />
          </div>
        </div>
      </div>
      <div style={{ overflowY: 'auto', flex: 1, minHeight: 0 }}>
        {list.map((p) => (
          <div className="result" key={p.id} onClick={() => onPick(p)}>
            <span className="listitem" style={{ padding: 0 }}><span className="lead tertiary"><Icon n="inventory_2" s={20} /></span></span>
            <div className="body">
              <div className="nm">{p.name}</div>
              <div className="meta">
                <span className="chip" style={{ fontSize: 11, padding: '2px 7px' }}>HSN {p.hsn}</span>
                <span className="chip" style={{ fontSize: 11, padding: '2px 7px' }}>GST {gstChip(p.gstRate)}</span>
                {p.variants && <span className="chip tertiary" style={{ fontSize: 11, padding: '2px 7px' }}>{p.variants.length} variants</span>}
              </div>
            </div>
            <span className="px">{inr(p.price)}<small style={{ color: 'var(--on-surface-variant)', fontFamily: 'var(--font-sans)' }}>/{p.units[0].name}</small></span>
          </div>
        ))}
        {query.trim() && (
          <div className="result create" onClick={() => onCreate(query.trim())}>
            <Icon n="add_circle" />
            <div className="body"><div className="nm" style={{ color: 'var(--primary)' }}>Create “{query.trim()}”</div>
              <div className="meta"><small style={{ color: 'var(--on-surface-variant)' }}>Add a new product to the catalog</small></div></div>
          </div>
        )}
      </div>
    </React.Fragment>
  );
}

// conversion hint string
function convHint(product, unit, price) {
  const base = product.units[0];
  if (unit.multiplier === 1) return `Base unit · ${unit.decimals} decimals allowed`;
  return `1 ${unit.name} = ${unit.multiplier} ${base.name} · ${inr(price)}/${unit.name} · ${unit.decimals} decimals`;
}

function UnitMenu({ product, value, basePrice, onPick }) {
  return (
    <div className="menu" style={{ minWidth: 240 }}>
      {product.units.map((u) => {
        const price = Calc.r2(basePrice * u.multiplier);
        return (
          <div key={u.id} className={'mi' + (u.id === value ? ' on' : '')} onClick={() => onPick(u)}>
            <span>{u.name}{u.multiplier === 1 ? '  (base)' : ''}</span>
            <span className="mul">×{u.multiplier} · {inr(price)}</span>
          </div>
        );
      })}
    </div>
  );
}

function VariantMenu({ product, value, onPick }) {
  return (
    <div className="menu" style={{ minWidth: 200 }}>
      {product.variants.map((v) => (
        <div key={v.sku} className={'mi' + (value && v.sku === value.sku ? ' on' : '')} onClick={() => onPick(v)}>
          <span>{v.label}</span><span className="mul">{inr(v.price)}</span>
        </div>
      ))}
    </div>
  );
}

function CreateProductForm({ initialName, onCreate, onCancel }) {
  const [name, setName] = useState(initialName || '');
  const [price, setPrice] = useState('');
  const [hsn, setHsn] = useState('');
  const [unit, setUnit] = useState('PCS');
  const HSN_GST = { '2523': 0.28, '1512': 0.05, '7214': 0.18, '1905': 0.18, '3306': 0.18, '4820': 0.12, '': 0 };
  const rate = HSN_GST[hsn] !== undefined ? HSN_GST[hsn] : 0.18;
  const valid = name.trim() && parseFloat(price) > 0;
  const fields = (
    <React.Fragment>
      <div className="field"><label>Product name</label>
        <input value={name} onChange={(e) => setName(e.target.value)} autoFocus /></div>
      <div style={{ display: 'flex', gap: 12 }}>
        <div className="field" style={{ flex: 1 }}><label>Selling price ₹ / {unit}</label>
          <input type="number" value={price} onChange={(e) => setPrice(e.target.value)} placeholder="0.00" /></div>
        <div className="field" style={{ flex: 1 }}><label>Base unit</label>
          <div className="fakeinput"><span>{unit}</span></div></div>
      </div>
      <div className="field"><label>HSN / tax code</label>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <input value={hsn} onChange={(e) => setHsn(e.target.value.replace(/\D/g, '').slice(0, 4))} placeholder="e.g. 3306" style={{ flex: 1, height: 48, border: '1px solid var(--outline)', borderRadius: 'var(--corner-xs)', background: 'transparent', padding: '0 14px', font: 'var(--body-large)', color: 'var(--on-surface)', outline: 'none', fontFamily: 'var(--font-mono)' }} />
          <span className="chip primary">→ GST {Math.round(rate * 100)}%</span>
        </div>
      </div>
      {!hsn && <div className="hint amber"><Icon n="info" s={18} />Blank tax code is allowed — the line will be flagged 0% / exempt, never silently untaxed.</div>}
      <div className="hint"><Icon n="cloud_off" s={18} />Saves to the catalog offline (PENDING_PUSH), then drops in as a line.</div>
    </React.Fragment>
  );
  function submit() {
    onCreate({
      id: 'PRD-' + Math.random().toString(36).slice(2, 6).toUpperCase(),
      name: name.trim(), hsn, gstRate: rate, price: parseFloat(price),
      units: [{ id: unit, name: unit, multiplier: 1, decimals: 0 }], variants: null, isNew: true,
    });
  }
  return { fields, valid, submit };
}

Object.assign(window, { ProductResults, UnitMenu, VariantMenu, CreateProductForm, convHint });
