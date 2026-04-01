-- ============================================================
-- Brahm AI — Supabase Schema v2
-- Run in Supabase Dashboard → SQL Editor → New Query
-- Safe to run multiple times (IF NOT EXISTS / OR REPLACE)
-- ============================================================

-- ── USERS ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
  id              TEXT PRIMARY KEY,        -- Clerk user_xxx ID (or session_id for now)
  session_id      TEXT UNIQUE,             -- legacy: keep for backwards compat
  phone           TEXT UNIQUE,
  email           TEXT UNIQUE,
  name            TEXT DEFAULT '',
  role            TEXT DEFAULT 'user',     -- 'user' | 'admin' | 'banned'
  status          TEXT DEFAULT 'active',   -- 'active' | 'suspended' | 'deleted'
  plan            TEXT DEFAULT 'free',     -- 'free' | 'standard' | 'premium'
  lang_pref       TEXT DEFAULT 'en',       -- 'en' | 'hi' | 'sa'
  city            TEXT,
  lat             FLOAT,
  lon             FLOAT,
  tz              FLOAT,
  birth_date      TEXT DEFAULT '',
  birth_time      TEXT DEFAULT '',
  birth_lat       FLOAT DEFAULT 0,
  birth_lon       FLOAT DEFAULT 0,
  birth_tz        FLOAT DEFAULT 5.5,
  birth_place     TEXT DEFAULT '',
  birth_city      TEXT DEFAULT '',
  gender          TEXT DEFAULT '',         -- 'Male' | 'Female' | 'Other' | 'Prefer not to say'
  rashi           TEXT DEFAULT '',
  nakshatra       TEXT DEFAULT '',
  kundali_json    TEXT,                    -- cached KundaliResponse JSON
  notif_panchang  BOOLEAN DEFAULT TRUE,
  notif_grahan    BOOLEAN DEFAULT TRUE,
  notif_festivals BOOLEAN DEFAULT FALSE,
  fcm_token       TEXT,
  google_id       TEXT UNIQUE,             -- set if signed up via Google
  apple_id        TEXT UNIQUE,             -- set if signed up via Apple
  phone_verified  BOOLEAN DEFAULT FALSE,
  signup_ip       TEXT,
  signup_device   TEXT,                    -- 'web' | 'android' | 'ios'
  language        TEXT DEFAULT 'english',  -- legacy alias for lang_pref
  created_at      TIMESTAMPTZ DEFAULT now(),
  updated_at      TIMESTAMPTZ DEFAULT now(),
  last_login      TIMESTAMPTZ,
  deleted_at      TIMESTAMPTZ
);

-- ── LOGIN LOG ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS login_log (
  id           BIGSERIAL PRIMARY KEY,
  user_id      TEXT REFERENCES users(id) ON DELETE CASCADE,
  phone        TEXT,
  ip           TEXT,
  device       TEXT,             -- "Mobile · Chrome · Android"
  user_agent   TEXT,
  login_method TEXT DEFAULT 'phone_otp', -- 'phone_otp' | 'google' | 'apple'
  client       TEXT DEFAULT 'web',       -- 'web' | 'android' | 'ios'
  country      TEXT,
  country_code TEXT,             -- ISO 3166-1 alpha-2, e.g. "IN"
  city         TEXT,
  region       TEXT,
  isp          TEXT,
  lat          FLOAT,
  lon          FLOAT,
  success      BOOLEAN DEFAULT TRUE,
  fail_reason  TEXT,
  logged_at    TIMESTAMPTZ DEFAULT now()
);

-- Migration: add new columns to existing login_log table
ALTER TABLE login_log ADD COLUMN IF NOT EXISTS login_method TEXT DEFAULT 'phone_otp';
ALTER TABLE login_log ADD COLUMN IF NOT EXISTS client       TEXT DEFAULT 'web';
ALTER TABLE login_log ADD COLUMN IF NOT EXISTS country      TEXT;
ALTER TABLE login_log ADD COLUMN IF NOT EXISTS country_code TEXT;
ALTER TABLE login_log ADD COLUMN IF NOT EXISTS city         TEXT;
ALTER TABLE login_log ADD COLUMN IF NOT EXISTS region       TEXT;
ALTER TABLE login_log ADD COLUMN IF NOT EXISTS isp          TEXT;
ALTER TABLE login_log ADD COLUMN IF NOT EXISTS lat          FLOAT;
ALTER TABLE login_log ADD COLUMN IF NOT EXISTS lon          FLOAT;

