import { useMutation } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { BirthDetails, KundaliResponse } from '../types/api';
import { useKundliStore } from '../store/kundliStore';

interface KundaliRequestPayload extends BirthDetails {
  ayanamsha?: string;
  rahu_mode?: string;
  calc_options?: string[];
}

export function useKundali() {
  const setKundaliData = useKundliStore((s) => s.setKundaliData);
  const settings = useKundliStore((s) => s.kundaliSettings);

  return useMutation<KundaliResponse, Error, BirthDetails>({
    mutationFn: (details) => {
      const payload: KundaliRequestPayload = {
        ...details,
        ayanamsha: settings.ayanamsha,
        rahu_mode: settings.rahuMode,
        calc_options: ["antardasha"],
      };
      return api.post<KundaliResponse>('/api/kundali', payload);
    },
    onSuccess: (data) => setKundaliData(data),
  });
}
