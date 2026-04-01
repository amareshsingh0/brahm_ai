"""
Payments Router — Brahm AI
Cashfree payment gateway integration.

POST /api/checkout          → Create Cashfree order → return payment_link
POST /api/payment/webhook   → Cashfree webhook → verify + activate subscription
GET  /api/payment/verify/{order_id} → Manual payment status check (fallback)
"""
import os
import hmac
import hashlib
import uuid
import httpx
from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel
from api.supabase_client import get_supabase

router = APIRouter()

# ── Cashfree config ───────────────────────────────────────────────────────────
def _cf_base() -> str:
    env = os.getenv("CASHFREE_ENV", "TEST").upper()
    return "https://sandbox.cashfree.com/pg" if env == "TEST" else "https://api.cashfree.com/pg"

def _cf_headers() -> dict:
    return {
        "x-api-version":    "2023-08-01",
        "x-client-id":      os.getenv("CASHFREE_APP_ID", ""),
        "x-client-secret":  os.getenv("CASHFREE_SECRET_KEY", ""),
        "Content-Type":     "application/json",
    }

# ── Plan pricing (INR) ────────────────────────────────────────────────────────
PLAN_PRICES = {
    ("standard", "monthly"): 199,
    ("standard", "yearly"):  1990,
    ("premium",  "monthly"): 499,
    ("premium",  "yearly"):  4990,
}

PLAN_DURATION_DAYS = {
    "monthly": 30,
    "yearly":  365,
}

# ── Auth helper ───────────────────────────────────────────────────────────────
def _get_user_id(request: Request) -> str | None:
    auth = request.headers.get("Authorization", "")
    if auth.startswith("Bearer "):
        try:
            from jose import jwt, JWTError
            secret = os.getenv("JWT_SECRET", "")
            payload = jwt.decode(auth[7:], secret, algorithms=["HS256"])
            return payload.get("sub")
        except Exception:
            pass
    return None


def _get_user_info(user_id: str) -> dict:
    """Fetch user email + phone from Supabase for Cashfree customer_details."""
    try:
        sb = get_supabase()
        res = sb.table("users").select("email,phone,name").eq("id", user_id).maybe_single().execute()
        return res.data or {}
    except Exception:
        return {}


# ── Helpers ───────────────────────────────────────────────────────────────────
def _parse_plan_from_note(order_note: str, amount: float) -> tuple[str, str]:
    """
    Derive plan + period from order_note string like 'Brahm AI Standard — monthly'
    Falls back to amount-based lookup.
    """
    note = order_note.lower()
    plan   = "standard"
    period = "monthly"
    if "premium" in note:  plan = "premium"
    if "yearly"  in note:  period = "yearly"
    # Amount-based fallback
    amount = int(amount)
    for (p, per), a in PLAN_PRICES.items():
        if a == amount:
            return p, per
    return plan, period


# ── Request models ────────────────────────────────────────────────────────────
class CheckoutRequest(BaseModel):
    plan:   str   # "standard" | "premium"
    period: str   # "monthly" | "yearly"


# ── POST /checkout ────────────────────────────────────────────────────────────
@router.post("/checkout")
async def create_checkout(body: CheckoutRequest, request: Request):
    """Create a Cashfree order and return the payment link."""
    user_id = _get_user_id(request)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    amount = PLAN_PRICES.get((body.plan, body.period))
    if not amount:
        raise HTTPException(status_code=400, detail=f"Invalid plan/period: {body.plan}/{body.period}")

    app_id = os.getenv("CASHFREE_APP_ID", "")
    secret = os.getenv("CASHFREE_SECRET_KEY", "")
    if not app_id or not secret:
        raise HTTPException(status_code=503, detail="Payment gateway not configured")

    user_info  = _get_user_info(user_id)
    order_id   = f"brahm_{user_id[:8]}_{uuid.uuid4().hex[:8]}"
    site_url   = os.getenv("SITE_URL", "https://brahmasmi.bimoraai.com")
    return_url = f"{site_url}/subscription?order_id={order_id}&plan={body.plan}&period={body.period}"
    notify_url = f"{site_url}/api/payment/webhook"

    payload = {
        "order_id":       order_id,
        "order_amount":   float(amount),
        "order_currency": "INR",
        "customer_details": {
            "customer_id":    user_id,
            "customer_email": user_info.get("email") or f"{user_id[:8]}@brahm.ai",
            "customer_phone": user_info.get("phone") or "9999999999",
            "customer_name":  user_info.get("name")  or "User",
        },
        "order_meta": {
            "return_url": return_url,
            "notify_url": notify_url,
        },
        "order_note": f"Brahm AI {body.plan.title()} — {body.period}",
    }

    try:
        async with httpx.AsyncClient(timeout=15) as client:
            resp = await client.post(
                f"{_cf_base()}/orders",
                json=payload,
                headers=_cf_headers(),
            )
        data = resp.json()
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Payment gateway error: {e}")

    if resp.status_code not in (200, 201):
        raise HTTPException(
            status_code=502,
            detail=data.get("message", "Cashfree order creation failed"),
        )

    # Persist pending order in Supabase payment_log
    try:
        sb = get_supabase()
        sb.table("payment_log").insert({
            "user_id":          user_id,
            "cashfree_order_id": order_id,
            "amount":           amount,
            "currency":         "INR",
            "status":           "PENDING",
        }).execute()
    except Exception:
        pass   # non-critical — don't fail checkout if log insert fails

    return {
        "order_id":          order_id,
        "payment_url":       data.get("payment_link") or data.get("payment_link_url"),
        "payment_session_id": data.get("payment_session_id"),
        "amount":            amount,
        "currency":          "INR",
    }


