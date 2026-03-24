"""
Auth router — OTP-based phone authentication.
In TEST_MODE, OTP is always "123456" for any number.
Set TEST_MODE=False and configure SMS (Twilio/MSG91) for production.
Users are persisted to Supabase on first verify.
"""
import uuid, time
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from api.supabase_client import get_supabase

router = APIRouter()

# In-memory OTP store: phone -> (otp, expiry_timestamp)
_otp_store: dict[str, tuple[str, float]] = {}

TEST_MODE = True          # set False + add Twilio/MSG91 for production
TEST_OTP  = "123456"
OTP_TTL   = 300           # 5 minutes


class SendOtpRequest(BaseModel):
    phone: str

class SendOtpResponse(BaseModel):
    sent: bool
    message: str

class VerifyOtpRequest(BaseModel):
    phone: str
    otp: str

class VerifyOtpResponse(BaseModel):
    access_token: str
    name: str
    plan: str
    phone: str


@router.post("/auth/send-otp", response_model=SendOtpResponse)
def send_otp(req: SendOtpRequest):
    phone = req.phone.strip()
    if not phone:
        raise HTTPException(400, "Phone number required")

    if TEST_MODE:
        otp = TEST_OTP
    else:
        import random
        otp = str(random.randint(100000, 999999))
        # TODO: send SMS via MSG91 / Twilio here

    _otp_store[phone] = (otp, time.time() + OTP_TTL)
    return SendOtpResponse(sent=True, message="OTP sent successfully")


@router.post("/auth/verify-otp", response_model=VerifyOtpResponse)
def verify_otp(req: VerifyOtpRequest):
    phone = req.phone.strip()
    otp   = req.otp.strip()

    entry = _otp_store.get(phone)
    if not entry:
        raise HTTPException(400, "OTP not sent or expired. Request a new OTP.")

    stored_otp, expiry = entry
    if time.time() > expiry:
        _otp_store.pop(phone, None)
        raise HTTPException(400, "OTP expired. Request a new OTP.")

    if otp != stored_otp:
        raise HTTPException(400, "Invalid OTP. Please try again.")

    # OTP correct — clear it
    _otp_store.pop(phone, None)

    # Look up or create user in Supabase
    sb = get_supabase()
    res = sb.table("users").select("session_id,name,plan").eq("phone", phone).maybe_single().execute()

    if res.data:
        row = res.data
        session_id = row["session_id"]
        name       = row.get("name", "")
        plan       = row.get("plan", "free")
    else:
        session_id = str(uuid.uuid4())
        name       = ""
        plan       = "free"
        sb.table("users").insert({
            "session_id": session_id,
            "phone":      phone,
            "name":       name,
            "plan":       plan,
        }).execute()

    return VerifyOtpResponse(
        access_token=session_id,
        name=name,
        plan=plan,
        phone=phone,
    )
