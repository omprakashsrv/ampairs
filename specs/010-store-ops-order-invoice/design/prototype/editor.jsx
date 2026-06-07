/* Editor pieces: AnchoredOverlay, TotalsPanel, Header, LineGrid, LineCards,
   LineEditorSheet → window */

// position an overlay relative to the enclosing .screen, anchored under a cell
function AnchoredOverlay({ anchor, onClose, children, align = 'left' }) {
  const ref = useRef(null);
  const [pos, setPos] = useState(null);
  useLayoutEffect(() => {
    if (!anchor) return;
    const screen = anchor.closest('.screen');
    const sr = screen.getBoundingClientRect();
    const ar = anchor.getBoundingClientRect();
    const w = ref.current ? ref.current.offsetWidth : 240;
    const h = ref.current ? ref.current.offsetHeight : 200;
    let left = align === 'right' ? ar.right - sr.left - w : ar.left - sr.left;
    let top = ar.bottom - sr.top + 4;
    left = Math.max(8, Math.min(left, sr.width - w - 8));
    if (top + h > sr.height - 8) top = Math.max(8, ar.top - sr.top - h - 4);
    setPos({ left, top });
  }, [anchor]);
  useClickOutside(ref, onClose, true);
  return (
    <div ref={ref} style={{ position: 'absolute', zIndex: 40, left: pos ? pos.left : -9999, top: pos ? pos.top : 0 }}>
      {children}
    </div>
  );
}

/* ─────────── GST breakdown / totals panel ─────────── */
function TotalsPanel({ result, doc, setDoc, calcKey, dense }) {
  const r = result;
  const intra = r.scenario === 'INTRA';
  const hasLineDisc = r.totalLineDiscount > 0;
  const preTax = doc.overallDiscountMode === 'PRE_TAX_APPORTIONED';
  const cls = 'totals' + (dense ? '' : '') ;
  return (
    <div className={cls} key={calcKey}>
      <div className="totrow"><span className="lab">Subtotal · {r.totalItems} items</span><span className="v">{inr(r.subtotalGross)}</span></div>
      {hasLineDisc && (
        <div className="totrow disc"><span className="lab">Line discounts</span><span className="v">− {inr(r.totalLineDiscount)}</span></div>
      )}

      {/* overall discount control */}
      <div style={{ paddingTop: 8 }}>
        <div className="totrow" style={{ padding: '4px 0' }}>
          <span className="lab" style={{ display: 'flex', flexDirection: 'column' }}>
            Overall discount
            <small style={{ color: 'var(--on-surface-variant)' }}>{preTax ? 'apportioned pre-tax' : 'post-tax reduction'}</small>
          </span>
          <DiscountInput value={doc.overallDiscount} compact
            onChange={(v) => setDoc({ ...doc, overallDiscount: v })} />
        </div>
      </div>
      {preTax && r.overallDiscount > 0 && (
        <div className="totrow disc"><span className="lab" style={{ paddingLeft: 0 }}>— apportioned across lines</span><span className="v">− {inr(r.overallDiscount)}</span></div>
      )}

      <div className="totdiv" />
      <div className="totrow tax"><span className="lab">Taxable value</span><span className="v">{inr(r.basePrice)}</span></div>

      {/* GST component breakdown */}
      {r.taxGroups.length === 0 && (
        <div className="totrow tax"><span className="lab">GST</span><span className="v">{inr(0)}</span></div>
      )}
      {r.taxGroups.map((g) => (
        <div className="totrow tax" key={g.name + g.rate}>
          <span className="lab">{g.name}<span className="badge">{(g.rate * 100).toFixed(g.rate * 100 % 1 ? 1 : 0)}%</span></span>
          <span className="v">{inr(g.value)}</span>
        </div>
      ))}
      <div className="totrow tax" style={{ fontWeight: 500 }}>
        <span className="lab" style={{ color: 'var(--on-surface)' }}>{intra ? 'Total CGST + SGST' : 'Total IGST'}</span>
        <span className="v">{inr(r.totalTax)}</span>
      </div>

      {!preTax && r.postTaxDiscount > 0 && (
        <div className="totrow disc"><span className="lab">Overall discount (post-tax)</span><span className="v">− {inr(r.postTaxDiscount)}</span></div>
      )}

      <div className="totdiv" />
      <div className="totrow grand"><span>Grand total</span><span className="v">{inr(r.grandTotal)}</span></div>

      <div className="recon"><Icon n="check_circle" s={15} /> Reconciles · taxable + GST{!preTax && r.postTaxDiscount ? ' − discount' : ''} = grand total</div>
      <div style={{ marginTop: 10 }}>
        <span className="chip" style={{ fontSize: 11 }}><Icon n={intra ? 'home' : 'local_shipping'} s={14} /> {intra ? 'Intra-state (CGST + SGST)' : 'Inter-state (IGST)'}</span>
      </div>
    </div>
  );
}

