"""
Admin panel router — Brahm AI v2
Protected by X-Admin-Key header.
Format: base64(username:secret_key)

Env vars:
  ADMIN_USERNAME  — admin login username  (default: brahm_admin)
  ADMIN_SECRET    — admin secret key      (default: brahm-admin-2024)

All endpoints require header: X-Admin-Key: <base64(username:secret_key)>
All mutating actions logged to admin_log table in Supabase.
"""
import os
import json
import base64
import threading
import sqlite3
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, HTTPException, Header, Query, Request
from pydantic import BaseModel
from typing import Optional

router = APIRouter()

ADMIN_USERNAME = os.getenv("ADMIN_USERNAME", "brahm_admin")
ADMIN_SECRET   = os.getenv("ADMIN_SECRET",   "brahm-admin-2024")

# ─── Auth ─────────────────────────────────────────────────────────────────────

def _check(key: str | None):
    """Accept base64(username:secret_key) or legacy plain secret key."""
    if not key:
        raise HTTPException(status_code=401, detail="Unauthorized — missing admin key")

    # Try new format: base64(username:secret_key)
    try:
        decoded = base64.b64decode(key.encode()).decode("utf-8")
        if ":" in decoded:
            username, secret = decoded.split(":", 1)
            if username == ADMIN_USERNAME and secret == ADMIN_SECRET:
                return  # ✓ valid
    except Exception:
        pass

    # Legacy fallback: plain secret key (for backward compatibility)
    if key == ADMIN_SECRET:
        return  # ✓ valid

    raise HTTPException(status_code=401, detail="Unauthorized — invalid credentials")


# ─── DB backend: try Supabase first, fall back to SQLite ─────────────────────

def _get_supabase():
    """Returns Supabase client or None if not configured."""
    try:
        from api.supabase_client import get_supabase
        return get_supabase()
    except Exception:
        return None


def _sb(fn):
    """
    Run a Supabase lambda with auto-reconnect on HTTP disconnect.
    Usage: result = _sb(lambda: sb.table(...).execute())
    Retries once after resetting the client on RemoteProtocolError / disconnected.
    """
    from api.supabase_client import reset_supabase
    try:
        return fn()
    except Exception as e:
        msg = str(e).lower()
        if any(k in msg for k in ("disconnected", "remoteprot", "connect", "reset", "broken pipe")):
            reset_supabase()
            # get fresh client and retry
            return fn()
        raise


def _sqlite_conn():
    """Legacy SQLite fallback."""
    from api.config import USERS_DB, DATA_DIR
    import os as _os
    db_path = USERS_DB
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    return conn


def _use_supabase() -> bool:
    return _get_supabase() is not None


def _sb_call(fn):
    """Execute a Supabase lambda, auto-reset client on disconnect and retry once."""
    from api.supabase_client import reset_supabase
    try:
        return fn()
    except Exception as e:
        if "disconnected" in str(e).lower() or "connect" in str(e).lower():
            reset_supabase()
            # retry once with fresh client
            return fn()
        raise


def _fmt(ts) -> str:
    if not ts:
        return None
    if isinstance(ts, str):
        return ts
    return ts.isoformat()


# ─── Activity log (SQLite — lightweight, no Supabase needed) ──────────────────

_log_lock = threading.Lock()

def _logs_conn():
    from api.config import DATA_DIR
    import os as _os
    db_path = _os.path.join(DATA_DIR, "activity_logs.db")
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    conn.execute("""
        CREATE TABLE IF NOT EXISTS activity_logs (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            ts          TEXT NOT NULL,
            method      TEXT,
            endpoint    TEXT,
            session_id  TEXT DEFAULT '',
            status_code INTEGER DEFAULT 0,
            duration_ms INTEGER DEFAULT 0,
            client      TEXT DEFAULT 'unknown'
        )
    """)
    # migrate: add client column if upgrading from older DB without it
    try:
        conn.execute("ALTER TABLE activity_logs ADD COLUMN client TEXT DEFAULT 'unknown'")
    except Exception:
        pass  # column already exists
    conn.commit()
    return conn


def log_request(method: str, endpoint: str, session_id: str, status_code: int, duration_ms: int, client: str = "unknown"):
    """Called from logging middleware — fire-and-forget."""
    try:
        ts = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
        with _log_lock:
            with _logs_conn() as conn:
                conn.execute(
                    "INSERT INTO activity_logs (ts,method,endpoint,session_id,status_code,duration_ms,client)"
                    " VALUES (?,?,?,?,?,?,?)",
                    (ts, method, endpoint, session_id or "", status_code, duration_ms, client),
                )
                conn.commit()
    except Exception:
        pass


def _admin_log(admin_id: str, action: str, target_id: str, target_type: str, details: dict):
    """Log admin action to Supabase admin_log table."""
    try:
        sb = _get_supabase()
        if sb:
            sb.table("admin_log").insert({
                "admin_id":   admin_id,
                "action":     action,
                "target_id":  target_id,
                "target_type": target_type,
                "details":    details,
            }).execute()
    except Exception:
        pass


# ─── STATS ────────────────────────────────────────────────────────────────────

@router.get("/admin/stats")
def get_stats(x_admin_key: str = Header(None)):
    _check(x_admin_key)
    today = datetime.now(timezone.utc).date().isoformat()
    week_ago = (datetime.now(timezone.utc) - timedelta(days=7)).date().isoformat()
    month_ago = (datetime.now(timezone.utc) - timedelta(days=30)).date().isoformat()

    sb = _get_supabase()
    if sb:
        try:
            # Run all queries in parallel — reduces 13 sequential round-trips to ~1 RTT
            def q(fn):
                return fn()

            queries = {
                "total_users":  lambda: sb.table("users").select("id", count="exact").execute().count or 0,
                "new_today":    lambda: sb.table("users").select("id", count="exact").gte("created_at", today).execute().count or 0,
                "new_week":     lambda: sb.table("users").select("id", count="exact").gte("created_at", week_ago).execute().count or 0,
                "dau":          lambda: sb.table("usage_log").select("user_id", count="exact").gte("used_at", today).execute().count or 0,
                "mau":          lambda: sb.table("usage_log").select("user_id", count="exact").gte("used_at", month_ago).execute().count or 0,
                "paid_users":   lambda: sb.table("subscriptions").select("id", count="exact").eq("status", "active").neq("plan", "free").execute().count or 0,
                "rev_today":    lambda: sb.table("payment_log").select("amount").eq("status", "SUCCESS").gte("paid_at", today).execute().data or [],
                "rev_month":    lambda: sb.table("payment_log").select("amount").eq("status", "SUCCESS").gte("paid_at", month_ago).execute().data or [],
                "rev_total":    lambda: sb.table("payment_log").select("amount").eq("status", "SUCCESS").execute().data or [],
                "chats_today":  lambda: sb.table("chat_messages").select("id", count="exact").gte("created_at", today).execute().count or 0,
                "kund_today":   lambda: sb.table("kundali_log").select("id", count="exact").gte("created_at", today).execute().count or 0,
                "palm_today":   lambda: sb.table("palmistry_log").select("id", count="exact").gte("created_at", today).execute().count or 0,
                "sub_sm":       lambda: sb.table("subscriptions").select("id", count="exact").eq("status","active").eq("plan","standard").eq("period","monthly").execute().count or 0,
                "sub_sy":       lambda: sb.table("subscriptions").select("id", count="exact").eq("status","active").eq("plan","standard").eq("period","yearly").execute().count or 0,
                "sub_pm":       lambda: sb.table("subscriptions").select("id", count="exact").eq("status","active").eq("plan","premium").eq("period","monthly").execute().count or 0,
                "sub_py":       lambda: sb.table("subscriptions").select("id", count="exact").eq("status","active").eq("plan","premium").eq("period","yearly").execute().count or 0,
            }

            results = {}
            with ThreadPoolExecutor(max_workers=16) as ex:
                futures = {ex.submit(q, fn): key for key, fn in queries.items()}
                for future in as_completed(futures):
                    results[futures[future]] = future.result()

            rev_today = sum(r.get("amount", 0) or 0 for r in results["rev_today"])
            rev_month = sum(r.get("amount", 0) or 0 for r in results["rev_month"])
            rev_total = sum(r.get("amount", 0) or 0 for r in results["rev_total"])

            return {
                "total_users": results["total_users"],
                "new_today":   results["new_today"],
                "new_week":    results["new_week"],
                "mau":         results["mau"],
                "dau":         results["dau"],
                "paid_users":  results["paid_users"],
                "revenue_today": rev_today,
                "revenue_month": rev_month,
                "revenue_total": rev_total,
                "chats_today":   results["chats_today"],
                "kundalis_today": results["kund_today"],
                "palm_today":    results["palm_today"],
                "active_subscriptions": {
                    "standard_monthly": results["sub_sm"],
                    "standard_yearly":  results["sub_sy"],
                    "premium_monthly":  results["sub_pm"],
                    "premium_yearly":   results["sub_py"],
                },
                "top_endpoints": _top_endpoints(),
            }
        except Exception as e:
            raise HTTPException(500, f"Supabase stats error: {e}")

    # ── SQLite fallback ──
    try:
        with _sqlite_conn() as uc:
            total_users = uc.execute("SELECT COUNT(*) FROM users").fetchone()[0]
    except Exception:
        total_users = 0

    return {
        "total_users": total_users,
        "new_today": 0, "new_week": 0, "mau": 0, "dau": 0,
        "paid_users": 0, "revenue_today": 0, "revenue_month": 0, "revenue_total": 0,
        "chats_today": 0, "kundalis_today": 0, "palm_today": 0,
        "active_subscriptions": {"standard_monthly":0,"standard_yearly":0,"premium_monthly":0,"premium_yearly":0},
        "top_endpoints": _top_endpoints(),
    }


