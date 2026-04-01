import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Check, ChevronRight, Sparkles, Star, Crown, Shield, Loader2, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/authStore";
import { usePlans, useCheckout } from "@/hooks/useSubscription";
import { useToast } from "@/hooks/use-toast";
import { useQueryClient } from "@tanstack/react-query";
import type { AuthPlan } from "@/store/authStore";
import { useTranslation } from 'react-i18next';
import { cn } from "@/lib/utils";
import { api } from "@/lib/api";

type Period = "monthly" | "yearly";

const PLAN_ICONS: Record<string, React.ElementType> = {
  free:     Shield,
  standard: Star,
  premium:  Crown,
};

const PLAN_COLORS: Record<string, { gradient: string; ring: string; btn: string; price: string }> = {
  free:     { gradient: "from-muted/30 to-muted/10",           ring: "ring-border/40",           btn: "border border-border/50 text-muted-foreground",  price: "bg-muted/30 border-border/30" },
  standard: { gradient: "from-blue-500/10 to-blue-600/5",      ring: "ring-blue-500/40",          btn: "bg-blue-600 hover:bg-blue-700 text-white",       price: "bg-blue-500/10 border-blue-500/30" },
  premium:  { gradient: "from-amber-500/15 to-amber-600/5",    ring: "ring-amber-500/50",         btn: "bg-amber-600 hover:bg-amber-700 text-white",     price: "bg-amber-500/10 border-amber-500/30" },
};

const POPULAR_PLAN = "standard";

const FAQ = [
  { q: "How does billing work?",          a: "You're billed at the start of each period. Cancel anytime — your plan stays active until the end of the paid period." },
  { q: "Which payment methods are accepted?", a: "UPI, credit/debit cards, net banking, and all major wallets via Cashfree — India's trusted payment gateway." },
  { q: "Can I switch plans?",             a: "Yes. Upgrade instantly, downgrade at the end of your current period." },
  { q: "Is my data safe?",               a: "Your birth details and charts are stored securely and never shared with third parties." },
];

