-- ============================================================
-- Brahm AI — Custom Auth Migration v1
-- Phase 1: Update users table + create otp_log + refresh_tokens
-- Safe to run multiple times (IF NOT EXISTS / DO $$ blocks)
-- Run in: Supabase Dashboard → SQL Editor → New Query
-- ============================================================

-- ── 1. UPDATE USERS TABLE ─────────────────────────────────────

-- Change id from TEXT (Clerk ID) to UUID
-- NOTE: If users table has existing data, keep id as TEXT and just add new columns.
-- We will migrate id to UUID in a future step when Clerk is fully removed.

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS google_id       TEXT UNIQUE,
  ADD COLUMN IF NOT EXISTS apple_id        TEXT UNIQUE,
  ADD COLUMN IF NOT EXISTS phone_verified  BOOLEAN DEFAULT false,
  ADD COLUMN IF NOT EXISTS password_hash   TEXT,
  ADD COLUMN IF NOT EXISTS failed_attempts INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS locked_until    TIMESTAMPTZ;

-- Index for social logins
CREATE INDEX IF NOT EXISTS idx_users_google ON users(google_id) WHERE google_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_apple  ON users(apple_id)  WHERE apple_id  IS NOT NULL;

-- ── 2. OTP LOG ────────────────────────────────────────────────
-- Stores hashed OTPs for phone verification
-- OTP itself is NEVER stored in plaintext — only bcrypt hash

CREATE TABLE IF NOT EXISTS otp_log (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  phone       TEXT NOT NULL,
  otp_hash    TEXT NOT NULL,          -- bcrypt hash of the 6-digit OTP
  purpose     TEXT NOT NULL,          -- 'login' | 'register' | 'verify_phone'
  expires_at  TIMESTAMPTZ NOT NULL,   -- now() + 5 minutes
  used        BOOLEAN DEFAULT false,
  attempts    INTEGER DEFAULT 0,      -- how many times wrong OTP tried
  ip_address  TEXT,
  user_agent  TEXT,
  created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_otp_phone_expiry ON otp_log(phone, expires_at DESC);
CREATE INDEX IF NOT EXISTS idx_otp_cleanup      ON otp_log(expires_at) WHERE used = false;

-- ── 3. REFRESH TOKENS ─────────────────────────────────────────
-- Stores hashed refresh tokens — actual token sent to client only once

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      TEXT REFERENCES users(id) ON DELETE CASCADE,
  token_hash   TEXT NOT NULL UNIQUE,  -- SHA-256 hash of the refresh token
  device_name  TEXT,                  -- e.g. 'Chrome on Windows', 'Android App'
  device_type  TEXT DEFAULT 'web',    -- 'web' | 'android' | 'ios'
  ip_address   TEXT,
  user_agent   TEXT,
  expires_at   TIMESTAMPTZ NOT NULL,  -- now() + 30 days
  last_used_at TIMESTAMPTZ,
  revoked      BOOLEAN DEFAULT false,
  revoked_at   TIMESTAMPTZ,
  created_at   TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refresh_user      ON refresh_tokens(user_id, expires_at DESC);
CREATE INDEX IF NOT EXISTS idx_refresh_hash      ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_cleanup   ON refresh_tokens(expires_at) WHERE revoked = false;

-- ── 4. CLEANUP FUNCTION (run via cron or manually) ────────────
-- Deletes expired OTPs older than 1 hour and expired refresh tokens older than 7 days

CREATE OR REPLACE FUNCTION cleanup_auth_tables()
RETURNS void AS $$
BEGIN
  -- Delete old used/expired OTPs
  DELETE FROM otp_log
  WHERE expires_at < now() - INTERVAL '1 hour';

  -- Delete old revoked/expired refresh tokens
  DELETE FROM refresh_tokens
  WHERE expires_at < now() - INTERVAL '7 days'
     OR (revoked = true AND revoked_at < now() - INTERVAL '7 days');
END;
$$ LANGUAGE plpgsql;

-- ── 5. RLS (Row Level Security) ───────────────────────────────
-- Backend uses service role key → bypasses RLS (full access)
-- Frontend (anon key) blocked — access only via FastAPI

ALTER TABLE otp_log       ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;

-- ── DONE ──────────────────────────────────────────────────────
-- After running this:
-- 1. users table has: google_id, apple_id, phone_verified, password_hash, failed_attempts, locked_until
-- 2. otp_log table created — stores hashed OTPs
-- 3. refresh_tokens table created — stores hashed refresh tokens
-- 4. cleanup_auth_tables() function created
-- Next: Phase 2 — Backend OTP send/verify endpoints
