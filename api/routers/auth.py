"""
Brahm AI — Custom Auth Router (Phase 2 + 3)
- Phone OTP: send via MSG91 (TEST_MODE=True uses fixed OTP 123456)
- OTP stored as bcrypt hash in otp_log table (never plaintext)
- JWT access token (15min) + refresh token (30 days, stored hashed in DB)
- Rate limiting: max 5 OTP sends per phone per 15 min
- Brute force: max 5 wrong OTP attempts → lock otp entry
- Google OAuth: verify id_token via google-auth library
"""
import os, secrets, hashlib, uuid
from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, HTTPException, Request, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
from passlib.context import CryptContext
from jose import jwt, JWTError
from api.supabase_client import get_supabase

router  = APIRouter()
bearer  = HTTPBearer(auto_error=False)
bcrypt  = CryptContext(schemes=["bcrypt"], deprecated="auto")

# ── Config ────────────────────────────────────────────────────────────────────
TEST_MODE       = os.getenv("TEST_MODE", "true").lower() == "true"
TEST_OTP        = "123456"
OTP_TTL_MINUTES = 5
OTP_MAX_ATTEMPTS = 5
OTP_RATE_LIMIT  = 5          # max sends per phone per window
OTP_RATE_WINDOW = 15         # minutes

JWT_SECRET      = os.getenv("JWT_SECRET", "change-me-in-production-use-long-random-string")
JWT_ALGORITHM   = "HS256"
ACCESS_TTL_MIN  = 15         # minutes
REFRESH_TTL_DAYS = 30

MSG91_AUTH_KEY  = os.getenv("MSG91_AUTH_KEY", "")
MSG91_TEMPLATE_ID = os.getenv("MSG91_TEMPLATE_ID", "")
MSG91_SENDER_ID = os.getenv("MSG91_SENDER_ID", "BRAHAI")

# ── Pydantic models ───────────────────────────────────────────────────────────
class SendOtpRequest(BaseModel):
    phone: str
    purpose: str = "login"     # 'login' | 'register' | 'verify_phone'

class SendOtpResponse(BaseModel):
    sent: bool
    message: str
    test_otp: str | None = None   # only returned in TEST_MODE

class VerifyOtpRequest(BaseModel):
    phone: str
    otp: str
    purpose: str = "login"

class AuthResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    user_id: str
    name: str
    plan: str
    phone: str
    phone_verified: bool

class RefreshRequest(BaseModel):
    refresh_token: str

class AccessTokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"

class LogoutRequest(BaseModel):
    refresh_token: str

# ── Helpers ───────────────────────────────────────────────────────────────────

def _now() -> datetime:
    return datetime.now(timezone.utc)

def _hash_token(token: str) -> str:
    """SHA-256 hash for refresh token storage."""
    return hashlib.sha256(token.encode()).hexdigest()

def _make_access_token(user_id: str, phone: str, plan: str) -> str:
    payload = {
        "sub":   user_id,
        "phone": phone,
        "plan":  plan,
        "exp":   _now() + timedelta(minutes=ACCESS_TTL_MIN),
        "iat":   _now(),
        "type":  "access",
    }
    return jwt.encode(payload, JWT_SECRET, algorithm=JWT_ALGORITHM)

def _make_refresh_token() -> str:
    """Cryptographically secure random refresh token."""
    return secrets.token_urlsafe(48)

def _get_ip(request: Request | None) -> str:
    if request is None:
        return ""
    forwarded = request.headers.get("X-Forwarded-For")
    return forwarded.split(",")[0].strip() if forwarded else (request.client.host if request.client else "")

def _send_sms_msg91(phone: str, otp: str):
    """Send OTP via MSG91 API."""
    import httpx
    # Normalize phone: remove + and spaces
    clean_phone = phone.replace("+", "").replace(" ", "").replace("-", "")
    url = "https://control.msg91.com/api/v5/otp"
    params = {
        "template_id": MSG91_TEMPLATE_ID,
        "mobile":      clean_phone,
        "authkey":     MSG91_AUTH_KEY,
        "otp":         otp,
    }
    try:
        resp = httpx.post(url, json=params, timeout=8)
        data = resp.json()
        if data.get("type") != "success":
            raise Exception(f"MSG91 error: {data}")
    except Exception as e:
        raise HTTPException(500, f"SMS delivery failed: {str(e)}")

