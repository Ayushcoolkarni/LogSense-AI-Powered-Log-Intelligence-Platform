import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const TYPE_LABELS = {
  ERROR_RATE_SPIKE: 'Error Spike',
  REPEATED_ERROR_PATTERN: 'Repeated Error',
  SERVICE_UNAVAILABLE: 'Svc Unavailable',
  LOG_VOLUME_SPIKE: 'Volume Spike',
  CASCADING_FAILURE: 'Cascading Fail',
  LATENCY_ANOMALY: 'Latency',
};

export default function AnomalyTypeChart({ data = {} }) {
  const chartData = Object.entries(data).map(([name, value]) => ({
    name: TYPE_LABELS[name] || name,
    value,
  }));

  return (
    <div style={{
      background: '#1e293b', border: '1px solid #334155',
      borderRadius: 10, padding: '20px 24px',
    }}>
      <h3 style={{ color: '#f1f5f9', fontSize: 14, fontWeight: 600, margin: '0 0 20px' }}>
        Anomaly Types
      </h3>
      <ResponsiveContainer width="100%" height={200}>
        <BarChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" />
          <XAxis dataKey="name" tick={{ fill: '#64748b', fontSize: 10 }} axisLine={false} tickLine={false} />
          <YAxis tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} allowDecimals={false} />
          <Tooltip
            contentStyle={{
              background: '#0f172a', border: '1px solid #334155',
              borderRadius: 8, fontSize: 13,
            }}
          />
          <Bar dataKey="value" fill="#6366f1" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
