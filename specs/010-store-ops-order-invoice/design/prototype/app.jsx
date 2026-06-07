/* Main orchestrator. Mounts everything; owns document + customer + settings state. */
const { useState: uS, useEffect: uE, useMemo: uM, useRef: uR } = React;

function Picker({ onPick, onCreate, autoFocus }) {
  const [q, setQ] = uS('');
  return <ProductResults query={q} setQuery={setQ} onPick={onPick} onCreate={onCreate} autoFocus={autoFocus} />;
}

function freshDoc(type, settings) {
  return {
    type,
    priceMode: settings.defaultPriceMode,
    overallDiscountMode: settings.defaultOverallDiscountMode,
    overallDiscount: { kind: 'FLAT', amount: 0 },
    scenario: 'INTRA',
    lines: AppData.seedLines(),
  };
}

function App() {
  const [mode, setMode] = uS('desktop');
  const [dark, setDark] = uS(false);
  const [settings, setSettings] = uS(AppData.settings);
  const [customer, setCustomer] = uS({ ...AppData.customers[0] });
  const [doc, setDoc] = uS(() => freshDoc('invoice', AppData.settings));
  const [screen, setScreen] = uS('editor');     // editor | doc | settings
  const [sync, setSync] = uS('offline');
  const [savedNo, setSavedNo] = uS(null);
  const [seqUsed, setSeqUsed] = uS(null);
  const [phoneTotals, setPhoneTotals] = uS(false);
  const [overlay, setOverlay] = uS(null);        // {kind, lineId, anchor, createName}
  // mode/dark are plain React state — changing them must NOT post a tweak-persist
  // message (that round-trips through the host and reloads the whole prototype).

  const scenario = Calc.scenario(customer.stateCode, AppData.seller.stateCode);
  const docFull = { ...doc, scenario };
  const result = uM(() => Calc.computeDocument(docFull), [doc, scenario]);
  const [calcKey, setCalcKey] = uS(0);
  const sig = result.grandTotal + '|' + result.totalTax + '|' + result.basePrice;
  uE(() => { setCalcKey((k) => k + 1); }, [sig]);

  const previewNo = (doc.type === 'invoice')
    ? seriesPreview(settings)
    : 'ORD-' + String(118 + 1).padStart(4, '0');

  // ── line mutators ──
  const patchLine = (id, patch) => setDoc((d) => ({ ...d, lines: d.lines.map((l) => l.id === id ? { ...l, ...patch } : l) }));
  const removeLine = (id) => setDoc((d) => ({ ...d, lines: d.lines.filter((l) => l.id !== id) }));
  const addLineFromProduct = (p) => {
    const ln = AppData.newLine(p);
    setDoc((d) => ({ ...d, lines: [...d.lines, ln] }));
    if (sync === 'synced') setSync('offline');
    return ln;
  };
  const setUnit = (ln, u) => {
    const price = ln.priceOverridden ? ln.unitPrice : AppData.unitPriceFor(ln.product, u, ln.variant);
    patchLine(ln.id, { unitId: u.id, unitName: u.name, multiplier: u.multiplier, decimals: u.decimals, unitPrice: price });
  };
  const setVariant = (ln, v) => {
    const u = ln.product.units.find((x) => x.id === ln.unitId) || ln.product.units[0];
    const price = ln.priceOverridden ? ln.unitPrice : Calc.r2(v.price * u.multiplier);
    patchLine(ln.id, { variant: v, unitPrice: price });
  };
  const replaceProduct = (ln, p) => {
    const fresh = AppData.newLine(p);
    patchLine(ln.id, { ...fresh, id: ln.id });
  };

  // ── customer ──
  const pickCustomer = (c) => {
    setCustomer(c);
    setDoc((d) => ({ ...d, scenario: Calc.scenario(c.stateCode, AppData.seller.stateCode) }));
    setOverlay(null);
  };

  // ── save / convert ──
  const save = () => {
    setSync('syncing');
    const seq = settings.nextSequence;
    const no = doc.type === 'invoice' ? seriesPreview(settings, seq) : previewNo;
    setSavedNo(no); setSeqUsed(seq);
    if (doc.type === 'invoice') setSettings((s) => ({ ...s, nextSequence: s.nextSequence + 1 }));
    setTimeout(() => { setSync('synced'); setScreen('doc'); }, 1300);
  };
  const newDoc = (type) => {
    setDoc(freshDoc(type, settings)); setCustomer({ ...AppData.customers[0] });
    setSavedNo(null); setSync('offline'); setScreen('editor');
    setDoc((d) => ({ ...freshDoc(type, settings), scenario: Calc.scenario(AppData.customers[0].stateCode, AppData.seller.stateCode) }));
  };

  // ── overlays render ──
  const editingLine = overlay && overlay.lineId ? doc.lines.find((l) => l.id === overlay.lineId) : null;
  const editingCalc = editingLine ? result.lines.find((l) => l.id === editingLine.id) || {} : {};

  function openPicker(target, anchor) { setOverlay({ kind: 'picker', target, anchor }); }
  function onPickProduct(p) {
    if (overlay.target === 'add') addLineFromProduct(p);
    else if (overlay.target && overlay.target.replace) replaceProduct(overlay.target.replace, p);
    setOverlay(null);
  }
  function onCreateProduct(name) { setOverlay({ kind: 'create', createName: name, target: overlay.target }); }

  // ── pieces ──
  const totalsPanel = <TotalsPanel result={result} doc={doc} setDoc={setDoc} calcKey={calcKey} />;
  const saveRow = (
    <div style={{ marginTop: 'auto', padding: 16, borderTop: '1px solid var(--outline-variant)', display: 'flex', gap: 10, alignItems: 'center' }}>
      <SyncChip state={sync} />
      <span className="spacer1" />
      <button className="btn outlined sm" onClick={() => setOverlay({ kind: 'settings' })}><Icon n="tune" s={18} /></button>
      <button className="btn" onClick={save}><Icon n="save" s={18} /> Save</button>
    </div>
  );

  const isExpanded = mode !== 'phone';

  function EditorScreen() {
    return (
      <div className="screen">
        <div className="appbar">
          <button className="iconbtn"><Icon n="arrow_back" /></button>
          <div className="title"><span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>New {doc.type === 'invoice' ? 'Invoice' : 'Order'}</span> <small>draft · {customer.name}</small></div>
          {isExpanded && <SyncChip state={sync} />}
          {isExpanded && <button className="iconbtn" onClick={() => setScreen('settings')}><Icon n="settings" /></button>}
          {isExpanded && <button className="btn sm" onClick={save}><Icon n="save" s={18} /> Save <span style={{ opacity: .7, font: 'var(--label-small)', marginLeft: 2 }}>⌘S</span></button>}
        </div>
        <Header doc={doc} setDoc={setDoc} customer={customer} compact={!isExpanded}
          seriesPreview={previewNo} onCustomer={() => setOverlay({ kind: 'customer' })} />
        {isExpanded ? (
          <div className="editor">
            <div className="main">
              <LineGrid lines={doc.lines} calcLines={result.lines}
                onEdit={(ln, a) => openPicker({ replace: ln }, a)}
                onUnit={(ln, a) => setOverlay({ kind: 'unit', lineId: ln.id, anchor: a })}
                onVariant={(ln, a) => setOverlay({ kind: 'variant', lineId: ln.id, anchor: a })}
                onPatch={patchLine} onRemove={removeLine}
                onAdd={(a) => openPicker('add', a)} priceMode={doc.priceMode} />
            </div>
            <div className="rail">
              <div className="scrollY" style={{ flex: 1 }}>{totalsPanel}</div>
              {saveRow}
            </div>
          </div>
        ) : (
          <React.Fragment>
            <LineCards lines={doc.lines} calcLines={result.lines}
              onEdit={(ln) => setOverlay({ kind: 'lineedit', lineId: ln.id })}
              onAdd={() => openPicker('add', null)} />
            <div className="totbar">
              <div className="head" onClick={() => setPhoneTotals(true)}>
                <div className="g" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Icon n="expand_less" /> Grand total
                </div>
                <div className="g"><b className="mono">{inr(result.grandTotal)}</b></div>
              </div>
              <div className="savebar">
                <SyncChip state={sync} />
                <span className="spacer1" />
                <button className="btn" style={{ flex: 1 }} onClick={save}><Icon n="save" s={18} /> Save {doc.type === 'invoice' ? 'invoice' : 'order'}</button>
              </div>
            </div>
          </React.Fragment>
        )}
      </div>
    );
  }

  return (
    <div className={'oi' + (dark ? ' dark' : '')}>
      <div className="oi-stage">
        <div className="oi-stagebar">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginRight: 'auto' }}>
            <img src="ampairs-mark.png" style={{ width: 26, height: 26 }} alt="" />
            <b style={{ font: 'var(--title-medium)', color: 'var(--on-surface)' }}>Order &amp; Invoice</b>
            <span style={{ font: 'var(--label-medium)' }}>hi-fi M3 prototype</span>
          </div>
          <div className="segmented sm">
            <button className={doc.type === 'order' ? 'on' : ''} onClick={() => newDoc('order')}>Order</button>
            <button className={doc.type === 'invoice' ? 'on' : ''} onClick={() => newDoc('invoice')}>Invoice</button>
          </div>
          <div className="seg-mode">
            {['phone', 'tablet', 'desktop'].map((m) => (
              <button key={m} className={mode === m ? 'on' : ''} onClick={() => setMode(m)}>
                <Icon n={m === 'phone' ? 'smartphone' : m === 'tablet' ? 'tablet' : 'desktop_windows'} s={16} />{m[0].toUpperCase() + m.slice(1)}
              </button>
            ))}
          </div>
          <button className={'gbtn' + (dark ? ' active' : '')} onClick={() => setDark(!dark)}>
            <Icon n={dark ? 'dark_mode' : 'light_mode'} s={16} />{dark ? 'Dark' : 'Light'}
          </button>
        </div>

        <div className={'device ' + mode}>
          {screen === 'editor' && <EditorScreen />}
          {screen === 'doc' && (
            <InvoiceView doc={doc} result={result} customer={customer} invoiceNo={savedNo || previewNo} sync={sync}
              onBack={() => setScreen('editor')} onPrint={() => window.print()} />
          )}
          {screen === 'settings' && (
            <SettingsScreen settings={settings} setSettings={setSettings} onBack={() => setScreen('editor')} />
          )}

          {/* order detail → convert action */}
          {screen === 'doc' && doc.type === 'order' && (
            <button className="fab" onClick={() => setOverlay({ kind: 'convert' })}><Icon n="receipt_long" s={22} /> Create invoice</button>
          )}

          {/* ── OVERLAYS ── */}
          {overlay && overlay.kind === 'picker' && (
            isExpanded && overlay.anchor
              ? <AnchoredOverlay anchor={overlay.anchor} onClose={() => setOverlay(null)} align={overlay.target === 'add' ? 'left' : 'left'}>
                  <div className="popover" style={{ width: 380, height: 360, padding: 12 }}>
                    <Picker autoFocus onPick={onPickProduct} onCreate={onCreateProduct} />
                  </div>
                </AnchoredOverlay>
              : <Sheet title={overlay.target === 'add' ? 'Add product' : 'Change product'} onClose={() => setOverlay(null)}>
                  <div style={{ display: 'flex', flexDirection: 'column', minHeight: 380 }}>
                    <Picker autoFocus onPick={onPickProduct} onCreate={onCreateProduct} />
                  </div>
                </Sheet>
          )}

          {overlay && overlay.kind === 'unit' && editingLine && (
            <AnchoredOverlay anchor={overlay.anchor} onClose={() => setOverlay(null)}>
              <UnitMenu product={editingLine.product} value={editingLine.unitId}
                basePrice={editingLine.variant ? editingLine.variant.price : editingLine.product.price}
                onPick={(u) => { setUnit(editingLine, u); setOverlay(null); }} />
            </AnchoredOverlay>
          )}
          {overlay && overlay.kind === 'variant' && editingLine && (
            <AnchoredOverlay anchor={overlay.anchor} onClose={() => setOverlay(null)}>
              <VariantMenu product={editingLine.product} value={editingLine.variant}
                onPick={(v) => { setVariant(editingLine, v); setOverlay(null); }} />
            </AnchoredOverlay>
          )}

          {overlay && overlay.kind === 'lineedit' && editingLine && (
            <LineEditorSheet ln={editingLine} basePrice={editingLine.variant ? editingLine.variant.price : editingLine.product.price}
              calc={editingCalc} onPatch={patchLine}
              onUnit={() => setOverlay({ kind: 'unit', lineId: editingLine.id, anchor: document.querySelector('.sheet .fakeinput') })}
              onVariant={() => setOverlay({ kind: 'variant', lineId: editingLine.id, anchor: document.querySelector('.sheet .fakeinput') })}
              onChangeProduct={() => openPicker({ replace: editingLine }, null)}
              onDelete={() => { removeLine(editingLine.id); setOverlay(null); }}
              onClose={() => setOverlay(null)} />
          )}

          {overlay && overlay.kind === 'customer' && (
            <CustomerPicker onPick={pickCustomer} onClose={() => setOverlay(null)} />
          )}

          {overlay && overlay.kind === 'create' && (
            <CreateProductOverlay name={overlay.createName} expanded={isExpanded}
              onCancel={() => setOverlay(null)}
              onCreate={(p) => {
                if (overlay.target === 'add') addLineFromProduct(p);
                else if (overlay.target && overlay.target.replace) replaceProduct(overlay.target.replace, p);
                setOverlay(null);
              }} />
          )}

          {overlay && overlay.kind === 'convert' && (
            <ConfirmConvert orderNo={savedNo || previewNo}
              onCancel={() => setOverlay(null)}
              onConfirm={() => { setDoc((d) => ({ ...d, type: 'invoice' })); setSavedNo(seriesPreview(settings)); setSettings((s) => ({ ...s, nextSequence: s.nextSequence + 1 })); setOverlay(null); }} />
          )}

          {overlay && overlay.kind === 'settings' && (
            <div className="scrim center" onMouseDown={() => setOverlay(null)}>
              <div className="dialog" style={{ width: 560, maxHeight: '86%', overflow: 'auto', padding: 0 }} onMouseDown={(e) => e.stopPropagation()}>
                <SettingsScreen settings={settings} setSettings={setSettings} onBack={() => setOverlay(null)} />
              </div>
            </div>
          )}
        </div>

        {/* phone totals expanded sheet */}
        {phoneTotals && mode === 'phone' && screen === 'editor' && (
          <PhoneTotalsSheet result={result} doc={doc} setDoc={setDoc} calcKey={calcKey} onClose={() => setPhoneTotals(false)} />
        )}
      </div>

      <TweaksPanel>
        <TweakSection label="Presentation" />
        <TweakRadio label="Layout" value={mode} options={['phone', 'tablet', 'desktop']} onChange={(v) => setMode(v)} />
        <TweakToggle label="Dark mode" value={dark} onChange={(v) => setDark(v)} />
        <TweakSection label="Document" />
        <TweakRadio label="Type" value={doc.type} options={['order', 'invoice']} onChange={(v) => newDoc(v)} />
        <TweakRadio label="Customer" value={customer.id === 'CUS-2' ? 'Inter-state' : customer.walkIn ? 'Walk-in' : 'Intra-state'}
          options={['Intra-state', 'Inter-state', 'Walk-in']}
          onChange={(v) => {
            if (v === 'Intra-state') pickCustomer({ ...AppData.customers[0] });
            else if (v === 'Inter-state') pickCustomer({ ...AppData.customers[1] });
            else pickCustomer({ id: 'WALKIN', name: 'Walk-in customer', phone: '', gstin: '', stateCode: AppData.seller.stateCode, state: AppData.seller.state, initials: 'WI', walkIn: true });
          }} />
        <TweakSection label="Tax & discount" />
        <TweakRadio label="Price mode" value={doc.priceMode === 'TAX_INCLUSIVE' ? 'Inclusive' : 'Exclusive'}
          options={['Exclusive', 'Inclusive']} onChange={(v) => setDoc((d) => ({ ...d, priceMode: v === 'Inclusive' ? 'TAX_INCLUSIVE' : 'TAX_EXCLUSIVE' }))} />
        <TweakRadio label="Overall discount" value={doc.overallDiscountMode === 'PRE_TAX_APPORTIONED' ? 'Pre-tax' : 'Post-tax'}
          options={['Pre-tax', 'Post-tax']} onChange={(v) => setDoc((d) => ({ ...d, overallDiscountMode: v === 'Pre-tax' ? 'PRE_TAX_APPORTIONED' : 'POST_TAX_REDUCTION' }))} />
        <TweakSection label="Jump to" />
        <TweakButton label="Open saved document" onClick={() => setScreen('doc')} />
        <TweakButton label="Business settings" onClick={() => setScreen('settings')} />
        <TweakButton label="Back to editor" onClick={() => setScreen('editor')} />
      </TweaksPanel>
    </div>
  );
}

