import axios from 'axios';

// All calls go through nginx on port 3000 — nginx proxies to the correct service
const BASE    = process.env.REACT_APP_API_URL      || '';   // same origin → nginx
const INGESTION = process.env.REACT_APP_INGESTION_URL || '';
const ANOMALY   = process.env.REACT_APP_ANOMALY_URL   || '';

const api         = axios.create({ baseURL: BASE,      timeout: 15000 });
const ingestionApi = axios.create({ baseURL: INGESTION, timeout: 15000 });
const anomalyApi   = axios.create({ baseURL: ANOMALY,   timeout: 15000 });

// ── Dashboard / Incidents ──────────────────────────────────────────────────

export const getDashboardStats = () =>
  api.get('/api/v1/incidents/dashboard').then(r => r.data);

export const getIncidents = (params = {}) =>
  api.get('/api/v1/incidents', { params }).then(r => r.data);

export const getIncidentById = (id) =>
  api.get(`/api/v1/incidents/${id}`).then(r => r.data);

export const getIncidentByAnomalyId = (anomalyId) =>
  api.get(`/api/v1/incidents/anomaly/${anomalyId}`).then(r => r.data);

export const updateIncidentStatus = (id, status) =>
  api.patch(`/api/v1/incidents/${id}/status`, null, { params: { status } }).then(r => r.data);

// ── Logs ───────────────────────────────────────────────────────────────────

export const getIngestionStats = () =>
  ingestionApi.get('/api/v1/logs/stats').then(r => r.data?.data ?? r.data);

export const getLogs = (params = {}) =>
  ingestionApi.get('/api/v1/logs', { params }).then(r => r.data?.data ?? r.data);

export const getLogsByTrace = (traceId) =>
  ingestionApi.get(`/api/v1/logs/trace/${traceId}`).then(r => r.data?.data ?? r.data);

// ── Anomalies ──────────────────────────────────────────────────────────────

export const getAnomalyStats = () =>
  anomalyApi.get('/api/v1/anomalies/stats').then(r => r.data);

export const getAnomalies = (params = {}) =>
  anomalyApi.get('/api/v1/anomalies', { params }).then(r => r.data);

export const updateAnomalyStatus = (id, status) =>
  anomalyApi.patch(`/api/v1/anomalies/${id}/status`, null, { params: { status } }).then(r => r.data);

// ── Alerts ─────────────────────────────────────────────────────────────────

export const getAlerts = (params = {}) =>
  axios.get('/api/v1/alerts', { params, timeout: 15000 }).then(r => r.data);

export const acknowledgeAlert = (id) =>
  axios.post(`/api/v1/alerts/${id}/acknowledge`, null, { timeout: 15000 }).then(r => r.data);
