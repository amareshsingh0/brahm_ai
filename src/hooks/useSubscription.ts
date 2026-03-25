import { useQuery, useMutation } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuthStore } from "@/store/authStore";

export interface Plan {
  id: "free" | "standard" | "premium";
  name: string;
  name_hi: string;
  price_monthly: number;
  price_yearly: number;
  currency: string;
  features: string[];
  limits: { ai_chat_daily: number; kundali_saves: number };
}

export interface SubscriptionStatus {
  plan: "free" | "standard" | "premium";
  status: "active" | "cancelled" | "expired";
  started_at: string | null;
  expires_at: string | null;
  period: "monthly" | "yearly" | null;
}

export interface CheckoutResponse {
  payment_session_id: string;
  order_id: string;
  payment_url: string;
}

const PLAN_ORDER: Record<string, number> = { free: 0, standard: 1, premium: 2 };

export function usePlans() {
  return useQuery<Plan[]>({
    queryKey: ["subscription", "plans"],
    queryFn: () => api.get<{ plans: Plan[] }>("/api/subscription/plans").then((r) => r.plans),
    staleTime: 60 * 60 * 1000,
    placeholderData: [
      {
        id: "free",
        name: "Free",
        name_hi: "निःशुल्क",
        price_monthly: 0,
        price_yearly: 0,
        currency: "INR",
        features: [
          "Daily Horoscope",
          "Today's Panchang",
          "Festival Calendar",
          "Palmistry AI Analysis",
          "Basic Kundali (view only)",
          "5 AI Chat messages / day",
        ],
        limits: { ai_chat_daily: 5, kundali_saves: 1 },
      },
      {
        id: "standard",
        name: "Standard",
        name_hi: "मानक",
        price_monthly: 199,
        price_yearly: 1999,
        currency: "INR",
        features: [
          "Everything in Free, plus:",
          "Unlimited AI Chat",
          "Full Kundali + All 7 Tabs",
          "Gochar Transits",
          "Compatibility Analysis",
          "Muhurta Finder",
          "Save unlimited charts",
        ],
        limits: { ai_chat_daily: -1, kundali_saves: -1 },
      },
      {
        id: "premium",
        name: "Premium",
        name_hi: "प्रीमियम",
        price_monthly: 399,
        price_yearly: 3999,
        currency: "INR",
        features: [
          "Everything in Standard, plus:",
          "Gemstone Recommendations",
          "Dosha + Sade Sati Reports",
          "Varshphal Annual Chart",
          "Prashna Kundali",
          "KP System",
          "Vedic Scripture Library",
          "Priority GPU inference",
        ],
        limits: { ai_chat_daily: -1, kundali_saves: -1 },
      },
    ],
  });
}

export function useSubscriptionStatus() {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn);
  return useQuery<SubscriptionStatus>({
    queryKey: ["subscription", "status"],
    queryFn: () => api.get<SubscriptionStatus>("/api/subscription/status"),
    enabled: isLoggedIn,
    staleTime: 30 * 1000,
    retry: false,
    placeholderData: { plan: "free", status: "active", started_at: null, expires_at: null, period: null },
  });
}

export function useCheckout() {
  return useMutation({
    mutationFn: ({ plan, period }: { plan: string; period: "monthly" | "yearly" }) =>
      api.post<CheckoutResponse>("/api/subscription/checkout", { plan, period }),
  });
}

/** Returns true if current plan meets the minimum required plan. */
export function usePlanCheck(minPlan: "free" | "standard" | "premium") {
  const plan = useAuthStore((s) => s.plan);
  return PLAN_ORDER[plan] >= PLAN_ORDER[minPlan];
}
