import { useState } from "react";
import { motion } from "framer-motion";
import { Check, ChevronRight, Sparkles, Zap, Crown, Shield } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/authStore";
import { usePlans, useCheckout } from "@/hooks/useSubscription";
import { useToast } from "@/hooks/use-toast";
import type { AuthPlan } from "@/store/authStore";

type Period = "monthly" | "yearly";

const PLAN_ICONS: Record<string, React.ElementType> = {
  free:     Shield,
  jyotishi: Sparkles,
  acharya:  Crown,
};

const PLAN_GRADIENTS: Record<string, string> = {
  free:     "from-muted/20 to-muted/5 border-border/30",
  jyotishi: "from-amber-500/10 to-amber-500/5 border-amber-500/30",
  acharya:  "from-purple-500/10 to-purple-500/5 border-purple-500/30",
};

const PLAN_HIGHLIGHT: Record<string, boolean> = {
  free: false, jyotishi: true, acharya: false,
};

const FAQ = [
  {
    q: "How does billing work?",
    a: "You're billed at the start of each period (monthly/yearly). Cancel anytime — your plan stays active until the end of the paid period.",
  },
  {
    q: "Which payment methods are accepted?",
    a: "UPI, credit/debit cards, net banking, and all major wallets via Cashfree — India's trusted payment gateway.",
  },
  {
    q: "Can I switch plans?",
    a: "Yes. Upgrade instantly, downgrade at the end of your current period.",
  },
  {
    q: "Is my data safe?",
    a: "Your birth details and charts are stored securely on our VM and never shared with third parties.",
  },
];