# ── Rate limit check ──────────────────────────────────────────────────────────

def _check_rate_limit(phone: str):
    """Raise 429 if phone has sent too many OTPs recently."""
    sb = get_supabase()
    window_start = (_now() - timedelta(minutes=OTP_RATE_WINDOW)).isoformat()
    res = sb.table("otp_log") \
        .select("id", count="exact") \
        .eq("phone", phone) \
        .gte("created_at", window_start) \
        .execute()
    count = res.count or 0
    if count >= OTP_RATE_LIMIT:
        raise HTTPException(429, f"Too many OTP requests. Try again after {OTP_RATE_WINDOW} minutes.")

# ── Endpoints ─────────────────────────────────────────────────────────────────

@router.post("/auth/otp/send", response_model=SendOtpResponse)
def send_otp(req: SendOtpRequest, request: Request):
    phone = req.phone.strip()
    if not phone or len(phone) < 10:
        raise HTTPException(400, "Valid phone number required")

    _check_rate_limit(phone)

    # Generate OTP
    if TEST_MODE:
        otp = TEST_OTP
    else:
        otp = str(secrets.randbelow(900000) + 100000)   # 6 digits, crypto-safe

    # Hash OTP before storing
    otp_hash = bcrypt.hash(otp)
    expires_at = (_now() + timedelta(minutes=OTP_TTL_MINUTES)).isoformat()

    sb = get_supabase()
    sb.table("otp_log").insert({
        "phone":      phone,
        "otp_hash":   otp_hash,
        "purpose":    req.purpose,
        "expires_at": expires_at,
        "ip_address": _get_ip(request),
        "user_agent": request.headers.get("user-agent", ""),
    }).execute()

    # Send SMS (skip in TEST_MODE)
    if not TEST_MODE:
        _send_sms_msg91(phone, otp)

    return SendOtpResponse(
        sent=True,
        message="OTP sent successfully" if not TEST_MODE else "TEST MODE: OTP is 123456",
        test_otp=TEST_OTP if TEST_MODE else None,
    )


@router.post("/auth/otp/verify", response_model=AuthResponse)
def verify_otp(req: VerifyOtpRequest, request: Request):
    phone = req.phone.strip()
    otp   = req.otp.strip()

    if not phone or not otp:
        raise HTTPException(400, "Phone and OTP required")

    sb = get_supabase()
    now_iso = _now().isoformat()

    # Fetch latest unused non-expired OTP for this phone+purpose
    res = sb.table("otp_log") \
        .select("*") \
        .eq("phone", phone) \
        .eq("purpose", req.purpose) \
        .eq("used", False) \
        .gte("expires_at", now_iso) \
        .order("created_at", desc=True) \
        .limit(1) \
        .execute()

    if not res.data:
        raise HTTPException(400, "OTP expired or not found. Request a new OTP.")

    row = res.data[0]

    # Check attempt limit
    if row["attempts"] >= OTP_MAX_ATTEMPTS:
        raise HTTPException(400, "Too many wrong attempts. Request a new OTP.")

    # Verify OTP hash
    if not bcrypt.verify(otp, row["otp_hash"]):
        # Increment attempts
        sb.table("otp_log").update({"attempts": row["attempts"] + 1}).eq("id", row["id"]).execute()
        remaining = OTP_MAX_ATTEMPTS - row["attempts"] - 1
        raise HTTPException(400, f"Invalid OTP. {remaining} attempts remaining.")

    # Mark OTP as used
    sb.table("otp_log").update({"used": True}).eq("id", row["id"]).execute()

    # Upsert user
    _ur = sb.table("users").select("id,name,plan,phone_verified").eq("phone", phone).execute()
    user_data = _ur.data[0] if _ur.data else None

    if user_data:
        user = user_data
        user_id = user["id"]
        name    = user.get("name", "") or ""
        plan    = user.get("plan", "free")
        # Mark phone verified if not already
        if not user.get("phone_verified"):
            sb.table("users").update({
                "phone_verified": True,
                "last_login": _now().isoformat(),
            }).eq("id", user_id).execute()
    else:
        user_id = str(uuid.uuid4())
        name    = ""
        plan    = "free"
        sb.table("users").insert({
            "id":             user_id,
            "phone":          phone,
            "phone_verified": True,
            "name":           name,
            "plan":           plan,
            "signup_ip":      _get_ip(request),
            "signup_device":  "web",
            "last_login":     _now().isoformat(),
        }).execute()

    # Log login
    sb.table("login_log").insert({
        "user_id":    user_id,
        "phone":      phone,
        "ip":         _get_ip(request),
        "user_agent": request.headers.get("user-agent", ""),
        "success":    True,
    }).execute()

    # Generate tokens
    access_token   = _make_access_token(user_id, phone, plan)
    refresh_token  = _make_refresh_token()
    refresh_hash   = _hash_token(refresh_token)
    refresh_expiry = (_now() + timedelta(days=REFRESH_TTL_DAYS)).isoformat()

    ua = request.headers.get("user-agent", "")
    device_type = "android" if "Android" in ua else "ios" if ("iPhone" in ua or "iPad" in ua) else "web"

    sb.table("refresh_tokens").insert({
        "user_id":     user_id,
        "token_hash":  refresh_hash,
        "device_name": ua[:120] if ua else "",
        "device_type": device_type,
        "ip_address":  _get_ip(request),
        "user_agent":  ua,
        "expires_at":  refresh_expiry,
    }).execute()

    return AuthResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user_id=user_id,
        name=name,
        plan=plan,
        phone=phone,
        phone_verified=True,
    )


