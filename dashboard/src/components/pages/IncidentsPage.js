import React, { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Filter } from 'lucide-react';
import { formatDistanceToNow, parseISO } from 'date-fns';
import Header from '../components/layout/Header';
import Badge from '../components/layout/Badge';
import { useIncidents } from '../hooks/useDashboard';

const SEVERITIES = ['', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const STATUSES   = ['', 'OPEN', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE'];

function Select({ value, onChange, options, placeholder }) {
  return (
    <select
      value={value}
      onChange={e => onChange(e.target.value)}
      style={{
        background: '#1e293b', border: '1px solid #334155', borderRadius: 6,
        color: value ? '#f1f5f9' : '#64748b', padding: '7px 10px', fontSize: 13,
      }}
    >
      <option value="">{placeholder}</option>
      {options.filter(Boolean).map(o => <option key={o} value={o}>{o}</option>)}
    </select>
  );
}

export default function IncidentsPage() {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [severity, setSeverity] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);

  const params = { page, size: 20 };
  if (search) params.q = search;
  if (severity) params.severity = severity;
  if (status) params.status = status;

  const { data, loading } = useIncidents(params, 30000);
  const incidents = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const handleSearch = useCallback(e => {
    setSearch(e.target.value);
    setPage(0);
  }, []);

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <Header title="Incidents" onRefresh={() => {}} />

      <div style={{ flex: 1, overflowY: 'auto', padding: 24, background: '#0f172a' }}>

        {/* Filters */}
        <div style={{
          background: '#1e293b', border: '1px solid #334155',
          borderRadius: 10, padding: '14px 18px',
          display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20, flexWrap: 'wrap',
        }}>
          <div style={{ position: 'relative', flex: '1 1 200px' }}>
            <Search size={14} color="#475569" style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)' }} />
            <input
              value={search}
              onChange={handleSearch}
              placeholder="Search incidents…"
              style={{
                width: '100%', paddingLeft: 32, padding: '7px 10px 7px 32px',
                background: '#0f172a', border: '1px solid #334155', borderRadius: 6,
                color: '#f1f5f9', fontSize: 13,
              }}
            />
          </div>
          <Select value={severity} onChange={v => { setSeverity(v); setPage(0); }} options={SEVERITIES} placeholder="All Severities" />
          <Select value={status} onChange={v => { setStatus(v); setPage(0); }} options={STATUSES} placeholder="All Statuses" />
          <span style={{ color: '#475569', fontSize: 13, marginLeft: 'auto' }}>
            {totalElements} incidents
          </span>
        </div>

        {/* Table */}
        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 10, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #334155' }}>
                {['Service', 'Type', 'Severity', 'Status', 'Detected', 'RCA'].map(h => (
                  <th key={h} style={{ padding: '12px 16px', textAlign: 'left', color: '#64748b', fontWeight: 500, fontSize: 12 }}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading && incidents.length === 0 ? (
                <tr><td colSpan={6} style={{ padding: 40, textAlign: 'center', color: '#475569' }}>Loading…</td></tr>
              ) : incidents.length === 0 ? (
                <tr><td colSpan={6} style={{ padding: 40, textAlign: 'center', color: '#475569' }}>No incidents found</td></tr>
              ) : (
                incidents.map(inc => (
                  <tr
                    key={inc.id}
                    onClick={() => navigate(`/incidents/${inc.anomalyId}`)}
                    style={{ borderBottom: '1px solid #0f172a', cursor: 'pointer' }}
                    onMouseEnter={e => e.currentTarget.style.background = '#0f172a'}
                    onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                  >
                    <td style={{ padding: '12px 16px', color: '#e2e8f0', fontWeight: 500 }}>{inc.serviceName}</td>
                    <td style={{ padding: '12px 16px', color: '#94a3b8' }}>
                      {(inc.anomalyType || '').replace(/_/g, ' ')}
                    </td>
                    <td style={{ padding: '12px 16px' }}><Badge label={inc.severity} /></td>
                    <td style={{ padding: '12px 16px' }}><Badge label={inc.status} /></td>
                    <td style={{ padding: '12px 16px', color: '#64748b' }}>
                      {inc.detectedAt
                        ? formatDistanceToNow(parseISO(inc.detectedAt), { addSuffix: true })
                        : '—'}
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{ color: inc.rcaReportId ? '#22c55e' : '#334155', fontSize: 12 }}>
                        {inc.rcaReportId ? '✓ Available' : '—'}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 20 }}>
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
              style={{ background: '#1e293b', border: '1px solid #334155', color: page === 0 ? '#334155' : '#94a3b8', borderRadius: 6, padding: '6px 14px', cursor: page === 0 ? 'default' : 'pointer' }}>
              ← Prev
            </button>
            <span style={{ color: '#64748b', fontSize: 13, alignSelf: 'center' }}>
              {page + 1} / {totalPages}
            </span>
            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
              style={{ background: '#1e293b', border: '1px solid #334155', color: page >= totalPages - 1 ? '#334155' : '#94a3b8', borderRadius: 6, padding: '6px 14px', cursor: page >= totalPages - 1 ? 'default' : 'pointer' }}>
              Next →
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
