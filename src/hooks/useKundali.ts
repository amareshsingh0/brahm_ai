import { useMutation } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { BirthDetails, KundaliResponse } from '../types/api';
import { useKundliStore } from '../store/kundliStore';

export function useKundali() {
  const setKundaliData = useKundliStore((s) => s.setKundaliData);

  return useMutation<KundaliResponse, Error, BirthDetails>({
    mutationFn: (details) => api.post<KundaliResponse>('/api/kundali', details),
    onSuccess: (data) => setKundaliData(data),
  });
}
