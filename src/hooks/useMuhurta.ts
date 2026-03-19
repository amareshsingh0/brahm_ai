import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { MuhurtaResponse } from '../types/api';

export function useMuhurta(event = 'general', date?: string, lat = 28.6139, lon = 77.209, tz = 5.5) {
  const queryDate = date ?? new Date().toISOString().split('T')[0];

  return useQuery<MuhurtaResponse>({
    queryKey: ['muhurta', event, queryDate, lat, lon, tz],
    queryFn: () =>
      api.get<MuhurtaResponse>(
        `/api/muhurta?event=${encodeURIComponent(event)}&date=${queryDate}&lat=${lat}&lon=${lon}&tz=${tz}`
      ),
    staleTime: 5 * 60 * 1000,
  });
}
