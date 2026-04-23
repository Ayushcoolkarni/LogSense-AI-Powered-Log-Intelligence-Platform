import React from 'react';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const COLORS = {
  CRITICAL: '#ef4444',
  HIGH: '#f97316',
  MEDIUM: '#eab308',
  LOW: '#22c55e',
};

export default function SeverityChart({ data = {} }) {
  const chartData = Object.entries(data).map(([name, value]) => ({ name, value }));

  if (!chartData.length) {
    return (
      <div style={{
        background: '#1e293b', border: '1px solid #334155',
        borderRadius: 10, padding: '20px 24px', height: 260,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <span style={{ color: '#475569', fontSize: 13 }}>No severity data</span>
      </div>
    );
  }

  return (
    <div style={{
      background: '#1e293b', border: '1px solid #334155',
      borderRadius: 10, padding: '20px 24px',
    }}>
      <h3 style={{ color: '#f1f5f9', fontSize: 14, fontWeight: 600, margin: '0 0 4px' }}>
        Severity Breakdown
      </h3>
      <span style={{ color: '#64748b', fontSize: 12 }}>Last 24 hours</span>
      <ResponsiveContainer width="100%" height={200}>
        <PieChart>
          <Pie
            data={chartData}
            cx="50%" cy="50%"
            innerRadius={55} outerRadius={80}
            paddingAngle={3}
            dataKey="value"
          >
            {chartData.map((entry) => (
              <Cell key={entry.name} fill={COLORS[entry.name] || '#6366f1'} />
            ))}
          </Pie>
          <Tooltip
            contentStyle={{
              background: '#0f172a', border: '1px solid #334155',
              borderRadius: 8, fontSize: 13,
            }}
            labelStyle={{ color: '#f1f5f9' }}
          />
          <Legend
            formatter={(v) => <span style={{ color: '#94a3b8', fontSize: 12 }}>{v}</span>}
          />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
