import React from 'react';
import { useNavigate } from 'react-router-dom';
import { formatDistanceToNow, parseISO } from 'date-fns';

const sevColor = s => ({ CRITICAL: '#ef4444', HIGH: '#f97316', MEDIUM: '#eab308', LOW: '#22c55e' }[s] || '#64748b');
const sevBg    = s => ({ CRITICAL: '#450a0a', HIGH: '#431407', MEDIUM: '#422006', LOW: '#052e16' }[s] || '#1e293b');

export default function IncidentFeed({ items = [] }) {
  const navigate = useNavigate();

  if (!items.length) return (
    <div style={{ padding: 32, textAlign: 'center', color: '#334155', fontSize: 13 }}>
      No recent incidents
    </div>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      {items.map(inc => (
        <div
          key={inc.id}
          onClick={() => navigate(`/incidents/${inc.anomalyId}`)}
          style={{
            background: '#0f172a', border: `1px solid ${sevColor(inc.severity)}33`,
            borderLeft: `3px solid ${sevColor(inc.severity)}`,
            borderRadius: 8, padding: '12px 16px', cursor: 'pointer',
            transition: 'background 0.15s',
          }}
          onMouseEnter={e => e.currentTarget.style.background = '#1e293b'}
          onMouseLeave={e => e.currentTarget.style.background = '#0f172a'}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
            <span style={{
              background: sevBg(inc.severity), color: sevColor(inc.severity),
              border: `1px solid ${sevColor(inc.severity)}44`,
              borderRadius: 4, padding: '2px 7px', fontSize: 10, fontWeight: 700,
            }}>{inc.severity}</span>
            <span style={{ color: '#e2e8f0', fontWeight: 600, fontSize: 13 }}>{inc.serviceName}</span>
            <span style={{ marginLeft: 'auto' }}>
              <span style={{
                background: '#1e3a5f', color: '#93c5fd', border: '1px solid #1e3a5f',
                borderRadius: 4, padding: '2px 7px', fontSize: 10,
              }}>{inc.status}</span>
            </span>
          </div>
          <p style={{ color: '#94a3b8', fontSize: 12, margin: '0 0 4px', lineHeight: 1.5 }}>
            {inc.description?.substring(0, 100)}{inc.description?.length > 100 ? '…' : ''}
          </p>
          <div style={{ display: 'flex', gap: 12, fontSize: 11, color: '#475569' }}>
            <span>{(inc.anomalyType || '').replace(/_/g, ' ')}</span>
            <span>·</span>
            <span>{inc.detectedAt ? formatDistanceToNow(parseISO(inc.detectedAt), { addSuffix: true }) : ''}</span>
            {inc.hasRca && <span style={{ color: '#6366f1' }}>· RCA available</span>}
          </div>
        </div>
      ))}
    </div>
  );
}
