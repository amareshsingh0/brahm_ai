import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { api } from '../lib/api';
import type { PanchangResponse } from '../types/api';

interface PanchangParams {
  date?: string;  // YYYY-MM-DD, defaults to today
  lat?: number;
  lon?: number;
  tz?: number;
}

function todayString() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function usePanchang(params: PanchangParams = {}) {
  const { date, lat = 28.6139, lon = 77.209, tz = 5.5 } = params;
  const queryDate = date ?? todayString();
  const queryClient = useQueryClient();

  // Invalidate at midnight so data refreshes for the new day
  useEffect(() => {
    if (date) return; // skip if caller passed explicit date
    const now = new Date();
    const msUntilMidnight =
      new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1).getTime() - now.getTime();
    const t = setTimeout(() => {
      queryClient.invalidateQueries({ queryKey: ['panchang'] });
    }, msUntilMidnight);
    return () => clearTimeout(t);
  }, [queryDate, queryClient, date]);

  return useQuery<PanchangResponse>({
    queryKey: ['panchang', queryDate, lat, lon, tz],
    queryFn: () =>
      api.get<PanchangResponse>(
        `/api/panchang?date=${queryDate}&lat=${lat}&lon=${lon}&tz=${tz}`
      ),
    staleTime: 6 * 60 * 60 * 1000,  // 6 hours — panchang is daily data
    gcTime:    6 * 60 * 60 * 1000,
    refetchInterval: false,
    refetchOnWindowFocus: false,
  });
}
