import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { BirthDetails, KundaliResponse } from '../types/api';
import { useKundliStore } from '../store/kundliStore';
import { useAuthStore } from '../store/authStore';

interface KundaliRequestPayload extends BirthDetails {
  ayanamsha?: string;
  rahu_mode?: string;
  calc_options?: string[];
}

async function saveKundaliToBackend(details: BirthDetails, data: KundaliResponse) {
  try {
    await api.post('/api/user/kundali', {
      name: details.name,
      birth_date: details.dateOfBirth,
      birth_time: details.timeOfBirth,
      birth_lat: details.lat ?? 0,
      birth_lon: details.lon ?? 0,
      birth_tz: details.tz ?? 5.5,
      birth_place: details.birthPlace,
      kundali_json: JSON.stringify(data),
    });
  } catch {
    // Non-critical — local store already has data
  }
}

export function useKundali() {
  const setKundaliData = useKundliStore((s) => s.setKundaliData);
  const setBirthDetails = useKundliStore((s) => s.setBirthDetails);
  const settings = useKundliStore((s) => s.kundaliSettings);

  return useMutation<KundaliResponse, Error, BirthDetails>({
    mutationFn: (details) => {
      const payload = {
        name: details.name,
        date: details.dateOfBirth,
        time: details.timeOfBirth,
        place: details.birthPlace,
        lat: details.lat,
        lon: details.lon,
        tz: details.tz,
        ayanamsha: settings.ayanamsha,
        rahu_mode: settings.rahuMode,
        calc_options: ["antardasha", "pratyantar", "sukshma", "upagraha", "shadbala", "ashtakavarga"],
      };
      return api.post<KundaliResponse>('/api/kundali', payload);
    },
    onSuccess: (data, details) => {
      setKundaliData(data);
      setBirthDetails(details);
      saveKundaliToBackend(details, data);
    },
  });
}

/** Load saved kundali from backend on page mount (runs once per session). */
export function useSavedKundali() {
  const setKundaliData = useKundliStore((s) => s.setKundaliData);
  const kundaliData = useKundliStore((s) => s.kundaliData);
  const isLoggedIn = useAuthStore((s) => !!s.userId);

  return useQuery({
    queryKey: ['saved-kundali'],
    queryFn: async () => {
      const res = await api.get<{ found: boolean; kundali: Record<string, unknown> | null }>('/api/user/kundali');
      if (res.found && res.kundali?.kundali_json) {
        try {
          const data = JSON.parse(res.kundali.kundali_json as string) as KundaliResponse;
          setKundaliData(data);
          return data;
        } catch {}
      }
      return null;
    },
    enabled: isLoggedIn && !kundaliData,  // Only fetch if we don't already have data
    staleTime: Infinity,                   // Don't refetch — user must explicitly regenerate
    retry: false,
  });
}
