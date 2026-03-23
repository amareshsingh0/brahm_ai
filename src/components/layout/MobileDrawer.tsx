import { useState } from "react";
import {
  Menu, X, Moon, LogOut, LayoutDashboard, Star, Globe, Clock,
  Sparkles, BookOpen, Moon as MoonIcon, Heart, Gem, Sun, Compass,
  User, Zap, Hand, Eclipse, Calendar, Bot, Library, Music, TreePine,
  Database, CreditCard, HelpCircle, ShieldAlert, Activity, ChevronUp, Settings,
} from "lucide-react";
import { NavLink as RouterNavLink, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/hooks/useAuth";
import { useAuthStore } from "@/store/authStore";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { motion, AnimatePresence } from "framer-motion";

const mainNavItems = [
  { key: "dashboard", url: "/dashboard", icon: LayoutDashboard },
  { key: "chat",      url: "/chat",      icon: Bot },
  { key: "kundli",   url: "/kundli",    icon: Star },
  { key: "sky",      url: "/sky",       icon: Globe },
  { key: "timeline", url: "/timeline",  icon: Clock },
  { key: "horoscope",url: "/horoscope", icon: Sun },
];

const exploreNavItems = [
  { key: "rashi",          url: "/rashi",          icon: Sparkles },
  { key: "nakshatra",      url: "/nakshatra",       icon: Compass },
  { key: "yogas",          url: "/yogas",           icon: Zap },
  { key: "remedies",       url: "/remedies",        icon: Gem },
  { key: "gemstones",      url: "/gemstones",       icon: Gem },
  { key: "compatibility",  url: "/compatibility",   icon: Heart },
  { key: "palmistry",      url: "/palmistry",       icon: Hand },
  { key: "gochar",         url: "/gochar",          icon: MoonIcon },
  { key: "rectification",  url: "/rectification",   icon: Clock },
  { key: "prashna",        url: "/prashna",         icon: HelpCircle },
  { key: "varshphal",      url: "/varshphal",       icon: Sun },
  { key: "kp",             url: "/kp",              icon: Star },
  { key: "dosha",          url: "/dosha",           icon: ShieldAlert },
  { key: "sade_sati",      url: "/sade-sati",       icon: Activity },
  { key: "today",          url: "/today",           icon: Calendar },
  { key: "panchang",       url: "/panchang",        icon: Eclipse },
  { key: "library",        url: "/library",         icon: Library },
  { key: "mantras",        url: "/mantras",         icon: Music },
  { key: "gotra",          url: "/gotra",           icon: TreePine },
  { key: "knowledge",      url: "/knowledge",       icon: Database },
  { key: "stories",        url: "/stories",         icon: BookOpen },
];

const PLAN_BADGE: Record<string, { label: string; className: string }> = {
  free:     { label: "Free",     className: "bg-muted/40 text-muted-foreground" },
  jyotishi: { label: "Jyotishi", className: "bg-amber-500/20 text-amber-400" },
  acharya:  { label: "Acharya",  className: "bg-purple-500/20 text-purple-400" },
};

function getInitials(name: string) {
  return name
    .split(" ")
    .map((w) => w[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

export function MobileDrawer() {
  const [open, setOpen]           = useState(false);
  const [profileOpen, setProfile] = useState(false);
  const { t } = useTranslation();
  const { logout } = useAuth();
  const navigate  = useNavigate();
  const plan  = useAuthStore((s) => s.plan);
  const name  = useAuthStore((s) => s.name) ?? "";
  const badge = PLAN_BADGE[plan] ?? PLAN_BADGE.free;

  const handleLogout = () => {
    logout();
    navigate("/");
    setOpen(false);
    setProfile(false);
  };

  const handleNav = (url?: string) => {
    setOpen(false);
    setProfile(false);
    if (url) navigate(url);
  };

  return (
    <>
      {/* ── Hamburger trigger ─────────────────────────────────────── */}
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
              onClick={() => { setOpen(false); setProfile(false); }}
            />

            {/* Drawer panel */}
            <motion.div
              key="drawer"
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{ type: "spring", damping: 28, stiffness: 300 }}
              className="md:hidden fixed top-0 left-0 bottom-0 z-[60] w-[280px] flex flex-col border-r border-border/40"
              style={{ background: "hsl(220 25% 8% / 0.98)" }}
            >
              {/* ── Top header ── */}
              <div className="flex items-center justify-between px-4 py-4 border-b border-border/20 shrink-0">
                <div className="flex items-center gap-2.5">
                  <Moon className="h-6 w-6 text-primary zodiac-glow" />
                  <span className="font-display text-base text-primary text-glow-gold">
                    {t("appTitle")}
                  </span>
                </div>
                <button
                  onClick={() => { setOpen(false); setProfile(false); }}
                  className="flex items-center justify-center w-8 h-8 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted/30 transition-colors"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {/* ── Scrollable nav ── */}
              <div className="flex-1 overflow-y-auto py-2 px-2">
                <NavSection label={t("nav_group.main")}    items={mainNavItems}    t={t} onNav={() => handleNav()} />
                <NavSection label={t("nav_group.explore")} items={exploreNavItems} t={t} onNav={() => handleNav()} />
              </div>

              {/* ── Fixed profile card at bottom (ChatGPT / Grok style) ── */}
              <div className="shrink-0 border-t border-border/20 px-3 py-2 relative">
                {/* Profile popup — opens upward */}
                <AnimatePresence>
                  {profileOpen && (
                    <motion.div
                      key="profile-popup"
                      initial={{ opacity: 0, y: 8, scale: 0.97 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: 8, scale: 0.97 }}
                      transition={{ duration: 0.15 }}
                      className="absolute bottom-full left-3 right-3 mb-2 rounded-2xl border border-border/30 overflow-hidden"
                      style={{ background: "hsl(220 20% 13%)" }}
                    >
                      {/* User info row */}
                      <div className="flex items-center gap-3 px-4 py-3 border-b border-border/20">
                        <div
                          className="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold text-primary shrink-0"
                          style={{ background: "hsl(38 80% 32% / 0.2)", border: "1px solid hsl(38 80% 32% / 0.4)" }}
                        >
                          {name ? getInitials(name) : <User className="h-4 w-4" />}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-foreground truncate">{name || "—"}</p>
                          <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${badge.className}`}>
                            {badge.label}
                          </span>
                        </div>
                      </div>

                      {/* Menu items */}
                      <div className="py-1.5">
                        <ProfileMenuItem
                          icon={User}
                          label={t("nav.profile")}
                          onClick={() => handleNav("/profile")}
                        />
                        <ProfileMenuItem
                          icon={CreditCard}
                          label={t("nav.subscription")}
                          onClick={() => handleNav("/subscription")}
                        />
                        <ProfileMenuItem
                          icon={Settings}
                          label={t("nav.profile")}
                          onClick={() => handleNav("/profile")}
                        />
                        {/* Language row */}
                        <div className="flex items-center gap-3 px-4 py-2.5">
                          <Globe className="h-4 w-4 text-muted-foreground shrink-0" />
                          <LanguageSwitcher variant="full" />
                        </div>
                        <div className="mx-3 my-1 border-t border-border/20" />
                        <ProfileMenuItem
                          icon={LogOut}
                          label={t("nav.logout")}
                          onClick={handleLogout}
                          danger
                        />
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>

                {/* Profile card button */}
                <button
                  onClick={() => setProfile((v) => !v)}
                  className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-muted/20 transition-colors"
                >
                  <div
                    className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold text-primary shrink-0"
                    style={{ background: "hsl(38 80% 32% / 0.2)", border: "1px solid hsl(38 80% 32% / 0.4)" }}
                  >
                    {name ? getInitials(name) : <User className="h-3.5 w-3.5" />}
                  </div>
                  <div className="flex-1 min-w-0 text-left">
                    <p className="text-sm font-medium text-foreground truncate">{name || "—"}</p>
                    <p className="text-xs text-muted-foreground">{badge.label}</p>
                  </div>
                  <ChevronUp
                    className={`h-4 w-4 text-muted-foreground transition-transform duration-200 ${profileOpen ? "rotate-180" : ""}`}
                  />
                </button>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </>
  );
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function ProfileMenuItem({
  icon: Icon, label, onClick, danger = false,
}: {
  icon: React.ElementType;
  label: string;
  onClick: () => void;
  danger?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      className={`w-full flex items-center gap-3 px-4 py-2.5 text-sm transition-colors hover:bg-muted/20 ${
        danger ? "text-destructive hover:text-destructive" : "text-muted-foreground hover:text-foreground"
      }`}
    >
      <Icon className="h-4 w-4 shrink-0" />
      <span>{label}</span>
    </button>
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
      <p className="text-[10px] text-muted-foreground/50 uppercase tracking-widest px-3 py-2">
        {label}
      </p>
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
