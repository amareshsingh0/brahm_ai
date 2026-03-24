/**
 * apiFetch — drop-in replacement for fetch()
 * - Automatically attaches Bearer token from authStore
 * - On 401: tries to refresh access token once, then retries
 * - On refresh failure: logs user out
 */
import { useAuthStore } from "@/store/authStore";

const API = "/api";

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = useAuthStore.getState().refreshToken;
  if (!refreshToken) return null;

  const res = await fetch(`${API}/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refresh_token: refreshToken }),
  });

  if (!res.ok) {
    useAuthStore.getState().logout();
    return null;
  }

  const data = await res.json();
  const state = useAuthStore.getState();
  state.setAuth(data.access_token, {
    id:    state.userId!,
    name:  state.name!,
    phone: state.phone!,
    plan:  state.plan,
  });
  return data.access_token;
}

export async function apiFetch(
  input: string,
  init: RequestInit = {}
): Promise<Response> {
  const token = useAuthStore.getState().token;

  const headers = new Headers(init.headers);
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (!headers.has("Content-Type") && !(init.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  let res = await fetch(input, { ...init, headers });

  // 401 — try refresh once
  if (res.status === 401) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      headers.set("Authorization", `Bearer ${newToken}`);
      res = await fetch(input, { ...init, headers });
    }
  }

  return res;
}
