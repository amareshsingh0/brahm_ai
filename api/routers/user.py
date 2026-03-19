"""
User profile router — SQLite backed, keyed by session_id (localStorage UUID).
"""
import sqlite3, json
from fastapi import APIRouter, HTTPException, Query
from api.models.user import UserProfile
from api.config import USERS_DB

router = APIRouter()


def _conn():
    conn = sqlite3.connect(USERS_DB)
    conn.row_factory = sqlite3.Row
    conn.execute("""
        CREATE TABLE IF NOT EXISTS users (
            session_id TEXT PRIMARY KEY,
            data TEXT NOT NULL
        )
    """)
    conn.commit()
    return conn


@router.get("/user", response_model=UserProfile)
def get_user(session_id: str = Query(...)):
    with _conn() as conn:
        row = conn.execute("SELECT data FROM users WHERE session_id=?", (session_id,)).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="User not found")
        return json.loads(row["data"])


@router.post("/user", response_model=UserProfile)
def upsert_user(profile: UserProfile):
    with _conn() as conn:
        conn.execute(
            "INSERT OR REPLACE INTO users (session_id, data) VALUES (?, ?)",
            (profile.session_id, profile.model_dump_json())
        )
        conn.commit()
    return profile
