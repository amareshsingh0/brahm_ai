/**
 * City lookup — fetched once from /api/cities, cached in memory.
 * searchCities() covers worldwide via /api/geocode fallback.
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

/**
 * Search cities worldwide via /api/cities/search (200K+ GeoNames DB).
 * Falls back to local 730-city cache if server unavailable.
 */
export async function searchCities(q: string): Promise<City[]> {
  if (q.length < 2) return [];
  try {
    const res = await api.get<{ results: Array<{ name: string; lat: number; lon: number; tz: number; label: string; country: string }> }>(
      `/api/cities/search?q=${encodeURIComponent(q)}&limit=10`
    );
    if (res?.results?.length) {
      return res.results.map((r) => ({
        name: r.label ?? r.name,
        lat: r.lat,
        lon: r.lon,
        tz: r.tz,
      }));
    }
  } catch { /* fallback below */ }

  // Fallback: local 730 cities
  await getCities();
  return (_cache ?? [])
    .filter((c) => c.name.toLowerCase().includes(q.toLowerCase()))
    .slice(0, 10);
}
