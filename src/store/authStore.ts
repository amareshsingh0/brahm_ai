import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { zustandStorage } from '@/lib/storage';

export type AuthPlan = 'free' | 'jyotishi' | 'acharya';

export interface AuthUser {
  id: string;
  name: string;
  phone: string;
  plan: AuthPlan;
}

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  userId: string | null;
  name: string | null;
  phone: string | null;
  plan: AuthPlan;
  isLoggedIn: boolean;
  setAuth: (token: string, user: AuthUser) => void;
  setRefreshToken: (token: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      refreshToken: null,
      userId: null,
      name: null,
      phone: null,
      plan: 'free',
      isLoggedIn: false,
      setAuth: (token, user) => {
        set({
          token,
          userId: user.id,
          name: user.name,
          phone: user.phone,
          plan: user.plan,
          isLoggedIn: true,
        });
      },
      setRefreshToken: (token) => set({ refreshToken: token }),
      logout: () => {
        set({
          token: null,
          refreshToken: null,
          userId: null,
          name: null,
          phone: null,
          plan: 'free',
          isLoggedIn: false,
        });
      },
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => zustandStorage),
    }
  )
);
