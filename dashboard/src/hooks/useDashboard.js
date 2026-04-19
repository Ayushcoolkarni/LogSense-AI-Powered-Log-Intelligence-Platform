import { useState, useEffect, useCallback } from 'react';
import { getDashboardStats, getIngestionStats } from '../services/api';

export function useDashboard(refreshInterval = 15000) {
  const [stats, setStats] = useState(null);
  const [ingestion, setIngestion] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastRefresh, setLastRefresh] = useState(null);

  const fetch = useCallback(async () => {
    try {
      const [dashData, ingData] = await Promise.allSettled([
        getDashboardStats(),
        getIngestionStats(),
      ]);
      if (dashData.status === 'fulfilled') setStats(dashData.value);
      if (ingData.status === 'fulfilled') setIngestion(ingData.value);
      setError(null);
      setLastRefresh(new Date());
    } catch (e) {
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetch();
    const timer = setInterval(fetch, refreshInterval);
    return () => clearInterval(timer);
  }, [fetch, refreshInterval]);

  return { stats, ingestion, loading, error, lastRefresh, refresh: fetch };
}

export function useIncidents(params = {}, refreshInterval = 30000) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const paramKey = JSON.stringify(params);

  useEffect(() => {
    let active = true;
    const { getIncidents } = require('../services/api');

    const load = async () => {
      setLoading(true);
      try {
        const result = await getIncidents(params);
        if (active) { setData(result); setError(null); }
      } catch (e) {
        if (active) setError('Failed to load incidents');
      } finally {
        if (active) setLoading(false);
      }
    };

    load();
    const timer = setInterval(load, refreshInterval);
    return () => { active = false; clearInterval(timer); };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [paramKey]);

  return { data, loading, error };
}
