import { useState, useRef, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import { useNavigate, Link } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Moon, ArrowLeft, RefreshCw, Phone } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { useTranslation } from "react-i18next";
import { GoogleOAuthProvider, useGoogleLogin } from "@react-oauth/google";

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? "";

const OTP_LENGTH = 6;
const RESEND_SECONDS = 60;

// ── Google login button ───────────────────────────────────────────────────────
function GoogleButton({ onSuccess }: { onSuccess: (idToken: string) => void }) {
  const login = useGoogleLogin({
    onSuccess: async (resp) => {
      // useGoogleLogin with flow="implicit" gives access_token not id_token
      // We need to exchange it for user info and create our own token
      // Use flow="auth-code" is server-side — instead fetch userinfo
      const res = await fetch("https://www.googleapis.com/oauth2/v3/userinfo", {
        headers: { Authorization: `Bearer ${resp.access_token}` },
      });
      const userInfo = await res.json();
      // Pass the access_token — backend will verify via tokeninfo
      onSuccess(resp.access_token);
    },
    onError: () => {},
    flow: "implicit",
  });

  return (
    <button
      type="button"
      onClick={() => login()}
      className="w-full flex items-center justify-center gap-3 px-4 py-2.5 rounded-xl border border-border/40 bg-muted/20 hover:bg-muted/40 transition-colors text-sm font-medium text-foreground"
    >
      <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24">
        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
        <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
      </svg>
      Continue with Google
    </button>
  );
}

