import { useEffect } from 'react';
import { usePageBotStore } from '@/store/pageBotStore';

/**
 * Call this in any page to register page context + data with the PageBot.
 * PageBot in AppLayout will automatically pick it up.
 * Pass `data` as a stable object (useMemo or useState) to avoid infinite loops.
 */
export function useRegisterPageBot(context: string, data: Record<string, unknown> = {}) {
  const { setPageBot, clearPageBot } = usePageBotStore();

  useEffect(() => {
    setPageBot(context, data);
    return () => clearPageBot();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [context, JSON.stringify(data)]);
}
