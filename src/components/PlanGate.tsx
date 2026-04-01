/**
 * FeatureGate / PlanGate — dynamic feature access control.
 *
 * Uses the admin-controlled feature flag system. The backend determines
 * which feature keys are unlocked per plan. No hardcoded plan logic here.
 *
 * Usage:
 *   <FeatureGate feature="kundali" featureName="Kundali">
 *     <KundaliPage />
 *   </FeatureGate>
 *
 *   <FeatureGate feature="palm_reading" mode="overlay">
 *     <PalmistryCard />
 *   </FeatureGate>
 */
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { Lock, ChevronRight, X, Sparkles, Zap } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useSubscription } from "@/hooks/useSubscription";

// ── Feature display names ─────────────────────────────────────────────────────

const FEATURE_META: Record<string, { icon: string; tagline: string }> = {
  ai_chat:        { icon: "💬", tagline: "Ask anything about your Vedic chart" },
  kundali:        { icon: "🔮", tagline: "Full birth chart + 7-tab deep analysis" },
  palm_reading:   { icon: "🤚", tagline: "AI-powered palmistry reading" },
  live_sky_today: { icon: "🌌", tagline: "Today's cosmic influence on your chart" },
  muhurta:        { icon: "⏰", tagline: "Find the most auspicious timing" },
  horoscope_ai:   { icon: "♈", tagline: "AI-personalized daily horoscope" },
  compatibility:  { icon: "💑", tagline: "Kundali matching & compatibility score" },
  gochar:         { icon: "🪐", tagline: "Current planetary transits over your chart" },
  kp_system:      { icon: "📐", tagline: "Krishnamurti Paddhati predictions" },
  dosha:          { icon: "⚠️", tagline: "Dosha detection and remedies" },
  gemstone:       { icon: "💎", tagline: "Personalized gemstone recommendations" },
  nakshatra:      { icon: "⭐", tagline: "Your nakshatra's deep influence" },
  prashna:        { icon: "❓", tagline: "Horary astrology — answers right now" },
  varshphal:      { icon: "📅", tagline: "Annual solar return chart" },
  rectification:  { icon: "🎯", tagline: "Find your exact birth time" },
  pdf_export:     { icon: "📄", tagline: "Download your kundali as PDF" },
  chat_history:   { icon: "📜", tagline: "Access all your past AI conversations" },
};

// ── Main component ────────────────────────────────────────────────────────────

interface FeatureGateProps {
  /** Feature flag key — must match backend feature_flags.key */
  feature: string;
  /** Human-readable name shown in lock screen */
  featureName?: string;
  /** "block" = full lock screen replacement, "overlay" = blurred overlay */
  mode?: "block" | "overlay";
  children: React.ReactNode;
}

