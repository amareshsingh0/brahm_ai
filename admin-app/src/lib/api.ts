// ── Admin API client ──────────────────────────────────────────────────────────
// All requests go through aFetch — adds X-Admin-Key header automatically.
// GET responses are cached for 5 min (module-level cache, survives re-renders).
// Any mutation (POST/PATCH/DELETE) clears the entire cache.
//
// To add a new API call: just call aFetch() from your page/component.
// Add typed helpers below as the API grows.

const BASE = (import.meta.env.VITE_API_URL ?? "https://brahmasmi.bimoraai.com/api").replace(/\/$/, "");

// ── Cache ──────────────────────────────────────────────────────────────────────
const _cache = new Map<string, { data: unknown; ts: number }>();
const CACHE_TTL = 5 * 60 * 1000; // 5 minutes

export function invalidateCache(path?: string) {
  if (path) _cache.delete(path);
  else _cache.clear();
}

// ── Core fetch ─────────────────────────────────────────────────────────────────
export async function aFetch<T = unknown>(path: string, opts: RequestInit = {}): Promise<T> {
  const key    = sessionStorage.getItem("admin-key") ?? "";
  const isGet  = !opts.method || opts.method === "GET";
  const fullPath = path.startsWith("/api") ? path : `/api${path}`;

  if (isGet) {
    const cached = _cache.get(fullPath);
    if (cached && Date.now() - cached.ts < CACHE_TTL) return cached.data as T;
  }

  const res = await fetch(`${BASE}${path.replace(/^\/api/, "")}`, {
    ...opts,
    headers: {
      "Content-Type": "application/json",
      "X-Admin-Key":  key,
      ...(opts.headers ?? {}),
    },
  });

  if (res.status === 401) {
    sessionStorage.removeItem("admin-key");
    window.location.replace("/admin/login");
    throw new Error("Unauthorized");
  }

  if (!res.ok) throw new Error(await res.text());
  const data = await res.json() as T;

  if (isGet) {
    _cache.set(fullPath, { data, ts: Date.now() });
  } else {
    _cache.clear();
  }

  return data;
}

// ── Preload all tabs on login ──────────────────────────────────────────────────
// Call once after successful login to warm the cache for instant tab switching
export function preloadAll() {
  const paths = [
    "/admin/stats",
    "/admin/users?page=1&limit=25",
    "/admin/payments?page=1&limit=30",
    "/admin/revenue",
    "/admin/chats?page=1&limit=40",
    "/admin/logs?page=1&limit=50",
  ];
  paths.forEach((p) => aFetch(p).catch(() => {}));
}
