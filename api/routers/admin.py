"""
Admin panel router — protected by X-Admin-Key header.
Set ADMIN_SECRET env var on the VM (default: brahm-admin-2024).
All endpoints require the header: X-Admin-Key: <secret>
"""
import os, sqlite3, json, threading
from datetime import datetime, timezone
from fastapi import APIRouter, HTTPException, Header, Query
from pydantic import BaseModel
from api.config import USERS_DB, DATA_DIR

router = APIRouter()

ADMIN_SECRET = os.getenv("ADMIN_SECRET", "brahm-admin-2024")
LOGS_DB = os.path.join(DATA_DIR, "activity_logs.db")

_log_lock = threading.Lock()


# ─── Auth helper ─────────────────────────────────────────────────────────────

def _check(key: str):
    if key != ADMIN_SECRET:
        raise HTTPException(status_code=401, detail="Unauthorized — wrong admin key")


# ─── DB helpers ──────────────────────────────────────────────────────────────

def _users_conn():
    conn = sqlite3.connect(USERS_DB)
    conn.row_factory = sqlite3.Row
    return conn


def _logs_conn():
    conn = sqlite3.connect(LOGS_DB)
    conn.row_factory = sqlite3.Row
    conn.execute("""
        CREATE TABLE IF NOT EXISTS activity_logs (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            ts            TEXT    NOT NULL,
            method        TEXT,
            endpoint      TEXT,
            session_id    TEXT    DEFAULT '',
            status_code   INTEGER DEFAULT 0,
            duration_ms   INTEGER DEFAULT 0
        )
    """)
    conn.commit()
    return conn


def log_request(method: str, endpoint: str, session_id: str, status_code: int, duration_ms: int):
    """Called from the logging middleware — fire-and-forget, never raises."""
    try:
        ts = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
        with _log_lock:
            with _logs_conn() as conn:
                conn.execute(
                    "INSERT INTO activity_logs (ts, method, endpoint, session_id, status_code, duration_ms)"
                    " VALUES (?,?,?,?,?,?)",
                    (ts, method, endpoint, session_id or "", status_code, duration_ms),
                )
                conn.commit()
    except Exception:
        pass


# ─── Stats ───────────────────────────────────────────────────────────────────

@router.get("/admin/stats")
def get_stats(x_admin_key: str = Header(None)):
    _check(x_admin_key)
    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    with _users_conn() as uc:
        total_users = uc.execute("SELECT COUNT(*) FROM users").fetchone()[0]

    with _logs_conn() as lc:
        total_logs = lc.execute("SELECT COUNT(*) FROM activity_logs").fetchone()[0]
        today_req  = lc.execute(
            "SELECT COUNT(*) FROM activity_logs WHERE ts LIKE ?", (f"{today}%",)
        ).fetchone()[0]
        active_today = lc.execute(
            "SELECT COUNT(DISTINCT session_id) FROM activity_logs"
            " WHERE ts LIKE ? AND session_id != ''",
            (f"{today}%",),
        ).fetchone()[0]
        top_ep = lc.execute(
            "SELECT endpoint, COUNT(*) cnt FROM activity_logs"
            " GROUP BY endpoint ORDER BY cnt DESC LIMIT 10"
        ).fetchall()

    return {
        "total_users":        total_users,
        "total_requests":     total_logs,
        "requests_today":     today_req,
        "active_users_today": active_today,
        "top_endpoints": [{"endpoint": r["endpoint"], "count": r["cnt"]} for r in top_ep],
    }


# ─── Users ───────────────────────────────────────────────────────────────────

