import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { KundaliResponse } from '../types/api';
import { useKundliStore, type BirthDetails } from '../store/kundliStore';
import { useAuthStore } from '../store/authStore';

async function saveKundaliToBackend(details: BirthDetails, data: KundaliResponse) {
  try {
    // Delete stale saved kundali first, then insert fresh — prevents stale cache on other devices
    await api.delete('/api/user/kundali').catch(() => {});
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
        calc_options: ["antardasha", "pratyantar", "sukshma", "upagraha", "shadbala", "ashtakavarga",
          // Core varga charts for AI analysis
          "d2","d3","d4","d5","d6","d7","d8","d9","d10","d11","d12","d16","d20","d24","d27","d30","d40","d45","d60"],
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
