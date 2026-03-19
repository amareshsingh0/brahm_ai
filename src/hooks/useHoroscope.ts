import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { HoroscopeResponse } from '../types/api';

export function useHoroscope(rashi: string, period = 'daily') {
  return useQuery<HoroscopeResponse>({
    queryKey: ['horoscope', rashi, period],
    queryFn: () => api.get<HoroscopeResponse>(`/api/horoscope/${rashi}?period=${period}`),
    enabled: Boolean(rashi),
    staleTime: 60 * 60 * 1000, // 1 hour
  });
}