-- Migration: add auth columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id      TEXT UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS apple_id       TEXT UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT FALSE;

-- ── SUBSCRIPTIONS ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS subscriptions (
  id                TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
  user_id           TEXT REFERENCES users(id) ON DELETE CASCADE,
  plan              TEXT NOT NULL,              -- 'free' | 'standard' | 'premium'
  period            TEXT,                  -- 'monthly' | 'yearly'
  status            TEXT DEFAULT 'active', -- 'active' | 'cancelled' | 'expired'
  cashfree_order_id TEXT UNIQUE,
  cashfree_sub_id   TEXT,
  amount_paid       INTEGER,               -- paise (₹199 = 19900)
  currency          TEXT DEFAULT 'INR',
  started_at        TIMESTAMPTZ,
  expires_at        TIMESTAMPTZ,
  cancelled_at      TIMESTAMPTZ,
  cancel_reason     TEXT,
  cancelled_by      TEXT,
  -- legacy columns
  payment_id        TEXT,
  amount            NUMERIC,
  created_at        TIMESTAMPTZ DEFAULT now()
);

-- ── PAYMENT LOG ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment_log (
  id                  BIGSERIAL PRIMARY KEY,
  user_id             TEXT REFERENCES users(id),
  subscription_id     TEXT REFERENCES subscriptions(id),
  cashfree_order_id   TEXT,
  cashfree_payment_id TEXT,
  amount              INTEGER,
  currency            TEXT DEFAULT 'INR',
  status              TEXT,                -- 'SUCCESS' | 'FAILED' | 'PENDING' | 'REFUNDED'
  payment_method      TEXT,
  fail_reason         TEXT,
  refund_amount       INTEGER,
  refund_at           TIMESTAMPTZ,
  webhook_raw         JSONB,
  paid_at             TIMESTAMPTZ DEFAULT now()
);

-- ── USAGE LOG ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS usage_log (
  id          BIGSERIAL PRIMARY KEY,
  user_id     TEXT REFERENCES users(id) ON DELETE CASCADE,
  session_id  TEXT,                        -- legacy
  endpoint    TEXT,                        -- legacy
  method      TEXT DEFAULT 'GET',          -- legacy
  status_code INT,                         -- legacy
  duration_ms INT,                         -- legacy
  feature     TEXT,    -- 'ai_chat'|'kundali'|'search'|'palmistry'|'compatibility'
  source      TEXT DEFAULT 'web',
  metadata    JSONB,
  used_at     TIMESTAMPTZ DEFAULT now(),
  created_at  TIMESTAMPTZ DEFAULT now()
);

-- ── CHAT MESSAGES ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS chat_messages (
  id           BIGSERIAL PRIMARY KEY,
  user_id      TEXT REFERENCES users(id) ON DELETE CASCADE,
  session_id   TEXT NOT NULL,
  page_context TEXT DEFAULT 'general',
  page         TEXT DEFAULT '',            -- legacy alias
  role         TEXT NOT NULL,
  content      TEXT NOT NULL,
  confidence   TEXT,
  sources      JSONB,
  tokens_used  INTEGER,
  response_ms  INTEGER,
  flagged      BOOLEAN DEFAULT FALSE,
  flag_reason  TEXT,
  created_at   TIMESTAMPTZ DEFAULT now()
);

-- ── KUNDALI LOG ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS kundali_log (
  id           BIGSERIAL PRIMARY KEY,
  user_id      TEXT REFERENCES users(id),
  birth_date   TEXT,
  birth_time   TEXT,
  birth_city   TEXT,
  birth_lat    FLOAT,
  birth_lon    FLOAT,
  birth_tz     FLOAT,
  result_json  TEXT,
  calc_ms      INTEGER,
  source       TEXT DEFAULT 'web',
  is_saved     BOOLEAN DEFAULT FALSE,
  created_at   TIMESTAMPTZ DEFAULT now()
);

-- ── SAVED KUNDALIS ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS saved_kundalis (
  id           TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
  user_id      TEXT REFERENCES users(id) ON DELETE CASCADE,
  session_id   TEXT,                       -- legacy
  label        TEXT DEFAULT 'My Chart',
  birth_date   TEXT NOT NULL,
  birth_time   TEXT NOT NULL,
  birth_lat    FLOAT NOT NULL,
  birth_lon    FLOAT NOT NULL,
  birth_tz     FLOAT DEFAULT 5.5,
  birth_place  TEXT DEFAULT '',
  birth_city   TEXT DEFAULT '',
  kundali_json TEXT,
  is_shared    BOOLEAN DEFAULT FALSE,
  share_token  TEXT UNIQUE,
  created_at   TIMESTAMPTZ DEFAULT now()
);