/* ─────────── header band ─────────── */
function Header({ doc, setDoc, customer, onCustomer, seriesPreview, compact }) {
  const intra = doc.scenario === 'INTRA';
  return (
    <div className="headband" style={compact ? { padding: '12px 14px' } : null}>
      <div>
        <span className="cap">Customer · place of supply</span>
        <div className="customerchip" onClick={onCustomer} style={compact ? { minWidth: 0, width: '100%' } : null}>
          <span className={'av' + (customer.walkIn ? ' walkin' : '')}>{customer.walkIn ? <Icon n="person" s={20} /> : customer.initials}</span>
          <div className="t">
            <b>{customer.name}</b>
            <small className={intra ? '' : 'inter'}>{customer.state} ({customer.stateCode}) · {intra ? 'CGST+SGST' : 'IGST'}{customer.walkIn ? ' · walk-in' : ''}</small>
          </div>
          <Icon n="expand_more" c="var(--on-surface-variant)" />
        </div>
      </div>
      {!compact && (
        <div>
          <span className="cap">Date</span>
          <div className="field"><div className="fakeinput" style={{ width: 168, height: 44 }}><span className="mono" style={{ whiteSpace: 'nowrap' }}>06 Jun 2026</span><Icon n="event" s={18} /></div></div>
        </div>
      )}
      <div>
        <span className="cap">Price mode</span>
        <Segmented value={doc.priceMode} sm={compact}
          onChange={(v) => setDoc({ ...doc, priceMode: v })}
          options={[
            { value: 'TAX_EXCLUSIVE', label: 'Tax-exclusive' },
            { value: 'TAX_INCLUSIVE', label: 'Tax-inclusive' },
          ]} />
      </div>
      {doc.type === 'invoice' && !compact && (
        <div>
          <span className="cap">Invoice no. (on save)</span>
          <div className="field"><div className="fakeinput" style={{ width: 170, height: 44, borderStyle: 'dashed' }}><span className="mono" style={{ color: 'var(--on-surface-variant)' }}>{seriesPreview}</span></div></div>
        </div>
      )}
    </div>
  );
}