export function FeatureGate({ feature, featureName, mode = "block", children }: FeatureGateProps) {
  const { hasFeature, info, isLoading } = useSubscription();
  const [showModal, setShowModal] = useState(false);
  const navigate = useNavigate();

  // While loading, render children (fail-open UX — no flicker)
  if (isLoading) return <>{children}</>;

  if (hasFeature(feature)) return <>{children}</>;

  const meta      = FEATURE_META[feature] ?? { icon: "🔒", tagline: "Upgrade to unlock this feature" };
  const name      = featureName ?? feature.replace(/_/g, " ").replace(/\b\w/g, c => c.toUpperCase());
  const isPaid    = !info.is_free;
  const isExpired = info.status === "expired";

  if (mode === "overlay") {
    return (
      <div className="relative">
        <div className="select-none pointer-events-none opacity-40 blur-sm">{children}</div>
        <div className="absolute inset-0 flex items-center justify-center">
          <button onClick={() => setShowModal(true)} className="flex flex-col items-center gap-2 group">
            <div className="w-12 h-12 rounded-full bg-amber-100 border border-amber-300 flex items-center justify-center group-hover:bg-amber-200 transition-colors shadow-md">
              <Lock className="h-5 w-5 text-amber-700" />
            </div>
            <span className="text-xs text-amber-700 font-semibold bg-white/90 px-2 py-0.5 rounded-full shadow">
              {isExpired ? "Plan expired" : "Upgrade to unlock"}
            </span>
          </button>
        </div>
        <UpgradeModal
          open={showModal}
          onClose={() => setShowModal(false)}
          feature={feature}
          featureName={name}
          meta={meta}
          isExpired={isExpired}
          onUpgrade={() => navigate("/subscription")}
        />
      </div>
    );
  }

  // Block mode — full screen replacement
  return (
    <div className="flex flex-col items-center justify-center min-h-[50vh] px-6 text-center space-y-6 py-12">
      <motion.div
        initial={{ opacity: 0, scale: 0.85 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-20 h-20 rounded-full bg-amber-50 border-2 border-amber-200 flex items-center justify-center text-3xl shadow-sm"
      >
        {meta.icon}
      </motion.div>

      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.08 }}>
        <h2 className="font-display text-xl text-foreground mb-1.5">
          {isExpired ? `${name} — Plan Expired` : `${name} is a Premium Feature`}
        </h2>
        <p className="text-sm text-muted-foreground max-w-xs mx-auto">
          {isExpired
            ? "Your subscription has expired. Renew to continue accessing all features."
            : meta.tagline
          }
        </p>
      </motion.div>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.15 }}
        className="bg-gradient-to-br from-amber-50 to-orange-50 border border-amber-200 rounded-2xl p-5 max-w-xs w-full text-left space-y-2 shadow-sm"
      >
        <div className="flex items-center gap-2 mb-3">
          <Sparkles className="h-4 w-4 text-amber-700" />
          <span className="text-xs font-semibold uppercase tracking-wide text-amber-700">
            {isExpired ? "Renew your plan" : "What you unlock"}
          </span>
        </div>
        {[
          "AI Chat — ask anything about your chart",
          "Kundali with full 7-tab analysis",
          "Gochar transits & daily insights",
          "Palmistry, Muhurta, Compatibility & more",
        ].map((f) => (
          <div key={f} className="flex items-center gap-2 text-sm text-foreground/80">
            <ChevronRight className="h-3.5 w-3.5 text-amber-600 flex-shrink-0" />
            {f}
          </div>
        ))}
        <p className="text-xs text-muted-foreground pt-1">Plans starting at ₹299/month</p>
      </motion.div>

      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.22 }} className="flex gap-3">
        <Button
          onClick={() => navigate("/subscription")}
          className="gap-2 bg-amber-600 hover:bg-amber-700 text-white font-semibold shadow"
        >
          <Zap className="h-4 w-4" />
          {isExpired ? "Renew Plan" : "Upgrade Now"}
        </Button>
        <Button variant="ghost" onClick={() => navigate(-1)} className="text-muted-foreground">
          Go Back
        </Button>
      </motion.div>
    </div>
  );
}

// ── Upgrade Modal (overlay mode) ──────────────────────────────────────────────

function UpgradeModal({ open, onClose, featureName, meta, isExpired, onUpgrade }: {
  open:        boolean;
  onClose:     () => void;
  feature:     string;
  featureName: string;
  meta:        { icon: string; tagline: string };
  isExpired:   boolean;
  onUpgrade:   () => void;
}) {
  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div
            className="fixed inset-0 z-50 bg-black/30 backdrop-blur-sm"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.div
            className="fixed z-50 inset-x-4 sm:inset-x-auto sm:left-1/2 sm:-translate-x-1/2 bottom-4 sm:bottom-auto sm:top-1/2 sm:-translate-y-1/2 max-w-sm w-full bg-white rounded-2xl p-6 shadow-2xl border border-amber-100"
            initial={{ opacity: 0, y: 40, scale: 0.95 }}
            animate={{ opacity: 1, y: 0,  scale: 1     }}
            exit={{    opacity: 0, y: 20,  scale: 0.95  }}
          >
            <button onClick={onClose} className="absolute top-4 right-4 text-muted-foreground hover:text-foreground">
              <X className="h-4 w-4" />
            </button>

            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 rounded-xl bg-amber-50 border border-amber-200 flex items-center justify-center text-2xl">
                {meta.icon}
              </div>
              <div>
                <p className="font-semibold text-foreground text-sm">{featureName}</p>
                <p className="text-xs text-muted-foreground">{isExpired ? "Plan expired — renew to continue" : meta.tagline}</p>
              </div>
            </div>

            <p className="text-sm text-muted-foreground mb-4">
              {isExpired
                ? "Renew your subscription to regain full access."
                : "Upgrade your plan to unlock this and all other premium features."
              }
            </p>

            <Button onClick={onUpgrade} className="w-full gap-2 bg-amber-600 hover:bg-amber-700 text-white font-semibold">
              <Zap className="h-4 w-4" />
              {isExpired ? "Renew Now" : "Upgrade Now"}
            </Button>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}

// ── Backward-compat alias ─────────────────────────────────────────────────────
export { FeatureGate as PlanGate };
