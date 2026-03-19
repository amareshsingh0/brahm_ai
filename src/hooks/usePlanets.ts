import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { PlanetsResponse } from '../types/api';

export function usePlanets(lat = 28.6139, lon = 77.209, tz = 5.5) {
  return useQuery<PlanetsResponse>({
    queryKey: ['planets', lat, lon, tz],
    queryFn: () =>
      api.get<PlanetsResponse>(`/api/planets/now?lat=${lat}&lon=${lon}&tz=${tz}`),
    refetchInterval: 5 * 60 * 1000, // refetch every 5 min
    staleTime: 4 * 60 * 1000,
  });
}
