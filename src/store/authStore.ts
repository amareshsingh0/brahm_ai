import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { zustandStorage } from '@/lib/storage';

export type AuthPlan = 'free' | 'standard' | 'premium';

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
  /** true once user has saved or explicitly skipped profile setup */
  profileSetupSeen: boolean;
  setAuth: (token: string, user: AuthUser) => void;
  setRefreshToken: (token: string) => void;
  setName: (name: string) => void;
  setProfileSetupSeen: () => void;
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
      profileSetupSeen: false,
      setAuth: (token, user) => {
        set({
          token,
          userId: user.id,
          name: user.name,
          phone: user.phone,
          plan: user.plan,
          isLoggedIn: true,
          profileSetupSeen: false, // reset on each new login so loadProfileIntoStore can re-check
        });
      },
      setRefreshToken: (token) => set({ refreshToken: token }),
      setName: (name) => set({ name }),
      setProfileSetupSeen: () => set({ profileSetupSeen: true }),
      logout: () => {
        set({
          token: null,
          refreshToken: null,
          userId: null,
          name: null,
          phone: null,
          plan: 'free',
          isLoggedIn: false,
          // profileSetupSeen intentionally NOT reset here — reset happens in setAuth on next login
        });
      },
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => zustandStorage),
    }
  )
);
