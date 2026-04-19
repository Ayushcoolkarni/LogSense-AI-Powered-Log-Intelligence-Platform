import React from 'react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer,
} from 'recharts';
import { format, parseISO } from 'date-fns';

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{
      background: '#0f172a', border: '1px solid #334155',
      borderRadius: 8, padding: '10px 14px',
    }}>
      <p style={{ color: '#94a3b8', fontSize: 12, margin: '0 0 4px' }}>
        {label}
      </p>
      <p style={{ color: '#818cf8', fontWeight: 700, margin: 0 }}>
        {payload[0].value} incidents
      </p>
    </div>
  );
};

export default function IncidentTimeline({ data = [] }) {
  const formatted = data.map(d => ({
    ...d,
    label: (() => {
      try { return format(parseISO(d.hour), 'HH:mm'); } catch { return d.hour; }
    })(),
  }));

  return (
    <div style={{
      background: '#1e293b', border: '1px solid #334155',
      borderRadius: 10, padding: '20px 24px',
    }}>
      <h3 style={{ color: '#f1f5f9', fontSize: 14, fontWeight: 600, margin: '0 0 20px' }}>
        Incidents — Last 24 Hours
      </h3>
      <ResponsiveContainer width="100%" height={200}>
        <AreaChart data={formatted}>
          <defs>
            <linearGradient id="incidentGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
              <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" />
          <XAxis dataKey="label" tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} />
          <YAxis tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} allowDecimals={false} />
          <Tooltip content={<CustomTooltip />} />
          <Area
            type="monotone" dataKey="count"
            stroke="#6366f1" strokeWidth={2}
            fill="url(#incidentGrad)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
