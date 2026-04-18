import React from 'react';
import { RefreshCw, Wifi, WifiOff } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

export default function Header({ title, lastRefresh, error, onRefresh }) {
  return (
    <header style={{
      height: 56,
      background: '#0f172a',
      borderBottom: '1px solid #1e293b',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 24px',
      flexShrink: 0,
    }}>
      <h1 style={{ color: '#f1f5f9', fontSize: 16, fontWeight: 600, margin: 0 }}>
        {title}
      </h1>

      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        {error ? (
          <span style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#f87171', fontSize: 13 }}>
            <WifiOff size={14} /> Connection error
          </span>
        ) : lastRefresh ? (
          <span style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#64748b', fontSize: 13 }}>
            <Wifi size={14} color="#22c55e" />
            Updated {formatDistanceToNow(lastRefresh, { addSuffix: true })}
          </span>
        ) : null}

        <button
          onClick={onRefresh}
          style={{
            background: '#1e293b',
            border: '1px solid #334155',
            borderRadius: 6,
            color: '#94a3b8',
            cursor: 'pointer',
            padding: '6px 10px',
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            fontSize: 13,
          }}
        >
          <RefreshCw size={13} /> Refresh
        </button>
      </div>
    </header>
  );
}