# ── POST /payment/webhook ─────────────────────────────────────────────────────
@router.post("/payment/webhook")
async def cashfree_webhook(request: Request):
    """
    Cashfree webhook — called after payment completes.
    Verifies HMAC signature, then activates subscription.
    """
    raw_body = await request.body()
    sig_header = request.headers.get("x-webhook-signature", "")

    secret = os.getenv("CASHFREE_SECRET_KEY", "")
    ts     = request.headers.get("x-webhook-timestamp", "")

    # Verify signature: HMAC-SHA256(timestamp + raw_body, secret)
    if secret and sig_header and ts:
        import base64
        expected_b64 = base64.b64encode(
            hmac.new(secret.encode(), (ts + raw_body.decode()).encode(), hashlib.sha256).digest()
        ).decode()
        if not hmac.compare_digest(expected_b64, sig_header):
            raise HTTPException(status_code=403, detail="Invalid webhook signature")

    import json
    try:
        event = json.loads(raw_body)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON")

    event_type = event.get("type", "")
    data       = event.get("data", {})
    order_data = data.get("order", {})
    payment    = data.get("payment", {})

    order_id     = order_data.get("order_id", "")
    order_status = order_data.get("order_status", "")
    payment_status = payment.get("payment_status", "")

    # Only activate on confirmed payment
    if order_status != "PAID" and payment_status != "SUCCESS":
        return {"status": "ignored", "reason": f"order_status={order_status}"}

    # Look up the order in payment_log
    try:
        sb = get_supabase()
        log = sb.table("payment_log") \
            .select("user_id,amount") \
            .eq("cashfree_order_id", order_id) \
            .maybe_single() \
            .execute()
        if not log.data:
            return {"status": "ignored", "reason": "order not found"}
        row = log.data
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"DB lookup failed: {e}")

    user_id = row["user_id"]
    amount  = row.get("amount", 0)
    # Derive plan/period from order_id prefix stored in order_note (or from order amount)
    # We store plan+period in the Cashfree order_note — parse it back
    order_note = order_data.get("order_note", "")
    plan, period = _parse_plan_from_note(order_note, amount)

    await _activate_subscription(user_id, plan, period, amount, order_id)
    return {"status": "ok"}


# ── GET /payment/verify/{order_id} ────────────────────────────────────────────
@router.get("/payment/verify/{order_id}")
async def verify_payment(order_id: str, request: Request):
    """
    Manual payment status check — called from frontend return_url page
    as fallback if webhook hasn't fired yet.
    """
    user_id = _get_user_id(request)
    if not user_id:
        raise HTTPException(status_code=401, detail="Not authenticated")

    # Fetch order status from Cashfree
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(
                f"{_cf_base()}/orders/{order_id}",
                headers=_cf_headers(),
            )
        data = resp.json()
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Gateway error: {e}")

    order_status = data.get("order_status", "")

    if order_status != "PAID":
        return {"status": order_status, "activated": False}

    # Check if already activated (idempotent)
    try:
        sb = get_supabase()
        log = sb.table("payment_log") \
            .select("user_id,amount,status") \
            .eq("cashfree_order_id", order_id) \
            .maybe_single() \
            .execute()
        if log.data and log.data.get("status") == "PAID":
            return {"status": "PAID", "activated": True, "already_done": True}

        row = log.data or {}
    except Exception:
        row = {}

    amount = row.get("amount", data.get("order_amount", 0))
    order_note = data.get("order_note", "")
    plan, period = _parse_plan_from_note(order_note, amount)

    await _activate_subscription(user_id, plan, period, amount, order_id)
    return {"status": "PAID", "activated": True}


# ── Subscription activation ───────────────────────────────────────────────────
async def _activate_subscription(
    user_id: str, plan: str, period: str, amount: float, order_id: str
):
    """
    Activate or extend a user's subscription in Supabase.
    Idempotent — safe to call multiple times for same order.
    """
    duration_days = PLAN_DURATION_DAYS.get(period, 30)
    now     = datetime.now(timezone.utc)
    expires = now + timedelta(days=duration_days)

    try:
        sb = get_supabase()

        # Check if subscription exists for this user
        existing = sb.table("subscriptions") \
            .select("id") \
            .eq("user_id", user_id) \
            .maybe_single() \
            .execute()

        sub_data = {
            "user_id":           user_id,
            "plan":              plan,
            "period":            period,
            "status":            "active",
            "started_at":        now.isoformat(),
            "expires_at":        expires.isoformat(),
            "cashfree_order_id": order_id,
            "amount_paid":       int(amount),
            "amount":            amount,
            "currency":          "INR",
        }

        if existing.data:
            sb.table("subscriptions") \
                .update(sub_data) \
                .eq("user_id", user_id) \
                .execute()
        else:
            sb.table("subscriptions").insert(sub_data).execute()

        # Update payment_log status
        sb.table("payment_log") \
            .update({"status": "PAID", "paid_at": now.isoformat()}) \
            .eq("cashfree_order_id", order_id) \
            .execute()

        # Update users.plan for quick lookup
        sb.table("users") \
            .update({"plan": plan}) \
            .eq("id", user_id) \
            .execute()

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Subscription activation failed: {e}")