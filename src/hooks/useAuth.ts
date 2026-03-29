import { useAuthStore, type AuthUser } from '@/store/authStore';
import { useKundliStore } from '@/store/kundliStore';
import { apiFetch } from '@/lib/apiFetch';

const API = "/api";

/** After login, load saved profile + kundali JSON from backend in parallel */
async function loadProfileIntoStore(token: string) {
  try {
    const headers = { Authorization: `Bearer ${token}` };
    const [profileRes, kundaliRes] = await Promise.all([
      fetch(`${API}/user`, { headers }),
      fetch(`${API}/user/kundali`, { headers }),
    ]);

    if (profileRes.ok) {
      const data = await profileRes.json();
      const hasBirth = data.date && data.place;
      if (hasBirth) {
        const { setBirthDetails } = useKundliStore.getState();
        setBirthDetails({
          name:        data.name || '',
          dateOfBirth: data.date,
          timeOfBirth: data.time || '',
          birthPlace:  data.place,
          lat:         data.lat,
          lon:         data.lon,
          tz:          data.tz,
        });
        useAuthStore.getState().setProfileSetupSeen();
      }
      if (data.name) useAuthStore.getState().setName(data.name);
    }

    // Pre-populate kundali so KundliPage renders instantly (no second fetch needed)
    if (kundaliRes.ok) {
      const kundaliData = await kundaliRes.json();
      if (kundaliData.found && kundaliData.kundali?.kundali_json) {
        try {
          const parsed = JSON.parse(kundaliData.kundali.kundali_json);
          useKundliStore.getState().setKundaliData(parsed);
        } catch {}
      }
    }
  } catch {
    // Silently ignore — non-critical
  }
}

export const useAuth = () => {
  const { setAuth, setRefreshToken, logout: storeLogout, ...user } = useAuthStore();

  // ── Phone OTP ────────────────────────────────────────────────
  const sendOtp = async (phone: string) => {
    let res: Response;
    try {
      res = await fetch(`${API}/auth/otp/send`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ phone: `+91${phone}`, purpose: "login" }),
      });
    } catch {
      throw new Error("Unable to connect. Please check your internet connection.");
    }
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail || "Failed to send OTP. Please try again.");
    }
    return res.json();
  };

  const verifyOtp = async (phone: string, otp: string) => {
    let res: Response;
    try {
      res = await fetch(`${API}/auth/otp/verify`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ phone: `+91${phone}`, otp, purpose: "login" }),
      });
    } catch {
      throw new Error("Unable to connect. Please check your internet connection.");
    }
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail || "Invalid OTP. Please try again.");
    }
    const data = await res.json();
    const authUser: AuthUser = {
      id:    data.user_id,
      name:  data.name,
      phone: data.phone,
      plan:  data.plan,
    };
    setAuth(data.access_token, authUser);
    setRefreshToken(data.refresh_token);
    // Load existing birth profile in background
    loadProfileIntoStore(data.access_token);
    return { token: data.access_token, user: authUser };
  };

  // ── Google OAuth ─────────────────────────────────────────────
  const googleLogin = async (idToken: string) => {
    const res = await apiFetch(`${API}/auth/google`, {
      method: "POST",
      body: JSON.stringify({ id_token: idToken, device_type: "web" }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail || "Google login failed");
    }
    const data = await res.json();
    const authUser: AuthUser = {
      id:    data.user_id,
      name:  data.name,
      phone: data.phone,
      plan:  data.plan,
    };
    setAuth(data.access_token, authUser);
    setRefreshToken(data.refresh_token);
    // Load existing birth profile in background
    loadProfileIntoStore(data.access_token);
    return { token: data.access_token, user: authUser };
  };

  // ── Logout ───────────────────────────────────────────────────
  const logout = async () => {
    const refreshToken = useAuthStore.getState().refreshToken;
    if (refreshToken) {
      fetch(`${API}/auth/logout`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refresh_token: refreshToken }),
      }).catch(() => {});
    }
    // Clear kundli store on logout
    useKundliStore.getState().clearKundaliData();
    useKundliStore.getState().setBirthDetails({ name: '', dateOfBirth: '', timeOfBirth: '', birthPlace: '' });
    useKundliStore.getState().setHasKundli(false);
    storeLogout();
  };

  return {
    ...user,
    sendOtp,
    verifyOtp,
    googleLogin,
    logout,
  };
};
