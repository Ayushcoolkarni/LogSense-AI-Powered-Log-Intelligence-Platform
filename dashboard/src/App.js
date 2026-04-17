import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './components/layout/Sidebar';
import DashboardPage from './components/pages/DashboardPage';
import IncidentsPage from './components/pages/IncidentsPage';
import IncidentDetailPage from './components/pages/IncidentDetailPage';
import AnomaliesPage from './components/pages/AnomaliesPage';
import LogSearchPage from './components/pages/LogSearchPage';

function AlertsPage() {
  return (
    <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#0f172a' }}>
      <div style={{ textAlign: 'center', color: '#475569' }}>
        <p style={{ fontSize: 16, marginBottom: 8 }}>Alerts Page</p>
        <p style={{ fontSize: 13 }}>Connect to alert-service on :8084/api/v1/alerts</p>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <div style={{
        display: 'flex',
        height: '100vh',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        background: '#0f172a',
        overflow: 'hidden',
      }}>
        <Sidebar />
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, overflow: 'hidden' }}>
          <Routes>
            <Route path="/"               element={<DashboardPage />} />
            <Route path="/incidents"      element={<IncidentsPage />} />
            <Route path="/incidents/:anomalyId" element={<IncidentDetailPage />} />
            <Route path="/anomalies"      element={<AnomaliesPage />} />
            <Route path="/logs"           element={<LogSearchPage />} />
            <Route path="/alerts"         element={<AlertsPage />} />
            <Route path="*"              element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </div>
    </BrowserRouter>
  );
}
