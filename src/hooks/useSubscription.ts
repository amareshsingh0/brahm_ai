/**
 * useSubscription — dynamic subscription + feature flag hook.
 * Fetches from GET /api/subscription which returns the admin-configured
 * plan, daily usage, and exact feature list for this user.
 */
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuthStore } from "@/store/authStore";

// ── Types ──────────────────────────────────────────────────────────────────────

export interface SubscriptionInfo {
  plan_id:               string;        // "free" | "basic" | "pro" | custom
  plan_name:             string;
  price_inr:             number;
  duration_days:         number;
  daily_message_limit:   number | null; // null = unlimited
  daily_token_limit:     number | null;
  badge_text:            string | null;
  status:                "active" | "expired" | "none";
  expires_at:            string | null;
  days_remaining:        number | null;
  messages_used_today:   number;
  tokens_used_today:     number;
  features:              string[];      // e.g. ["ai_chat", "kundali", ...]
  is_free:               boolean;
}

const FREE_FALLBACK: SubscriptionInfo = {
  plan_id:             "free",
  plan_name:           "Free",
  price_inr:           0,
  duration_days:       0,
  daily_message_limit: 0,
  daily_token_limit:   0,
  badge_text:          null,
  status:              "none",
  expires_at:          null,
  days_remaining:      null,
  messages_used_today: 0,
  tokens_used_today:   0,
  features:            ["panchang"],
  is_free:             true,
};

// ── Main hook ─────────────────────────────────────────────────────────────────

export function useSubscription() {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);

  const query = useQuery<SubscriptionInfo>({
    queryKey: ["subscription"],
    queryFn:  () => api.get<SubscriptionInfo>("/api/subscription"),
    enabled:  isLoggedIn,
    staleTime: 60 * 1000,        // 1 min — usage updates after each chat
    retry:    false,
    placeholderData: FREE_FALLBACK,
  });

  const info = query.data ?? FREE_FALLBACK;

  return {
    ...query,
    info,
    /** Check if the user has access to a specific feature key */
    hasFeature: (key: string) => info.features.includes(key),
    /** How many messages remaining today (null = unlimited) */
    messagesRemaining: info.daily_message_limit == null
      ? null
      : Math.max(0, (info.daily_message_limit ?? 0) - info.messages_used_today),
    /** 0-1 fraction of daily message quota used */
    usageFraction: (!info.daily_message_limit || info.daily_message_limit === 0)
      ? 0
      : Math.min(1, info.messages_used_today / info.daily_message_limit),
    isPaid: !info.is_free && info.status === "active",
    isExpired: info.status === "expired",
  };
}

// ── Convenience selector ──────────────────────────────────────────────────────

/** Returns true if the user has access to a specific feature. */
export function useFeatureAccess(featureKey: string): boolean {
  const { hasFeature } = useSubscription();
  return hasFeature(featureKey);
}

/** Backward-compat alias — returns { data: SubscriptionInfo } shape. */
export function useSubscriptionStatus() {
  const { info, ...rest } = useSubscription();
  return { ...rest, data: info };
}