function PhoneTotalsSheet({ result, doc, setDoc, calcKey, onClose }) {
  return (
    <div className="scrim bottom" onMouseDown={onClose} style={{ zIndex: 25 }}>
      <div className="sheet" onMouseDown={(e) => e.stopPropagation()}>
        <div className="grab" />
        <div className="shead"><span className="ttl">Totals & GST breakdown</span><button className="iconbtn" onClick={onClose}><Icon n="close" /></button></div>
        <div className="scont"><TotalsPanel result={result} doc={doc} setDoc={setDoc} calcKey={calcKey} /></div>
      </div>
    </div>
  );
}

function CreateProductOverlay({ name, expanded, onCreate, onCancel }) {
  const form = CreateProductForm({ initialName: name, onCreate, onCancel });
  if (expanded) {
    return (
      <Dialog onClose={onCancel}>
        <h3>New product</h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>{form.fields}</div>
        <div className="actions">
          <button className="btn text" onClick={onCancel}>Cancel</button>
          <button className="btn" disabled={!form.valid} onClick={form.submit}>Create &amp; add</button>
        </div>
      </Dialog>
    );
  }
  return (
    <Sheet title="New product" onClose={onCancel}
      foot={<div style={{ display: 'flex', gap: 10, padding: '4px 16px 18px' }}>
        <button className="btn text" onClick={onCancel}>Cancel</button><span className="spacer1" />
        <button className="btn" disabled={!form.valid} onClick={form.submit}>Create &amp; add</button></div>}>
      {form.fields}
    </Sheet>
  );
}

function mount() { ReactDOM.createRoot(document.getElementById('root')).render(<App />); }
// Force the icon + text fonts to download BEFORE first paint, so Material
// Symbols ligatures shape correctly on the first render (already-painted text
// nodes don't re-shape after a late font swap).
(function () {
  let done = false;
  const go = () => { if (!done) { done = true; mount(); } };
  if (document.fonts && document.fonts.load) {
    Promise.all([
      document.fonts.load('24px "Material Symbols Outlined"'),
      document.fonts.load('400 16px "Roboto"'),
      document.fonts.load('500 16px "Roboto"'),
      document.fonts.load('400 14px "Roboto Mono"'),
    ]).then(go).catch(go);
    setTimeout(go, 2500);
  } else {
    go();
  }
})();