@router.post("/auth/refresh", response_model=AccessTokenResponse)
def refresh_access_token(req: RefreshRequest):
    """Exchange a valid refresh token for a new access token."""
    token_hash = _hash_token(req.refresh_token)
    now_iso    = _now().isoformat()

    try:
        sb = get_supabase()
        # Fetch refresh token row only — join separately to avoid nested-select
        # type ambiguity (list vs dict) across supabase-py versions
        res = sb.table("refresh_tokens") \
            .select("id, user_id") \
            .eq("token_hash", token_hash) \
            .eq("revoked", False) \
            .gte("expires_at", now_iso) \
            .limit(1) \
            .execute()

        row = res.data[0] if res.data else None
        if not row:
            raise HTTPException(401, "Invalid or expired refresh token. Please login again.")

        # Fetch user separately — safe, no nested join
        user_res = sb.table("users") \
            .select("id, phone, plan") \
            .eq("id", row["user_id"]) \
            .limit(1) \
            .execute()
        user = user_res.data[0] if user_res.data else None
        if not user:
            raise HTTPException(401, "User not found. Please login again.")

        # Update last_used_at
        sb.table("refresh_tokens").update({"last_used_at": _now().isoformat()}).eq("id", row["id"]).execute()

        access_token = _make_access_token(user["id"], user["phone"] or "", user["plan"] or "free")
        return AccessTokenResponse(access_token=access_token)

    except HTTPException:
        raise
    except Exception as e:
        import logging
        logging.getLogger(__name__).error("refresh_access_token error: %s", e)
        raise HTTPException(500, "Token refresh failed. Please login again.")


@router.post("/auth/logout")
def logout(req: LogoutRequest):
    """Revoke a specific refresh token (logout from this device)."""
    token_hash = _hash_token(req.refresh_token)
    sb = get_supabase()
    sb.table("refresh_tokens").update({
        "revoked":    True,
        "revoked_at": _now().isoformat(),
    }).eq("token_hash", token_hash).execute()
    return {"message": "Logged out successfully"}


