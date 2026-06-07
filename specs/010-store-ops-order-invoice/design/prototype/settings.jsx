/* SettingsScreen + CustomerPicker → window */

function seriesPreview(s, seq) {
  return `${s.invoiceSeriesPrefix}/${s.financialYear}/${String(seq != null ? seq : s.nextSequence).padStart(4, '0')}`;
}

function SettingsScreen({ settings, setSettings, onBack }) {
  const s = settings;
  const set = (patch) => setSettings({ ...s, ...patch });
  return (
    <div className="screen">
      <div className="appbar">
        <button className="iconbtn" onClick={onBack}><Icon n="arrow_back" /></button>
        <div className="title">Business settings <small>Order &amp; Invoice defaults</small></div>
      </div>
      <div className="settings">
        <div className="wrap">
          <div className="hint"><Icon n="info" s={18} />These seed every new document and the numbering series. They live in the business module and sync to each device (FR-B10).</div>

          <div className="card">
            <div className="sectionhdr">Document defaults</div>
            <div className="setrow">
              <div className="t"><b>Default price mode</b><small>Tax-exclusive adds GST on top; inclusive extracts it from the price.</small></div>
              <Segmented value={s.defaultPriceMode} onChange={(v) => set({ defaultPriceMode: v })}
                options={[{ value: 'TAX_EXCLUSIVE', label: 'Exclusive' }, { value: 'TAX_INCLUSIVE', label: 'Inclusive' }]} />
            </div>
            <div className="setrow">
              <div className="t"><b>Overall-discount mode</b>
                <small>{s.defaultOverallDiscountMode === 'PRE_TAX_APPORTIONED'
                  ? 'Apportioned across lines before tax — GST-compliant, per-line CGST/SGST/IGST stays correct.'
                  : 'Subtracted from the grand total after GST — per-HSN tax unaffected.'}</small></div>
              <Segmented value={s.defaultOverallDiscountMode} onChange={(v) => set({ defaultOverallDiscountMode: v })}
                options={[{ value: 'PRE_TAX_APPORTIONED', label: 'Pre-tax' }, { value: 'POST_TAX_REDUCTION', label: 'Post-tax' }]} />
            </div>
          </div>

          <div className="card">
            <div className="sectionhdr">Invoice numbering · this device</div>
            <div className="setrow">
              <div className="t"><b>Counter / series</b><small>Each device owns its own series prefix so two offline devices never collide (C5).</small></div>
              <span className="chip"><Icon n="point_of_sale" s={15} /> {s.counterName}</span>
            </div>
            <div className="setrow">
              <div className="t"><b>Series prefix</b><small>Prefix + financial year + sequence.</small></div>
              <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                <div className="field" style={{ width: 110 }}><label>Prefix</label>
                  <input value={s.invoiceSeriesPrefix} onChange={(e) => set({ invoiceSeriesPrefix: e.target.value.toUpperCase() })} style={{ fontFamily: 'var(--font-mono)' }} /></div>
                <div className="field" style={{ width: 100 }}><label>FY</label>
                  <input value={s.financialYear} onChange={(e) => set({ financialYear: e.target.value })} style={{ fontFamily: 'var(--font-mono)' }} /></div>
              </div>
            </div>
            <div className="setrow">
              <div className="t"><b>Next invoice number</b><small>Assigned on save, offline-capable, strictly sequential within the series.</small></div>
              <span className="seriesprev">{seriesPreview(s)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function CustomerPicker({ onPick, onClose }) {
  const [walk, setWalk] = useState(null); // null | {name,phone,gstin}
  if (walk) {
    const valid = walk.name.trim();
    return (
      <Sheet title="Walk-in customer" onClose={onClose}
        foot={<div style={{ display: 'flex', gap: 10, padding: '4px 16px 18px' }}>
          <button className="btn text" onClick={() => setWalk(null)}>Back</button><span className="spacer1" />
          <button className="btn" disabled={!valid} onClick={() => onPick({
            id: 'WALKIN', name: walk.name.trim() || 'Walk-in customer', phone: walk.phone, gstin: walk.gstin,
            stateCode: walk.gstin ? walk.gstin.slice(0, 2) : AppData.seller.stateCode,
            state: walk.gstin ? 'GSTIN state' : AppData.seller.state, initials: 'WI', walkIn: true,
          })}>Use customer</button>
        </div>}>
        <div className="field"><label>Name</label><input autoFocus value={walk.name} onChange={(e) => setWalk({ ...walk, name: e.target.value })} /></div>
        <div className="field"><label>Phone</label><input value={walk.phone} onChange={(e) => setWalk({ ...walk, phone: e.target.value })} placeholder="+91 " /></div>
        <div className="field"><label>GSTIN / state (optional)</label><input value={walk.gstin} onChange={(e) => setWalk({ ...walk, gstin: e.target.value.toUpperCase() })} style={{ fontFamily: 'var(--font-mono)' }} /></div>
        <div className="hint"><Icon n="info" s={18} />No GSTIN → place-of-supply defaults to the seller state ({AppData.seller.state}); GST is intra-state.</div>
      </Sheet>
    );
  }
  return (
    <Sheet title="Select customer" onClose={onClose}>
      {AppData.customers.map((c) => (
        <div className="listitem" key={c.id} style={{ background: 'var(--surface-container)' }} onClick={() => onPick({ ...c })}>
          <span className="lead avatar primary">{c.initials}</span>
          <div className="body"><div className="h">{c.name}</div>
            <div className="s">{c.phone} · {c.state} ({c.stateCode}){c.gstin ? '' : ' · unregistered'}</div></div>
          <span className="chip" style={{ fontSize: 11 }}>{c.stateCode === AppData.seller.stateCode ? 'CGST+SGST' : 'IGST'}</span>
        </div>
      ))}
      <button className="btn tonal" onClick={() => setWalk({ name: '', phone: '+91 ', gstin: '' })}><Icon n="person_add" s={18} /> New walk-in customer</button>
    </Sheet>
  );
}

Object.assign(window, { SettingsScreen, CustomerPicker, seriesPreview });
