import React, { useState } from 'react';
import { formatDistanceToNow, parseISO } from 'date-fns';
import Header from '../layout/Header';
import Badge from '../layout/Badge';
import { getAlerts, acknowledgeAlert } from '../../services/api';

export default function AlertsPage() {
  const [page, setPage] = useState(0);
  const [alerts, setAlerts] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = (p = 0) => {
    setLoading(true);
    setError(null);
    getAlerts({ page: p, size: 20, sort: 'createdAt,desc' })
      .then(data => { setAlerts(data); setPage(p); })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  };

  React.useEffect(() => { load(0); }, []);

  const acknowledge = async (id) => {
    try {
      await acknowledgeAlert(id);
      setAlerts(prev => ({
        ...prev,
        content: prev.content.map(a =>
          a.id === id ? { ...a, status: 'ACKNOWLEDGED' } : a
        ),
      }));
    } catch (e) {
      console.error('Acknowledge failed:', e.message);
    }
  };

  const items = alerts?.content ?? [];
  const totalPages = alerts?.totalPages ?? 0;

  const channelColor = (ch) => ({ EMAIL: '#6366f1', SLACK: '#22c55e' }[ch] || '#64748b');

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <Header title="Alerts" onRefresh={() => load(page)} />

      <div style={{ flex: 1, overflowY: 'auto', padding: 24, background: '#0f172a' }}>

        {error && (
          <div style={{ background: '#450a0a', border: '1px solid #7f1d1d', borderRadius: 8, padding: '12px 16px', marginBottom: 16, color: '#fca5a5', fontSize: 13 }}>
            Failed to load alerts: {error}
          </div>
        )}

        {/* Summary bar */}
        {alerts && (
          <div style={{ display: 'flex', gap: 16, marginBottom: 20, flexWrap: 'wrap' }}>
            {[
              { label: 'Total', value: alerts.totalElements, color: '#64748b' },
              { label: 'Sent', value: items.filter(a => a.status === 'SENT').length, color: '#6366f1' },
              { label: 'Acknowledged', value: items.filter(a => a.status === 'ACKNOWLEDGED').length, color: '#22c55e' },
              { label: 'Failed', value: items.filter(a => a.status === 'FAILED').length, color: '#ef4444' },
            ].map(s => (
              <div key={s.label} style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 8, padding: '10px 18px', minWidth: 100 }}>
                <div style={{ color: s.color, fontSize: 22, fontWeight: 700 }}>{s.value}</div>
                <div style={{ color: '#64748b', fontSize: 12 }}>{s.label}</div>
              </div>
            ))}
          </div>
        )}

        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 10, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #334155' }}>
                {['Service', 'Type', 'Severity', 'Status', 'Channels', 'Message', 'Created', 'Action'].map(h => (
                  <th key={h} style={{ padding: '12px 16px', textAlign: 'left', color: '#64748b', fontWeight: 500, fontSize: 12 }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading && !items.length ? (
                <tr><td colSpan={8} style={{ padding: 40, textAlign: 'center', color: '#475569' }}>Loading…</td></tr>
              ) : items.length === 0 ? (
                <tr><td colSpan={8} style={{ padding: 40, textAlign: 'center', color: '#475569' }}>No alerts found</td></tr>
              ) : items.map(a => (
                <tr key={a.id}
                  style={{ borderBottom: '1px solid #0f172a' }}
                  onMouseEnter={e => e.currentTarget.style.background = '#0f172a'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
                  <td style={{ padding: '12px 16px', color: '#e2e8f0', fontWeight: 500 }}>{a.serviceName}</td>
                  <td style={{ padding: '12px 16px', color: '#94a3b8', fontSize: 12 }}>{(a.anomalyType || '').replace(/_/g, ' ')}</td>
                  <td style={{ padding: '12px 16px' }}><Badge label={a.severity} /></td>
                  <td style={{ padding: '12px 16px' }}><Badge label={a.status} /></td>
                  <td style={{ padding: '12px 16px' }}>
                    <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                      {(a.channelsSent || []).map(ch => (
                        <span key={ch} style={{
                          background: channelColor(ch) + '22', color: channelColor(ch),
                          border: `1px solid ${channelColor(ch)}44`,
                          borderRadius: 4, padding: '2px 6px', fontSize: 10, fontWeight: 600
                        }}>{ch}</span>
                      ))}
                    </div>
                  </td>
                  <td style={{ padding: '12px 16px', color: '#94a3b8', fontSize: 12, maxWidth: 280 }}>
                    <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{a.message}</div>
                  </td>
                  <td style={{ padding: '12px 16px', color: '#64748b', whiteSpace: 'nowrap' }}>
                    {a.createdAt ? formatDistanceToNow(parseISO(a.createdAt), { addSuffix: true }) : '—'}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    {a.status === 'SENT' && (
                      <button onClick={() => acknowledge(a.id)} style={{
                        background: '#172554', border: '1px solid #1e3a5f', color: '#93c5fd',
                        borderRadius: 5, padding: '4px 10px', fontSize: 11, cursor: 'pointer',
                      }}>
                        Ack
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 20 }}>
            <button onClick={() => load(Math.max(0, page - 1))} disabled={page === 0}
              style={{ background: '#1e293b', border: '1px solid #334155', color: '#94a3b8', borderRadius: 6, padding: '6px 14px', cursor: 'pointer' }}>
              ← Prev
            </button>
            <span style={{ color: '#64748b', alignSelf: 'center', fontSize: 13 }}>{page + 1} / {totalPages}</span>
            <button onClick={() => load(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1}
              style={{ background: '#1e293b', border: '1px solid #334155', color: '#94a3b8', borderRadius: 6, padding: '6px 14px', cursor: 'pointer' }}>
              Next →
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