@router.get("/admin/users")
def list_users(
    x_admin_key: str = Header(None),
    page:   int = Query(1,   ge=1),
    limit:  int = Query(20,  ge=1, le=100),
    search: str = Query(""),
):
    _check(x_admin_key)
    offset = (page - 1) * limit

    with _users_conn() as conn:
        if search:
            rows  = conn.execute(
                "SELECT session_id, data FROM users WHERE data LIKE ? LIMIT ? OFFSET ?",
                (f"%{search}%", limit, offset),
            ).fetchall()
            total = conn.execute(
                "SELECT COUNT(*) FROM users WHERE data LIKE ?", (f"%{search}%",)
            ).fetchone()[0]
        else:
            rows  = conn.execute(
                "SELECT session_id, data FROM users LIMIT ? OFFSET ?", (limit, offset)
            ).fetchall()
            total = conn.execute("SELECT COUNT(*) FROM users").fetchone()[0]

    users = []
    for row in rows:
        try:
            d = json.loads(row["data"])
        except Exception:
            d = {}
        users.append({
            "session_id": row["session_id"],
            "name":       d.get("name", ""),
            "place":      d.get("place", ""),
            "date":       d.get("date", ""),
            "rashi":      d.get("rashi", ""),
            "nakshatra":  d.get("nakshatra", ""),
            "language":   d.get("language", "english"),
            "plan":       d.get("plan", "free"),
        })

    return {
        "users": users,
        "total": total,
        "page":  page,
        "pages": max(1, (total + limit - 1) // limit),
    }


@router.get("/admin/users/{session_id}")
def get_user_detail(session_id: str, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    with _users_conn() as conn:
        row = conn.execute(
            "SELECT data FROM users WHERE session_id=?", (session_id,)
        ).fetchone()
    if not row:
        raise HTTPException(status_code=404, detail="User not found")
    return json.loads(row["data"])


class UserUpdate(BaseModel):
    plan: str | None = None   # free | jyotishi | acharya
    name: str | None = None


@router.put("/admin/users/{session_id}")
def update_user(session_id: str, body: UserUpdate, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    with _users_conn() as conn:
        row = conn.execute(
            "SELECT data FROM users WHERE session_id=?", (session_id,)
        ).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="User not found")
        d = json.loads(row["data"])
        if body.plan is not None:
            d["plan"] = body.plan
        if body.name is not None:
            d["name"] = body.name
        conn.execute(
            "UPDATE users SET data=? WHERE session_id=?", (json.dumps(d), session_id)
        )
        conn.commit()
    return d


@router.delete("/admin/users/{session_id}")
def delete_user(session_id: str, x_admin_key: str = Header(None)):
    _check(x_admin_key)
    with _users_conn() as conn:
        conn.execute("DELETE FROM users WHERE session_id=?", (session_id,))
        conn.commit()
    return {"deleted": session_id}


# ─── Activity Logs ───────────────────────────────────────────────────────────

@router.get("/admin/logs")
def list_logs(
    x_admin_key:     str = Header(None),
    page:            int = Query(1,   ge=1),
    limit:           int = Query(50,  ge=1, le=200),
    endpoint_filter: str = Query(""),
):
    _check(x_admin_key)
    offset = (page - 1) * limit

    with _logs_conn() as conn:
        if endpoint_filter:
            rows  = conn.execute(
                "SELECT * FROM activity_logs WHERE endpoint LIKE ? ORDER BY id DESC LIMIT ? OFFSET ?",
                (f"%{endpoint_filter}%", limit, offset),
            ).fetchall()
            total = conn.execute(
                "SELECT COUNT(*) FROM activity_logs WHERE endpoint LIKE ?",
                (f"%{endpoint_filter}%",),
            ).fetchone()[0]
        else:
            rows  = conn.execute(
                "SELECT * FROM activity_logs ORDER BY id DESC LIMIT ? OFFSET ?", (limit, offset)
            ).fetchall()
            total = conn.execute("SELECT COUNT(*) FROM activity_logs").fetchone()[0]

    return {
        "logs":  [dict(r) for r in rows],
        "total": total,
        "page":  page,
        "pages": max(1, (total + limit - 1) // limit),
    }


@router.delete("/admin/logs")
def clear_logs(x_admin_key: str = Header(None)):
    _check(x_admin_key)
    with _logs_conn() as conn:
        conn.execute("DELETE FROM activity_logs")
        conn.commit()
    return {"cleared": True}
