import React, { useState } from 'react';
import { formatDistanceToNow, parseISO } from 'date-fns';
import Header from '../layout/Header';
import Badge from '../layout/Badge';
import { getAnomalies, updateAnomalyStatus } from '../../services/api';


export default function AnomaliesPage() {
  const [page, setPage] = useState(0);
  const [anomalies, setAnomalies] = useState(null);
  const [loading, setLoading] = useState(false);

  React.useEffect(() => {
    setLoading(true);
    getAnomalies({ page, size: 20, sort: 'detectedAt,desc' })
      .then(setAnomalies)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [page]);

  const items = anomalies?.content ?? [];
  const totalPages = anomalies?.totalPages ?? 0;

  const acknowledge = async (id) => {
    await updateAnomalyStatus(id, 'ACKNOWLEDGED');
    setAnomalies(prev => ({
      ...prev,
      content: prev.content.map(a => a.id === id ? { ...a, status: 'ACKNOWLEDGED' } : a),
    }));
  };

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <Header title="Anomalies" onRefresh={() => setPage(p => p)} />

      <div style={{ flex: 1, overflowY: 'auto', padding: 24, background: '#0f172a' }}>
        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 10, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #334155' }}>
                {['Service', 'Type', 'Severity', 'Status', 'Actual / Threshold', 'Detected', 'Action'].map(h => (
                  <th key={h} style={{ padding: '12px 16px', textAlign: 'left', color: '#64748b', fontWeight: 500, fontSize: 12 }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading && !items.length ? (
                <tr><td colSpan={7} style={{ padding: 40, textAlign: 'center', color: '#475569' }}>Loading…</td></tr>
              ) : items.length === 0 ? (
                <tr><td colSpan={7} style={{ padding: 40, textAlign: 'center', color: '#475569' }}>No anomalies found</td></tr>
              ) : items.map(a => (
                <tr key={a.id} style={{ borderBottom: '1px solid #0f172a' }}
                  onMouseEnter={e => e.currentTarget.style.background = '#0f172a'}
                  onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
                  <td style={{ padding: '12px 16px', color: '#e2e8f0', fontWeight: 500 }}>{a.serviceName}</td>
                  <td style={{ padding: '12px 16px', color: '#94a3b8' }}>{(a.anomalyType || '').replace(/_/g, ' ')}</td>
                  <td style={{ padding: '12px 16px' }}><Badge label={a.severity} /></td>
                  <td style={{ padding: '12px 16px' }}><Badge label={a.status} /></td>
                  <td style={{ padding: '12px 16px', color: '#64748b', fontFamily: 'monospace', fontSize: 12 }}>
                    {a.actualValue?.toFixed(0)} / {a.threshold?.toFixed(0)}
                  </td>
                  <td style={{ padding: '12px 16px', color: '#64748b' }}>
                    {a.detectedAt ? formatDistanceToNow(parseISO(a.detectedAt), { addSuffix: true }) : '—'}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    {a.status === 'OPEN' && (
                      <button onClick={() => acknowledge(a.id)} style={{
                        background: '#172554', border: '1px solid #1e3a5f', color: '#93c5fd',
                        borderRadius: 5, padding: '4px 10px', fontSize: 11, cursor: 'pointer',
                      }}>
                        Acknowledge
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
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
              style={{ background: '#1e293b', border: '1px solid #334155', color: '#94a3b8', borderRadius: 6, padding: '6px 14px', cursor: 'pointer' }}>
              ← Prev
            </button>
            <span style={{ color: '#64748b', alignSelf: 'center', fontSize: 13 }}>{page + 1} / {totalPages}</span>
            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
              style={{ background: '#1e293b', border: '1px solid #334155', color: '#94a3b8', borderRadius: 6, padding: '6px 14px', cursor: 'pointer' }}>
              Next →
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