export default function SubscriptionPage() {
  const { t } = useTranslation();
  const [period, setPeriod] = useState<Period>("monthly");
  const [openFaq, setOpenFaq] = useState<number | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [paymentSuccess, setPaymentSuccess] = useState(false);
  const currentPlan = useAuthStore((s) => s.plan);
  const setPlan     = useAuthStore((s) => s.setPlan);
  const { data: plans = [] } = usePlans();
  const checkout    = useCheckout();
  const { toast }   = useToast();
  const queryClient = useQueryClient();

  // ── Handle return from Cashfree payment ─────────────────────────────────
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const orderId = params.get("order_id");
    const plan    = params.get("plan");
    if (!orderId) return;

    // Clean URL without reload
    window.history.replaceState({}, "", "/subscription");

    setVerifying(true);
    api.get<{ status: string; activated: boolean }>(`/api/payment/verify/${orderId}`)
      .then((res) => {
        if (res.status === "PAID" && res.activated) {
          setPaymentSuccess(true);
          if (plan) setPlan(plan as AuthPlan);
          queryClient.invalidateQueries({ queryKey: ["subscription"] });
          toast({ title: "Payment successful!", description: "Your plan is now active." });
        } else {
          toast({ title: "Payment pending", description: "We'll activate your plan once confirmed.", variant: "destructive" });
        }
      })
      .catch(() => {
        toast({ title: "Could not verify payment", description: "Contact support if amount was deducted.", variant: "destructive" });
      })
      .finally(() => setVerifying(false));
  }, []);

  const yearlyDiscount = (monthly: number, yearly: number) => {
    if (!monthly) return 0;
    return Math.round(((monthly * 12 - yearly) / (monthly * 12)) * 100);
  };

  const handleUpgrade = async (planId: string) => {
    if (planId === "free" || planId === currentPlan) return;
    try {
      const result = await checkout.mutateAsync({ plan: planId, period });
      if (result.payment_url) {
        window.location.href = result.payment_url;  // same tab — return_url brings back
      } else {
        toast({ title: "Checkout created", description: `Order: ${result.order_id}` });
      }
    } catch {
      toast({ title: "Checkout failed", description: "Please try again.", variant: "destructive" });
    }
  };

  return (
    <div className="p-4 sm:p-6 space-y-8 max-w-4xl mx-auto w-full">

      {/* Payment return banners */}
      {verifying && (
        <div className="flex items-center gap-3 rounded-xl bg-amber-50 border border-amber-200 px-4 py-3 text-amber-700">
          <Loader2 className="w-4 h-4 animate-spin shrink-0" />
          <span className="text-sm font-medium">Verifying your payment…</span>
        </div>
      )}
      {paymentSuccess && (
        <div className="flex items-center gap-3 rounded-xl bg-green-50 border border-green-200 px-4 py-3 text-green-700">
          <CheckCircle2 className="w-4 h-4 shrink-0" />
          <span className="text-sm font-medium">Payment successful! Your plan is now active.</span>
        </div>
      )}

      {/* Header */}
      <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="text-center space-y-2">
        <p className="text-xs uppercase tracking-widest text-primary/60">Plans</p>
        <h1 className="font-display text-3xl text-foreground text-glow-gold">Choose your plan</h1>
        <p className="text-sm text-muted-foreground max-w-md mx-auto">
          Unlock the full power of Vedic astrology — from free daily guidance to deep scripture analysis.
        </p>
      </motion.div>

      {/* Period Toggle */}
      <div className="flex justify-center">
        <div className="flex items-center gap-1 bg-muted/20 border border-border/30 rounded-full p-1">
          {(["monthly", "yearly"] as Period[]).map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={cn(
                "px-5 py-1.5 rounded-full text-sm font-medium transition-all",
                period === p ? "bg-primary text-primary-foreground shadow" : "text-muted-foreground hover:text-foreground"
              )}
            >
              {p === "yearly" ? "Yearly · Save up to 17%" : "Monthly"}
            </button>
          ))}
        </div>
      </div>

      {/* Plan Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {plans.map((plan, i) => {
          const Icon  = PLAN_ICONS[plan.id] ?? Sparkles;
          const color = PLAN_COLORS[plan.id] ?? PLAN_COLORS.free;
          const isCurrent   = plan.id === currentPlan;
          const isPopular   = plan.id === POPULAR_PLAN;
          const price       = period === "monthly" ? plan.price_monthly : Math.round(plan.price_yearly / 12);
          const yearlyTotal = plan.price_yearly;
          const discount    = yearlyDiscount(plan.price_monthly, plan.price_yearly);

          return (
            <motion.div
              key={plan.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.08 }}
              className={cn(
                "relative cosmic-card rounded-2xl p-6 flex flex-col gap-5 bg-gradient-to-br border",
                color.gradient,
                isPopular ? `ring-2 ${color.ring} border-transparent` : "border-border/30",
              )}
            >
              {/* Badges */}
              {isPopular && !isCurrent && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-0.5 rounded-full bg-primary text-primary-foreground text-xs font-semibold uppercase tracking-wide shadow">
                  Most Popular
                </div>
              )}
              {isCurrent && (
                <div className="absolute -top-3 right-4 px-3 py-0.5 rounded-full bg-green-500/20 text-green-500 text-xs font-semibold border border-green-500/30">
                  Current Plan
                </div>
              )}

              {/* Plan header */}
              <div className="space-y-1">
                <div className="flex items-center gap-2 mb-3">
                  <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
                    <Icon className="h-4 w-4 text-primary" />
                  </div>
                  <div>
                    <p className="font-display text-lg text-foreground leading-tight">{plan.name}</p>
                    <p className="text-xs text-muted-foreground">{plan.name_hi}</p>
                  </div>
                </div>

                {/* Price box — like Claude */}
                <div className={cn("rounded-xl border p-3", color.price)}>
                  <div className="flex items-baseline gap-1">
                    <span className="font-display text-3xl text-foreground">
                      {price === 0 ? "₹0" : `₹${price}`}
                    </span>
                    {price > 0 && <span className="text-sm text-muted-foreground">/ month</span>}
                  </div>
                  {period === "yearly" && yearlyTotal > 0 ? (
                    <p className="text-xs text-primary mt-0.5">₹{yearlyTotal} / year · Save {discount}%</p>
                  ) : price === 0 ? (
                    <p className="text-xs text-muted-foreground mt-0.5">Free forever</p>
                  ) : (
                    <p className="text-xs text-muted-foreground mt-0.5">Billed monthly</p>
                  )}
                </div>
              </div>

              {/* CTA button */}
              <Button
                onClick={() => handleUpgrade(plan.id)}
                disabled={isCurrent || plan.id === "free" || checkout.isPending}
                className={cn(
                  "w-full gap-2 rounded-xl font-semibold",
                  isCurrent || plan.id === "free" ? "opacity-50 cursor-not-allowed border border-border/40 bg-transparent text-muted-foreground" : color.btn,
                )}
              >
                {checkout.isPending ? "Processing…" : isCurrent ? "Current Plan" : plan.id === "free" ? "Free Plan" : `Get ${plan.name} plan`}
                {!isCurrent && plan.id !== "free" && <ChevronRight className="h-4 w-4" />}
              </Button>

              {/* Features */}
              <ul className="space-y-2 flex-1">
                {plan.features.map((f) => (
                  <li key={f} className={cn("flex items-start gap-2 text-xs", f.endsWith(":") || f.endsWith("plus:") ? "text-foreground font-semibold mt-1" : "text-muted-foreground")}>
                    {!(f.endsWith(":") || f.endsWith("plus:")) && (
                      <Check className="h-3.5 w-3.5 text-primary mt-0.5 flex-shrink-0" />
                    )}
                    {f}
                  </li>
                ))}
              </ul>
            </motion.div>
          );
        })}
      </div>

      {/* Trust badges */}
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.4 }}
        className="flex flex-wrap items-center justify-center gap-6 text-xs text-muted-foreground"
      >
        {["🔒 Cashfree Secured", "⚡ UPI / Cards / Wallets", "🔁 Cancel Anytime", "🇮🇳 Made in India"].map((b) => (
          <span key={b}>{b}</span>
        ))}
      </motion.div>

      {/* FAQ */}
      <div className="max-w-2xl mx-auto space-y-3">
        <h2 className="font-display text-xl text-foreground text-center mb-6">Frequently Asked Questions</h2>
        {FAQ.map((item, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.5 + i * 0.05 }}
            className="cosmic-card rounded-xl overflow-hidden"
          >
            <button
              onClick={() => setOpenFaq(openFaq === i ? null : i)}
              className="w-full flex items-center justify-between px-5 py-4 text-sm text-foreground hover:bg-muted/10 transition-colors text-left"
            >
              <span className="font-medium">{item.q}</span>
              <ChevronRight className={`h-4 w-4 text-muted-foreground flex-shrink-0 transition-transform ${openFaq === i ? "rotate-90" : ""}`} />
            </button>
            {openFaq === i && (
              <motion.div
                initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }}
                className="px-5 pb-4 text-xs text-muted-foreground leading-relaxed"
              >
                {item.a}
              </motion.div>
            )}
          </motion.div>
        ))}
      </div>
    </div>
  );
}
