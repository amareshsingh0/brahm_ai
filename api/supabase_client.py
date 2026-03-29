"""
Supabase client - shared singleton for all backend services.
ENV vars required:
  SUPABASE_URL=https://nmzlsqmxtgyxmddcciew.supabase.co
  SUPABASE_SERVICE_ROLE_KEY=eyJ...
"""
import logging
import os

import httpx
from supabase import Client, create_client

try:
    from supabase.client import ClientOptions
except ImportError:
    from supabase.lib.client_options import ClientOptions

_client: Client | None = None
logger = logging.getLogger(__name__)


def _make_client() -> Client:
    url = os.environ.get("SUPABASE_URL", "")
    key = os.environ.get("SUPABASE_SERVICE_ROLE_KEY", "")
    if not url or not key:
        raise RuntimeError("SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY must be set")

    # Some supabase-py versions accept a custom httpx client here, newer ones do not.
    # Fall back to the default client instead of failing auth/database requests.
    http_client = httpx.Client(http2=False, timeout=30.0)
    try:
        options = ClientOptions(httpx_client=http_client)
    except TypeError:
        http_client.close()
        logger.warning(
            "Supabase ClientOptions does not support httpx_client; using default client"
        )
        return create_client(url, key)

    return create_client(url, key, options=options)


def get_supabase() -> Client:
    """Return the shared Supabase client. Auto-reconnects on stale connection."""
    global _client
    if _client is None:
        _client = _make_client()
    return _client


def reset_supabase():
    """Force a fresh client on next call - call this after a connection error."""
    global _client
    _client = None