def _top_endpoints():
    try:
        with _logs_conn() as lc:
            rows = lc.execute(
                "SELECT endpoint, COUNT(*) cnt FROM activity_logs GROUP BY endpoint ORDER BY cnt DESC LIMIT 10"
            ).fetchall()
            return [{"endpoint": r["endpoint"], "count": r["cnt"]} for r in rows]
    except Exception:
        return []


# ─── API MONITOR ──────────────────────────────────────────────────────────────

@router.get("/admin/api-stats")
def get_api_stats(
    x_admin_key: str = Header(None),
    period: str = Query("today", regex="^(today|7d|30d)$"),
):
    """Dedicated API stats endpoint — top endpoints, error rates, avg latency, method breakdown."""
    _check(x_admin_key)

    try:
        with _logs_conn() as lc:
            # Time filter
            if period == "today":
                ts_filter = datetime.now(timezone.utc).strftime("%Y-%m-%d")
                where = f"WHERE ts >= '{ts_filter}'"
            elif period == "7d":
                ts_filter = (datetime.now(timezone.utc) - timedelta(days=7)).strftime("%Y-%m-%d")
                where = f"WHERE ts >= '{ts_filter}'"
            else:
                ts_filter = (datetime.now(timezone.utc) - timedelta(days=30)).strftime("%Y-%m-%d")
                where = f"WHERE ts >= '{ts_filter}'"

            # Total requests
            total = lc.execute(f"SELECT COUNT(*) FROM activity_logs {where}").fetchone()[0]

            # Top endpoints by hits
            top_endpoints = lc.execute(
                f"SELECT endpoint, COUNT(*) cnt, "
                f"AVG(duration_ms) avg_ms, "
                f"SUM(CASE WHEN status_code >= 400 THEN 1 ELSE 0 END) errors "
                f"FROM activity_logs {where} "
                f"GROUP BY endpoint ORDER BY cnt DESC LIMIT 20"
            ).fetchall()

            # Error rate by endpoint
            errors_by_ep = lc.execute(
                f"SELECT endpoint, COUNT(*) cnt, status_code "
                f"FROM activity_logs {where} AND status_code >= 400 "
                f"GROUP BY endpoint, status_code ORDER BY cnt DESC LIMIT 15"
            ).fetchall()

            # Method breakdown
            methods = lc.execute(
                f"SELECT method, COUNT(*) cnt FROM activity_logs {where} GROUP BY method"
            ).fetchall()

            # Status code distribution
            status_dist = lc.execute(
                f"SELECT status_code, COUNT(*) cnt FROM activity_logs {where} "
                f"GROUP BY status_code ORDER BY cnt DESC"
            ).fetchall()

            # Slowest endpoints (avg latency)
            slowest = lc.execute(
                f"SELECT endpoint, AVG(duration_ms) avg_ms, COUNT(*) cnt "
                f"FROM activity_logs {where} "
                f"GROUP BY endpoint HAVING cnt >= 3 ORDER BY avg_ms DESC LIMIT 10"
            ).fetchall()

            # Client breakdown (web / android / unknown)
            client_breakdown = lc.execute(
                f"SELECT client, COUNT(*) cnt FROM activity_logs {where} GROUP BY client ORDER BY cnt DESC"
            ).fetchall()

            # Requests over time (hourly for today, daily for 7d/30d)
            if period == "today":
                timeline = lc.execute(
                    f"SELECT strftime('%H:00', ts) hour, COUNT(*) cnt "
                    f"FROM activity_logs {where} GROUP BY hour ORDER BY hour"
                ).fetchall()
                timeline_data = [{"label": r["hour"], "count": r["cnt"]} for r in timeline]
            else:
                timeline = lc.execute(
                    f"SELECT strftime('%Y-%m-%d', ts) day, COUNT(*) cnt "
                    f"FROM activity_logs {where} GROUP BY day ORDER BY day"
                ).fetchall()
                timeline_data = [{"label": r["day"], "count": r["cnt"]} for r in timeline]

            return {
                "period": period,
                "total_requests": total,
                "top_endpoints": [
                    {
                        "endpoint": r["endpoint"],
                        "count": r["cnt"],
                        "avg_ms": round(r["avg_ms"] or 0),
                        "errors": r["errors"],
                        "error_rate": round((r["errors"] / r["cnt"]) * 100, 1) if r["cnt"] else 0,
                    }
                    for r in top_endpoints
                ],
                "errors_by_endpoint": [
                    {"endpoint": r["endpoint"], "status_code": r["status_code"], "count": r["cnt"]}
                    for r in errors_by_ep
                ],
                "method_breakdown": [{"method": r["method"], "count": r["cnt"]} for r in methods],
                "status_distribution": [{"status": r["status_code"], "count": r["cnt"]} for r in status_dist],
                "slowest_endpoints": [
                    {"endpoint": r["endpoint"], "avg_ms": round(r["avg_ms"]), "count": r["cnt"]}
                    for r in slowest
                ],
                "timeline": timeline_data,
                "client_breakdown": [{"client": r["client"] or "unknown", "count": r["cnt"]} for r in client_breakdown],
            }
    except Exception as e:
        raise HTTPException(500, f"API stats error: {e}")


# ─── USERS ────────────────────────────────────────────────────────────────────

