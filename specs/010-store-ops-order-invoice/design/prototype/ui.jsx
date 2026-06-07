/* Shared UI primitives → window */
const { useState, useRef, useEffect, useLayoutEffect, useMemo } = React;
const inr = (x, dp) => Calc.inr(x, dp);

function Icon({ n, s = 20, c, style }) {
  return (
    <span className="material-symbols-outlined"
      style={{
        fontFamily: 'var(--font-icon)', fontSize: s, lineHeight: 1, color: c,
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        width: s, height: s, overflow: 'hidden', flex: 'none', verticalAlign: 'middle',
        ...(style || {}),
      }}>
      {n}
    </span>
  );
}

function SyncChip({ state }) {
  const map = {
    offline: ['cloud_off', 'Saved offline'],
    syncing: ['sync', 'Syncing…'],
    synced: ['cloud_done', 'Synced'],
    failed: ['error', 'Sync failed'],
  };
  const [ic, label] = map[state] || map.offline;
  return (<span className={'sync ' + state}><Icon n={ic} s={15} /> {label}</span>);
}

// click-outside hook
function useClickOutside(ref, onOut, active) {
  useEffect(() => {
    if (!active) return;
    function h(e) { if (ref.current && !ref.current.contains(e.target)) onOut(); }
    document.addEventListener('mousedown', h, true);
    return () => document.removeEventListener('mousedown', h, true);
  }, [active]);
}

// Generic bottom sheet (renders inside the device .screen)
function Sheet({ title, onClose, children, foot }) {
  return (
    <div className="scrim bottom" onMouseDown={onClose}>
      <div className="sheet" onMouseDown={(e) => e.stopPropagation()}>
        <div className="grab" />
        <div className="shead">
          <span className="ttl">{title}</span>
          <button className="iconbtn" onClick={onClose}><Icon n="close" /></button>
        </div>
        <div className="scont">{children}</div>
        {foot}
      </div>
    </div>
  );
}

function Dialog({ children, onClose }) {
  return (
    <div className="scrim center" onMouseDown={onClose}>
      <div className="dialog" onMouseDown={(e) => e.stopPropagation()}>{children}</div>
    </div>
  );
}

// segmented control
function Segmented({ value, options, onChange, sm }) {
  return (
    <div className={'segmented' + (sm ? ' sm' : '')}>
      {options.map((o) => (
        <button key={o.value} className={value === o.value ? 'on' : ''} onClick={() => onChange(o.value)}>
          {o.icon && <Icon n={o.icon} s={16} />}{o.label}
        </button>
      ))}
    </div>
  );
}

// discount input: %/₹ toggle + number
function DiscountInput({ value, onChange, compact }) {
  return (
    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
      <div className="segmented sm">
        <button className={value.kind === 'PERCENT' ? 'on' : ''} onClick={() => onChange({ ...value, kind: 'PERCENT' })}>%</button>
        <button className={value.kind === 'FLAT' ? 'on' : ''} onClick={() => onChange({ ...value, kind: 'FLAT' })}>₹</button>
      </div>
      <input className="cellinput num" style={{ width: compact ? 90 : 110 }} type="number" min="0"
        value={value.amount || ''} placeholder="0"
        onChange={(e) => onChange({ ...value, amount: parseFloat(e.target.value) || 0 })} />
    </div>
  );
}

Object.assign(window, { Icon, SyncChip, useClickOutside, Sheet, Dialog, Segmented, DiscountInput, inr });
