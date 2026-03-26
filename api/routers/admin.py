"""
Admin panel router — Brahm AI v2
Protected by X-Admin-Key header (set ADMIN_SECRET in env, default: brahm-admin-2024).

All endpoints require header: X-Admin-Key: <secret>
All mutating actions logged to admin_log table in Supabase.
"""
import os
import json
import threading
import sqlite3
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, HTTPException, Header, Query, Request
from pydantic import BaseModel
from typing import Optional

router = APIRouter()

ADMIN_SECRET = os.getenv("ADMIN_SECRET", "brahm-admin-2024")

# ─── Auth ─────────────────────────────────────────────────────────────────────

def _check(key: str | None):
    if not key or key != ADMIN_SECRET:
        raise HTTPException(status_code=401, detail="Unauthorized — wrong admin key")


# ─── DB backend: try Supabase first, fall back to SQLite ─────────────────────

def _get_supabase():
    """Returns Supabase client or None if not configured."""
    try:
        from api.supabase_client import get_supabase
        return get_supabase()
    except Exception:
        return None


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
            duration_ms INTEGER DEFAULT 0
        )
    """)
    conn.commit()
    return conn


def log_request(method: str, endpoint: str, session_id: str, status_code: int, duration_ms: int):
    """Called from logging middleware — fire-and-forget."""
    try:
        ts = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
        with _log_lock:
            with _logs_conn() as conn:
                conn.execute(
                    "INSERT INTO activity_logs (ts,method,endpoint,session_id,status_code,duration_ms)"
                    " VALUES (?,?,?,?,?,?)",
                    (ts, method, endpoint, session_id or "", status_code, duration_ms),
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
        try:
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
            # Get total count
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

            # Enrich with aggregate counts
            users = []
            for u in rows:
                uid = u.get("id") or u.get("session_id", "")
                total_chats    = sb.table("chat_messages").select("id", count="exact").eq("user_id", uid).execute().count or 0
                total_kundalis = sb.table("kundali_log").select("id", count="exact").eq("user_id", uid).execute().count or 0
                total_palm     = sb.table("palmistry_log").select("id", count="exact").eq("user_id", uid).execute().count or 0
                pay_r          = sb.table("payment_log").select("amount").eq("user_id", uid).eq("status", "SUCCESS").execute()
                lifetime_paid  = sum(r.get("amount", 0) or 0 for r in (pay_r.data or [])) / 100

                users.append({**u, "total_chats": total_chats, "total_kundalis": total_kundalis,
                               "total_palm": total_palm, "lifetime_paid_inr": lifetime_paid})

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
        total = sb.table("deleted_accounts").select("id", count="exact") \
            .gte("deleted_at", cutoff).execute().count or 0
        offset = (page - 1) * limit
        rows = sb.table("deleted_accounts").select("*") \
            .gte("deleted_at", cutoff) \
            .order("deleted_at", desc=True) \
            .range(offset, offset + limit - 1) \
            .execute().data or []
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
    try:
        q = sb.table("chat_messages").select(
            "id, user_id, page_context, role, content, confidence, tokens_used, response_ms, flagged, flag_reason, created_at"
        )
        if flagged:
            q = q.eq("flagged", True)
        total = (sb.table("chat_messages").select("id", count="exact")
                   .eq("flagged", True) if flagged else sb.table("chat_messages").select("id", count="exact")).execute().count or 0
        offset = (page - 1) * limit
        rows = q.order("created_at", desc=True).range(offset, offset + limit - 1).execute().data or []

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
        total = count_q.execute().count or 0
        offset = (page - 1) * limit
        rows = q.order("paid_at", desc=True).range(offset, offset + limit - 1).execute().data or []

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
        t = _sum(sb.table("payment_log").select("amount").eq("status","SUCCESS").gte("paid_at", today).execute().data)
        m = _sum(sb.table("payment_log").select("amount").eq("status","SUCCESS").gte("paid_at", month_ago).execute().data)
        a = _sum(sb.table("payment_log").select("amount").eq("status","SUCCESS").execute().data)
        return {"today": t, "month": m, "total": a}
    except Exception as e:
        raise HTTPException(500, f"Revenue error: {e}")


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
            total = sb.table("admin_log").select("id", count="exact").execute().count or 0
            offset = (page - 1) * limit
            rows = sb.table("admin_log").select(
                "id, admin_id, action, target_id, target_type, details, performed_at"
            ).order("performed_at", desc=True).range(offset, offset + limit - 1).execute().data or []
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
