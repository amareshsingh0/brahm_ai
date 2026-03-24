import { useAuthStore, type AuthUser } from '@/store/authStore';

const API = "/api";

export const useAuth = () => {
  const { setAuth, setRefreshToken, logout: storeLogout, ...user } = useAuthStore();

  // ── Phone OTP ────────────────────────────────────────────────
  const sendOtp = async (phone: string) => {
    const res = await fetch(`${API}/auth/otp/send`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ phone: `+91${phone}`, purpose: "login" }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail || "Failed to send OTP");
    }
    return res.json();
  };

  const verifyOtp = async (phone: string, otp: string) => {
    const res = await fetch(`${API}/auth/otp/verify`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ phone: `+91${phone}`, otp, purpose: "login" }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.detail || "Invalid OTP");
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
    return { token: data.access_token, user: authUser };
  };

  // ── Google OAuth ─────────────────────────────────────────────
  const googleLogin = async (idToken: string) => {
    const res = await fetch(`${API}/auth/google`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
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
    return { token: data.access_token, user: authUser };
  };

  // ── Refresh access token ─────────────────────────────────────
  const refreshAccessToken = async () => {
    const refreshToken = useAuthStore.getState().refreshToken;
    if (!refreshToken) throw new Error("No refresh token");
    const res = await fetch(`${API}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refresh_token: refreshToken }),
    });
    if (!res.ok) {
      storeLogout();
      throw new Error("Session expired. Please login again.");
    }
    const data = await res.json();
    useAuthStore.getState().setAuth(data.access_token, {
      id:    useAuthStore.getState().userId!,
      name:  useAuthStore.getState().name!,
      phone: useAuthStore.getState().phone!,
      plan:  useAuthStore.getState().plan,
    });
    return data.access_token;
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
    storeLogout();
  };

  return {
    ...user,
    sendOtp,
    verifyOtp,
    googleLogin,
    refreshAccessToken,
    logout,
  };
};
