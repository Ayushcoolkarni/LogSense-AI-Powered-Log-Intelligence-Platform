import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Brain, Lightbulb, AlertCircle, CheckCircle, Clock } from 'lucide-react';
import { formatDistanceToNow, parseISO, format } from 'date-fns';
import Header from '../components/layout/Header';
import Badge from '../components/layout/Badge';
import { getIncidentByAnomalyId, updateIncidentStatus } from '../services/api';

function Section({ title, icon: Icon, color = '#6366f1', children }) {
  return (
    <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 10, padding: '18px 22px', marginBottom: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
        {Icon && <Icon size={15} color={color} />}
        <h3 style={{ color: '#f1f5f9', fontSize: 14, fontWeight: 600, margin: 0 }}>{title}</h3>
      </div>
      {children}
    </div>
  );
}

function InfoRow({ label, value }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #0f172a' }}>
      <span style={{ color: '#64748b', fontSize: 13 }}>{label}</span>
      <span style={{ color: '#e2e8f0', fontSize: 13, fontWeight: 500, textAlign: 'right', maxWidth: '60%', wordBreak: 'break-word' }}>{value || '—'}</span>
    </div>
  );
}

function BulletList({ items = [], color = '#6366f1' }) {
  if (!items.length) return <p style={{ color: '#475569', fontSize: 13 }}>None recorded</p>;
  return (
    <ul style={{ margin: 0, paddingLeft: 18 }}>
      {items.map((item, i) => (
        <li key={i} style={{ color: '#94a3b8', fontSize: 13, marginBottom: 6, lineHeight: 1.6 }}>
          {item}
        </li>
      ))}
    </ul>
  );
}

export default function IncidentDetailPage() {
  const { anomalyId } = useParams();
  const navigate = useNavigate();
  const [incident, setIncident] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    setLoading(true);
    getIncidentByAnomalyId(anomalyId)
      .then(setIncident)
      .catch(() => setError('Incident not found'))
      .finally(() => setLoading(false));
  }, [anomalyId]);

  const handleStatusUpdate = async (newStatus) => {
    if (!incident) return;
    setUpdating(true);
    try {
      const updated = await updateIncidentStatus(incident.id, newStatus);
      setIncident(updated);
    } catch (e) {
      alert('Failed to update status');
    } finally {
      setUpdating(false);
    }
  };

  if (loading) return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Header title="Incident Detail" onRefresh={() => {}} />
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#0f172a', color: '#475569' }}>
        Loading incident…
      </div>
    </div>
  );

  if (error || !incident) return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Header title="Incident Detail" onRefresh={() => {}} />
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#0f172a', color: '#f87171' }}>
        {error || 'Incident not found'}
      </div>
    </div>
  );

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <Header title="Incident Detail" onRefresh={() => {}} />

      <div style={{ flex: 1, overflowY: 'auto', padding: 24, background: '#0f172a' }}>

        {/* Back + title */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
          <button onClick={() => navigate('/incidents')} style={{
            background: 'none', border: 'none', color: '#6366f1', cursor: 'pointer',
            display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, padding: 0,
          }}>
            <ArrowLeft size={15} /> Back
          </button>
          <h2 style={{ color: '#f1f5f9', fontSize: 18, fontWeight: 700, margin: 0 }}>
            {incident.serviceName}
          </h2>
          <Badge label={incident.severity} size="lg" />
          <Badge label={incident.status} size="lg" />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 0 }}>
          {/* Left column */}
          <div>
            <Section title="Incident Details" icon={AlertCircle}>
              <InfoRow label="Anomaly ID" value={incident.anomalyId} />
              <InfoRow label="Service" value={incident.serviceName} />
              <InfoRow label="Anomaly Type" value={(incident.anomalyType || '').replace(/_/g, ' ')} />
              <InfoRow label="Severity" value={incident.severity} />
              <InfoRow label="Status" value={incident.status} />
              <InfoRow label="Detected"
                value={incident.detectedAt
                  ? `${format(parseISO(incident.detectedAt), 'dd MMM yyyy HH:mm')} (${formatDistanceToNow(parseISO(incident.detectedAt), { addSuffix: true })})`
                  : '—'} />
              {incident.traceId && <InfoRow label="Trace ID" value={incident.traceId} />}
            </Section>

            <Section title="Description" icon={Clock}>
              <p style={{ color: '#94a3b8', fontSize: 13, margin: 0, lineHeight: 1.7 }}>
                {incident.anomalyDescription || 'No description available.'}
              </p>
            </Section>

            {/* Status actions */}
            <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 10, padding: '16px 20px' }}>
              <h3 style={{ color: '#f1f5f9', fontSize: 14, fontWeight: 600, margin: '0 0 12px' }}>Update Status</h3>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {['OPEN', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE'].map(s => (
                  <button key={s} onClick={() => handleStatusUpdate(s)} disabled={updating || incident.status === s}
                    style={{
                      background: incident.status === s ? '#334155' : '#0f172a',
                      border: '1px solid #334155', borderRadius: 6, color: incident.status === s ? '#6366f1' : '#94a3b8',
                      padding: '6px 12px', fontSize: 12, cursor: incident.status === s ? 'default' : 'pointer',
                      fontWeight: incident.status === s ? 600 : 400,
                    }}>
                    {s.replace(/_/g, ' ')}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Right column — RCA */}
          <div>
            {incident.rcaReportId ? (
              <>
                <Section title="Root Cause Analysis" icon={Brain} color="#818cf8">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                    <span style={{ color: '#64748b', fontSize: 12 }}>Generated by:</span>
                    <span style={{
                      background: '#2e1065', color: '#c4b5fd', border: '1px solid #4c1d95',
                      borderRadius: 4, padding: '2px 8px', fontSize: 11, fontWeight: 600,
                    }}>
                      {incident.rcaGeneratedBy || 'RULE_BASED'}
                    </span>
                    {incident.rcaConfidenceScore != null && (
                      <span style={{ color: '#64748b', fontSize: 12 }}>
                        Confidence: {(incident.rcaConfidenceScore * 100).toFixed(0)}%
                      </span>
                    )}
                  </div>
                  <p style={{ color: '#94a3b8', fontSize: 13, lineHeight: 1.7, margin: 0 }}>
                    {incident.rootCauseSummary || 'Analysis pending…'}
                  </p>
                </Section>

                {incident.aiAnalysis && (
                  <Section title="AI Analysis" icon={Brain} color="#22c55e">
                    <p style={{ color: '#94a3b8', fontSize: 13, lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>
                      {incident.aiAnalysis}
                    </p>
                  </Section>
                )}

                <Section title="Alert Status" icon={CheckCircle} color="#22c55e">
                  <InfoRow label="Alert ID" value={incident.alertId} />
                  <InfoRow label="Alert Status" value={incident.alertStatus} />
                  <InfoRow label="Sent At" value={incident.alertSentAt ? format(parseISO(incident.alertSentAt), 'dd MMM HH:mm') : null} />
                  <InfoRow label="Acknowledged By" value={incident.alertAcknowledgedBy} />
                </Section>
              </>
            ) : (
              <div style={{
                background: '#1e293b', border: '1px solid #334155', borderRadius: 10,
                padding: '40px 24px', textAlign: 'center',
              }}>
                <Brain size={32} color="#334155" style={{ marginBottom: 12 }} />
                <p style={{ color: '#475569', fontSize: 13, margin: 0 }}>
                  RCA report is being generated…
                </p>
                <p style={{ color: '#334155', fontSize: 12, marginTop: 8 }}>
                  Check back in a few seconds
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
