import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { GrahanResponse } from '../types/api';

export function useGrahan(year = new Date().getFullYear(), tz = 5.5) {
  return useQuery<GrahanResponse>({
    queryKey: ['grahan', year, tz],
    queryFn: () => api.get<GrahanResponse>(`/api/grahan?year=${year}&tz=${tz}`),
    staleTime: 24 * 60 * 60 * 1000, // 1 day
  });
}
