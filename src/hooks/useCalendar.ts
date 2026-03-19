import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { MonthlyCalendarResponse } from '../types/api';

interface CalendarParams {
  year: number;
  month: number;
  lat?: number;
  lon?: number;
  tz?: number;
  tradition?: string;
  lunar_system?: string;
}

export function useCalendar({
  year, month,
  lat = 28.6139, lon = 77.209, tz = 5.5,
  tradition = 'smarta', lunar_system = 'amanta',
}: CalendarParams) {
  return useQuery<MonthlyCalendarResponse>({
    queryKey: ['calendar', year, month, lat, lon, tz, tradition, lunar_system],
    queryFn: () =>
      api.get<MonthlyCalendarResponse>(
        `/api/calendar/month?year=${year}&month=${month}&lat=${lat}&lon=${lon}&tz=${tz}&tradition=${tradition}&lunar_system=${lunar_system}`
      ),
    staleTime: 24 * 60 * 60 * 1000,  // cache 1 day
    retry: 2,
  });
}
