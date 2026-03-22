"""
Auth router — OTP-based phone authentication.
In TEST_MODE, OTP is always "123456" for any number.
Set TEST_MODE=False and configure SMS (Twilio) for production.
"""
import uuid, time
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

router = APIRouter()

# In-memory OTP store: phone -> (otp, expiry_timestamp)
_otp_store: dict[str, tuple[str, float]] = {}

TEST_MODE = True          # set False + add Twilio for production
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
        # TODO: send SMS via Twilio here

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

    token = str(uuid.uuid4())
    return VerifyOtpResponse(
        access_token=token,
        name="",          # user fills name in onboarding
        plan="free",
        phone=phone,
    )
