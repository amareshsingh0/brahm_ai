import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { PanchangResponse } from '../types/api';

interface PanchangParams {
  date?: string;  // YYYY-MM-DD, defaults to today
  lat?: number;
  lon?: number;
  tz?: number;
}

export function usePanchang(params: PanchangParams = {}) {
  const { date, lat = 28.6139, lon = 77.209, tz = 5.5 } = params;
  const queryDate = date ?? (() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  })();

  return useQuery<PanchangResponse>({
    queryKey: ['panchang', queryDate, lat, lon, tz],
    queryFn: () =>
      api.get<PanchangResponse>(
        `/api/panchang?date=${queryDate}&lat=${lat}&lon=${lon}&tz=${tz}`
      ),
    staleTime: 5 * 60 * 1000,
    refetchInterval: 5 * 60 * 1000, // auto-refetch every 5 min
  });
}
