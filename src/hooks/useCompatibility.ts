import { useMutation } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { BirthDetails, CompatibilityResponse } from '../types/api';

interface CompatibilityRequest {
  person_a: BirthDetails;
  person_b: BirthDetails;
  varna_system?: 'nakshatra' | 'rashi' | 'both';
}

export function useCompatibility() {
  return useMutation<CompatibilityResponse, Error, CompatibilityRequest>({
    mutationFn: (req) => api.post<CompatibilityResponse>('/api/compatibility', req),
  });
}
