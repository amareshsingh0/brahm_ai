/**
 * City lookup — fetched once from /api/cities, cached in memory.
 * Single source of truth for city → lat/lon/tz mapping.
 */
import { api } from './api';
import type { City, CitiesResponse } from '../types/api';
export type { City };

let _cache: City[] | null = null;

export async function getCities(): Promise<City[]> {
  if (_cache) return _cache;
  const res = await api.get<CitiesResponse>('/api/cities');
  _cache = res.cities;
  return _cache;
}

export function getCityByName(name: string): City | undefined {
  return _cache?.find((c) => c.name.toLowerCase() === name.toLowerCase());
}

export function clearCitiesCache(): void {
  _cache = null;
}
