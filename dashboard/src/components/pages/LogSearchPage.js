import React, { useState } from 'react';
import { Search } from 'lucide-react';
import { format, parseISO } from 'date-fns';
import Header from '../components/layout/Header';
import Badge from '../components/layout/Badge';
import { getLogs } from '../services/api';

const LEVELS = ['', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE'];

export default function LogSearchPage() {
  const [serviceName, setServiceName] = useState('');
  const [logLevel, setLogLevel] = useState('');
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);

  const search = async (p = 0) => {
    setLoading(true);
    try {
      const params = { page: p, size: 50 };
      if (serviceName) params.serviceName = serviceName;
      if (logLevel) params.logLevel = logLevel;
      const data = await getLogs(params);
      setResults(data);
      setPage(p);
    } catch (e) {
      alert('Failed to load logs. Is log-ingestion-service running?');
    } finally {
      setLoading(false);
    }
  };

  const logs = results?.content ?? [];
  const totalPages = results?.totalPages ?? 0;

  const levelColor = (level) => ({
    ERROR: '#ef4444', WARN: '#f97316',
    INFO: '#6366f1', DEBUG: '#22c55e', TRACE: '#64748b',
  }[level] || '#94a3b8');

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <Header title="Log Search" onRefresh={() => search(page)} />

      <div style={{ flex: 1, overflowY: 'auto', padding: 24, background: '#0f172a' }}>

        {/* Search bar */}
        <div style={{
          background: '#1e293b', border: '1px solid #334155', borderRadius: 10,
          padding: '16px 20px', display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap',
          alignItems: 'flex-end',
        }}>
          <div style={{ flex: '1 1 180px' }}>
            <label style={{ color: '#64748b', fontSize: 12, display: 'block', marginBottom: 6 }}>Service Name</label>
            <input
              value={serviceName}
              onChange={e => setServiceName(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && search(0)}
              placeholder="e.g. payment-service"
              style={{
                width: '100%', padding: '8px 10px', background: '#0f172a',
                border: '1px solid #334155', borderRadius: 6, color: '#f1f5f9', fontSize: 13,
              }}
            />
          </div>
          <div>
            <label style={{ color: '#64748b', fontSize: 12, display: 'block', marginBottom: 6 }}>Log Level</label>
            <select
              value={logLevel}
              onChange={e => setLogLevel(e.target.value)}
              style={{
                padding: '8px 10px', background: '#0f172a',
                border: '1px solid #334155', borderRadius: 6, color: logLevel ? '#f1f5f9' : '#64748b', fontSize: 13,
              }}
            >
              {LEVELS.map(l => <option key={l} value={l}>{l || 'All Levels'}</option>)}
            </select>
          </div>
          <button
            onClick={() => search(0)}
            disabled={loading}
            style={{
              background: '#6366f1', border: 'none', borderRadius: 6, color: '#fff',
              padding: '8px 18px', fontSize: 13, fontWeight: 600, cursor: 'pointer',
              display: 'flex', alignItems: 'center', gap: 6,
            }}
          >
            <Search size={14} /> {loading ? 'Searching…' : 'Search'}
          </button>
          {results && (
            <span style={{ color: '#475569', fontSize: 13, alignSelf: 'center', marginLeft: 'auto' }}>
              {results.totalElements?.toLocaleString()} logs found
            </span>
          )}
        </div>

        {/* Results */}
        {logs.length > 0 && (
          <div style={{ fontFamily: 'monospace' }}>
            {logs.map(log => (
              <div key={log.id} style={{
                background: '#1e293b', border: '1px solid #1e293b',
                borderLeft: `3px solid ${levelColor(log.logLevel)}`,
                borderRadius: 6, padding: '10px 14px', marginBottom: 4,
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 4 }}>
                  <span style={{ color: levelColor(log.logLevel), fontSize: 11, fontWeight: 700, minWidth: 40 }}>
                    {log.logLevel}
                  </span>
                  <span style={{ color: '#475569', fontSize: 11 }}>
                    {log.timestamp ? format(parseISO(log.timestamp), 'dd MMM HH:mm:ss.SSS') : ''}
                  </span>
                  <span style={{ color: '#6366f1', fontSize: 11 }}>{log.serviceName}</span>
                  {log.traceId && (
                    <span style={{ color: '#334155', fontSize: 11 }}>trace:{log.traceId.substring(0, 8)}…</span>
                  )}
                </div>
                <p style={{ color: '#cbd5e1', fontSize: 12, margin: 0, lineHeight: 1.5, wordBreak: 'break-all' }}>
                  {log.message}
                </p>
                {log.stackTrace && (
                  <details style={{ marginTop: 6 }}>
                    <summary style={{ color: '#ef4444', fontSize: 11, cursor: 'pointer' }}>Stack trace</summary>
                    <pre style={{ color: '#94a3b8', fontSize: 10, margin: '6px 0 0', overflowX: 'auto', whiteSpace: 'pre-wrap' }}>
                      {log.stackTrace}
                    </pre>
                  </details>
                )}
              </div>
            ))}

            {/* Pagination */}
            {totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 16 }}>
                <button onClick={() => search(Math.max(0, page - 1))} disabled={page === 0}
                  style={{ background: '#1e293b', border: '1px solid #334155', color: '#94a3b8', borderRadius: 6, padding: '6px 14px', cursor: 'pointer' }}>
                  ← Prev
                </button>
                <span style={{ color: '#64748b', alignSelf: 'center', fontSize: 13 }}>{page + 1} / {totalPages}</span>
                <button onClick={() => search(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1}
                  style={{ background: '#1e293b', border: '1px solid #334155', color: '#94a3b8', borderRadius: 6, padding: '6px 14px', cursor: 'pointer' }}>
                  Next →
                </button>
              </div>
            )}
          </div>
        )}

        {!loading && results && logs.length === 0 && (
          <div style={{ textAlign: 'center', color: '#475569', padding: 60, fontSize: 14 }}>
            No logs found matching your criteria
          </div>
        )}

        {!results && !loading && (
          <div style={{ textAlign: 'center', color: '#334155', padding: 60, fontSize: 14 }}>
            Enter a service name or level and press Search
          </div>
        )}
      </div>
    </div>
  );
}