@router.get("/admin/users")
def list_users(
    x_admin_key: str = Header(None),
    page:   int = Query(1, ge=1),
    limit:  int = Query(25, ge=1, le=100),
    search: str = Query(""),
    plan:   str = Query(""),
    status: str = Query(""),
):
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        def _build_user_query():
            q = sb.table("users").select(
                "id, session_id, name, phone, email, role, status, plan, lang_pref, "
                "birth_city, birth_date, created_at, last_login"
            )
            if search:
                q = q.or_(f"name.ilike.%{search}%,phone.ilike.%{search}%")
            if plan:
                q = q.eq("plan", plan)
            if status:
                q = q.eq("status", status)
            count_q = sb.table("users").select("id", count="exact")
            if search:
                count_q = count_q.or_(f"name.ilike.%{search}%,phone.ilike.%{search}%")
            if plan:
                count_q = count_q.eq("plan", plan)
            if status:
                count_q = count_q.eq("status", status)
            total = count_q.execute().count or 0
            offset = (page - 1) * limit
            rows = q.order("created_at", desc=True).range(offset, offset + limit - 1).execute().data or []
            return total, rows

        try:
            try:
                total, rows = _build_user_query()
            except Exception:
                # Retry once on HTTP/2 connection drop
                total, rows = _build_user_query()

            # Batch-fetch aggregate counts using IN queries (avoids N+1 HTTP/2 calls)
            uids = [u.get("id") or u.get("session_id", "") for u in rows if u.get("id") or u.get("session_id")]
            chat_counts: dict = {}
            kundali_counts: dict = {}
            palm_counts: dict = {}
            pay_totals: dict = {}
            if uids:
                try:
                    for row in (sb.table("chat_messages").select("user_id", count="exact").in_("user_id", uids).execute().data or []):
                        chat_counts[row["user_id"]] = chat_counts.get(row["user_id"], 0) + 1
                except Exception:
                    pass
                try:
                    for row in (sb.table("kundali_log").select("user_id", count="exact").in_("user_id", uids).execute().data or []):
                        kundali_counts[row["user_id"]] = kundali_counts.get(row["user_id"], 0) + 1
                except Exception:
                    pass
                try:
                    for row in (sb.table("palmistry_log").select("user_id", count="exact").in_("user_id", uids).execute().data or []):
                        palm_counts[row["user_id"]] = palm_counts.get(row["user_id"], 0) + 1
                except Exception:
                    pass
                try:
                    for row in (sb.table("payment_log").select("user_id,amount").in_("user_id", uids).eq("status", "SUCCESS").execute().data or []):
                        pay_totals[row["user_id"]] = pay_totals.get(row["user_id"], 0) + (row.get("amount") or 0)
                except Exception:
                    pass

            users = []
            for u in rows:
                uid = u.get("id") or u.get("session_id", "")
                users.append({**u,
                    "total_chats": chat_counts.get(uid, 0),
                    "total_kundalis": kundali_counts.get(uid, 0),
                    "total_palm": palm_counts.get(uid, 0),
                    "lifetime_paid_inr": pay_totals.get(uid, 0) / 100,
                })

            return {"users": users, "total": total, "page": page, "pages": max(1, (total + limit - 1) // limit)}
        except Exception as e:
            raise HTTPException(500, f"Supabase users error: {e}")

    # SQLite fallback
    offset = (page - 1) * limit
    try:
        with _sqlite_conn() as conn:
            if search:
                rows  = conn.execute("SELECT session_id, data FROM users WHERE data LIKE ? LIMIT ? OFFSET ?", (f"%{search}%", limit, offset)).fetchall()
                total = conn.execute("SELECT COUNT(*) FROM users WHERE data LIKE ?", (f"%{search}%",)).fetchone()[0]
            else:
                rows  = conn.execute("SELECT session_id, data FROM users LIMIT ? OFFSET ?", (limit, offset)).fetchall()
                total = conn.execute("SELECT COUNT(*) FROM users").fetchone()[0]
        users = []
        for row in rows:
            try:
                d = json.loads(row["data"])
            except Exception:
                d = {}
            users.append({"id": row["session_id"], "session_id": row["session_id"],
                "name": d.get("name",""), "phone": d.get("phone",""), "plan": d.get("plan","free"),
                "status": "active", "role": "user", "created_at": "", "last_login": "",
                "total_chats": 0, "total_kundalis": 0, "total_palm": 0, "lifetime_paid_inr": 0})
        return {"users": users, "total": total, "page": page, "pages": max(1, (total + limit - 1) // limit)}
    except Exception as e:
        raise HTTPException(500, f"SQLite users error: {e}")


@router.get("/admin/users/{user_id}")
def get_user_detail(user_id: str, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        try:
            u = sb.table("users").select("*").eq("id", user_id).single().execute().data
            if not u:
                u = sb.table("users").select("*").eq("session_id", user_id).single().execute().data
            if not u:
                raise HTTPException(404, "User not found")
            uid = u.get("id") or user_id

            sub   = sb.table("subscriptions").select("*").eq("user_id", uid).eq("status", "active").limit(1).execute().data
            usage = sb.table("usage_log").select("feature").eq("user_id", uid).gte("used_at", datetime.now(timezone.utc).date().isoformat()).execute().data or []
            total_chats    = sb.table("chat_messages").select("id", count="exact").eq("user_id", uid).execute().count or 0
            total_kundalis = sb.table("kundali_log").select("id", count="exact").eq("user_id", uid).execute().count or 0
            total_palm     = sb.table("palmistry_log").select("id", count="exact").eq("user_id", uid).execute().count or 0
            pay_r          = sb.table("payment_log").select("amount").eq("user_id", uid).eq("status", "SUCCESS").execute()
            lifetime_paid  = sum(r.get("amount", 0) or 0 for r in (pay_r.data or [])) / 100

            # Usage breakdown today
            from collections import Counter
            usage_counts = [{"feature": k, "count": v} for k, v in Counter(r.get("feature") for r in usage if r.get("feature")).items()]

            return {**u, "subscription": sub[0] if sub else None,
                    "usage_today": usage_counts, "total_chats": total_chats,
                    "total_kundalis": total_kundalis, "total_palm": total_palm,
                    "lifetime_paid_inr": lifetime_paid}
        except HTTPException:
            raise
        except Exception as e:
            raise HTTPException(500, f"User detail error: {e}")

    # SQLite fallback
    try:
        with _sqlite_conn() as conn:
            row = conn.execute("SELECT data FROM users WHERE session_id=?", (user_id,)).fetchone()
        if not row:
            raise HTTPException(404, "User not found")
        d = json.loads(row["data"])
        return {"id": user_id, "session_id": user_id, **d, "subscription": None, "usage_today": []}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"SQLite user detail error: {e}")


class UserUpdate(BaseModel):
    plan:   Optional[str] = None
    name:   Optional[str] = None
    note:   Optional[str] = None
    # status and role are intentionally NOT in this model.
    # Use dedicated /suspend, /unsuspend, /ban, /unban endpoints instead.


@router.patch("/admin/users/{user_id}")
def update_user(user_id: str, body: UserUpdate, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    updates = {k: v for k, v in body.model_dump().items() if v is not None and k != "note"}
    if not updates:
        raise HTTPException(400, "Nothing to update")
    if sb:
        try:
            sb.table("users").update(updates).eq("id", user_id).execute()
            _admin_log("admin", f"update_user:{list(updates.keys())}", user_id, "user",
                       {"updates": updates, "note": body.note})
            return {"updated": user_id, **updates}
        except Exception as e:
            raise HTTPException(500, f"Update error: {e}")
    # SQLite fallback — only plan/name
    try:
        with _sqlite_conn() as conn:
            row = conn.execute("SELECT data FROM users WHERE session_id=?", (user_id,)).fetchone()
            if not row:
                raise HTTPException(404, "User not found")
            d = json.loads(row["data"])
            d.update(updates)
            conn.execute("UPDATE users SET data=? WHERE session_id=?", (json.dumps(d), user_id))
            conn.commit()
        return {"updated": user_id, **updates}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"SQLite update error: {e}")


@router.delete("/admin/users/{user_id}")
def delete_user(user_id: str, x_admin_key: str = Header(None), reason: str = Query("admin_request")):
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        try:
            u = sb.table("users").select("name,phone,plan").eq("id", user_id).single().execute().data or {}
            total_chats = sb.table("chat_messages").select("id", count="exact").eq("user_id", user_id).execute().count or 0
            pay_r       = sb.table("payment_log").select("id", count="exact").eq("user_id", user_id).eq("status","SUCCESS").execute()
            sb.table("deleted_accounts").insert({"user_id": user_id, "phone": u.get("phone"),
                "name": u.get("name"), "plan_at_delete": u.get("plan"),
                "total_chats": total_chats, "total_payments": pay_r.count or 0,
                "delete_reason": reason, "deleted_by": "admin"}).execute()
            sb.table("users").update({"status": "deleted", "deleted_at": datetime.now(timezone.utc).isoformat()}).eq("id", user_id).execute()
            _admin_log("admin", "delete_user", user_id, "user", {"reason": reason})
            return {"deleted": user_id}
        except Exception as e:
            raise HTTPException(500, f"Delete error: {e}")
    try:
        with _sqlite_conn() as conn:
            conn.execute("DELETE FROM users WHERE session_id=?", (user_id,))
            conn.commit()
        return {"deleted": user_id}
    except Exception as e:
        raise HTTPException(500, f"SQLite delete error: {e}")


@router.post("/admin/users/{user_id}/ban")
def ban_user(user_id: str, x_admin_key: str = Header(None), reason: str = Query("")):
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        sb.table("users").update({"role": "banned", "status": "suspended"}).eq("id", user_id).execute()
        _admin_log("admin", "ban_user", user_id, "user", {"reason": reason})
    return {"banned": user_id}


@router.get("/admin/deleted-accounts")
def get_deleted_accounts(
    x_admin_key: str = Header(None),
    page:  int = Query(1, ge=1),
    limit: int = Query(30, ge=1, le=100),
):
    """Return accounts deleted in the last 30 days (GDPR review window)."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"items": [], "total": 0, "page": 1, "pages": 1}
    cutoff = (datetime.now(timezone.utc) - timedelta(days=30)).isoformat()
    try:
        total = _sb(lambda: sb.table("deleted_accounts").select("id", count="exact")
            .gte("deleted_at", cutoff).execute()).count or 0
        offset = (page - 1) * limit
        rows = _sb(lambda: sb.table("deleted_accounts").select("*")
            .gte("deleted_at", cutoff)
            .order("deleted_at", desc=True)
            .range(offset, offset + limit - 1)
            .execute()).data or []
        return {"items": rows, "total": total, "page": page, "pages": max(1, (total + limit - 1) // limit)}
    except Exception as e:
        raise HTTPException(500, f"Deleted accounts error: {e}")


@router.post("/admin/users/{user_id}/unban")
def unban_user(user_id: str, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        sb.table("users").update({"role": "user", "status": "active"}).eq("id", user_id).execute()
        _admin_log("admin", "unban_user", user_id, "user", {})
    return {"unbanned": user_id}


@router.post("/admin/users/{user_id}/suspend")
def suspend_user(user_id: str, x_admin_key: str = Header(None), reason: str = Query("")):
    """Temporarily suspend a user — keeps role intact, blocks access."""
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        sb.table("users").update({"status": "suspended"}).eq("id", user_id).execute()
        _admin_log("admin", "suspend_user", user_id, "user", {"reason": reason})
    return {"suspended": user_id}


@router.post("/admin/users/{user_id}/unsuspend")
def unsuspend_user(user_id: str, x_admin_key: str = Header(None)):
    """Lift a suspension — restores status to active (does not change role)."""
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        sb.table("users").update({"status": "active"}).eq("id", user_id).execute()
        _admin_log("admin", "unsuspend_user", user_id, "user", {})
    return {"unsuspended": user_id}


class GrantPlanBody(BaseModel):
    plan:   str = "standard"
    days:   int = 30
    reason: str = "admin_grant"


@router.post("/admin/users/{user_id}/grant-plan")
def grant_plan(user_id: str, body: GrantPlanBody, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        expires = (datetime.now(timezone.utc) + timedelta(days=body.days)).isoformat()
        sb.table("subscriptions").insert({"user_id": user_id, "plan": body.plan, "period": "manual",
            "status": "active", "started_at": datetime.now(timezone.utc).isoformat(),
            "expires_at": expires, "cancel_reason": body.reason, "amount_paid": 0}).execute()
        sb.table("users").update({"plan": body.plan}).eq("id", user_id).execute()
        _admin_log("admin", "grant_plan", user_id, "user", {"plan": body.plan, "days": body.days, "reason": body.reason})
    return {"granted": body.plan, "days": body.days}


@router.post("/admin/users/{user_id}/subscription/cancel")
def cancel_subscription(user_id: str, x_admin_key: str = Header(None), reason: str = Query("admin")):
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        sb.table("subscriptions").update({"status": "cancelled", "cancelled_at": datetime.now(timezone.utc).isoformat(),
            "cancel_reason": reason, "cancelled_by": "admin"}).eq("user_id", user_id).eq("status", "active").execute()
        sb.table("users").update({"plan": "free"}).eq("id", user_id).execute()
        _admin_log("admin", "cancel_subscription", user_id, "subscription", {"reason": reason})
    return {"cancelled": user_id}


class ExtendBody(BaseModel):
    days:   int = 30
    reason: str = "admin"


@router.post("/admin/users/{user_id}/subscription/extend")
def extend_subscription(user_id: str, body: ExtendBody, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        sub = sb.table("subscriptions").select("id,expires_at").eq("user_id", user_id).eq("status","active").limit(1).execute().data
        if sub:
            old_exp = datetime.fromisoformat(sub[0]["expires_at"].replace("Z", "+00:00"))
            new_exp = (old_exp + timedelta(days=body.days)).isoformat()
            sb.table("subscriptions").update({"expires_at": new_exp}).eq("id", sub[0]["id"]).execute()
            _admin_log("admin", "extend_subscription", user_id, "subscription", {"days": body.days, "reason": body.reason})
    return {"extended_days": body.days}


# ─── CHATS ────────────────────────────────────────────────────────────────────

@router.get("/admin/users/{user_id}/chats")
def get_user_chats(
    user_id: str,
    x_admin_key: str = Header(None),
    page_context: str = Query(""),
    page:  int = Query(1, ge=1),
    limit: int = Query(30, ge=1, le=100),
):
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"items": [], "total": 0, "page": 1, "pages": 1}
    try:
        q = sb.table("chat_messages").select("*").eq("user_id", user_id)
        if page_context:
            q = q.eq("page_context", page_context)
        total = (sb.table("chat_messages").select("id", count="exact").eq("user_id", user_id)
                   .execute().count or 0)
        offset = (page - 1) * limit
        rows = q.order("created_at").range(offset, offset + limit - 1).execute().data or []
        return {"items": rows, "total": total, "page": page, "pages": max(1, (total + limit - 1) // limit)}
    except Exception as e:
        raise HTTPException(500, f"Chats error: {e}")



@router.post("/admin/chats/{message_id}/flag")
def flag_message(message_id: str, x_admin_key: str = Header(None), reason: str = Query("admin flag")):
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        sb.table("chat_messages").update({"flagged": True, "flag_reason": reason}).eq("id", message_id).execute()
        _admin_log("admin", "flag_message", message_id, "chat_message", {"reason": reason})
    return {"flagged": message_id}


# ─── ALL CHATS (monitor) ──────────────────────────────────────────────────────

@router.get("/admin/chats")
def list_all_chats(
    x_admin_key: str = Header(None),
    flagged: bool = Query(False),
    page:  int = Query(1, ge=1),
    limit: int = Query(40, ge=1, le=100),
):
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"items": [], "total": 0, "page": 1, "pages": 1}
    def _fetch_chats():
        q = sb.table("chat_messages").select(
            "id, user_id, page_context, role, content, confidence, tokens_used, response_ms, flagged, flag_reason, created_at"
        )
        if flagged:
            q = q.eq("flagged", True)
        cnt_q = sb.table("chat_messages").select("id", count="exact")
        if flagged:
            cnt_q = cnt_q.eq("flagged", True)
        total = cnt_q.execute().count or 0
        offset = (page - 1) * limit
        rows = q.order("created_at", desc=True).range(offset, offset + limit - 1).execute().data or []
        return total, rows

    try:
        try:
            total, rows = _fetch_chats()
        except Exception:
            # Retry once — HTTP/2 connections to Supabase occasionally drop
            total, rows = _fetch_chats()

        # Batch-fetch user info for all unique user_ids in one query
        user_ids = list({r["user_id"] for r in rows if r.get("user_id")})
        user_map: dict = {}
        if user_ids:
            try:
                users = sb.table("users").select("id,name,phone").in_("id", user_ids).execute().data or []
                user_map = {u["id"]: u for u in users}
            except Exception:
                pass

        enriched = []
        for row in rows:
            uid = row.get("user_id")
            u = user_map.get(uid, {})
            enriched.append({**row, "user_name": u.get("name"), "user_phone": u.get("phone")})

        return {"items": enriched, "total": total, "page": page, "pages": max(1, (total + limit - 1) // limit)}
    except Exception as e:
        raise HTTPException(500, f"Chats error: {e}")


# ─── KUNDALIS ─────────────────────────────────────────────────────────────────

@router.get("/admin/users/{user_id}/kundalis")
def get_user_kundalis(user_id: str, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"items": []}
    try:
        rows = sb.table("kundali_log").select(
            "id, birth_date, birth_time, birth_city, calc_ms, is_saved, source, created_at"
        ).eq("user_id", user_id).order("created_at", desc=True).execute().data or []
        return {"items": rows}
    except Exception as e:
        raise HTTPException(500, f"Kundalis error: {e}")


# ─── PALMISTRY ────────────────────────────────────────────────────────────────

@router.get("/admin/users/{user_id}/palmistry")
def get_user_palmistry(user_id: str, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"items": []}
    try:
        rows = sb.table("palmistry_log").select(
            "id, lines_found, confidence, tokens_used, response_ms, created_at"
        ).eq("user_id", user_id).order("created_at", desc=True).execute().data or []
        return {"items": rows}
    except Exception as e:
        raise HTTPException(500, f"Palmistry error: {e}")


# ─── PAYMENTS ─────────────────────────────────────────────────────────────────

@router.get("/admin/users/{user_id}/payments")
def get_user_payments(user_id: str, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"items": []}
    try:
        rows = sb.table("payment_log").select("*").eq("user_id", user_id).order("paid_at", desc=True).execute().data or []
        return {"items": rows}
    except Exception as e:
        raise HTTPException(500, f"Payments error: {e}")


@router.get("/admin/payments")
def list_all_payments(
    x_admin_key: str = Header(None),
    status: str = Query(""),
    page:   int = Query(1, ge=1),
    limit:  int = Query(30, ge=1, le=100),
):
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"items": [], "total": 0, "page": 1, "pages": 1}
    try:
        q = sb.table("payment_log").select("*")
        if status:
            q = q.eq("status", status)
        count_q = sb.table("payment_log").select("id", count="exact")
        if status:
            count_q = count_q.eq("status", status)
        total = _sb(lambda: count_q.execute()).count or 0
        offset = (page - 1) * limit
        rows = _sb(lambda: q.order("paid_at", desc=True).range(offset, offset + limit - 1).execute()).data or []

        # Enrich with user name/phone
        enriched = []
        for row in rows:
            uid = row.get("user_id")
            if uid:
                try:
                    u = sb.table("users").select("name,phone").eq("id", uid).single().execute().data or {}
                    row = {**row, "user_name": u.get("name"), "user_phone": u.get("phone")}
                except Exception:
                    pass
            enriched.append(row)
        return {"items": enriched, "total": total, "page": page, "pages": max(1, (total + limit - 1) // limit)}
    except Exception as e:
        raise HTTPException(500, f"Payments list error: {e}")


@router.post("/admin/payments/{payment_id}/refund")
def refund_payment(payment_id: str, x_admin_key: str = Header(None), amount: Optional[int] = None, reason: str = Query("admin refund")):
    _check(x_admin_key)
    sb = _get_supabase()
    # TODO: Call Cashfree refund API here when Cashfree is integrated
    if sb:
        updates = {"status": "REFUNDED"}
        if amount:
            updates["refund_amount"] = amount
        updates["refund_at"] = datetime.now(timezone.utc).isoformat()
        sb.table("payment_log").update(updates).eq("id", payment_id).execute()
        _admin_log("admin", "refund", str(payment_id), "payment", {"amount": amount, "reason": reason})
    return {"refunded": payment_id}


@router.get("/admin/revenue")
def get_revenue(x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"today": 0, "month": 0, "total": 0}
    today    = datetime.now(timezone.utc).date().isoformat()
    month_ago = (datetime.now(timezone.utc) - timedelta(days=30)).date().isoformat()
    try:
        def _sum(rows): return sum(r.get("amount", 0) or 0 for r in (rows or []))
        t = _sum(_sb(lambda: sb.table("payment_log").select("amount").eq("status","SUCCESS").gte("paid_at", today).execute()).data)
        m = _sum(_sb(lambda: sb.table("payment_log").select("amount").eq("status","SUCCESS").gte("paid_at", month_ago).execute()).data)
        a = _sum(_sb(lambda: sb.table("payment_log").select("amount").eq("status","SUCCESS").execute()).data)
        return {"today": t, "month": m, "total": a}
    except Exception as e:
        raise HTTPException(500, f"Revenue error: {e}")


# ─── SUBSCRIPTIONS (dedicated section) ───────────────────────────────────────

@router.get("/admin/subscriptions")
def list_subscriptions(
    x_admin_key: str = Header(None),
    status:  str = Query(""),   # active | cancelled | expired | all
    plan:    str = Query(""),   # free | standard | premium
    period:  str = Query(""),   # monthly | yearly | manual
    page:    int = Query(1, ge=1),
    limit:   int = Query(30, ge=1, le=100),
    search:  str = Query(""),   # name or phone
):
    """Full subscription list with user enrichment, revenue, churn metrics."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"items": [], "total": 0, "page": 1, "pages": 1, "summary": {}}
    try:
        q = sb.table("subscriptions").select(
            "id, user_id, plan, period, status, amount_paid, "
            "started_at, expires_at, cancelled_at, cancel_reason, cancelled_by"
        )
        if status:  q = q.eq("status", status)
        if plan:    q = q.eq("plan", plan)
        if period:  q = q.eq("period", period)

        count_q = sb.table("subscriptions").select("id", count="exact")
        if status:  count_q = count_q.eq("status", status)
        if plan:    count_q = count_q.eq("plan", plan)
        if period:  count_q = count_q.eq("period", period)
        total = count_q.execute().count or 0

        offset = (page - 1) * limit
        rows = q.order("started_at", desc=True).range(offset, offset + limit - 1).execute().data or []

        # Batch-fetch user info
        uids = list({r["user_id"] for r in rows if r.get("user_id")})
        user_map: dict = {}
        if uids:
            try:
                users = sb.table("users").select("id,name,phone,email").in_("id", uids).execute().data or []
                user_map = {u["id"]: u for u in users}
            except Exception:
                pass

        # Apply search filter post-fetch (name/phone)
        enriched = []
        for r in rows:
            u = user_map.get(r.get("user_id", ""), {})
            name  = u.get("name", "")
            phone = u.get("phone", "")
            if search and search.lower() not in (name + phone).lower():
                continue
            days_left = None
            if r.get("expires_at"):
                try:
                    exp = datetime.fromisoformat(r["expires_at"].replace("Z", "+00:00"))
                    days_left = (exp - datetime.now(timezone.utc)).days
                except Exception:
                    pass
            enriched.append({**r, "user_name": name, "user_phone": phone,
                              "user_email": u.get("email"), "days_left": days_left})

        # Summary metrics (always over full dataset, ignore page filter)
        try:
            now_iso = datetime.now(timezone.utc).isoformat()
            month_ago = (datetime.now(timezone.utc) - timedelta(days=30)).isoformat()
            active_count  = sb.table("subscriptions").select("id", count="exact").eq("status","active").execute().count or 0
            new_this_month= sb.table("subscriptions").select("id", count="exact").gte("started_at", month_ago).execute().count or 0
            cancelled_mo  = sb.table("subscriptions").select("id", count="exact").eq("status","cancelled").gte("cancelled_at", month_ago).execute().count or 0
            expiring_7d   = sb.table("subscriptions").select("id", count="exact").eq("status","active").lte("expires_at", (datetime.now(timezone.utc)+timedelta(days=7)).isoformat()).gte("expires_at", now_iso).execute().count or 0
            rev_rows      = sb.table("payment_log").select("amount").eq("status","SUCCESS").execute().data or []
            total_rev     = sum(r.get("amount",0) or 0 for r in rev_rows)
            plan_dist_raw = sb.table("subscriptions").select("plan,period,status").eq("status","active").execute().data or []
            plan_dist: dict = {}
            for r in plan_dist_raw:
                k = f"{r['plan']}_{r['period']}"
                plan_dist[k] = plan_dist.get(k, 0) + 1
            summary = {
                "active": active_count,
                "new_month": new_this_month,
                "cancelled_month": cancelled_mo,
                "expiring_7d": expiring_7d,
                "total_revenue_paise": total_rev,
                "plan_distribution": plan_dist,
            }
        except Exception:
            summary = {}

        if search:
            total = len(enriched)
        return {"items": enriched, "total": total, "page": page,
                "pages": max(1, (total + limit - 1) // limit), "summary": summary}
    except Exception as e:
        raise HTTPException(500, f"Subscriptions error: {e}")


# ─── LOGINS ───────────────────────────────────────────────────────────────────

@router.get("/admin/users/{user_id}/logins")
def get_user_logins(user_id: str, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"items": []}
    try:
        rows = sb.table("login_log").select("*").eq("user_id", user_id).order("logged_at", desc=True).limit(50).execute().data or []
        return {"items": rows}
    except Exception as e:
        raise HTTPException(500, f"Logins error: {e}")


# ─── USER ACTIVITY TIMELINE ───────────────────────────────────────────────────

@router.get("/admin/users/{user_id}/activity")
def get_user_activity(
    user_id: str,
    days:  int = Query(90, ge=1, le=365),
    page:  int = Query(1, ge=1),
    limit: int = Query(60, ge=1, le=200),
    x_admin_key: str = Header(None),
):
    """
    Unified activity timeline for a single user.
    Merges: logins, kundali calculations, saved kundalis, palmistry,
            chat messages (paired as Q→A conversations), payments.
    Returns events sorted newest-first, grouped by date on the frontend.
    """
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"events": [], "total": 0, "page": page, "pages": 1}

    since = (datetime.now(timezone.utc) - timedelta(days=days)).isoformat()

    try:
        def q(fn):
            return fn()

        queries = {
            "logins": lambda: sb.table("login_log")
                .select("id,logged_at,ip,device,success,fail_reason")
                .eq("user_id", user_id).gte("logged_at", since)
                .order("logged_at", desc=True).limit(500).execute().data or [],

            "kundalis": lambda: sb.table("kundali_log")
                .select("id,created_at,birth_date,birth_time,birth_city,source,calc_ms,is_saved")
                .eq("user_id", user_id).gte("created_at", since)
                .order("created_at", desc=True).limit(200).execute().data or [],

            "saved_kundalis": lambda: sb.table("saved_kundalis")
                .select("id,created_at,label,birth_date,birth_time,birth_place")
                .eq("user_id", user_id).gte("created_at", since)
                .order("created_at", desc=True).limit(200).execute().data or [],

            "palmistry": lambda: sb.table("palmistry_log")
                .select("id,created_at,confidence,lines_found,tokens_used,response_ms")
                .eq("user_id", user_id).gte("created_at", since)
                .order("created_at", desc=True).limit(100).execute().data or [],

            "chats": lambda: sb.table("chat_messages")
                .select("id,created_at,session_id,page_context,role,content,confidence,tokens_used,response_ms,flagged,flag_reason")
                .eq("user_id", user_id).gte("created_at", since)
                .order("created_at", desc=False).limit(2000).execute().data or [],

            "payments": lambda: sb.table("payment_log")
                .select("id,paid_at,amount,status,payment_method,fail_reason")
                .eq("user_id", user_id).gte("paid_at", since)
                .order("paid_at", desc=True).limit(100).execute().data or [],
        }

        results = {}
        with ThreadPoolExecutor(max_workers=6) as ex:
            futs = {ex.submit(q, fn): key for key, fn in queries.items()}
            for fut in as_completed(futs):
                results[futs[fut]] = fut.result()

        events = []

        # ── Logins ────────────────────────────────────────────────────────────
        for r in results["logins"]:
            events.append({
                "type":    "login",
                "ts":      r.get("logged_at"),
                "icon":    "🔑",
                "title":   "Login" if r.get("success") else "Failed Login",
                "detail": {
                    "success":    r.get("success"),
                    "ip":         r.get("ip"),
                    "device":     r.get("device"),
                    "fail_reason": r.get("fail_reason"),
                },
            })

        # ── Kundali calculations ───────────────────────────────────────────────
        for r in results["kundalis"]:
            events.append({
                "type":  "kundali",
                "ts":    r.get("created_at"),
                "icon":  "🪐",
                "title": f"Kundali Calculated — {r.get('birth_date','?')} {r.get('birth_time','')}",
                "detail": {
                    "birth_date":  r.get("birth_date"),
                    "birth_time":  r.get("birth_time"),
                    "birth_city":  r.get("birth_city"),
                    "source":      r.get("source"),
                    "calc_ms":     r.get("calc_ms"),
                    "is_saved":    r.get("is_saved"),
                },
            })

        # ── Saved kundalis ─────────────────────────────────────────────────────
        for r in results["saved_kundalis"]:
            events.append({
                "type":  "saved_kundali",
                "ts":    r.get("created_at"),
                "icon":  "💾",
                "title": f"Kundali Saved — {r.get('label','My Chart')}",
                "detail": {
                    "label":       r.get("label"),
                    "birth_date":  r.get("birth_date"),
                    "birth_time":  r.get("birth_time"),
                    "birth_place": r.get("birth_place"),
                },
            })

        # ── Palmistry ─────────────────────────────────────────────────────────
        for r in results["palmistry"]:
            lines = r.get("lines_found") or {}
            line_names = ", ".join(lines.keys()) if isinstance(lines, dict) else str(lines)
            events.append({
                "type":  "palmistry",
                "ts":    r.get("created_at"),
                "icon":  "✋",
                "title": f"Palm Read — {r.get('confidence','?')} confidence",
                "detail": {
                    "confidence":  r.get("confidence"),
                    "lines_found": line_names,
                    "tokens_used": r.get("tokens_used"),
                    "response_ms": r.get("response_ms"),
                },
            })

        # ── Payments ──────────────────────────────────────────────────────────
        for r in results["payments"]:
            amt = r.get("amount", 0) or 0
            events.append({
                "type":  "payment",
                "ts":    r.get("paid_at"),
                "icon":  "💳",
                "title": f"Payment ₹{amt//100 if amt > 1000 else amt} — {r.get('status','?')}",
                "detail": {
                    "amount":         amt,
                    "status":         r.get("status"),
                    "payment_method": r.get("payment_method"),
                    "fail_reason":    r.get("fail_reason"),
                },
            })

        # ── Chat conversations — pair user+assistant by session ───────────────
        # Group messages by session_id, then pair consecutive user→assistant turns
        from collections import defaultdict
        sessions: dict = defaultdict(list)
        for m in results["chats"]:
            sessions[m["session_id"]].append(m)

        for sid, msgs in sessions.items():
            # Walk through messages pairing user question → next assistant reply
            i = 0
            while i < len(msgs):
                m = msgs[i]
                if m["role"] == "user":
                    question = m["content"]
                    reply    = None
                    reply_meta = {}
                    if i + 1 < len(msgs) and msgs[i + 1]["role"] == "assistant":
                        r2 = msgs[i + 1]
                        reply = r2["content"]
                        reply_meta = {
                            "confidence": r2.get("confidence"),
                            "tokens_used": r2.get("tokens_used"),
                            "response_ms": r2.get("response_ms"),
                        }
                        i += 2
                    else:
                        i += 1
                    # Truncate for summary title (first 80 chars)
                    title_q = question[:80] + ("…" if len(question) > 80 else "")
                    events.append({
                        "type":  "chat",
                        "ts":    m["created_at"],
                        "icon":  "💬",
                        "title": title_q,
                        "detail": {
                            "page_context": m.get("page_context"),
                            "session_id":   sid,
                            "question":     question,
                            "reply":        reply,
                            "flagged":      m.get("flagged") or (reply_meta and msgs[i-1].get("flagged")),
                            "flag_reason":  m.get("flag_reason"),
                            **reply_meta,
                        },
                    })
                else:
                    i += 1  # orphan assistant message — skip

        # ── Sort newest-first ─────────────────────────────────────────────────
        events.sort(key=lambda e: e["ts"] or "", reverse=True)

        # ── Paginate ──────────────────────────────────────────────────────────
        total  = len(events)
        pages  = max(1, (total + limit - 1) // limit)
        offset = (page - 1) * limit
        paged  = events[offset: offset + limit]

        return {"events": paged, "total": total, "page": page, "pages": pages}

    except Exception as e:
        import traceback
        raise HTTPException(500, f"Activity error: {e}\n{traceback.format_exc()}")


# ─── ANALYTICS ────────────────────────────────────────────────────────────────

@router.get("/admin/analytics/chat")
def chat_analytics(x_admin_key: str = Header(None)):
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        return {"top_questions": [], "context_dist": []}
    month_ago = (datetime.now(timezone.utc) - timedelta(days=30)).isoformat()
    try:
        msgs = sb.table("chat_messages").select("content, page_context").eq("role", "user").gte("created_at", month_ago).execute().data or []
        from collections import Counter
        questions = Counter(m.get("content","").strip() for m in msgs if m.get("content"))
        contexts  = Counter(m.get("page_context","general") for m in msgs)
        top_q = [{"content": c, "times": n} for c, n in questions.most_common(20)]
        ctx_d = [{"page_context": c, "count": n} for c, n in contexts.most_common()]
        return {"top_questions": top_q, "context_dist": ctx_d}
    except Exception as e:
        raise HTTPException(500, f"Analytics error: {e}")


# ─── ADMIN LOG ────────────────────────────────────────────────────────────────

@router.get("/admin/logs")
def list_admin_logs(
    x_admin_key: str = Header(None),
    page:  int = Query(1, ge=1),
    limit: int = Query(50, ge=1, le=200),
    endpoint_filter: str = Query(""),   # legacy compat
):
    _check(x_admin_key)
    sb = _get_supabase()
    if sb:
        try:
            total = _sb(lambda: sb.table("admin_log").select("id", count="exact").execute()).count or 0
            offset = (page - 1) * limit
            rows = _sb(lambda: sb.table("admin_log").select(
                "id, admin_id, action, target_id, target_type, details, performed_at"
            ).order("performed_at", desc=True).range(offset, offset + limit - 1).execute()).data or []
            # Enrich with admin name
            enriched = []
            for row in rows:
                try:
                    u = sb.table("users").select("name").eq("id", row.get("admin_id","")).single().execute().data or {}
                    row = {**row, "admin_name": u.get("name", "admin")}
                except Exception:
                    row = {**row, "admin_name": "admin"}
                enriched.append(row)
            return {"items": enriched, "total": total, "page": page, "pages": max(1, (total + limit - 1) // limit)}
        except Exception as e:
            raise HTTPException(500, f"Admin log error: {e}")

    # Activity log fallback (legacy)
    try:
        offset = (page - 1) * limit
        with _logs_conn() as conn:
            if endpoint_filter:
                rows  = conn.execute("SELECT * FROM activity_logs WHERE endpoint LIKE ? ORDER BY id DESC LIMIT ? OFFSET ?", (f"%{endpoint_filter}%", limit, offset)).fetchall()
                total = conn.execute("SELECT COUNT(*) FROM activity_logs WHERE endpoint LIKE ?", (f"%{endpoint_filter}%",)).fetchone()[0]
            else:
                rows  = conn.execute("SELECT * FROM activity_logs ORDER BY id DESC LIMIT ? OFFSET ?", (limit, offset)).fetchall()
                total = conn.execute("SELECT COUNT(*) FROM activity_logs").fetchone()[0]
        return {"items": [dict(r) for r in rows], "total": total, "page": page, "pages": max(1, (total + limit - 1) // limit)}
    except Exception as e:
        raise HTTPException(500, f"Logs error: {e}")


@router.delete("/admin/logs")
def clear_logs(x_admin_key: str = Header(None)):
    _check(x_admin_key)
    try:
        with _logs_conn() as conn:
            conn.execute("DELETE FROM activity_logs")
            conn.commit()
    except Exception:
        pass
    return {"cleared": True}


# ─── PLAN MANAGEMENT ──────────────────────────────────────────────────────────

class PlanBody(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    price_inr: int = 0
    duration_days: int = 30
    daily_message_limit: Optional[int] = None
    daily_token_limit: Optional[int] = None
    features: list = []
    is_active: bool = True
    sort_order: int = 0
    badge_text: Optional[str] = None


class PlanUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    price_inr: Optional[int] = None
    duration_days: Optional[int] = None
    daily_message_limit: Optional[int] = None
    daily_token_limit: Optional[int] = None
    features: Optional[list] = None
    is_active: Optional[bool] = None
    sort_order: Optional[int] = None
    badge_text: Optional[str] = None


@router.get("/admin/plans")
def list_plans(x_admin_key: str = Header(None)):
    """List all subscription plans (including inactive)."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        raise HTTPException(503, "Database unavailable")
    try:
        rows = _sb(lambda: sb.table("subscription_plans").select("*").order("sort_order").execute()).data or []
        return {"plans": rows}
    except Exception as e:
        raise HTTPException(500, f"Plans error: {e}")


@router.post("/admin/plans")
def create_plan(body: PlanBody, x_admin_key: str = Header(None)):
    """Create a new subscription plan."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        raise HTTPException(503, "Database unavailable")
    try:
        row = body.model_dump()
        row["updated_at"] = datetime.now(timezone.utc).isoformat()
        _sb(lambda: sb.table("subscription_plans").insert(row).execute())
        _admin_log("admin", "create_plan", body.id, "plan", {"name": body.name, "price_inr": body.price_inr})
        return {"created": body.id}
    except Exception as e:
        raise HTTPException(500, f"Create plan error: {e}")


@router.put("/admin/plans/{plan_id}")
def update_plan(plan_id: str, body: PlanUpdate, x_admin_key: str = Header(None)):
    """Update an existing subscription plan."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        raise HTTPException(503, "Database unavailable")
    updates = {k: v for k, v in body.model_dump().items() if v is not None}
    if not updates:
        raise HTTPException(400, "Nothing to update")
    updates["updated_at"] = datetime.now(timezone.utc).isoformat()
    try:
        _sb(lambda: sb.table("subscription_plans").update(updates).eq("id", plan_id).execute())
        _admin_log("admin", "update_plan", plan_id, "plan", {"updates": list(updates.keys())})
        return {"updated": plan_id}
    except Exception as e:
        raise HTTPException(500, f"Update plan error: {e}")


@router.delete("/admin/plans/{plan_id}")
def delete_plan(plan_id: str, x_admin_key: str = Header(None)):
    """Delete a plan — only allowed if no active subscriptions use it."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        raise HTTPException(503, "Database unavailable")
    try:
        # Check for active subscriptions using this plan
        active_count = _sb(
            lambda: sb.table("subscriptions")
            .select("id", count="exact")
            .eq("plan", plan_id)
            .eq("status", "active")
            .execute()
        ).count or 0
        if active_count > 0:
            raise HTTPException(400, f"Cannot delete plan '{plan_id}': {active_count} active subscription(s) use it.")
        _sb(lambda: sb.table("subscription_plans").delete().eq("id", plan_id).execute())
        _admin_log("admin", "delete_plan", plan_id, "plan", {})
        return {"deleted": plan_id}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"Delete plan error: {e}")


@router.patch("/admin/plans/{plan_id}/toggle")
def toggle_plan(plan_id: str, x_admin_key: str = Header(None)):
    """Toggle is_active for a plan."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        raise HTTPException(503, "Database unavailable")
    try:
        res = _sb(lambda: sb.table("subscription_plans").select("is_active").eq("id", plan_id).maybe_single().execute())
        if not res or not res.data:
            raise HTTPException(404, f"Plan '{plan_id}' not found")
        current = res.data.get("is_active", True)
        new_val = not current
        _sb(lambda: sb.table("subscription_plans").update({
            "is_active": new_val,
            "updated_at": datetime.now(timezone.utc).isoformat(),
        }).eq("id", plan_id).execute())
        _admin_log("admin", "toggle_plan", plan_id, "plan", {"is_active": new_val})
        return {"plan_id": plan_id, "is_active": new_val}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"Toggle plan error: {e}")


# ─── FEATURE FLAG MANAGEMENT ──────────────────────────────────────────────────

class FeatureFlagBody(BaseModel):
    key: str
    name: str
    description: Optional[str] = None
    category: str = "general"
    is_globally_enabled: bool = True


class FeatureFlagUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    is_globally_enabled: Optional[bool] = None


@router.get("/admin/feature-flags")
def list_feature_flags(x_admin_key: str = Header(None)):
    """List all feature flags."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        raise HTTPException(503, "Database unavailable")
    try:
        rows = _sb(lambda: sb.table("feature_flags").select("*").order("category").order("key").execute()).data or []
        return {"flags": rows}
    except Exception as e:
        raise HTTPException(500, f"Feature flags error: {e}")


@router.post("/admin/feature-flags")
def create_feature_flag(body: FeatureFlagBody, x_admin_key: str = Header(None)):
    """Create a new feature flag."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        raise HTTPException(503, "Database unavailable")
    try:
        _sb(lambda: sb.table("feature_flags").insert(body.model_dump()).execute())
        _admin_log("admin", "create_feature_flag", body.key, "feature_flag", {"name": body.name})
        return {"created": body.key}
    except Exception as e:
        raise HTTPException(500, f"Create feature flag error: {e}")


@router.patch("/admin/feature-flags/{key}")
def update_feature_flag(key: str, body: FeatureFlagUpdate, x_admin_key: str = Header(None)):
    """Update a feature flag (name, description, is_globally_enabled)."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        raise HTTPException(503, "Database unavailable")
    updates = {k: v for k, v in body.model_dump().items() if v is not None}
    if not updates:
        raise HTTPException(400, "Nothing to update")
    try:
        res = _sb(lambda: sb.table("feature_flags").select("key").eq("key", key).maybe_single().execute())
        if not res or not res.data:
            raise HTTPException(404, f"Feature flag '{key}' not found")
        _sb(lambda: sb.table("feature_flags").update(updates).eq("key", key).execute())
        _admin_log("admin", "update_feature_flag", key, "feature_flag", {"updates": updates})
        return {"updated": key}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(500, f"Update feature flag error: {e}")


@router.delete("/admin/feature-flags/{key}")
def delete_feature_flag(key: str, x_admin_key: str = Header(None)):
    """Delete a feature flag."""
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        raise HTTPException(503, "Database unavailable")
    try:
        _sb(lambda: sb.table("feature_flags").delete().eq("key", key).execute())
        _admin_log("admin", "delete_feature_flag", key, "feature_flag", {})
        return {"deleted": key}
    except Exception as e:
        raise HTTPException(500, f"Delete feature flag error: {e}")


# ─── USAGE STATS ──────────────────────────────────────────────────────────────

@router.get("/admin/usage/stats")
def get_usage_stats(
    x_admin_key: str = Header(None),
    days: int = Query(7, ge=1, le=90),
):
    """
    Aggregated usage stats.
    Returns:
      daily_totals: [{date, total_messages, total_tokens, unique_users}]
      top_users:    [{user_id, name, messages, tokens}]
      plan_breakdown: {basic: 120, pro: 45, ...}
    """
    _check(x_admin_key)
    sb = _get_supabase()
    if not sb:
        raise HTTPException(503, "Database unavailable")

    from datetime import timezone as _tz
    ist_offset = timedelta(hours=5, minutes=30)
    since_ist = (datetime.now(_tz.utc) + ist_offset - timedelta(days=days)).date().isoformat()

    try:
        # Daily totals
        daily_rows = _sb(
            lambda: sb.table("daily_usage")
            .select("usage_date, messages_used, tokens_used, user_id")
            .gte("usage_date", since_ist)
            .order("usage_date")
            .execute()
        ).data or []

        # Aggregate by date
        from collections import defaultdict
        date_agg: dict = defaultdict(lambda: {"total_messages": 0, "total_tokens": 0, "users": set()})
        user_agg: dict = defaultdict(lambda: {"messages": 0, "tokens": 0})

        for r in daily_rows:
            d = r.get("usage_date", "")
            date_agg[d]["total_messages"] += r.get("messages_used", 0)
            date_agg[d]["total_tokens"]   += r.get("tokens_used", 0)
            date_agg[d]["users"].add(r.get("user_id"))
            uid = r.get("user_id", "")
            user_agg[uid]["messages"] += r.get("messages_used", 0)
            user_agg[uid]["tokens"]   += r.get("tokens_used", 0)

        daily_totals = [
            {
                "date":           d,
                "total_messages": v["total_messages"],
                "total_tokens":   v["total_tokens"],
                "unique_users":   len(v["users"]),
            }
            for d, v in sorted(date_agg.items())
        ]

        # Top users (by message count)
        top_user_ids = sorted(user_agg.keys(), key=lambda u: user_agg[u]["messages"], reverse=True)[:20]
        top_users = []
        if top_user_ids:
            try:
                user_rows = sb.table("users").select("id, name").in_("id", top_user_ids).execute().data or []
                name_map = {u["id"]: u.get("name", "") for u in user_rows}
            except Exception:
                name_map = {}
            for uid in top_user_ids:
                top_users.append({
                    "user_id":  uid,
                    "name":     name_map.get(uid, ""),
                    "messages": user_agg[uid]["messages"],
                    "tokens":   user_agg[uid]["tokens"],
                })

        # Plan breakdown (active subscriptions)
        plan_breakdown: dict = {}
        try:
            plan_rows = _sb(
                lambda: sb.table("daily_usage")
                .select("plan_id")
                .gte("usage_date", since_ist)
                .execute()
            ).data or []
            from collections import Counter
            plan_counts = Counter(r.get("plan_id") or "free" for r in plan_rows)
            plan_breakdown = dict(plan_counts)
        except Exception:
            pass

        return {
            "daily_totals":   daily_totals,
            "top_users":      top_users,
            "plan_breakdown": plan_breakdown,
        }

    except Exception as e:
        raise HTTPException(500, f"Usage stats error: {e}")
