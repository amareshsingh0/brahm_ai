import { useCallback, useEffect, useState } from 'react';
import { api } from '../lib/api';
import type { UserProfile } from '../types/api';

const SESSION_KEY = 'brahm_session_id';

function getOrCreateSessionId(): string {
  let id = localStorage.getItem(SESSION_KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(SESSION_KEY, id);
  }
  return id;
}

export function useUser() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const sid = getOrCreateSessionId();
    api.get<UserProfile>(`/api/user?session_id=${sid}`)
      .then(setProfile)
      .catch(() => setProfile({ session_id: sid }))
      .finally(() => setLoading(false));
  }, []);

  const saveProfile = useCallback(async (updates: Partial<UserProfile>) => {
    const sid = getOrCreateSessionId();
    const merged = { session_id: sid, ...profile, ...updates };
    const saved = await api.post<UserProfile>('/api/user', merged);
    setProfile(saved);
    return saved;
  }, [profile]);

  return { profile, loading, saveProfile };
}