@router.post("/auth/logout/all")
def logout_all(credentials: HTTPAuthorizationCredentials = Depends(bearer)):
    """Revoke all refresh tokens for current user (logout from all devices)."""
    if not credentials:
        raise HTTPException(401, "Not authenticated")

    try:
        payload = jwt.decode(credentials.credentials, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        user_id = payload.get("sub")
    except JWTError:
        raise HTTPException(401, "Invalid token")

    sb = get_supabase()
    sb.table("refresh_tokens").update({
        "revoked":    True,
        "revoked_at": _now().isoformat(),
    }).eq("user_id", user_id).eq("revoked", False).execute()
    return {"message": "Logged out from all devices"}


# ── Google OAuth ─────────────────────────────────────────────────────────────

GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID", "")

class GoogleAuthRequest(BaseModel):
    id_token: str
    device_type: str = "web"   # 'web' | 'android' | 'ios'

@router.post("/auth/google", response_model=AuthResponse)
def google_login(req: GoogleAuthRequest, request: Request):
    """Verify Google id_token and return JWT tokens."""
    if not GOOGLE_CLIENT_ID:
        raise HTTPException(500, "Google OAuth not configured")

    try:
        import httpx as _httpx
        token = req.id_token

        # Detect token type:
        # - Android Credential Manager sends a JWT ID token (3 dot-separated parts)
        # - Web implicit flow sends an opaque access token
        if token.count('.') == 2:
            # JWT ID token (Android) — verify via tokeninfo endpoint
            r = _httpx.get(
                f"https://oauth2.googleapis.com/tokeninfo?id_token={token}",
                timeout=8,
            )
            if r.status_code != 200:
                raise Exception(f"Invalid ID token: {r.text}")
            idinfo = r.json()
            # tokeninfo returns 'sub', 'email', 'name' (same fields as userinfo)
        else:
            # Opaque access token (web implicit flow) — fetch userinfo
            r = _httpx.get(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                headers={"Authorization": f"Bearer {token}"},
                timeout=8,
            )
            if r.status_code != 200:
                raise Exception(f"Google userinfo error: {r.text}")
            idinfo = r.json()
    except Exception as e:
        raise HTTPException(401, f"Invalid Google token: {str(e)}")

    google_id = idinfo.get("sub", "")
    email     = idinfo.get("email", "")
    name      = idinfo.get("name", "")
    if not google_id:
        raise HTTPException(401, "Could not fetch Google user info")

    sb = get_supabase()

    # Find existing user by google_id or email
    _ur = sb.table("users").select("id,phone,name,plan,status").eq("google_id", google_id).execute()
    user_data = _ur.data[0] if _ur.data else None

    if not user_data and email:
        _ur2 = sb.table("users").select("id,phone,name,plan,status").eq("email", email).execute()
        user_data = _ur2.data[0] if _ur2.data else None

    if user_data:
        user    = user_data
        user_id = user["id"]
        phone   = user.get("phone") or ""
        plan    = user.get("plan", "free")
        if user.get("status") in ("suspended", "banned", "deleted"):
            raise HTTPException(403, f"Account {user['status']}")
        # Link google_id if not already linked
        sb.table("users").update({
            "google_id":  google_id,
            "last_login": _now().isoformat(),
        }).eq("id", user_id).execute()
    else:
        user_id = str(uuid.uuid4())
        phone   = ""
        plan    = "free"
        sb.table("users").insert({
            "id":         user_id,
            "google_id":  google_id,
            "email":      email,
            "name":       name,
            "plan":       plan,
            "signup_ip":  _get_ip(request),
            "signup_device": req.device_type,
            "last_login": _now().isoformat(),
        }).execute()

    # Log login
    sb.table("login_log").insert({
        "user_id":    user_id,
        "ip":         _get_ip(request),
        "user_agent": request.headers.get("user-agent", ""),
        "success":    True,
    }).execute()

    # Generate tokens
    access_token  = _make_access_token(user_id, phone, plan)
    refresh_token = _make_refresh_token()
    refresh_hash  = _hash_token(refresh_token)
    refresh_expiry = (_now() + timedelta(days=REFRESH_TTL_DAYS)).isoformat()

    ua = request.headers.get("user-agent", "")
    sb.table("refresh_tokens").insert({
        "user_id":     user_id,
        "token_hash":  refresh_hash,
        "device_name": ua[:120] if ua else "",
        "device_type": req.device_type,
        "ip_address":  _get_ip(request),
        "user_agent":  ua,
        "expires_at":  refresh_expiry,
    }).execute()

    return AuthResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user_id=user_id,
        name=name,
        plan=plan,
        phone=phone,
        phone_verified=bool(phone),
    )


@router.get("/auth/me")
def get_me(credentials: HTTPAuthorizationCredentials = Depends(bearer)):
    """Get current user info from access token."""
    if not credentials:
        raise HTTPException(401, "Not authenticated")

    try:
        payload = jwt.decode(credentials.credentials, JWT_SECRET, algorithms=[JWT_ALGORITHM])
    except JWTError:
        raise HTTPException(401, "Invalid or expired token")

    user_id = payload.get("sub")
    sb = get_supabase()
    res = sb.table("users").select("id,phone,name,plan,phone_verified,role,status,created_at").eq("id", user_id).execute()
    user = res.data[0] if res.data else None

    if not user:
        raise HTTPException(404, "User not found")
    if user.get("status") in ("suspended", "banned", "deleted"):
        raise HTTPException(403, f"Account {user['status']}")

    return user
