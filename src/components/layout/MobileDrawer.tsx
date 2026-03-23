import { useState } from "react";
import { createPortal } from "react-dom";
import {
  Menu, X, Moon,
  LayoutDashboard, Star, Globe, Clock, Sun, Bot,
  Sparkles, BookOpen, Moon as MoonIcon, Heart, Gem, Compass,
  Zap, Hand, Eclipse, Calendar, Library, Music, TreePine,
  Database, HelpCircle, ShieldAlert, Activity, ChevronUp,
} from "lucide-react";
import { NavLink as RouterNavLink } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuthStore } from "@/store/authStore";
import { motion, AnimatePresence } from "framer-motion";
import { Avatar, ProfilePopup } from "./ProfilePopup";

const mainNavItems = [
  { key: "dashboard", url: "/dashboard", icon: LayoutDashboard },
  { key: "chat",      url: "/chat",      icon: Bot },
  { key: "kundli",    url: "/kundli",    icon: Star },
  { key: "sky",       url: "/sky",       icon: Globe },
  { key: "timeline",  url: "/timeline",  icon: Clock },
  { key: "horoscope", url: "/horoscope", icon: Sun },
];

const exploreNavItems = [
  { key: "rashi",         url: "/rashi",         icon: Sparkles },
  { key: "nakshatra",     url: "/nakshatra",      icon: Compass },
  { key: "yogas",         url: "/yogas",          icon: Zap },
  { key: "remedies",      url: "/remedies",       icon: Gem },
  { key: "gemstones",     url: "/gemstones",      icon: Gem },
  { key: "compatibility", url: "/compatibility",  icon: Heart },
  { key: "palmistry",     url: "/palmistry",      icon: Hand },
  { key: "gochar",        url: "/gochar",         icon: MoonIcon },
  { key: "rectification", url: "/rectification",  icon: Clock },
  { key: "prashna",       url: "/prashna",        icon: HelpCircle },
  { key: "varshphal",     url: "/varshphal",      icon: Sun },
  { key: "kp",            url: "/kp",             icon: Star },
  { key: "dosha",         url: "/dosha",          icon: ShieldAlert },
  { key: "sade_sati",     url: "/sade-sati",      icon: Activity },
  { key: "today",         url: "/today",          icon: Calendar },
  { key: "panchang",      url: "/panchang",       icon: Eclipse },
  { key: "library",       url: "/library",        icon: Library },
  { key: "mantras",       url: "/mantras",        icon: Music },
  { key: "gotra",         url: "/gotra",          icon: TreePine },
  { key: "knowledge",     url: "/knowledge",      icon: Database },
  { key: "stories",       url: "/stories",        icon: BookOpen },
];

const PLAN_BADGE: Record<string, { label: string; cls: string }> = {
  free:     { label: "Free",     cls: "bg-muted/40 text-muted-foreground" },
  jyotishi: { label: "Jyotishi", cls: "bg-amber-500/20 text-amber-400" },
  acharya:  { label: "Acharya",  cls: "bg-purple-500/20 text-purple-400" },
};

