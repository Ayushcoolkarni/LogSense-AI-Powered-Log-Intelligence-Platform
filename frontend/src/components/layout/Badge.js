import React from 'react';

const COLORS = {
  CRITICAL: { bg: '#450a0a', text: '#fca5a5', border: '#7f1d1d' },
  HIGH:     { bg: '#431407', text: '#fdba74', border: '#7c2d12' },
  MEDIUM:   { bg: '#422006', text: '#fcd34d', border: '#78350f' },
  LOW:      { bg: '#052e16', text: '#86efac', border: '#14532d' },
  OPEN:        { bg: '#172554', text: '#93c5fd', border: '#1e3a5f' },
  INVESTIGATING:{ bg: '#2e1065', text: '#c4b5fd', border: '#4c1d95' },
  RESOLVED:    { bg: '#052e16', text: '#86efac', border: '#14532d' },
  FALSE_POSITIVE: { bg: '#1c1917', text: '#a8a29e', border: '#292524' },
  PENDING:  { bg: '#172554', text: '#93c5fd', border: '#1e3a5f' },
  SENT:     { bg: '#052e16', text: '#86efac', border: '#14532d' },
  FAILED:   { bg: '#450a0a', text: '#fca5a5', border: '#7f1d1d' },
  ACKNOWLEDGED: { bg: '#1e293b', text: '#94a3b8', border: '#334155' },
};

export default function Badge({ label, size = 'sm' }) {
  const upper = (label || 'UNKNOWN').toUpperCase();
  const c = COLORS[upper] || { bg: '#1e293b', text: '#94a3b8', border: '#334155' };
  const pad = size === 'lg' ? '5px 12px' : '3px 8px';
  const fs = size === 'lg' ? 13 : 11;

  return (
    <span style={{
      background: c.bg,
      color: c.text,
      border: `1px solid ${c.border}`,
      borderRadius: 4,
      padding: pad,
      fontSize: fs,
      fontWeight: 600,
      letterSpacing: '0.03em',
      whiteSpace: 'nowrap',
    }}>
      {upper}
    </span>
  );
}
