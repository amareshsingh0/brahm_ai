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
  id          BIGSERIAL PRIMARY KEY,
  user_id     TEXT REFERENCES users(id) ON DELETE CASCADE,
  phone       TEXT,
  ip          TEXT,
  device      TEXT,
  user_agent  TEXT,
  success     BOOLEAN DEFAULT TRUE,
  fail_reason TEXT,
  logged_at   TIMESTAMPTZ DEFAULT now()
);

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