-- ── PALMISTRY LOG ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS palmistry_log (
  id           BIGSERIAL PRIMARY KEY,
  user_id      TEXT REFERENCES users(id),
  image_hash   TEXT,
  image_size   INTEGER,
  result_json  TEXT,
  lines_found  JSONB,
  confidence   TEXT,
  tokens_used  INTEGER,
  response_ms  INTEGER,
  created_at   TIMESTAMPTZ DEFAULT now()
);

-- ── CHAT SESSION METADATA (pin / archive / rename) ───────────
CREATE TABLE IF NOT EXISTS chat_session_meta (
  id          BIGSERIAL PRIMARY KEY,
  user_id     TEXT REFERENCES users(id) ON DELETE CASCADE,
  session_id  TEXT NOT NULL,
  is_pinned   BOOLEAN DEFAULT FALSE,
  is_archived BOOLEAN DEFAULT FALSE,
  custom_name TEXT,
  updated_at  TIMESTAMPTZ DEFAULT now(),
  UNIQUE(user_id, session_id)
);
CREATE INDEX IF NOT EXISTS idx_meta_user ON chat_session_meta(user_id);
ALTER TABLE chat_session_meta ENABLE ROW LEVEL SECURITY;

-- ── DELETED ACCOUNTS (audit) ──────────────────────────────────
CREATE TABLE IF NOT EXISTS deleted_accounts (
  id             BIGSERIAL PRIMARY KEY,
  user_id        TEXT,
  phone          TEXT,
  name           TEXT,
  plan_at_delete TEXT,
  total_chats    INTEGER,
  total_payments INTEGER,
  delete_reason  TEXT,
  deleted_by     TEXT,
  deleted_at     TIMESTAMPTZ DEFAULT now()
);

-- ── ADMIN LOG ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admin_log (
  id           BIGSERIAL PRIMARY KEY,
  admin_id     TEXT REFERENCES users(id),
  action       TEXT,
  target_id    TEXT,
  target_type  TEXT,
  details      JSONB,
  performed_at TIMESTAMPTZ DEFAULT now()
);

