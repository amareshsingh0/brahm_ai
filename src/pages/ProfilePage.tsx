import { motion } from "framer-motion";
import { useKundliStore } from "@/store/kundliStore";
import { useAuthStore } from "@/store/authStore";
import { useAuth } from "@/hooks/useAuth";
import { useNavigate, Link } from "react-router-dom";
import { useSubscriptionStatus } from "@/hooks/useSubscription";
import {
  User, Calendar, Clock, MapPin, Edit2,
  LogOut, CreditCard, ChevronRight, Crown, Shield, Sparkles, Phone,
} from "lucide-react";
import { Button } from "@/components/ui/button";

const PLAN_STYLE: Record<string, { icon: React.ElementType; color: string; bg: string; border: string }> = {
  free:     { icon: Shield,   color: "text-muted-foreground",  bg: "bg-muted/20",        border: "border-border/30" },
  jyotishi: { icon: Sparkles, color: "text-amber-400",         bg: "bg-amber-500/10",    border: "border-amber-500/30" },
  acharya:  { icon: Crown,    color: "text-purple-400",        bg: "bg-purple-500/10",   border: "border-purple-500/30" },
};

export default function ProfilePage() {
  const birthDetails = useKundliStore((s) => s.birthDetails);
  const name   = useAuthStore((s) => s.name);
  const phone  = useAuthStore((s) => s.phone);
  const plan   = useAuthStore((s) => s.plan);
  const { logout } = useAuth();
  const navigate = useNavigate();
  const { data: sub } = useSubscriptionStatus();

  const planStyle = PLAN_STYLE[plan] ?? PLAN_STYLE.free;
  const PlanIcon = planStyle.icon;

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <div className="p-6 space-y-6 max-w-xl">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">Profile</h1>
        <p className="text-sm text-muted-foreground">Your account, chart, and subscription</p>
      </motion.div>

      {/* ─── Account Info ─── */}
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.05 }}>
        <div className="cosmic-card rounded-xl p-5 space-y-3">
          <h2 className="text-xs uppercase tracking-wider text-muted-foreground">Account</h2>
          {name && <DetailRow icon={<User className="h-4 w-4" />} label="Name" value={name} />}
          {phone && <DetailRow icon={<Phone className="h-4 w-4" />} label="Mobile" value={phone} />}
        </div>
      </motion.div>

      {/* ─── Subscription Card ─── */}
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
        <div className={`cosmic-card rounded-xl p-5 space-y-4 bg-gradient-to-br ${planStyle.bg} border ${planStyle.border}`}>
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-lg ${planStyle.bg} border ${planStyle.border} flex items-center justify-center`}>
                <PlanIcon className={`h-5 w-5 ${planStyle.color}`} />
              </div>
              <div>
                <p className="text-xs text-muted-foreground uppercase tracking-wider">Current Plan</p>
                <p className={`font-display text-xl ${planStyle.color}`}>
                  {plan.charAt(0).toUpperCase() + plan.slice(1)}
                </p>
              </div>
            </div>
            {sub?.expires_at && (
              <div className="text-right">
                <p className="text-xs text-muted-foreground">Renews</p>
                <p className="text-xs text-foreground">{new Date(sub.expires_at).toLocaleDateString("en-IN")}</p>
              </div>
            )}
          </div>

          {plan === "free" ? (
            <div className="space-y-2">
              <p className="text-xs text-muted-foreground">
                Upgrade to unlock unlimited AI Chat, full Kundali analysis, and Compatibility.
              </p>
              <Link to="/subscription">
                <Button size="sm" className="gap-2 w-full sm:w-auto">
                  View Plans <ChevronRight className="h-3.5 w-3.5" />
                </Button>
              </Link>
            </div>
          ) : (
            <div className="flex items-center justify-between">
              <p className="text-xs text-muted-foreground">
                {sub?.period === "yearly" ? "Yearly plan" : "Monthly plan"} · Cancel anytime
              </p>
              <Link to="/subscription">
                <button className="text-xs text-primary hover:underline flex items-center gap-1">
                  <CreditCard className="h-3 w-3" /> Manage
                </button>
              </Link>
            </div>
          )}
        </div>
      </motion.div>

      {/* ─── Birth Details ─── */}
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.15 }}>
        {birthDetails ? (
          <div className="cosmic-card rounded-xl p-5 space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-xs uppercase tracking-wider text-muted-foreground">Birth Details</h2>
              <button
                onClick={() => navigate("/onboarding")}
                className="text-xs text-muted-foreground hover:text-primary flex items-center gap-1 transition-colors"
              >
                <Edit2 className="h-3 w-3" /> Edit
              </button>
            </div>
            <DetailRow icon={<User className="h-4 w-4" />}     label="Name"          value={birthDetails.name} />
            <DetailRow icon={<Calendar className="h-4 w-4" />} label="Date of Birth"  value={birthDetails.dateOfBirth} />
            <DetailRow icon={<Clock className="h-4 w-4" />}    label="Time of Birth"  value={birthDetails.timeOfBirth} />
            <DetailRow icon={<MapPin className="h-4 w-4" />}   label="Birth Place"    value={birthDetails.birthPlace} />
          </div>
        ) : (
          <div className="cosmic-card rounded-xl p-6 text-center space-y-3">
            <span className="text-3xl block">🌟</span>
            <h2 className="font-display text-base text-foreground">No Birth Details Yet</h2>
            <p className="text-xs text-muted-foreground">Generate your Vedic birth chart to unlock personalized insights.</p>
            <Button size="sm" onClick={() => navigate("/onboarding")}>
              Generate Kundali
            </Button>
          </div>
        )}
      </motion.div>

      {/* ─── Quick Stats ─── */}
      {birthDetails && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.2 }}>
          <div className="cosmic-card rounded-xl p-5">
            <h2 className="text-xs uppercase tracking-wider text-muted-foreground mb-3">Quick Summary</h2>
            <div className="grid grid-cols-3 gap-3">
              <QuickStat label="Moon Rashi" value="Cancer" />
              <QuickStat label="Nakshatra" value="Ashlesha" />
              <QuickStat label="Ascendant" value="Aries" />
            </div>
            <Link to="/kundli" className="block mt-3 text-xs text-primary hover:underline text-center">
              View Full Kundali →
            </Link>
          </div>
        </motion.div>
      )}

      {/* ─── Logout ─── */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.25 }}>
        <button
          onClick={handleLogout}
          className="flex items-center gap-2 text-sm text-muted-foreground hover:text-destructive transition-colors py-2"
        >
          <LogOut className="h-4 w-4" /> Logout
        </button>
      </motion.div>
    </div>
  );
}

function DetailRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center gap-3 bg-muted/20 rounded-lg p-3">
      <span className="text-primary">{icon}</span>
      <div>
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="text-sm text-foreground">{value}</p>
      </div>
    </div>
  );
}

function QuickStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="text-center bg-muted/20 rounded-lg p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm font-display text-foreground">{value}</p>
    </div>
  );
}
