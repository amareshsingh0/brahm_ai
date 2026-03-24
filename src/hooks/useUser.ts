import { useCallback, useEffect, useState } from 'react';
import { apiFetch } from '@/lib/apiFetch';
import type { UserProfile } from '../types/api';

export function useUser() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiFetch('/api/user')
      .then((r) => r.ok ? r.json() : null)
      .then((data) => { if (data) setProfile(data); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const saveProfile = useCallback(async (updates: Partial<UserProfile>) => {
    const merged = { ...profile, ...updates };
    const res = await apiFetch('/api/user', {
      method: 'POST',
      body: JSON.stringify(merged),
    });
    if (!res.ok) throw new Error('Failed to save profile');
    const saved = await res.json();
    setProfile(saved);
    return saved;
  }, [profile]);

  return { profile, loading, saveProfile };
}
