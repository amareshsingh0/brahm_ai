import { useState, useRef, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import { useNavigate, Link } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Moon, ArrowLeft, RefreshCw, Phone } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

const OTP_LENGTH = 6;
const RESEND_SECONDS = 30;

export default function LoginPage() {
  const [phone, setPhone] = useState("");
  const [otpDigits, setOtpDigits] = useState<string[]>(Array(OTP_LENGTH).fill(""));
  const [otpSent, setOtpSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [countdown, setCountdown] = useState(0);

  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
  const { sendOtp, verifyOtp } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();

  // Countdown timer for resend
  useEffect(() => {
    if (countdown <= 0) return;
    const t = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(t);
  }, [countdown]);

  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleaned = phone.trim();
    if (!cleaned || cleaned.length < 10) {
      toast({ title: "Invalid phone", description: "Enter a valid 10-digit mobile number.", variant: "destructive" });
      return;
    }
    setLoading(true);
    try {
      const result = await sendOtp(cleaned);
      if (result.sent) {
        setOtpSent(true);
        setCountdown(RESEND_SECONDS);
        setTimeout(() => inputRefs.current[0]?.focus(), 100);
      }
    } catch {
      toast({ title: "Failed to send OTP", description: "Please try again.", variant: "destructive" });
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
        toast({ title: "Welcome to Brahm AI 🙏" });
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
        <ArrowLeft className="h-3.5 w-3.5" /> Back
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
          <h1 className="font-display text-3xl text-primary text-glow-gold">Brahm AI</h1>
          <p className="text-xs text-muted-foreground mt-1">Your Personal Vedic Guide</p>
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
                  <h2 className="font-display text-xl text-foreground">Sign In</h2>
                  <p className="text-xs text-muted-foreground mt-1">
                    Enter your mobile number to receive an OTP
                  </p>
                </div>

                <form onSubmit={handleSendOtp} className="space-y-4">
                  <div className="flex items-center gap-2 bg-muted/20 border border-border/40 rounded-xl px-3 focus-within:ring-1 focus-within:ring-primary/50 transition-all">
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

                  <Button type="submit" className="w-full" disabled={loading || phone.length < 10}>
                    {loading ? "Sending…" : "Send OTP"}
                  </Button>
                </form>

                <p className="text-xs text-center text-muted-foreground/50 mt-4">
                  New users are automatically registered on first login.
                </p>
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
                  <h2 className="font-display text-xl text-foreground">Enter OTP</h2>
                  <p className="text-xs text-muted-foreground mt-1">
                    Sent to <span className="text-foreground">+91 {phone}</span>
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
                    {loading ? "Verifying…" : "Verify & Continue"}
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
                    {countdown > 0 ? `Resend in ${countdown}s` : "Resend OTP"}
                  </button>
                </div>

                {/* Change number */}
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
          By continuing you agree to our Terms of Service and Privacy Policy.
        </p>
      </motion.div>
    </div>
  );
}