import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function fmt(ts?: string) {
  if (!ts) return "—";
  return new Date(ts).toLocaleString("en-IN", {
    day: "2-digit", month: "short", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

export function fmtInr(paise?: number) {
  return paise ? `₹${(paise / 100).toLocaleString("en-IN")}` : "₹0";
}

// ── Badge color maps ──────────────────────────────────────────────────────────
// Add new entries here when new plan types / statuses are added

export const PLAN_CLS: Record<string, string> = {
  free:      "bg-gray-100 text-gray-500",
  jyotishi:  "bg-blue-50 text-blue-600",
  acharya:   "bg-amber-50 text-amber-700",
};

export const STATUS_CLS: Record<string, string> = {
  active:    "bg-emerald-50 text-emerald-700",
  suspended: "bg-yellow-50 text-yellow-700",
  banned:    "bg-red-50 text-red-600",
  deleted:   "bg-gray-100 text-gray-400",
};

export const PAY_CLS: Record<string, string> = {
  SUCCESS:  "bg-emerald-50 text-emerald-700",
  FAILED:   "bg-red-50 text-red-600",
  PENDING:  "bg-yellow-50 text-yellow-700",
  REFUNDED: "bg-purple-50 text-purple-600",
};

export const ACTION_CLS: Record<string, string> = {
  ban_user:              "text-red-600",
  delete_user:           "text-red-700",
  refund:                "text-purple-600",
  change_plan:           "text-blue-600",
  extend_subscription:   "text-emerald-700",
  flag_message:          "text-yellow-700",
  cancel_subscription:   "text-yellow-700",
};
