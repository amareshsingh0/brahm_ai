import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { Lock, ChevronRight, X, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { usePlanCheck } from "@/hooks/useSubscription";
import type { AuthPlan } from "@/store/authStore";

const PLAN_DISPLAY: Record<AuthPlan, { label: string; labelHi: string; price: string }> = {
  free:      { label: "Free",      labelHi: "निःशुल्क", price: "₹0/mo"    },
  jyotishi:  { label: "Jyotishi",  labelHi: "ज्योतिषी",  price: "₹199/mo"  },
  acharya:   { label: "Acharya",   labelHi: "आचार्य",    price: "₹499/mo"  },
};

const PLAN_FEATURES: Record<AuthPlan, string[]> = {
  free:     [],
  jyotishi: ["Unlimited AI Chat", "Full Kundali + Dashas", "Compatibility Analysis", "Muhurta Finder"],
  acharya:  ["Sanskrit Library Search (1.1M chunks)", "Varshaphala (coming soon)", "Priority GPU inference"],
};

interface PlanGateProps {
  /** Minimum plan required to access the children */
  minPlan: AuthPlan;
  /** Feature name shown in the lock overlay */
  featureName?: string;
  /** Show as full-page block (default) or inline locked overlay */
  mode?: "block" | "overlay";
  children: React.ReactNode;
}

/**
 * Wraps a component. If the user's plan doesn't meet `minPlan`,
 * shows a lock screen with an upgrade CTA instead of the children.
 */
export function PlanGate({ minPlan, featureName, mode = "block", children }: PlanGateProps) {
  const hasAccess = usePlanCheck(minPlan);
  const [showModal, setShowModal] = useState(false);
  const navigate = useNavigate();

  if (hasAccess) return <>{children}</>;

  const plan = PLAN_DISPLAY[minPlan];
  const features = PLAN_FEATURES[minPlan];

  if (mode === "overlay") {
    return (
      <div className="relative">
        <div className="select-none pointer-events-none opacity-50 blur-sm">{children}</div>
        <div className="absolute inset-0 flex items-center justify-center">
          <button
            onClick={() => setShowModal(true)}
            className="flex flex-col items-center gap-2 group"
          >
            <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center glow-border group-hover:bg-primary/30 transition-colors">
              <Lock className="h-5 w-5 text-primary" />
            </div>
            <span className="text-xs text-primary font-medium">{plan.label}+ required</span>
          </button>
        </div>
        <UpgradeModal
          open={showModal}
          onClose={() => setShowModal(false)}
          plan={plan}
          features={features}
          featureName={featureName}
          onUpgrade={() => navigate("/subscription")}
        />
      </div>
    );
  }

  return (
    <>
      <div className="flex flex-col items-center justify-center min-h-[40vh] px-4 text-center space-y-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          className="w-20 h-20 rounded-full bg-primary/10 flex items-center justify-center glow-border"
        >
          <Lock className="h-8 w-8 text-primary" />
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          <h2 className="font-display text-2xl text-foreground mb-1">
            {featureName ? `${featureName} requires ${plan.label}` : `${plan.label} Plan Required`}
          </h2>
          <p className="text-sm text-muted-foreground">
            Upgrade to <span className="text-primary">{plan.label} ({plan.labelHi})</span> to unlock this feature.
          </p>
        </motion.div>

        {features.length > 0 && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.2 }}
            className="cosmic-card rounded-xl p-5 max-w-sm w-full text-left space-y-2"
          >
            <div className="flex items-center gap-2 mb-3">
              <Sparkles className="h-4 w-4 text-primary" />
              <span className="text-xs uppercase tracking-wider text-muted-foreground">What you get</span>
            </div>
            {features.map((f) => (
              <div key={f} className="flex items-center gap-2 text-sm text-foreground">
                <ChevronRight className="h-3.5 w-3.5 text-primary flex-shrink-0" />
                {f}
              </div>
            ))}
            <p className="text-xs text-muted-foreground pt-1">Starting at {plan.price}</p>
          </motion.div>
        )}

        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.3 }} className="flex gap-3">
          <Button onClick={() => navigate("/subscription")} className="gap-2">
            Upgrade to {plan.label} <ChevronRight className="h-4 w-4" />
          </Button>
          <Button variant="ghost" onClick={() => navigate(-1)} className="text-muted-foreground">
            Go Back
          </Button>
        </motion.div>
      </div>
    </>
  );
}

// ─── Upgrade Modal (used in overlay mode) ────────────────────────────────────

interface UpgradeModalProps {
  open: boolean;
  onClose: () => void;
  plan: { label: string; labelHi: string; price: string };
  features: string[];
  featureName?: string;
  onUpgrade: () => void;
}

function UpgradeModal({ open, onClose, plan, features, featureName, onUpgrade }: UpgradeModalProps) {
  return (
    <AnimatePresence>
      {open && (
        <>
          {/* Backdrop */}
          <motion.div
            className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          {/* Modal */}
          <motion.div
            className="fixed z-50 inset-x-4 sm:inset-x-auto sm:left-1/2 sm:-translate-x-1/2 bottom-4 sm:bottom-auto sm:top-1/2 sm:-translate-y-1/2 max-w-sm w-full cosmic-card rounded-2xl p-6 shadow-2xl"
            initial={{ opacity: 0, y: 40, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.95 }}
          >
            <button onClick={onClose} className="absolute top-4 right-4 text-muted-foreground hover:text-foreground">
              <X className="h-4 w-4" />
            </button>

            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center">
                <Lock className="h-4 w-4 text-primary" />
              </div>
              <div>
                <p className="font-display text-base text-foreground">
                  {featureName || "This feature"} requires {plan.label}
                </p>
                <p className="text-xs text-muted-foreground">{plan.labelHi} · {plan.price}</p>
              </div>
            </div>

            {features.length > 0 && (
              <div className="space-y-1.5 mb-4">
                {features.slice(0, 4).map((f) => (
                  <div key={f} className="flex items-center gap-2 text-xs text-muted-foreground">
                    <ChevronRight className="h-3 w-3 text-primary flex-shrink-0" />
                    {f}
                  </div>
                ))}
              </div>
            )}

            <Button onClick={onUpgrade} className="w-full gap-2">
              Upgrade to {plan.label} <ChevronRight className="h-4 w-4" />
            </Button>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}