-- ── INDEXES ───────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_users_session    ON users(session_id);
CREATE INDEX IF NOT EXISTS idx_users_phone      ON users(phone);
CREATE INDEX IF NOT EXISTS idx_login_user       ON login_log(user_id, logged_at DESC);
CREATE INDEX IF NOT EXISTS idx_sub_user         ON subscriptions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_sub_expiry       ON subscriptions(expires_at) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_payment_user     ON payment_log(user_id, paid_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_status   ON payment_log(status, paid_at DESC);
CREATE INDEX IF NOT EXISTS idx_usage_user       ON usage_log(user_id, used_at DESC);
CREATE INDEX IF NOT EXISTS idx_usage_session    ON usage_log(session_id);
CREATE INDEX IF NOT EXISTS idx_usage_created    ON usage_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_user        ON chat_messages(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_session     ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_created     ON chat_messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_flagged     ON chat_messages(flagged) WHERE flagged = TRUE;
CREATE INDEX IF NOT EXISTS idx_kundali_user     ON kundali_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_kundali_session  ON saved_kundalis(session_id);
CREATE INDEX IF NOT EXISTS idx_palm_user        ON palmistry_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_log        ON admin_log(performed_at DESC);

-- ── AUTO-UPDATE updated_at ────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS users_updated_at ON users;
CREATE TRIGGER users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ── Feature Flags ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS feature_flags (
  key                  TEXT PRIMARY KEY,
  name                 TEXT NOT NULL,
  description          TEXT,
  category             TEXT DEFAULT 'general',
  is_globally_enabled  BOOLEAN NOT NULL DEFAULT true,
  created_at           TIMESTAMPTZ DEFAULT now()
);

-- ── Subscription Plans ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS subscription_plans (
  id                   TEXT PRIMARY KEY,
  name                 TEXT NOT NULL,
  description          TEXT,
  price_inr            INTEGER NOT NULL DEFAULT 0,
  duration_days        INTEGER NOT NULL DEFAULT 30,
  daily_message_limit  INTEGER,
  daily_token_limit    INTEGER,
  features             JSONB NOT NULL DEFAULT '[]',
  is_active            BOOLEAN NOT NULL DEFAULT true,
  sort_order           INTEGER NOT NULL DEFAULT 0,
  badge_text           TEXT,
  created_at           TIMESTAMPTZ DEFAULT now(),
  updated_at           TIMESTAMPTZ DEFAULT now()
);

-- ── Daily Usage ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS daily_usage (
  id            BIGSERIAL PRIMARY KEY,
  user_id       TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  usage_date    DATE NOT NULL DEFAULT CURRENT_DATE,
  messages_used INTEGER NOT NULL DEFAULT 0,
  tokens_used   INTEGER NOT NULL DEFAULT 0,
  plan_id       TEXT,
  UNIQUE(user_id, usage_date)
);
CREATE INDEX IF NOT EXISTS idx_daily_usage_user_date ON daily_usage(user_id, usage_date);

-- ── Default seed data ─────────────────────────────────────────────────────────
INSERT INTO feature_flags (key, name, description, category) VALUES
  ('ai_chat',         'AI Chat',              'Main AI chat assistant',            'ai'),
  ('kundali',         'Kundali',              'Kundali generation and analysis',   'astrology'),
  ('palm_reading',    'Palm Reading',         'AI palmistry analysis',             'ai'),
  ('live_sky_today',  'Live Sky Today',       'Today for You in live sky',         'astrology'),
  ('muhurta',         'Muhurta',              'Auspicious time calculator',        'astrology'),
  ('horoscope_ai',    'Horoscope AI',         'AI horoscope analysis',             'ai'),
  ('compatibility',   'Compatibility',        'Kundali matching',                  'astrology'),
  ('gochar',          'Gochar',               'Transit analysis',                  'astrology'),
  ('kp_system',       'KP System',            'Krishnamurti Paddhati',             'astrology'),
  ('dosha',           'Dosha Analysis',       'Dosha detection and remedies',      'astrology'),
  ('gemstone',        'Gemstone',             'Gemstone recommendations',          'astrology'),
  ('nakshatra',       'Nakshatra',            'Nakshatra analysis',                'astrology'),
  ('prashna',         'Prashna Kundali',      'Horary astrology',                  'astrology'),
  ('varshphal',       'Varshphal',            'Solar return chart',                'astrology'),
  ('rectification',   'Rectification',        'Birth time rectification',          'astrology'),
  ('pdf_export',      'PDF Export',           'Export kundali as PDF',             'tools'),
  ('chat_history',    'Chat History',         'Save and view chat history',        'tools'),
  ('panchang',        'Panchang',             'Daily panchang (always free)',       'astrology')
ON CONFLICT (key) DO NOTHING;

INSERT INTO subscription_plans (id, name, description, price_inr, duration_days, daily_message_limit, daily_token_limit, features, is_active, sort_order, badge_text) VALUES
  ('free',  'Free',  'Basic access with panchang',   0,   0,  0,  0,      '["panchang"]', true, 0, NULL),
  ('basic', 'Basic', 'Perfect for regular users',  299,  30, 30, 100000,
   '["ai_chat","kundali","palm_reading","live_sky_today","muhurta","horoscope_ai","compatibility","gochar","kp_system","dosha","gemstone","nakshatra","prashna","varshphal","rectification","pdf_export","chat_history","panchang"]',
   true, 1, 'Popular'),
  ('pro',   'Pro',   'For power users',            499,  30, 65, 250000,
   '["ai_chat","kundali","palm_reading","live_sky_today","muhurta","horoscope_ai","compatibility","gochar","kp_system","dosha","gemstone","nakshatra","prashna","varshphal","rectification","pdf_export","chat_history","panchang"]',
   true, 2, 'Best Value')
ON CONFLICT (id) DO NOTHING;

-- ── ROW LEVEL SECURITY ────────────────────────────────────────
-- Service role key (backend) bypasses RLS — full access
-- Frontend (anon key) blocked — all access via FastAPI backend only
ALTER TABLE users            ENABLE ROW LEVEL SECURITY;
ALTER TABLE login_log        ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions    ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_log      ENABLE ROW LEVEL SECURITY;
ALTER TABLE usage_log        ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_messages    ENABLE ROW LEVEL SECURITY;
ALTER TABLE kundali_log      ENABLE ROW LEVEL SECURITY;
ALTER TABLE saved_kundalis   ENABLE ROW LEVEL SECURITY;
ALTER TABLE palmistry_log    ENABLE ROW LEVEL SECURITY;
ALTER TABLE deleted_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_log        ENABLE ROW LEVEL SECURITY;
ALTER TABLE feature_flags    ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE daily_usage      ENABLE ROW LEVEL SECURITY;
