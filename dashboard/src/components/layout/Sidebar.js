import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard, AlertTriangle, FileSearch,
  Activity, Bell, Settings, ChevronLeft, ChevronRight,
  Zap
} from 'lucide-react';

const NAV = [
  { to: '/',          icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/incidents', icon: AlertTriangle,   label: 'Incidents'  },
  { to: '/anomalies', icon: Activity,        label: 'Anomalies'  },
  { to: '/logs',      icon: FileSearch,      label: 'Log Search' },
  { to: '/alerts',    icon: Bell,            label: 'Alerts'     },
];

export default function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <aside style={{
      width: collapsed ? 64 : 220,
      minHeight: '100vh',
      background: '#0f172a',
      borderRight: '1px solid #1e293b',
      display: 'flex',
      flexDirection: 'column',
      transition: 'width 0.2s',
      flexShrink: 0,
    }}>
      {/* Logo */}
      <div style={{
        padding: collapsed ? '20px 0' : '20px 16px',
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        borderBottom: '1px solid #1e293b',
        justifyContent: collapsed ? 'center' : 'flex-start',
      }}>
        <Zap size={22} color="#6366f1" />
        {!collapsed && (
          <span style={{ color: '#f1f5f9', fontWeight: 700, fontSize: 15 }}>
            LogIntel
          </span>
        )}
      </div>

      {/* Nav links */}
      <nav style={{ flex: 1, padding: '12px 0' }}>
        {NAV.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              padding: collapsed ? '11px 0' : '11px 16px',
              justifyContent: collapsed ? 'center' : 'flex-start',
              color: isActive ? '#6366f1' : '#94a3b8',
              background: isActive ? '#1e293b' : 'transparent',
              textDecoration: 'none',
              fontSize: 14,
              fontWeight: isActive ? 600 : 400,
              borderLeft: isActive ? '3px solid #6366f1' : '3px solid transparent',
              transition: 'all 0.15s',
            })}
          >
            <Icon size={18} />
            {!collapsed && label}
          </NavLink>
        ))}
      </nav>

      {/* Collapse toggle */}
      <button
        onClick={() => setCollapsed(c => !c)}
        style={{
          background: 'none',
          border: 'none',
          color: '#475569',
          cursor: 'pointer',
          padding: '14px',
          display: 'flex',
          justifyContent: collapsed ? 'center' : 'flex-end',
          borderTop: '1px solid #1e293b',
        }}
      >
        {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
      </button>
    </aside>
  );
}
