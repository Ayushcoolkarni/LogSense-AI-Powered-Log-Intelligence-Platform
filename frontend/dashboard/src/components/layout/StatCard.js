import React from 'react';
import { useNavigate } from 'react-router-dom';

export default function StatCard({ icon: Icon, label, value, sub, color = '#6366f1', trend, to }) {
  const navigate = useNavigate();

  return (
    <div
      onClick={() => to && navigate(to)}
      style={{
        background: '#1e293b',
        border: `1px solid ${to ? color + '44' : '#334155'}`,
        borderRadius: 10,
        padding: '20px 22px',
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
        minWidth: 0,
        cursor: to ? 'pointer' : 'default',
        transition: 'background 0.15s, border-color 0.15s',
      }}
      onMouseEnter={e => { if (to) e.currentTarget.style.background = '#0f172a'; }}
      onMouseLeave={e => { if (to) e.currentTarget.style.background = '#1e293b'; }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ color: '#64748b', fontSize: 13, fontWeight: 500 }}>{label}</span>
        {Icon && (
          <div style={{ background: color + '22', borderRadius: 8, padding: 8, display: 'flex' }}>
            <Icon size={16} color={color} />
          </div>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
        <span style={{ color: '#f1f5f9', fontSize: 28, fontWeight: 700, lineHeight: 1 }}>
          {value ?? '—'}
        </span>
        {trend != null && (
          <span style={{ fontSize: 12, color: trend >= 0 ? '#f87171' : '#22c55e' }}>
            {trend >= 0 ? '↑' : '↓'} {Math.abs(trend)}%
          </span>
        )}
      </div>

      {sub && <span style={{ color: '#64748b', fontSize: 12 }}>{sub}</span>}
      {to && <span style={{ color: color, fontSize: 11, marginTop: 2 }}>Click to view →</span>}
    </div>
  );
}
