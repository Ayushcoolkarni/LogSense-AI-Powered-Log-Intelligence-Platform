import React from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Cell,
} from 'recharts';

const GRADIENT_COLORS = ['#6366f1', '#818cf8', '#a5b4fc', '#c7d2fe', '#e0e7ff'];

export default function ServiceBarChart({ data = {} }) {
  const chartData = Object.entries(data)
    .map(([name, value]) => ({ name: name.replace(/-service$/, ''), value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 8);

  return (
    <div style={{
      background: '#1e293b', border: '1px solid #334155',
      borderRadius: 10, padding: '20px 24px',
    }}>
      <h3 style={{ color: '#f1f5f9', fontSize: 14, fontWeight: 600, margin: '0 0 20px' }}>
        Open Incidents by Service
      </h3>
      <ResponsiveContainer width="100%" height={220}>
        <BarChart data={chartData} layout="vertical" margin={{ left: 10, right: 20 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#1e3a5f" horizontal={false} />
          <XAxis type="number" tick={{ fill: '#64748b', fontSize: 11 }} axisLine={false} tickLine={false} allowDecimals={false} />
          <YAxis type="category" dataKey="name" tick={{ fill: '#94a3b8', fontSize: 12 }} axisLine={false} tickLine={false} width={110} />
          <Tooltip
            contentStyle={{
              background: '#0f172a', border: '1px solid #334155',
              borderRadius: 8, fontSize: 13,
            }}
            cursor={{ fill: '#1e3a5f' }}
          />
          <Bar dataKey="value" radius={[0, 4, 4, 0]}>
            {chartData.map((_, i) => (
              <Cell key={i} fill={GRADIENT_COLORS[i % GRADIENT_COLORS.length]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