/* ─────────── desktop / tablet line grid ─────────── */
function LineGrid({ lines, calcLines, onEdit, onUnit, onVariant, onPatch, onRemove, onAdd, priceMode }) {
  return (
    <div className="linescroll">
      <table className="linegrid" style={{ minWidth: 760 }}>
        <thead><tr>
          <th>Product</th><th style={{ width: 78 }}>Variant</th><th style={{ width: 84 }}>Unit</th>
          <th style={{ width: 54 }}>Qty</th><th style={{ width: 90 }}>Unit price</th><th style={{ width: 96 }}>Disc</th>
          <th className="num" style={{ width: 88 }}>Taxable</th><th style={{ width: 52 }}>GST</th><th className="num" style={{ width: 96 }}>Line total</th><th style={{ width: 36 }}></th>
        </tr></thead>
        <tbody>
          {lines.map((ln, i) => {
            const c = calcLines[i] || {};
            return (
              <tr className="lrow" key={ln.id}>
                <td>
                  <div className="cellbtn" onClick={(e) => onEdit(ln, e.currentTarget)} style={{ border: 'none', justifyContent: 'flex-start' }}>
                    <div style={{ minWidth: 0 }}>
                      <div className="pname" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{ln.name}</div>
                      <div className="pmeta">HSN {ln.hsn}{ln.exempt ? '' : ''}</div>
                    </div>
                  </div>
                </td>
                <td>{ln.product.variants
                  ? <button className="cellbtn" onClick={(e) => onVariant(ln, e.currentTarget)}><span style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{ln.variant ? ln.variant.label : '—'}</span><Icon n="expand_more" s={16} /></button>
                  : <span style={{ color: 'var(--on-surface-variant)' }}>—</span>}</td>
                <td><button className="cellbtn" onClick={(e) => onUnit(ln, e.currentTarget)}><span className="mono">{ln.unitName}</span><Icon n="expand_more" s={16} /></button></td>
                <td><input className="cellinput num" type="number" min="0" step={ln.decimals ? '0.001' : '1'} value={ln.quantity}
                  onChange={(e) => onPatch(ln.id, { quantity: clampDec(e.target.value, ln.decimals) })} /></td>
                <td><input className="cellinput num" type="number" min="0" value={ln.unitPrice}
                  onChange={(e) => onPatch(ln.id, { unitPrice: parseFloat(e.target.value) || 0, priceOverridden: true })} /></td>
                <td><div style={{ display: 'flex', gap: 3, alignItems: 'center' }}>
                  <button className="cellbtn" style={{ width: 28, padding: 0, justifyContent: 'center', flex: 'none' }}
                    onClick={() => onPatch(ln.id, { lineDiscount: { ...ln.lineDiscount, kind: ln.lineDiscount.kind === 'PERCENT' ? 'FLAT' : 'PERCENT' } })}>{ln.lineDiscount.kind === 'PERCENT' ? '%' : '₹'}</button>
                  <input className="cellinput num" style={{ minWidth: 0 }} type="number" min="0" value={ln.lineDiscount.amount || ''}
                    placeholder="0" onChange={(e) => onPatch(ln.id, { lineDiscount: { ...ln.lineDiscount, amount: parseFloat(e.target.value) || 0 } })} />
                </div></td>
                <td className="num mono">{inr(c.taxable || 0)}</td>
                <td>{ln.exempt ? <span className="chip" style={{ fontSize: 10, padding: '1px 6px' }}>exempt</span>
                  : <span className="chip" style={{ fontSize: 10, padding: '1px 6px' }}>{Math.round(ln.gstRate * 100)}%</span>}</td>
                <td className="num mono" style={{ fontWeight: 500 }}>{inr(c.lineTotal || 0)}</td>
                <td><button className="iconbtn" style={{ width: 34, height: 34 }} onClick={() => onRemove(ln.id)}><Icon n="close" s={18} /></button></td>
              </tr>
            );
          })}
          <tr><td colSpan={10}>
            <div className="addline">
              <button className="btn tonalp sm" onClick={(e) => onAdd(e.currentTarget)}><Icon n="add" s={18} /> Add line</button>
              <span style={{ color: 'var(--on-surface-variant)', font: 'var(--label-medium)' }}>Tab across cells · Enter commits + new line</span>
            </div>
          </td></tr>
        </tbody>
      </table>
    </div>
  );
}
function clampDec(val, dec) {
  let n = parseFloat(val); if (isNaN(n)) return 0;
  const f = Math.pow(10, dec || 0);
  return Math.round(n * f) / f;
}

/* ─────────── phone line cards ─────────── */
function LineCards({ lines, calcLines, onEdit, onAdd }) {
  return (
    <div className="linecards">
      {lines.map((ln, i) => {
        const c = calcLines[i] || {};
        return (
          <div className="linecard" key={ln.id} onClick={(e) => onEdit(ln, e.currentTarget)}>
            <div className="top">
              <div style={{ minWidth: 0 }}>
                <div className="nm">{ln.name}</div>
                {ln.variant && <div style={{ font: 'var(--label-small)', color: 'var(--on-surface-variant)' }}>{ln.variant.label}</div>}
              </div>
              <div className="mono" style={{ font: 'var(--title-small)', fontFamily: 'var(--font-mono)', whiteSpace: 'nowrap' }}>{inr(c.lineTotal || 0)}</div>
            </div>
            <div className="meta">
              <span className="chip" style={{ fontSize: 11 }}>{ln.quantity} {ln.unitName}</span>
              <span className="chip" style={{ fontSize: 11 }}>{inr(ln.unitPrice)}/{ln.unitName}</span>
              {ln.lineDiscount.amount > 0 && <span className="chip sel" style={{ fontSize: 11 }}>−{ln.lineDiscount.kind === 'PERCENT' ? ln.lineDiscount.amount + '%' : inr(ln.lineDiscount.amount)}</span>}
              {ln.exempt ? <span className="chip" style={{ fontSize: 11 }}>exempt</span> : <span className="chip" style={{ fontSize: 11 }}>GST {Math.round(ln.gstRate * 100)}%</span>}
            </div>
          </div>
        );
      })}
      <button className="btn outlined" style={{ alignSelf: 'flex-start' }} onClick={(e) => onAdd(e.currentTarget)}><Icon n="add" s={18} /> Add line</button>
    </div>
  );
}

