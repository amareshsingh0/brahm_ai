"""
Supabase client — shared singleton for all backend services.
ENV vars required:
  SUPABASE_URL=https://nmzlsqmxtgyxmddcciew.supabase.co
  SUPABASE_SERVICE_ROLE_KEY=eyJ...
"""
import os
from supabase import create_client, Client

_client: Client | None = None


def _make_client() -> Client:
    url = os.environ.get("SUPABASE_URL", "")
    key = os.environ.get("SUPABASE_SERVICE_ROLE_KEY", "")
    if not url or not key:
        raise RuntimeError("SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY must be set")
    return create_client(url, key)


def get_supabase() -> Client:
    """Return the shared Supabase client. Auto-reconnects on stale connection."""
    global _client
    if _client is None:
        _client = _make_client()
    return _client


def reset_supabase():
    """Force a fresh client on next call — call this after a connection error."""
    global _client
    _client = None
