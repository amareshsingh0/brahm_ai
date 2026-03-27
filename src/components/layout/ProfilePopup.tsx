/**
 * ProfilePopup — shared between AppSidebar (desktop) and MobileDrawer (mobile)
 *
 * Features:
 *  - User info row (avatar + name + plan)
 *  - Profile / Subscription → navigate
 *  - Settings → inline settings panel (ChatGPT style)
 *  - Help → hover shows sub-menu (Report Issue, FAQ, Changelog, Community)
 *  - Language switcher
 *  - Logout
 */

import { useState } from "react";
import { createPortal } from "react-dom";
import {
  User, CreditCard, Settings, Globe, LogOut, HelpCircle,
  ChevronRight, ExternalLink, MessageSquare, FileText,
  RefreshCw, Users, X, Bell, Lock, Palette,
  Calendar, Clock, MapPin, Edit2, Phone, Shield, Sparkles, Crown, Archive,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { useAuthStore } from "@/store/authStore";
import { useKundliStore } from "@/store/kundliStore";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { motion, AnimatePresence } from "framer-motion";

// ── Plan badge ────────────────────────────────────────────────────────────────
const PLAN_BADGE: Record<string, { label: string; cls: string }> = {
  free:     { label: "Free",     cls: "bg-muted/40 text-muted-foreground" },
  standard: { label: "Standard", cls: "bg-blue-500/20 text-blue-400" },
  premium:  { label: "Premium",  cls: "bg-amber-500/20 text-amber-400" },
};

function initials(name: string) {
  return name.split(" ").map((w) => w[0]).join("").slice(0, 2).toUpperCase();
}

export function Avatar({ name, size }: { name: string; size: number }) {
  return (
    <div
      className="rounded-full flex items-center justify-center text-primary font-bold shrink-0"
      style={{
        width: size, height: size, fontSize: size * 0.38,
        background: "hsl(38 80% 32% / 0.18)",
        border: "1px solid hsl(38 80% 32% / 0.35)",
      }}
    >
      {name ? initials(name) : <User style={{ width: size * 0.45, height: size * 0.45 }} />}
    </div>
  );
}

// ── Plan style ────────────────────────────────────────────────────────────────
const PLAN_STYLE: Record<string, { icon: React.ElementType; color: string; bg: string; border: string }> = {
  free:     { icon: Shield,   color: "text-muted-foreground", bg: "bg-muted/20",      border: "border-border/30" },
  standard: { icon: Sparkles, color: "text-blue-400",         bg: "bg-blue-500/10",   border: "border-blue-500/30" },
  premium:  { icon: Crown,    color: "text-amber-400",        bg: "bg-amber-500/10",  border: "border-amber-500/30" },
};

// ── Reusable full modal wrapper ────────────────────────────────────────────────
function FullModal({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return createPortal(
    <AnimatePresence>
      <motion.div
        key="modal-backdrop"
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        transition={{ duration: 0.18 }}
        className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4"
        style={{ zIndex: 500 }}
        onClick={onClose}
      >
        <motion.div
          key="modal-content"
          initial={{ opacity: 0, scale: 0.96, y: 8 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 8 }}
          transition={{ duration: 0.18 }}
          className="w-full max-w-[560px] rounded-2xl overflow-hidden shadow-2xl flex flex-col"
          style={{
            background: "hsl(var(--card))",
            border: "1px solid hsl(var(--border))",
            maxHeight: "min(640px, 90vh)",
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center justify-between px-6 py-4 border-b border-border/40 shrink-0">
            <span className="text-[16px] font-semibold text-foreground">{title}</span>
            <button
              onClick={onClose}
              className="w-8 h-8 flex items-center justify-center rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted/60 transition-colors"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
          <div className="flex-1 overflow-y-auto">
            {children}
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>,
    document.body
  );
}

// ── Profile modal ──────────────────────────────────────────────────────────────
function ProfileModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const birthDetails = useKundliStore((s) => s.birthDetails);
  const name  = useAuthStore((s) => s.name);
  const phone = useAuthStore((s) => s.phone);
  const plan  = useAuthStore((s) => s.plan);
  const planStyle = PLAN_STYLE[plan] ?? PLAN_STYLE.free;
  const PlanIcon  = planStyle.icon;

  return (
    <FullModal title={t("profile.title")} onClose={onClose}>
      <div className="p-6 space-y-5">
        {/* Account */}
        <div className="space-y-2">
          <p className="text-[11px] uppercase tracking-wider text-muted-foreground">{t("profile.account")}</p>
          <div className="rounded-xl border border-border/30 divide-y divide-border/20 overflow-hidden">
            {name  && <MRow icon={<User  className="h-4 w-4"/>} label={t("profile.name")}   value={name}/>}
            {phone && <MRow icon={<Phone className="h-4 w-4"/>} label={t("profile.mobile")} value={phone}/>}
          </div>
        </div>

        {/* Plan */}
        <div className="space-y-2">
          <p className="text-[11px] uppercase tracking-wider text-muted-foreground">{t("common.current_plan")}</p>
          <div className={`rounded-xl p-4 flex items-center gap-3 border ${planStyle.border} ${planStyle.bg}`}>
            <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${planStyle.bg} border ${planStyle.border}`}>
              <PlanIcon className={`h-5 w-5 ${planStyle.color}`} />
            </div>
            <p className={`font-display text-lg ${planStyle.color}`}>{plan.charAt(0).toUpperCase() + plan.slice(1)}</p>
          </div>
        </div>

        {/* Birth details */}
        {birthDetails ? (
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <p className="text-[11px] uppercase tracking-wider text-muted-foreground">{t("profile.birth_details")}</p>
              <button
                onClick={() => { onClose(); navigate("/onboarding"); }}
                className="text-xs text-muted-foreground hover:text-primary flex items-center gap-1 transition-colors"
              >
                <Edit2 className="h-3 w-3" /> {t("common.edit")}
              </button>
            </div>
            <div className="rounded-xl border border-border/30 divide-y divide-border/20 overflow-hidden">
              <MRow icon={<User     className="h-4 w-4"/>} label={t("profile.name")}        value={birthDetails.name}/>
              <MRow icon={<Calendar className="h-4 w-4"/>} label={t("profile.dob")}         value={birthDetails.dateOfBirth}/>
              <MRow icon={<Clock    className="h-4 w-4"/>} label={t("profile.tob")}         value={birthDetails.timeOfBirth}/>
              <MRow icon={<MapPin   className="h-4 w-4"/>} label={t("profile.birth_place")} value={birthDetails.birthPlace}/>
            </div>
          </div>
        ) : (
          <div className="text-center py-6 space-y-2">
            <p className="text-3xl">🌟</p>
            <p className="text-sm font-display text-foreground">{t("profile.no_birth")}</p>
            <p className="text-xs text-muted-foreground">{t("profile.no_birth_desc")}</p>
            <button
              onClick={() => { onClose(); navigate("/onboarding"); }}
              className="mt-2 text-sm text-primary hover:underline"
            >
              {t("common.generate_kundli")}
            </button>
          </div>
        )}
      </div>
    </FullModal>
  );
}

function MRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center gap-3 px-4 py-3 bg-muted/10">
      <span className="text-primary">{icon}</span>
      <div>
        <p className="text-[11px] text-muted-foreground">{label}</p>
        <p className="text-[13px] text-foreground">{value}</p>
      </div>
    </div>
  );
}

// ── Subscription modal ─────────────────────────────────────────────────────────
function SubscriptionModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation();
  const plan  = useAuthStore((s) => s.plan);

  const PLANS = [
    { id: "free",     name: "Free",     price: "₹0",    period: "/forever",  features: ["5 AI queries/day", "Basic Kundli", "Panchang"] },
    { id: "standard", name: "Standard", price: "₹199",  period: "/month",    features: ["Unlimited AI Chat", "Full Kundali + 7 Tabs", "Gochar Transits", "Compatibility", "Muhurta"] },
    { id: "premium",  name: "Premium",  price: "₹399",  period: "/month",    features: ["Everything in Standard", "Gemstone Guide", "Dosha & Sade Sati", "Varshphal", "KP System", "Scripture Library"] },
  ];

  return (
    <FullModal title={t("nav.subscription")} onClose={onClose}>
      <div className="p-6 space-y-4">
        {PLANS.map((p) => {
          const isCurrent = plan === p.id;
          const planStyle = PLAN_STYLE[p.id] ?? PLAN_STYLE.free;
          const PlanIcon  = planStyle.icon;
          return (
            <div
              key={p.id}
              className={`rounded-xl p-4 border transition-colors ${
                isCurrent
                  ? `${planStyle.border} ${planStyle.bg}`
                  : "border-border/30 hover:border-border/60"
              }`}
            >
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2.5">
                  <PlanIcon className={`h-5 w-5 ${planStyle.color}`} />
                  <span className={`font-display text-[15px] ${planStyle.color}`}>{p.name}</span>
                  {isCurrent && <span className="text-[10px] bg-primary/10 text-primary px-2 py-0.5 rounded-full font-medium">Current</span>}
                </div>
                <div className="text-right">
                  <span className="text-[16px] font-bold text-foreground">{p.price}</span>
                  <span className="text-[11px] text-muted-foreground">{p.period}</span>
                </div>
              </div>
              <ul className="space-y-1">
                {p.features.map((f) => (
                  <li key={f} className="text-[12px] text-muted-foreground flex items-center gap-2">
                    <span className="w-1 h-1 rounded-full bg-primary/60 shrink-0" />
                    {f}
                  </li>
                ))}
              </ul>
              {!isCurrent && (
                <button className="mt-3 w-full py-2 rounded-lg text-[13px] font-medium bg-primary/10 text-primary hover:bg-primary/20 transition-colors">
                  {t("common.view_plans")}
                </button>
              )}
            </div>
          );
        })}
      </div>
    </FullModal>
  );
}

// ── Settings modal (full Grok/ChatGPT style) ──────────────────────────────────
const SETTINGS_TABS = [
  { id: "general",       label: "General",       icon: Settings },
  { id: "notifications", label: "Notifications", icon: Bell },
  { id: "appearance",    label: "Appearance",    icon: Palette },
  { id: "security",      label: "Security",      icon: Lock },
];

function SettingsModal({ onClose }: { onClose: () => void }) {
  const [tab, setTab] = useState("general");

  return (
    <FullModal title="Settings" onClose={onClose}>
      <div className="flex overflow-hidden" style={{ minHeight: "360px" }}>
        {/* Left sidebar tabs */}
        <div className="w-[180px] shrink-0 border-r border-border/40 py-3 px-2">
          {SETTINGS_TABS.map((s) => (
            <button
              key={s.id}
              onClick={() => setTab(s.id)}
              className={`w-full flex items-center gap-3 px-3 py-2.5 text-[13.5px] transition-colors rounded-xl mb-0.5 ${
                tab === s.id
                  ? "bg-primary/10 text-primary font-medium"
                  : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
              }`}
            >
              <s.icon className="h-4 w-4 shrink-0" />
              {s.label}
            </button>
          ))}
        </div>

        {/* Right content */}
        <div className="flex-1 p-6 overflow-y-auto text-[13.5px]">
          {tab === "general" && (
            <div className="space-y-5">
              <SettingRow label="Language" description="App display language">
                <LanguageSwitcher variant="full" />
              </SettingRow>
              <div className="border-t border-border/30" />
              <SettingRow label="Plan" description="Your current subscription">
                <span className={`text-xs px-2 py-1 rounded-full font-medium ${PLAN_BADGE[useAuthStore.getState().plan]?.cls ?? PLAN_BADGE.free.cls}`}>
                  {PLAN_BADGE[useAuthStore.getState().plan]?.label ?? "Free"}
                </span>
              </SettingRow>
            </div>
          )}
          {tab === "notifications" && (
            <div className="space-y-3">
              <p className="text-foreground font-medium mb-3">Notifications</p>
              <p className="text-muted-foreground text-[13px]">Notification settings coming soon.</p>
            </div>
          )}
          {tab === "appearance" && (
            <div className="space-y-3">
              <p className="text-foreground font-medium mb-3">Appearance</p>
              <p className="text-muted-foreground text-[13px]">Theme customization coming soon.</p>
            </div>
          )}
          {tab === "security" && (
            <div className="space-y-3">
              <p className="text-foreground font-medium mb-3">Security</p>
              <p className="text-muted-foreground text-[13px]">Security settings coming soon.</p>
            </div>
          )}
        </div>
      </div>
    </FullModal>
  );
}

function SettingRow({ label, description, children }: { label: string; description?: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div>
        <p className="text-foreground/90 font-medium">{label}</p>
        {description && <p className="text-muted-foreground text-[12px] mt-0.5">{description}</p>}
      </div>
      {children}
    </div>
  );
}

// ── Help submenu items ────────────────────────────────────────────────────────
const HELP_ITEMS = [
  { label: "Report Issue",  icon: MessageSquare, href: "mailto:support@bimoraai.com" },
  { label: "FAQ",           icon: FileText,      href: "#" },
  { label: "Changelog",     icon: RefreshCw,     href: "#" },
  { label: "Community",     icon: Users,         href: "#" },
];

// ── Main popup ────────────────────────────────────────────────────────────────
interface ProfilePopupProps {
  /** position above the trigger */
  className?: string;
  style?: React.CSSProperties;
  onClose: () => void;
}

export function ProfilePopup({ className = "", style, onClose }: ProfilePopupProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const plan  = useAuthStore((s) => s.plan);
  const name  = useAuthStore((s) => s.name) ?? "";
  const badge = PLAN_BADGE[plan] ?? PLAN_BADGE.free;

  const [settingsOpen,     setSettingsOpen]     = useState(false);
  const [profileOpen,      setProfileOpen]      = useState(false);
  const [subscriptionOpen, setSubscriptionOpen] = useState(false);
  const [helpOpen,         setHelpOpen]         = useState(false);

  return (
    <>
      {settingsOpen     && <SettingsModal     onClose={() => setSettingsOpen(false)} />}
      {profileOpen      && <ProfileModal      onClose={() => setProfileOpen(false)} />}
      {subscriptionOpen && <SubscriptionModal onClose={() => setSubscriptionOpen(false)} />}

      <motion.div
        key="profile-popup"
        initial={{ opacity: 0, y: 6, scale: 0.97 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 6, scale: 0.97 }}
        transition={{ duration: 0.14 }}
        className={`rounded-2xl overflow-hidden shadow-2xl ${className}`}
        style={{
          background: "hsl(var(--card))",
          border: "1px solid hsl(var(--border))",
          boxShadow: "0 8px 32px hsl(222 47% 11% / 0.13)",
          ...style,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <motion.div
          initial={{ opacity: 0 }} animate={{ opacity: 1 }}
          transition={{ duration: 0.1 }}>

          {/* User info */}
          <div className="flex items-center gap-3 px-4 py-3 border-b border-border/40">
            <Avatar name={name} size={36} />
            <div className="min-w-0">
              <p className="text-[13px] font-semibold text-foreground truncate leading-tight">{name || "—"}</p>
              <span className={`text-[11px] px-1.5 py-0.5 rounded-full font-medium ${badge.cls}`}>
                {badge.label}
              </span>
            </div>
          </div>

          {/* Menu items */}
          <div className="py-1">
            <MenuItem icon={User}       label={t("nav.profile")}      onClick={() => setProfileOpen(true)} showArrow />
            <MenuItem icon={CreditCard} label={t("nav.subscription")} onClick={() => setSubscriptionOpen(true)} showArrow />
            <MenuItem icon={Archive}    label="Archived Chats"        onClick={() => { onClose(); navigate("/chat-history?archived=1"); }} showArrow />
            <MenuItem icon={Settings}   label="Settings"              onClick={() => setSettingsOpen(true)} showArrow />

            {/* Help with hover submenu */}
            <div
              className="relative"
              onMouseEnter={() => setHelpOpen(true)}
              onMouseLeave={() => setHelpOpen(false)}
            >
              <MenuItem icon={HelpCircle} label="Help" onClick={() => setHelpOpen((v) => !v)} showArrow />
              <AnimatePresence>
                {helpOpen && (
                  <motion.div
                    initial={{ opacity: 0, x: -4 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -4 }}
                    transition={{ duration: 0.12 }}
                    className="absolute left-full top-0 ml-1 w-44 rounded-xl overflow-hidden shadow-xl"
                    style={{
                      background: "hsl(var(--card))",
                      border: "1px solid hsl(var(--border))",
                      boxShadow: "0 6px 24px hsl(222 47% 11% / 0.12)",
                    }}
                  >
                    {HELP_ITEMS.map((item) => (
                      <a
                        key={item.label}
                        href={item.href}
                        target={item.href.startsWith("http") ? "_blank" : undefined}
                        rel="noreferrer"
                        className="flex items-center gap-3 px-4 py-2.5 text-[13px] text-muted-foreground hover:text-foreground hover:bg-muted/60 transition-colors"
                        onClick={onClose}
                      >
                        <item.icon className="h-[16px] w-[16px] shrink-0" />
                        <span>{item.label}</span>
                        {item.href.startsWith("http") && (
                          <ExternalLink className="h-3 w-3 ml-auto opacity-40" />
                        )}
                      </a>
                    ))}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* Language */}
            <div className="px-4 py-2">
              <LanguageSwitcher variant="full" />
            </div>

            <div className="mx-3 my-1 border-t border-border/40" />
            <MenuItem
              icon={LogOut}
              label={t("nav.logout")}
              onClick={() => { logout(); navigate("/"); onClose(); }}
              danger
            />
          </div>
        </motion.div>
      </motion.div>
    </>
  );
}

function MenuItem({
  icon: Icon, label, onClick, showArrow = false, danger = false,
}: {
  icon: React.ElementType; label: string; onClick: () => void; showArrow?: boolean; danger?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      className={`w-full flex items-center gap-3 px-4 py-2.5 text-[13px] transition-colors hover:bg-muted/60
                  ${danger ? "text-destructive" : "text-muted-foreground hover:text-foreground"}`}
    >
      <Icon className="h-[17px] w-[17px] shrink-0" />
      <span className="flex-1 text-left">{label}</span>
      {showArrow && <ChevronRight className="h-3.5 w-3.5 opacity-40" />}
    </button>
  );
}