/* ─────────── phone per-line editor sheet ─────────── */
function LineEditorSheet({ ln, basePrice, calc, onPatch, onUnit, onVariant, onChangeProduct, onDelete, onClose }) {
  const u = ln.product.units.find((x) => x.id === ln.unitId) || ln.product.units[0];
  return (
    <Sheet title="Edit line" onClose={onClose}
      foot={
        <div style={{ display: 'flex', gap: 10, padding: '4px 16px 18px' }}>
          <button className="btn text error" style={{ color: 'var(--error)' }} onClick={onDelete}><Icon n="delete" s={18} /> Delete</button>
          <span className="spacer1" />
          <button className="btn" onClick={onClose}>Done <Icon n="check" s={18} /></button>
        </div>
      }>
      <div className="listitem" style={{ background: 'var(--surface-container)', padding: 12, borderRadius: 'var(--corner-md)' }} onClick={onChangeProduct}>
        <span className="lead tertiary"><Icon n="inventory_2" /></span>
        <div className="body"><div className="h">{ln.name}</div><div className="s">HSN {ln.hsn} · GST {Math.round(ln.gstRate * 100)}%</div></div>
        <Icon n="edit" s={18} c="var(--on-surface-variant)" />
      </div>

      {ln.product.variants && (
        <div className="field"><label>Variant</label>
          <div className="fakeinput" onClick={onVariant}><span>{ln.variant ? ln.variant.label : '— none —'}</span><Icon n="expand_more" /></div></div>
      )}

      <div style={{ display: 'flex', gap: 12 }}>
        <div className="field" style={{ flex: 1 }}><label>Unit</label>
          <div className="fakeinput" onClick={onUnit}><span className="mono">{ln.unitName}</span><Icon n="expand_more" /></div></div>
        <div className="field" style={{ flex: 1 }}><label>Quantity</label>
          <input type="number" min="0" step={ln.decimals ? '0.001' : '1'} value={ln.quantity}
            onChange={(e) => onPatch(ln.id, { quantity: clampDec(e.target.value, ln.decimals) })} /></div>
      </div>
      <div className="hint"><Icon n="straighten" s={18} />{convHint(ln.product, u, Calc.r2(basePrice * u.multiplier))} → base qty <b style={{ fontFamily: 'var(--font-mono)' }}>{Calc.r2(ln.quantity * ln.multiplier)} {ln.product.units[0].name}</b></div>

      <div style={{ display: 'flex', gap: 12 }}>
        <div className="field" style={{ flex: 1 }}><label>Unit price ✎</label>
          <input type="number" min="0" value={ln.unitPrice}
            onChange={(e) => onPatch(ln.id, { unitPrice: parseFloat(e.target.value) || 0, priceOverridden: true })} /></div>
        <div style={{ flex: 1 }}>
          <span className="cap" style={{ font: 'var(--label-small)', color: 'var(--on-surface-variant)' }}>Line discount</span>
          <div style={{ marginTop: 6 }}><DiscountInput value={ln.lineDiscount} onChange={(v) => onPatch(ln.id, { lineDiscount: v })} /></div>
        </div>
      </div>
      {ln.priceOverridden && <div style={{ font: 'var(--label-small)', color: 'var(--secondary)', marginTop: -6 }}>Price overridden — catalog was {inr(AppData.unitPriceFor(ln.product, u, ln.variant))}</div>}

      <div className="card low" style={{ padding: 12 }}>
        <div className="totrow" style={{ padding: '3px 0' }}><span>Taxable</span><span className="v">{inr(calc.taxable || 0)}</span></div>
        <div className="totrow" style={{ padding: '3px 0' }}><span>GST {Math.round(ln.gstRate * 100)}%</span><span className="v">{inr(calc.totalTax || 0)}</span></div>
        <div className="totdiv" style={{ margin: '6px 0' }} />
        <div className="totrow grand" style={{ padding: 0 }}><span>Line total</span><span className="v" style={{ font: 'var(--title-medium)', fontFamily: 'var(--font-mono)' }}>{inr(calc.lineTotal || 0)}</span></div>
      </div>
    </Sheet>
  );
}

Object.assign(window, { AnchoredOverlay, TotalsPanel, Header, LineGrid, LineCards, LineEditorSheet, clampDec });
