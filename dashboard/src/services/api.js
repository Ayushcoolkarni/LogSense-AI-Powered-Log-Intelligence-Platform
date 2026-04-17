import axios from 'axios';

const BASE = process.env.REACT_APP_API_URL || 'http://localhost:8085';
const INGESTION = process.env.REACT_APP_INGESTION_URL || 'http://localhost:8081';
const ANOMALY = process.env.REACT_APP_ANOMALY_URL || 'http://localhost:8082';

const api = axios.create({ baseURL: BASE, timeout: 10000 });

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

const ingestionApi = axios.create({ baseURL: INGESTION, timeout: 10000 });

export const getIngestionStats = () =>
  ingestionApi.get('/api/v1/logs/stats').then(r => r.data);

export const getLogs = (params = {}) =>
  ingestionApi.get('/api/v1/logs', { params }).then(r => r.data);

export const getLogsByTrace = (traceId) =>
  ingestionApi.get(`/api/v1/logs/trace/${traceId}`).then(r => r.data);

// ── Anomalies ──────────────────────────────────────────────────────────────

const anomalyApi = axios.create({ baseURL: ANOMALY, timeout: 10000 });

export const getAnomalyStats = () =>
  anomalyApi.get('/api/v1/anomalies/stats').then(r => r.data);

export const getAnomalies = (params = {}) =>
  anomalyApi.get('/api/v1/anomalies', { params }).then(r => r.data);

export const updateAnomalyStatus = (id, status) =>
  anomalyApi.patch(`/api/v1/anomalies/${id}/status`, null, { params: { status } }).then(r => r.data);
