import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { FestivalsResponse } from '../types/api';

export function useFestivals(year: number, lat = 28.6139, lon = 77.209, tz = 5.5) {
  return useQuery<FestivalsResponse>({
    queryKey: ['festivals', year, lat, lon, tz],
    queryFn: () =>
      api.get<FestivalsResponse>(
        `/api/festivals?year=${year}&lat=${lat}&lon=${lon}&tz=${tz}`
      ),
    staleTime: 24 * 60 * 60 * 1000, // cache 1 day — festivals don't change intraday
    retry: 2,
  });
}
