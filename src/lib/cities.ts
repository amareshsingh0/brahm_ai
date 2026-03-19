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
 * Search cities worldwide:
 * 1. Local cities.json (730 Indian cities) — instant
 * 2. /api/geocode (Nominatim/OSM) — worldwide fallback if < 2 local matches
 */
export async function searchCities(q: string): Promise<City[]> {
  if (q.length < 2) return [];
  await getCities(); // ensure cache loaded
  const local = (_cache ?? [])
    .filter((c) => c.name.toLowerCase().includes(q.toLowerCase()))
    .slice(0, 6);
  if (local.length >= 2) return local;
  try {
    const res = await api.get<City>(`/api/geocode?q=${encodeURIComponent(q)}`);
    if (res?.lat) {
      const geocoded: City = { name: q, lat: res.lat, lon: res.lon, tz: res.tz };
      return [...local, geocoded].slice(0, 6);
    }
  } catch { /* fallback to local results */ }
  return local;
}