export function MobileDrawer() {
  const [open, setOpen]   = useState(false);
  const [popup, setPopup] = useState(false);
  const { t }             = useTranslation();
  const plan  = useAuthStore((s) => s.plan);
  const name  = useAuthStore((s) => s.name) ?? "";
  const badge = PLAN_BADGE[plan] ?? PLAN_BADGE.free;

  const close = () => { setOpen(false); setPopup(false); };

  return (
    <>
      {/* Hamburger button */}
      <button
        onClick={() => setOpen(true)}
        className="md:hidden flex items-center justify-center w-9 h-9 rounded-lg
                   text-muted-foreground hover:text-primary hover:bg-muted/30 transition-colors"
        aria-label="Open menu"
      >
        <Menu className="h-5 w-5" />
      </button>

      {createPortal(
        <AnimatePresence>
          {open && (
            <>
              {/* Backdrop */}
              <motion.div
                key="bd"
                initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                transition={{ duration: 0.18 }}
                className="fixed inset-0 bg-black/55 backdrop-blur-sm"
                style={{ zIndex: 200 }}
                onClick={close}
              />

              {/* Drawer panel */}
              <motion.aside
                key="drawer"
                initial={{ x: "-100%" }} animate={{ x: 0 }} exit={{ x: "-100%" }}
                transition={{ type: "spring", damping: 30, stiffness: 320 }}
                className="fixed inset-y-0 left-0 w-[285px] flex flex-col border-r border-border/40 h-full"
                style={{ zIndex: 201, background: "hsl(var(--sidebar-background))" }}
              >
                {/* Logo row */}
                <div className="flex items-center justify-between px-4 h-14 shrink-0 border-b border-border/30">
                  <div className="flex items-center gap-2.5">
                    <Moon className="h-6 w-6 text-primary zodiac-glow" />
                    <span className="font-display text-[15px] text-primary text-glow-gold tracking-wide">
                      {t("appTitle")}
                    </span>
                  </div>
                  <button
                    onClick={close}
                    className="w-8 h-8 flex items-center justify-center rounded-lg
                               text-muted-foreground hover:text-foreground hover:bg-muted/60 transition-colors"
                  >
                    <X className="h-[18px] w-[18px]" />
                  </button>
                </div>

                {/* Scrollable nav */}
                <div className="flex-1 min-h-0 overflow-y-auto overscroll-contain px-2 py-3">
                  <Section label={t("nav_group.main")}    items={mainNavItems}    t={t} onNav={close} />
                  <Section label={t("nav_group.explore")} items={exploreNavItems} t={t} onNav={close} />
                </div>

                {/* Fixed profile card at bottom */}
                <div className="shrink-0 border-t border-border/30 px-2 py-2 relative">

                  {/* Profile popup (opens upward) */}
                  <AnimatePresence>
                    {popup && (
                      <ProfilePopup
                        className="absolute bottom-full left-0 right-0 mb-1"
                        style={{ zIndex: 202 }}
                        onClose={() => setPopup(false)}
                      />
                    )}
                  </AnimatePresence>

                  {/* Profile card button */}
                  <button
                    onClick={() => setPopup((v) => !v)}
                    className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-muted/60 transition-colors"
                  >
                    <Avatar name={name} size={32} />
                    <div className="flex-1 min-w-0 text-left">
                      <p className="text-[13px] font-medium text-foreground truncate leading-tight">{name || "—"}</p>
                      <p className="text-[11px] text-muted-foreground">{badge.label}</p>
                    </div>
                    <ChevronUp
                      className={`h-4 w-4 text-muted-foreground/60 transition-transform duration-200 ${popup ? "" : "rotate-180"}`}
                    />
                  </button>
                </div>
              </motion.aside>
            </>
          )}
        </AnimatePresence>,
        document.body
      )}
    </>
  );
}

function Section({
  label, items, t, onNav,
}: {
  label: string;
  items: { key: string; url: string; icon: React.ElementType }[];
  t: (k: string) => string;
  onNav: () => void;
}) {
  return (
    <div className="mb-2">
      <p className="text-[10px] font-semibold text-muted-foreground/40 uppercase tracking-[0.12em] px-3 py-1.5">
        {label}
      </p>
      {items.map((item) => (
        <RouterNavLink
          key={item.url}
          to={item.url}
          onClick={onNav}
          className={({ isActive }) =>
            `flex items-center gap-3 px-3 py-[9px] rounded-xl text-[13.5px] transition-colors ${
              isActive
                ? "bg-primary/10 text-primary font-medium"
                : "text-muted-foreground hover:text-foreground hover:bg-muted/60"
            }`
          }
        >
          <item.icon className="h-[17px] w-[17px] shrink-0" />
          <span>{t(`nav.${item.key}`)}</span>
        </RouterNavLink>
      ))}
    </div>
  );
}
