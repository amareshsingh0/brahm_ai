// ── All admin data types ──────────────────────────────────────────────────────
// Add new interfaces here as features grow. Keep in sync with api/routers/admin.py

export interface Stats {
  total_users:  number;
  new_today:    number;
  new_week:     number;
  mau:          number;
  dau:          number;
  paid_users:   number;
  revenue_today:  number;
  revenue_month:  number;
  revenue_total:  number;
  chats_today:    number;
  kundalis_today: number;
  palm_today:     number;
  active_subscriptions: {
    jyotishi_monthly: number;
    jyotishi_yearly:  number;
    acharya_monthly:  number;
    acharya_yearly:   number;
  };
  top_endpoints: { endpoint: string; count: number }[];
}

export interface UserRow {
  id:       string;
  name:     string;
  phone:    string;
  email?:   string;
  role:     string;
  status:   string;
  plan:     string;
  lang_pref:    string;
  birth_city?:  string;
  birth_date?:  string;
  created_at:   string;
  last_login?:  string;
  total_chats:    number;
  total_kundalis: number;
  total_palm:     number;
  lifetime_paid_inr: number;
}

export interface UserDetail extends UserRow {
  birth_time?: string;
  city?:       string;
  subscription?: {
    plan:              string;
    period:            string;
    status:            string;
    started_at:        string;
    expires_at:        string;
    amount_paid:       number;
    cashfree_order_id: string;
  };
  usage_today: { feature: string; count: number }[];
}

export interface ChatMsg {
  id:           string;
  session_id:   string;
  page_context: string;
  role:         string;
  content:      string;
  confidence?:  string;
  tokens_used?: number;
  response_ms?: number;
  flagged:      boolean;
  flag_reason?: string;
  created_at:   string;
  // present in global chat monitor (not user-specific)
  user_name?:   string;
  user_phone?:  string;
}

export interface KundaliEntry {
  id:         number;
  birth_date: string;
  birth_time: string;
  birth_city: string;
  calc_ms:    number;
  is_saved:   boolean;
  source:     string;
  created_at: string;
}

export interface PalmEntry {
  id:          number;
  lines_found: Record<string, string>;
  confidence:  string;
  tokens_used: number;
  created_at:  string;
}

export interface PaymentRow {
  id:                 number;
  user_id?:           string;
  user_name?:         string;
  user_phone?:        string;
  cashfree_order_id:  string;
  amount:             number;
  status:             string;
  payment_method?:    string;
  fail_reason?:       string;
  paid_at:            string;
}

export interface LoginEntry {
  id:          number;
  ip:          string;
  device:      string;
  success:     boolean;
  fail_reason?: string;
  logged_at:   string;
}

export interface AdminLogEntry {
  id:           number;
  admin_name:   string;
  action:       string;
  target_type:  string;
  target_id:    string;
  details:      Record<string, unknown>;
  performed_at: string;
}

export interface Paginated<T> {
  items:  T[];
  total:  number;
  page:   number;
  pages:  number;
}

export interface Revenue {
  today: number;
  month: number;
  total: number;
}

export interface ChatAnalytics {
  top_questions: { content: string; times: number }[];
  context_dist:  { page_context: string; count: number }[];
}