export default function LoginPage() {
  const { t } = useTranslation();
  const [phone, setPhone] = useState("");
  const [otpDigits, setOtpDigits] = useState<string[]>(Array(OTP_LENGTH).fill(""));
  const [otpSent, setOtpSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [countdown, setCountdown] = useState(0);

  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
  const { sendOtp, verifyOtp, googleLogin } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();

  // Countdown timer for resend
  useEffect(() => {
    if (countdown <= 0) return;
    const t = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(t);
  }, [countdown]);

  const isValidIndianPhone = (p: string) =>
    p.length === 10 && /^[6-9]\d{9}$/.test(p);

  const phoneError = phone.length === 10 && !isValidIndianPhone(phone)
    ? "Invalid number. Must start with 6, 7, 8, or 9."
    : "";

  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = phone.trim();
    if (!isValidIndianPhone(cleaned)) return;
    setLoading(true);
    try {
      const result = await sendOtp(cleaned);
      if (result.sent) {
        setOtpSent(true);
        setCountdown(RESEND_SECONDS);
        setTimeout(() => inputRefs.current[0]?.focus(), 100);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Please try again.";
      toast({ title: "Failed to send OTP", description: msg, variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  const handleOtpChange = (index: number, value: string) => {
    // Allow only digits, handle paste
    const digit = value.replace(/\D/g, "").slice(-1);
    const next = [...otpDigits];
    next[index] = digit;
    setOtpDigits(next);
    if (digit && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus();
    }
    // Auto-submit when all filled
    if (digit && index === OTP_LENGTH - 1 && next.every(Boolean)) {
      handleVerify(next.join(""));
    }
  };

  const handleOtpKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === "Backspace" && !otpDigits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handleOtpPaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, OTP_LENGTH);
    if (!pasted) return;
    const next = [...otpDigits];
    pasted.split("").forEach((ch, i) => { next[i] = ch; });
    setOtpDigits(next);
    const lastFilled = Math.min(pasted.length, OTP_LENGTH - 1);
    inputRefs.current[lastFilled]?.focus();
    if (pasted.length === OTP_LENGTH) handleVerify(pasted);
  };

  const handleVerify = async (otp: string) => {
    if (loading) return;
    setLoading(true);
    try {
      const result = await verifyOtp(phone.trim(), otp);
      if (result.token) {
        toast({ title: t('login.welcome_toast') });
        navigate("/dashboard");
      } else {
        toast({ title: "Invalid OTP", description: "Please check and try again.", variant: "destructive" });
        setOtpDigits(Array(OTP_LENGTH).fill(""));
        inputRefs.current[0]?.focus();
      }
    } catch {
      toast({ title: "Verification failed", description: "Please try again.", variant: "destructive" });
      setOtpDigits(Array(OTP_LENGTH).fill(""));
      inputRefs.current[0]?.focus();
    } finally {
      setLoading(false);
    }
  };

  const handleManualVerify = (e: React.FormEvent) => {
    e.preventDefault();
    const otp = otpDigits.join("");
    if (otp.length < OTP_LENGTH) return;
    handleVerify(otp);
  };

  const handleResend = async () => {
    if (countdown > 0 || loading) return;
    setOtpDigits(Array(OTP_LENGTH).fill(""));
    setLoading(true);
    try {
      await sendOtp(phone.trim());
      setCountdown(RESEND_SECONDS);
      toast({ title: "OTP resent" });
      setTimeout(() => inputRefs.current[0]?.focus(), 100);
    } catch {
      toast({ title: "Failed to resend", variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
    <div className="min-h-screen bg-background flex flex-col items-center justify-center px-4 star-field">
      {/* Background glow */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-[500px] h-[500px] rounded-full bg-primary/5 blur-3xl" />
      </div>

      {/* Back link */}
      <Link
        to="/"
        className="absolute top-5 left-5 flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors"
      >
        <ArrowLeft className="h-3.5 w-3.5" /> {t('login.back')}
      </Link>

      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-sm relative z-10"
      >
        {/* Logo */}
        <div className="text-center mb-8">
          <Moon className="h-10 w-10 text-primary zodiac-glow mx-auto mb-3" />
          <h1 className="font-display text-3xl text-primary text-glow-gold">{t('chat.title')}</h1>
          <p className="text-xs text-muted-foreground mt-1">{t('login.personal_guide')}</p>
        </div>

        <div className="cosmic-card rounded-2xl p-6 space-y-6">
          <AnimatePresence mode="wait">
            {/* ── STEP 1: Phone ── */}
            {!otpSent ? (
              <motion.div
                key="phone"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                transition={{ duration: 0.25 }}
              >
                <div className="text-center mb-5">
                  <h2 className="font-display text-xl text-foreground">{t('login.sign_in')}</h2>
                  <p className="text-xs text-muted-foreground mt-1">
                    {t('login.sign_in_desc')}
                  </p>
                </div>

                <form onSubmit={handleSendOtp} className="space-y-4">
                  <div className={`flex items-center gap-2 bg-muted/20 border rounded-xl px-3 focus-within:ring-1 transition-all ${phoneError ? "border-destructive/60 focus-within:ring-destructive/40" : "border-border/40 focus-within:ring-primary/50"}`}>
                    <div className="flex items-center gap-1.5 border-r border-border/30 pr-3 py-3">
                      <Phone className="h-3.5 w-3.5 text-muted-foreground" />
                      <span className="text-sm text-muted-foreground">+91</span>
                    </div>
                    <Input
                      type="tel"
                      inputMode="numeric"
                      value={phone}
                      onChange={(e) => setPhone(e.target.value.replace(/\D/g, "").slice(0, 10))}
                      placeholder="98765 43210"
                      className="border-0 bg-transparent focus-visible:ring-0 focus-visible:ring-offset-0 text-base px-2"
                      maxLength={10}
                      required
                      autoFocus
                    />
                  </div>
                  {phoneError && (
                    <p className="text-xs text-destructive mt-1 ml-1">{phoneError}</p>
                  )}

                  <Button type="submit" className="w-full" disabled={loading || !isValidIndianPhone(phone)}>
                    {loading ? t('login.sending') : t('login.send_otp')}
                  </Button>
                </form>

                <p className="text-xs text-center text-muted-foreground/50 mt-4">
                  {t('login.new_users')}
                </p>

                {/* Divider */}
                <div className="flex items-center gap-3 mt-2">
                  <div className="flex-1 h-px bg-border/40" />
                  <span className="text-xs text-muted-foreground/50">or</span>
                  <div className="flex-1 h-px bg-border/40" />
                </div>

                {/* Social buttons */}
                <div className="space-y-2.5 mt-2">
                  <GoogleButton
                    onSuccess={async (token) => {
                      setLoading(true);
                      try {
                        await googleLogin(token);
                        toast({ title: t('login.welcome_toast') });
                        navigate("/dashboard");
                      } catch {
                        toast({ title: "Google login failed", variant: "destructive" });
                      } finally {
                        setLoading(false);
                      }
                    }}
                  />
                  {/* Apple — UI only, no functionality yet */}
                  <button
                    type="button"
                    disabled
                    className="w-full flex items-center justify-center gap-3 px-4 py-2.5 rounded-xl border border-border/40 bg-muted/20 opacity-50 cursor-not-allowed text-sm font-medium text-foreground"
                  >
                    <svg className="h-5 w-5 shrink-0" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
                    </svg>
                    Continue with Apple
                    <span className="text-[10px] text-muted-foreground ml-auto">Coming soon</span>
                  </button>
                </div>
              </motion.div>
            ) : (
              /* ── STEP 2: OTP ── */
              <motion.div
                key="otp"
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.25 }}
              >
                <div className="text-center mb-5">
                  <h2 className="font-display text-xl text-foreground">{t('login.enter_otp')}</h2>
                  <p className="text-xs text-muted-foreground mt-1">
                    {t('login.otp_sent_to')} <span className="text-foreground">+91 {phone}</span>
                  </p>
                </div>

                <form onSubmit={handleManualVerify} className="space-y-5">
                  {/* 6-digit OTP boxes */}
                  <div className="flex justify-center gap-2" onPaste={handleOtpPaste}>
                    {otpDigits.map((digit, i) => (
                      <input
                        key={i}
                        ref={(el) => { inputRefs.current[i] = el; }}
                        type="text"
                        inputMode="numeric"
                        maxLength={1}
                        value={digit}
                        onChange={(e) => handleOtpChange(i, e.target.value)}
                        onKeyDown={(e) => handleOtpKeyDown(i, e)}
                        className={`w-11 h-13 text-center text-xl font-display rounded-xl border transition-all outline-none
                          bg-muted/20 text-foreground
                          ${digit ? "border-primary/60 glow-border" : "border-border/40"}
                          focus:border-primary focus:ring-1 focus:ring-primary/40`}
                        style={{ height: "3.25rem" }}
                        autoComplete="one-time-code"
                      />
                    ))}
                  </div>

                  <Button
                    type="submit"
                    className="w-full"
                    disabled={loading || otpDigits.some((d) => !d)}
                  >
                    {loading ? t('login.verifying') : t('login.verify')}
                  </Button>
                </form>

                {/* Resend */}
                <div className="flex items-center justify-center gap-2 mt-4">
                  <button
                    type="button"
                    onClick={handleResend}
                    disabled={countdown > 0 || loading}
                    className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-primary disabled:opacity-60 transition-colors"
                  >
                    <RefreshCw className="h-3 w-3" />
                    {countdown > 0 ? t('login.resend_in', { sec: countdown }) : t('login.resend')}
                  </button>
                </div>

                {/* {t('login.change_number')} */}
                <button
                  type="button"
                  onClick={() => { setOtpSent(false); setOtpDigits(Array(OTP_LENGTH).fill("")); }}
                  className="block mx-auto mt-2 text-xs text-muted-foreground/50 hover:text-muted-foreground transition-colors"
                >
                  Change number
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <p className="text-xs text-center text-muted-foreground/60 mt-6">
          {t('login.terms')}
        </p>
      </motion.div>
    </div>
    </GoogleOAuthProvider>
  );
}