import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { SearchResponse } from '../types/api';

export function useSearch(q: string, lang = 'all', limit = 10) {
  return useQuery<SearchResponse>({
    queryKey: ['search', q, lang, limit],
    queryFn: () =>
      api.get<SearchResponse>(
        `/api/search?q=${encodeURIComponent(q)}&lang=${lang}&limit=${limit}`
      ),
    enabled: q.trim().length > 2,
    staleTime: 30 * 1000, // 30 seconds
  });
}
