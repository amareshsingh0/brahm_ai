import { useState } from "react";
import { Menu, X, Moon, LogOut, LayoutDashboard, Star, Globe, Clock, Sparkles, BookOpen, Moon as MoonIcon, Heart, Gem, Sun, Compass, User, Zap, Hand, Eclipse, Calendar, Bot, Library, Music, TreePine, Database, CreditCard, HelpCircle, ShieldAlert, Activity } from "lucide-react";
import { NavLink as RouterNavLink, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/hooks/useAuth";
import { useAuthStore } from "@/store/authStore";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { motion, AnimatePresence } from "framer-motion";

const mainNavItems = [
  { key: "dashboard", url: "/dashboard", icon: LayoutDashboard },
  { key: "chat", url: "/chat", icon: Bot },
  { key: "kundli", url: "/kundli", icon: Star },
  { key: "sky", url: "/sky", icon: Globe },
  { key: "timeline", url: "/timeline", icon: Clock },
  { key: "horoscope", url: "/horoscope", icon: Sun },
];

const exploreNavItems = [
  { key: "rashi", url: "/rashi", icon: Sparkles },
  { key: "nakshatra", url: "/nakshatra", icon: Compass },
  { key: "yogas", url: "/yogas", icon: Zap },
  { key: "remedies", url: "/remedies", icon: Gem },
  { key: "gemstones", url: "/gemstones", icon: Gem },
  { key: "compatibility", url: "/compatibility", icon: Heart },
  { key: "palmistry", url: "/palmistry", icon: Hand },
  { key: "gochar", url: "/gochar", icon: MoonIcon },
  { key: "rectification", url: "/rectification", icon: Clock },
  { key: "prashna", url: "/prashna", icon: HelpCircle },
  { key: "varshphal", url: "/varshphal", icon: Sun },
  { key: "kp", url: "/kp", icon: Star },
  { key: "dosha", url: "/dosha", icon: ShieldAlert },
  { key: "sade_sati", url: "/sade-sati", icon: Activity },
  { key: "today", url: "/today", icon: Calendar },
  { key: "panchang", url: "/panchang", icon: Eclipse },
  { key: "library", url: "/library", icon: Library },
  { key: "mantras", url: "/mantras", icon: Music },
  { key: "gotra", url: "/gotra", icon: TreePine },
  { key: "knowledge", url: "/knowledge", icon: Database },
  { key: "stories", url: "/stories", icon: BookOpen },
];

const accountNavItems = [
  { key: "profile", url: "/profile", icon: User },
  { key: "subscription", url: "/subscription", icon: CreditCard },
];

const PLAN_BADGE: Record<string, { label: string; className: string }> = {
  free:     { label: "Free",      className: "bg-muted/40 text-muted-foreground" },
  jyotishi: { label: "Jyotishi",  className: "bg-amber-500/20 text-amber-400" },
  acharya:  { label: "Acharya",   className: "bg-purple-500/20 text-purple-400" },
};

export function MobileMenuButton({ onOpen }: { onOpen: () => void }) {
  return (
    <button
      onClick={onOpen}
      className="md:hidden flex items-center justify-center w-9 h-9 rounded-lg text-muted-foreground hover:text-primary hover:bg-muted/30 transition-colors"
      aria-label="Open menu"
    >
      <Menu className="h-5 w-5" />
    </button>
  );
}

export function MobileDrawer() {
  const [open, setOpen] = useState(false);
  const { t } = useTranslation();
  const { logout } = useAuth();
  const navigate = useNavigate();
  const plan = useAuthStore((s) => s.plan);
  const name = useAuthStore((s) => s.name);
  const badge = PLAN_BADGE[plan] ?? PLAN_BADGE.free;

  const handleLogout = () => {
    logout();
    navigate("/");
    setOpen(false);
  };

  const handleNav = () => setOpen(false);

  return (
    <>
      {/* Hamburger trigger — rendered in header */}
      <button
        onClick={() => setOpen(true)}
        className="md:hidden flex items-center justify-center w-9 h-9 rounded-lg text-muted-foreground hover:text-primary hover:bg-muted/30 transition-colors"
        aria-label="Open menu"
      >
        <Menu className="h-5 w-5" />
      </button>

      <AnimatePresence>
        {open && (
          <>
            {/* Backdrop */}
            <motion.div
              key="backdrop"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              className="md:hidden fixed inset-0 z-50 bg-black/60 backdrop-blur-sm"
              onClick={() => setOpen(false)}
            />

            {/* Drawer */}
            <motion.div
              key="drawer"
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{ type: "spring", damping: 28, stiffness: 300 }}
              className="md:hidden fixed top-0 left-0 bottom-0 z-50 w-[280px] flex flex-col glass border-r border-border/40"
              style={{ background: "hsl(220 25% 8% / 0.97)" }}
            >
              {/* Header */}
              <div className="flex items-center justify-between px-4 py-4 border-b border-border/20">
                <div className="flex items-center gap-2.5">
                  <Moon className="h-6 w-6 text-primary zodiac-glow" />
                  <span className="font-display text-base text-primary text-glow-gold">{t('appTitle')}</span>
                </div>
                <button
                  onClick={() => setOpen(false)}
                  className="flex items-center justify-center w-8 h-8 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted/30 transition-colors"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {/* User + plan */}
              {name && (
                <div className="mx-3 mt-3 px-3 py-2.5 rounded-xl bg-muted/10 border border-border/20 flex items-center justify-between">
                  <p className="text-sm text-foreground truncate max-w-[150px]">{name}</p>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${badge.className}`}>
                    {badge.label}
                  </span>
                </div>
              )}

              {/* Scrollable nav */}
              <div className="flex-1 overflow-y-auto py-2 px-2">
                <NavSection label={t('nav_group.main')} items={mainNavItems} t={t} onNav={handleNav} />
                <NavSection label={t('nav_group.explore')} items={exploreNavItems} t={t} onNav={handleNav} />
                <NavSection label={t('nav_group.account')} items={accountNavItems} t={t} onNav={handleNav} />
              </div>

              {/* Footer */}
              <div className="border-t border-border/20 px-3 py-3 space-y-1">
                <div className="flex items-center gap-2 px-2 py-1">
                  <LanguageSwitcher variant="full" />
                </div>
                <button
                  onClick={handleLogout}
                  className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors rounded-lg"
                >
                  <LogOut className="h-4 w-4" />
                  <span>{t('nav.logout')}</span>
                </button>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </>
  );
}

function NavSection({
  label, items, t, onNav,
}: {
  label: string;
  items: { key: string; url: string; icon: React.ElementType }[];
  t: (key: string) => string;
  onNav: () => void;
}) {
  return (
    <div className="mb-1">
      <p className="text-[10px] text-muted-foreground/50 uppercase tracking-widest px-3 py-2">{label}</p>
      {items.map((item) => (
        <RouterNavLink
          key={item.url}
          to={item.url}
          onClick={onNav}
          className={({ isActive }) =>
            `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm transition-colors ${
              isActive
                ? "bg-primary/10 text-primary font-medium"
                : "text-muted-foreground hover:text-foreground hover:bg-muted/20"
            }`
          }
        >
          <item.icon className="h-4 w-4 shrink-0" />
          <span>{t(`nav.${item.key}`)}</span>
        </RouterNavLink>
      ))}
    </div>
  );
}
