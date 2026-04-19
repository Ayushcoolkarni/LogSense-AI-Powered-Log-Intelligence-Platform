import React from 'react';
import { useNavigate } from 'react-router-dom';
import { formatDistanceToNow, parseISO } from 'date-fns';
import Badge from '../layout/Badge';
import { FileText } from 'lucide-react';

export default function IncidentFeed({ incidents = [] }) {
  const navigate = useNavigate();

  return (
    <div style={{
      background: '#1e293b', border: '1px solid #334155',
      borderRadius: 10, padding: '20px 24px',
    }}>
      <h3 style={{ color: '#f1f5f9', fontSize: 14, fontWeight: 600, margin: '0 0 16px' }}>
        Recent Incidents
      </h3>

      {incidents.length === 0 && (
        <p style={{ color: '#475569', fontSize: 13, textAlign: 'center', padding: '20px 0' }}>
          No recent incidents
        </p>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
        {incidents.map(incident => (
          <div
            key={incident.id}
            onClick={() => navigate(`/incidents/${incident.anomalyId}`)}
            style={{
              padding: '12px 0',
              borderBottom: '1px solid #0f172a',
              cursor: 'pointer',
              display: 'flex',
              flexDirection: 'column',
              gap: 6,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
                <Badge label={incident.severity} />
                <span style={{
                  color: '#cbd5e1', fontSize: 13, fontWeight: 500,
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                }}>
                  {incident.serviceName}
                </span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
                {incident.hasRca && (
                  <span title="RCA available" style={{ color: '#22c55e' }}>
                    <FileText size={13} />
                  </span>
                )}
                <Badge label={incident.status} />
              </div>
            </div>

            <p style={{
              color: '#64748b', fontSize: 12, margin: 0,
              overflow: 'hidden', textOverflow: 'ellipsis',
              display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
            }}>
              {incident.description}
            </p>

            <span style={{ color: '#475569', fontSize: 11 }}>
              {incident.anomalyType?.replace(/_/g, ' ')} ·{' '}
              {incident.detectedAt
                ? formatDistanceToNow(parseISO(incident.detectedAt), { addSuffix: true })
                : ''}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