export default function SubscriptionPage() {
  const [period, setPeriod] = useState<Period>("monthly");
  const [openFaq, setOpenFaq] = useState<number | null>(null);
  const currentPlan = useAuthStore((s) => s.plan);
  const { data: plans = [] } = usePlans();
  const checkout = useCheckout();
  const { toast } = useToast();

  const yearlyDiscount = (monthly: number, yearly: number) => {
    if (!monthly) return 0;
    return Math.round(((monthly * 12 - yearly) / (monthly * 12)) * 100);
  };

  const handleUpgrade = async (planId: string) => {
    if (planId === "free" || planId === currentPlan) return;
    try {
      const result = await checkout.mutateAsync({ plan: planId, period });
      // Open Cashfree payment URL
      if (result.payment_url) {
        window.open(result.payment_url, "_blank");
      } else {
        toast({ title: "Checkout created", description: `Order: ${result.order_id}` });
      }
    } catch {
      // Backend not wired yet — show friendly message
      toast({
        title: "Payment coming soon",
        description: "Cashfree integration will be enabled on production. Your plan: " + planId,
      });
    }
  };

  const getButtonLabel = (planId: AuthPlan) => {
    if (planId === currentPlan) return "Current Plan";
    if (planId === "free") return "Downgrade to Free";
    return `Upgrade to ${plans.find((p) => p.id === planId)?.name ?? planId}`;
  };

  return (
    <div className="p-6 space-y-10 max-w-5xl mx-auto">
      {/* Header */}
      <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} className="text-center space-y-2">
        <p className="text-xs uppercase tracking-widest text-primary/60">Brahm AI Plans</p>
        <h1 className="font-display text-3xl text-foreground text-glow-gold">Choose Your Journey</h1>
        <p className="text-sm text-muted-foreground max-w-md mx-auto">
          Start free. Upgrade when you're ready for deeper cosmic insight.
        </p>
      </motion.div>

      {/* Period Toggle */}
      <div className="flex justify-center">
        <div className="flex items-center gap-1 bg-muted/20 border border-border/30 rounded-full p-1">
          {(["monthly", "yearly"] as Period[]).map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`px-5 py-1.5 rounded-full text-sm font-medium transition-all ${
                period === p ? "bg-primary text-primary-foreground shadow" : "text-muted-foreground hover:text-foreground"
              }`}
            >
              {p === "yearly" ? "Yearly (save up to 37%)" : "Monthly"}
            </button>
          ))}
        </div>
      </div>

      {/* Plan Cards */}
      <div className="grid sm:grid-cols-3 gap-5">
        {plans.map((plan, i) => {
          const Icon = PLAN_ICONS[plan.id] ?? Zap;
          const isHighlight = PLAN_HIGHLIGHT[plan.id];
          const isCurrent = plan.id === currentPlan;
          const price = period === "monthly" ? plan.price_monthly : Math.round(plan.price_yearly / 12);
          const discount = yearlyDiscount(plan.price_monthly, plan.price_yearly);

          return (
            <motion.div
              key={plan.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }}
              className={`relative cosmic-card rounded-2xl p-6 flex flex-col gap-5 bg-gradient-to-br border
                ${PLAN_GRADIENTS[plan.id]}
                ${isHighlight ? "ring-1 ring-primary/40 glow-border" : ""}
              `}
            >
              {isHighlight && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-0.5 rounded-full bg-primary text-primary-foreground text-xs font-medium uppercase tracking-wide shadow">
                  Most Popular
                </div>
              )}

              {isCurrent && (
                <div className="absolute -top-3 right-4 px-3 py-0.5 rounded-full bg-green-500/20 text-green-400 text-xs font-medium border border-green-500/30">
                  Current Plan
                </div>
              )}

              {/* Plan header */}
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
                    <Icon className="h-4 w-4 text-primary" />
                  </div>
                  <div>
                    <p className="font-display text-lg text-foreground leading-tight">{plan.name}</p>
                    <p className="text-xs text-muted-foreground">{plan.name_hi}</p>
                  </div>
                </div>

                <div className="flex items-end gap-1 mt-2">
                  <span className="font-display text-4xl text-foreground">
                    {price === 0 ? "₹0" : `₹${price}`}
                  </span>
                  {price > 0 && (
                    <span className="text-sm text-muted-foreground pb-1">/month</span>
                  )}
                </div>

                {period === "yearly" && plan.price_yearly > 0 && (
                  <p className="text-xs text-primary">
                    ₹{plan.price_yearly}/year · Save {discount}%
                  </p>
                )}
                {period === "monthly" && plan.price_monthly > 0 && (
                  <p className="text-xs text-muted-foreground">
                    billed monthly · cancel anytime
                  </p>
                )}
              </div>

              {/* Features */}
              <ul className="space-y-2 flex-1">
                {plan.features.map((f) => (
                  <li key={f} className="flex items-start gap-2 text-xs text-muted-foreground">
                    <Check className="h-3.5 w-3.5 text-primary mt-0.5 flex-shrink-0" />
                    {f}
                  </li>
                ))}
              </ul>

              {/* CTA */}
              <Button
                onClick={() => handleUpgrade(plan.id)}
                disabled={isCurrent || plan.id === "free" || checkout.isPending}
                variant={isHighlight ? "default" : "outline"}
                className={`w-full gap-2 ${
                  isCurrent ? "opacity-50 cursor-not-allowed" : ""
                } ${plan.id === "free" ? "border-border/30 text-muted-foreground" : ""}`}
              >
                {checkout.isPending ? "Processing…" : getButtonLabel(plan.id as AuthPlan)}
                {!isCurrent && plan.id !== "free" && <ChevronRight className="h-4 w-4" />}
              </Button>
            </motion.div>
          );
        })}
      </div>

      {/* Trust badges */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.4 }}
        className="flex flex-wrap items-center justify-center gap-6 text-xs text-muted-foreground"
      >
        {[
          "🔒 Secured by Cashfree",
          "📱 UPI · Cards · Wallets",
          "↩️ Cancel anytime",
          "🇮🇳 Made for India",
        ].map((badge) => (
          <span key={badge} className="flex items-center gap-1">{badge}</span>
        ))}
      </motion.div>

      {/* FAQ */}
      <div className="max-w-2xl mx-auto space-y-3">
        <h2 className="font-display text-xl text-foreground text-center mb-6">Frequently Asked Questions</h2>
        {FAQ.map((item, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.5 + i * 0.05 }}
            className="cosmic-card rounded-xl overflow-hidden"
          >
            <button
              onClick={() => setOpenFaq(openFaq === i ? null : i)}
              className="w-full flex items-center justify-between px-5 py-4 text-sm text-foreground hover:bg-muted/10 transition-colors text-left"
            >
              <span className="font-medium">{item.q}</span>
              <ChevronRight
                className={`h-4 w-4 text-muted-foreground flex-shrink-0 transition-transform ${openFaq === i ? "rotate-90" : ""}`}
              />
            </button>
            {openFaq === i && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: "auto", opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
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
