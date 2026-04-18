import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { format, parseISO, formatDistanceToNow } from 'date-fns';
import { Brain, AlertTriangle, ArrowLeft, CheckCircle, Clock, Zap } from 'lucide-react';
import Badge from '../layout/Badge';
import {
  getIncidentByAnomalyId,
  getAnomalies,
  getAlerts,
  updateIncidentStatus,
} from '../../services/api';

const sevColor = s => ({ CRITICAL: '#ef4444', HIGH: '#f97316', MEDIUM: '#eab308', LOW: '#22c55e' }[s] || '#64748b');

function InfoRow({ label, value, mono }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #1e293b' }}>
      <span style={{ color: '#64748b', fontSize: 13 }}>{label}</span>
      <span style={{ color: mono ? '#a5b4fc' : '#e2e8f0', fontSize: 13, fontFamily: mono ? 'monospace' : 'inherit', textAlign: 'right', maxWidth: '60%' }}>{value || '—'}</span>
    </div>
  );
}

function Card({ title, icon, children }) {
  return (
    <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: 10, padding: '18px 22px', marginBottom: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
        {icon}
        <span style={{ color: '#e2e8f0', fontWeight: 600, fontSize: 14 }}>{title}</span>
      </div>
      {children}
    </div>
  );
}

export default function IncidentDetailPage() {
  const { anomalyId } = useParams();
  const navigate = useNavigate();

  const [incident, setIncident] = useState(null);
  const [anomaly, setAnomaly] = useState(null);
  const [alert, setAlert] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!anomalyId) return;
    setLoading(true);

    Promise.allSettled([
      getIncidentByAnomalyId(anomalyId),
      getAnomalies({ size: 1000 }),
      getAlerts({ size: 1000 }),
    ]).then(([incRes, anomRes, alertRes]) => {
      if (incRes.status === 'fulfilled') setIncident(incRes.value);
      else setError('Incident not found');

      if (anomRes.status === 'fulfilled') {
        const found = anomRes.value?.content?.find(a => a.id === anomalyId);
        setAnomaly(found || null);
      }

      if (alertRes.status === 'fulfilled') {
        const found = alertRes.value?.content?.find(a => a.anomalyId === anomalyId);
        setAlert(found || null);
      }
    }).finally(() => setLoading(false));
  }, [anomalyId]);

  const handleStatusUpdate = async (status) => {
    if (!incident) return;
    setUpdating(true);
    try {
      await updateIncidentStatus(incident.id, status);
      setIncident(prev => ({ ...prev, status }));
    } catch {
      alert('Failed to update status');
    } finally {
      setUpdating(false);
    }
  };

  if (loading) return (
    <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#0f172a' }}>
      <span style={{ color: '#475569' }}>Loading incident…</span>
    </div>
  );

  if (error || !incident) return (
    <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#0f172a', flexDirection: 'column', gap: 12 }}>
      <AlertTriangle color="#ef4444" size={32} />
      <span style={{ color: '#94a3b8' }}>{error || 'Incident not found'}</span>
      <button onClick={() => navigate(-1)} style={{ background: '#1e293b', border: '1px solid #334155', color: '#94a3b8', borderRadius: 6, padding: '6px 14px', cursor: 'pointer' }}>
        ← Go Back
      </button>
    </div>
  );

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', background: '#0f172a' }}>

      {/* Top bar */}
      <div style={{ padding: '16px 24px', borderBottom: '1px solid #1e293b', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button onClick={() => navigate(-1)} style={{
          background: '#1e293b', border: '1px solid #334155', color: '#94a3b8',
          borderRadius: 6, padding: '6px 12px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: 13,
        }}>
          <ArrowLeft size={14} /> Back
        </button>
        <Badge label={incident.severity} />
        <span style={{ color: '#e2e8f0', fontWeight: 600, fontSize: 16 }}>{incident.serviceName}</span>
        <span style={{ color: '#475569', fontSize: 13 }}>{(incident.anomalyType || '').replace(/_/g, ' ')}</span>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
          {['OPEN', 'INVESTIGATING', 'RESOLVED'].map(s => (
            <button key={s} onClick={() => handleStatusUpdate(s)}
              disabled={updating || incident.status === s}
              style={{
                background: incident.status === s ? '#334155' : '#1e293b',
                border: '1px solid #334155', color: incident.status === s ? '#e2e8f0' : '#64748b',
                borderRadius: 6, padding: '5px 12px', fontSize: 12, cursor: incident.status === s ? 'default' : 'pointer',
              }}>
              {s}
            </button>
          ))}
        </div>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: 24 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 0 }}>

          {/* LEFT column */}
          <div>
            {/* Incident summary */}
            <Card title="Incident Summary" icon={<AlertTriangle size={16} color="#f97316" />}>
              <InfoRow label="ID" value={incident.id} mono />
              <InfoRow label="Service" value={incident.serviceName} />
              <InfoRow label="Anomaly Type" value={(incident.anomalyType || '').replace(/_/g, ' ')} />
              <InfoRow label="Severity" value={incident.severity} />
              <InfoRow label="Status" value={incident.status} />
              <InfoRow label="Detected"
                value={incident.detectedAt ? format(parseISO(incident.detectedAt), 'dd MMM yyyy HH:mm:ss') + ' (' + formatDistanceToNow(parseISO(incident.detectedAt), { addSuffix: true }) + ')' : '—'} />
              <div style={{ padding: '8px 0', marginTop: 4 }}>
                <span style={{ color: '#64748b', fontSize: 13 }}>Description</span>
                <p style={{ color: '#94a3b8', fontSize: 13, margin: '6px 0 0', lineHeight: 1.6 }}>{incident.anomalyDescription}</p>
              </div>
            </Card>

            {/* Anomaly details */}
            {anomaly && (
              <Card title="Anomaly Details" icon={<Zap size={16} color="#eab308" />}>
                <InfoRow label="Actual Value" value={anomaly.actualValue?.toFixed(2)} mono />
                <InfoRow label="Threshold" value={anomaly.threshold?.toFixed(2)} mono />
                <InfoRow label="Expected" value={anomaly.expectedValue?.toFixed(2)} mono />
                <InfoRow label="Window Start" value={anomaly.windowStart ? format(parseISO(anomaly.windowStart), 'HH:mm:ss') : '—'} />
                <InfoRow label="Window End" value={anomaly.windowEnd ? format(parseISO(anomaly.windowEnd), 'HH:mm:ss') : '—'} />
                {anomaly.rawLogSnapshot && (
                  <div style={{ marginTop: 10 }}>
                    <span style={{ color: '#64748b', fontSize: 12 }}>Log Snapshot</span>
                    <div style={{ background: '#0f172a', borderRadius: 6, padding: '8px 12px', marginTop: 6, fontFamily: 'monospace', fontSize: 12, color: '#ef4444' }}>
                      {anomaly.rawLogSnapshot}
                    </div>
                  </div>
                )}
              </Card>
            )}

            {/* Alert */}
            {alert && (
              <Card title="Alert Fired" icon={<CheckCircle size={16} color="#22c55e" />}>
                <InfoRow label="Status" value={alert.status} />
                <InfoRow label="Channels" value={(alert.channelsSent || []).join(', ')} />
                <InfoRow label="Sent At" value={alert.sentAt ? format(parseISO(alert.sentAt), 'dd MMM HH:mm:ss') : '—'} />
                <InfoRow label="Retry Count" value={alert.retryCount?.toString()} mono />
                <InfoRow label="Acknowledged By" value={alert.acknowledgedBy} />
              </Card>
            )}
          </div>

          {/* RIGHT column — RCA */}
          <div>
            <Card title="AI Root Cause Analysis" icon={<Brain size={16} color="#6366f1" />}>
              {incident.rootCauseSummary ? (
                <>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
                    <span style={{ background: '#172554', color: '#93c5fd', border: '1px solid #1e3a5f', borderRadius: 4, padding: '2px 8px', fontSize: 11 }}>
                      {incident.rcaGeneratedBy || 'RULE_BASED'}
                    </span>
                    {incident.rcaConfidenceScore && (
                      <span style={{ color: '#64748b', fontSize: 12 }}>
                        Confidence: {(incident.rcaConfidenceScore * 100).toFixed(0)}%
                      </span>
                    )}
                    {incident.rcaCreatedAt && (
                      <span style={{ color: '#334155', fontSize: 11, marginLeft: 'auto' }}>
                        {formatDistanceToNow(parseISO(incident.rcaCreatedAt), { addSuffix: true })}
                      </span>
                    )}
                  </div>

                  <div style={{ marginBottom: 14 }}>
                    <span style={{ color: '#64748b', fontSize: 12, display: 'block', marginBottom: 6 }}>Root Cause Summary</span>
                    <p style={{ color: '#e2e8f0', fontSize: 13, lineHeight: 1.7, margin: 0, background: '#0f172a', padding: '10px 14px', borderRadius: 6 }}>
                      {incident.rootCauseSummary}
                    </p>
                  </div>

                  {incident.aiAnalysis && (
                    <div style={{ marginBottom: 14 }}>
                      <span style={{ color: '#64748b', fontSize: 12, display: 'block', marginBottom: 6 }}>AI Analysis</span>
                      <pre style={{ color: '#94a3b8', fontSize: 12, lineHeight: 1.7, margin: 0, background: '#0f172a', padding: '10px 14px', borderRadius: 6, whiteSpace: 'pre-wrap', fontFamily: 'inherit' }}>
                        {incident.aiAnalysis}
                      </pre>
                    </div>
                  )}

                  {incident.contributingFactors?.length > 0 && (
                    <div style={{ marginBottom: 14 }}>
                      <span style={{ color: '#64748b', fontSize: 12, display: 'block', marginBottom: 6 }}>Contributing Factors</span>
                      <ul style={{ margin: 0, padding: '0 0 0 18px' }}>
                        {incident.contributingFactors.map((f, i) => (
                          <li key={i} style={{ color: '#94a3b8', fontSize: 13, marginBottom: 6, lineHeight: 1.6 }}>{f}</li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {incident.recommendations?.length > 0 && (
                    <div>
                      <span style={{ color: '#64748b', fontSize: 12, display: 'block', marginBottom: 6 }}>Recommendations</span>
                      <ol style={{ margin: 0, padding: '0 0 0 18px' }}>
                        {incident.recommendations.map((r, i) => (
                          <li key={i} style={{ color: '#94a3b8', fontSize: 13, marginBottom: 6, lineHeight: 1.6 }}>{r}</li>
                        ))}
                      </ol>
                    </div>
                  )}
                </>
              ) : (
                <div style={{ textAlign: 'center', padding: '32px 0', color: '#334155' }}>
                  <Brain size={32} color="#334155" style={{ marginBottom: 12 }} />
                  <p style={{ margin: 0, fontSize: 13 }}>RCA not yet available</p>
                  <p style={{ color: '#1e293b', fontSize: 12, marginTop: 8 }}>Analysis is generated automatically after anomaly detection</p>
                </div>
              )}
            </Card>

            {/* Timeline */}
            <Card title="Timeline" icon={<Clock size={16} color="#64748b" />}>
              {[
                { label: 'Anomaly Detected', time: incident.detectedAt, color: '#ef4444' },
                { label: 'RCA Generated', time: incident.rcaCreatedAt, color: '#6366f1' },
                { label: 'Alert Fired', time: alert?.sentAt, color: '#22c55e' },
              ].filter(e => e.time).map((event, i) => (
                <div key={i} style={{ display: 'flex', gap: 12, alignItems: 'flex-start', marginBottom: 12 }}>
                  <div style={{ width: 8, height: 8, borderRadius: '50%', background: event.color, marginTop: 5, flexShrink: 0 }} />
                  <div>
                    <div style={{ color: '#e2e8f0', fontSize: 13 }}>{event.label}</div>
                    <div style={{ color: '#475569', fontSize: 12 }}>
                      {format(parseISO(event.time), 'dd MMM yyyy HH:mm:ss')}
                    </div>
                  </div>
                </div>
              ))}
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
