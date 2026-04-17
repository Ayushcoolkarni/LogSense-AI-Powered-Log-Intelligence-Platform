import React from 'react';
import { useDashboard } from '../../hooks/useDashboard';
import { AlertTriangle, Activity, CheckCircle, Zap, LogIn } from 'lucide-react';
import Header from '../layout/Header';
import StatCard from '../layout/StatCard';
import IncidentTimeline from '../charts/IncidentTimeline';
import SeverityChart from '../charts/SeverityChart';
import ServiceBarChart from '../charts/ServiceBarChart';
import AnomalyTypeChart from '../charts/AnomalyTypeChart';
import IncidentFeed from '../charts/IncidentFeed';

export default function DashboardPage() {
  const { stats, ingestion, loading, error, lastRefresh, refresh } = useDashboard(15000);

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
      <Header title="Dashboard" lastRefresh={lastRefresh} error={error} onRefresh={refresh} />

      <div style={{ flex: 1, overflowY: 'auto', padding: 24, background: '#0f172a' }}>
        {loading && !stats ? (
          <div style={{ color: '#64748b', textAlign: 'center', padding: 60, fontSize: 14 }}>
            Loading dashboard…
          </div>
        ) : (
          <>
            {/* KPI Row */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16, marginBottom: 24 }}>
              <StatCard
                icon={AlertTriangle} label="Open Incidents"
                value={stats?.openIncidents ?? 0}
                sub="Active right now"
                color="#f97316"
              />
              <StatCard
                icon={Zap} label="Critical (24h)"
                value={stats?.criticalIncidents24h ?? 0}
                sub="Last 24 hours"
                color="#ef4444"
              />
              <StatCard
                icon={Activity} label="High Severity (24h)"
                value={stats?.highIncidents24h ?? 0}
                sub="Last 24 hours"
                color="#eab308"
              />
              <StatCard
                icon={CheckCircle} label="Resolved"
                value={stats?.resolvedIncidents ?? 0}
                sub="Total resolved"
                color="#22c55e"
              />
              {ingestion && (
                <StatCard
                  icon={LogIn} label="Logs / min"
                  value={ingestion.avgIngestionRatePerMinute?.toFixed(1) ?? '—'}
                  sub={`${ingestion.totalLogsIngested?.toLocaleString() ?? 0} total`}
                  color="#6366f1"
                />
              )}
            </div>

            {/* Timeline + Severity row */}
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 16, marginBottom: 16 }}>
              <IncidentTimeline data={stats?.hourlyTimeline || []} />
              <SeverityChart data={stats?.incidentsBySeverity ?? {}} />
            </div>

            {/* Service + Type row */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
              <ServiceBarChart data={stats?.incidentsByService ?? {}} />
              <AnomalyTypeChart data={stats?.incidentsByType ?? {}} />
            </div>

            {/* Recent feed */}
            <IncidentFeed incidents={stats?.recentIncidents ?? []} />
          </>
        )}
      </div>
    </div>
  );
}
