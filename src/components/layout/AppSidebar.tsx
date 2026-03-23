import { useState } from "react";
import { useTranslation } from 'react-i18next';
import {
  LayoutDashboard, Star, Globe, Clock, Sparkles, BookOpen, Moon, Heart,
  Gem, Sun, Compass, Zap, Hand, Eclipse, Calendar, Bot, Library,
  Music, TreePine, HelpCircle, ShieldAlert, Activity, ChevronUp,
} from "lucide-react";
import { NavLink } from "@/components/NavLink";
import {
  Sidebar, SidebarContent, SidebarGroup, SidebarGroupContent,
  SidebarGroupLabel, SidebarMenu, SidebarMenuButton, SidebarMenuItem, useSidebar,
} from "@/components/ui/sidebar";
import { useAuthStore } from "@/store/authStore";
import { AnimatePresence } from "framer-motion";
import { Avatar, ProfilePopup } from "./ProfilePopup";


const mainNavItems = [
  { key: "dashboard", url: "/dashboard", icon: LayoutDashboard },
  { key: "chat",      url: "/chat",      icon: Bot },
  { key: "kundli",    url: "/kundli",    icon: Star },
  { key: "sky",       url: "/sky",       icon: Globe },
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
  { key: "gochar",        url: "/gochar",         icon: Moon },
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
  { key: "stories",       url: "/stories",        icon: BookOpen },
];

const PLAN_BADGE: Record<string, { label: string; cls: string }> = {
  free:     { label: "Free",     cls: "bg-muted/40 text-muted-foreground" },
  jyotishi: { label: "Jyotishi", cls: "bg-amber-500/20 text-amber-400" },
  acharya:  { label: "Acharya",  cls: "bg-purple-500/20 text-purple-400" },
};

export function AppSidebar() {
  const { t } = useTranslation();
  const { state } = useSidebar();
  const collapsed = state === "collapsed";
  const plan  = useAuthStore((s) => s.plan);
  const name  = useAuthStore((s) => s.name) ?? "";
  const badge = PLAN_BADGE[plan] ?? PLAN_BADGE.free;
  const [popup, setPopup] = useState(false);

  const mainNav    = mainNavItems.map(i => ({ ...i, title: t(`nav.${i.key}`) }));
  const exploreNav = exploreNavItems.map(i => ({ ...i, title: t(`nav.${i.key}`) }));

  return (
    <Sidebar collapsible="icon" className="border-r border-border/50">
      <SidebarContent className="star-field flex flex-col h-full">

        {/* Logo */}
        <div className="flex items-center gap-3 px-4 py-6 shrink-0">
          <Moon className="h-8 w-8 text-primary zodiac-glow" />
          {!collapsed && (
            <span className="font-display text-lg text-primary text-glow-gold">{t('appTitle')}</span>
          )}
        </div>

        {/* Scrollable nav */}
        <div className="flex-1 overflow-y-auto overflow-x-hidden">
          <NavGroup label={t('nav_group.main')}    items={mainNav}    collapsed={collapsed} />
          <NavGroup label={t('nav_group.explore')} items={exploreNav} collapsed={collapsed} />
        </div>

        {/* ── Fixed profile card at bottom ── */}
        <div className="shrink-0 border-t border-border/30 px-2 py-2 relative">

          {/* Click-outside */}
          {popup && (
            <div className="fixed inset-0 z-40" onClick={() => setPopup(false)} />
          )}

          {/* Popup */}
          <AnimatePresence>
            {popup && (
              <ProfilePopup
                className="absolute bottom-full left-1 right-1 mb-1 z-50"
                onClose={() => setPopup(false)}
              />
            )}
          </AnimatePresence>

          {/* Profile card button */}
          {collapsed ? (
            <button
              onClick={() => setPopup((v) => !v)}
              className="w-full flex items-center justify-center py-2 rounded-xl hover:bg-muted/60 transition-colors"
            >
              <Avatar name={name} size={30} />
            </button>
          ) : (
            <button
              onClick={() => setPopup((v) => !v)}
              className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-muted/60 transition-colors"
            >
              <Avatar name={name} size={30} />
              <div className="flex-1 min-w-0 text-left">
                <p className="text-[13px] font-medium text-foreground truncate leading-tight">{name || "—"}</p>
                <p className="text-[11px] text-muted-foreground">{badge.label}</p>
              </div>
              <ChevronUp
                className={`h-4 w-4 text-muted-foreground/60 transition-transform duration-200 ${popup ? "" : "rotate-180"}`}
              />
            </button>
          )}
        </div>
      </SidebarContent>
    </Sidebar>
  );
}

function NavGroup({
  label, items, collapsed,
}: {
  label: string;
  items: { title: string; url: string; icon: React.ElementType }[];
  collapsed: boolean;
}) {
  return (
    <SidebarGroup>
      <SidebarGroupLabel className="text-muted-foreground/60 text-xs uppercase tracking-widest">
        {label}
      </SidebarGroupLabel>
      <SidebarGroupContent>
        <SidebarMenu>
          {items.map((item) => (
            <SidebarMenuItem key={item.url}>
              <SidebarMenuButton asChild>
                <NavLink
                  to={item.url}
                  end={item.url === "/"}
                  className="transition-all duration-200 hover:bg-muted/50"
                  activeClassName="bg-muted text-primary font-medium glow-border"
                >
                  <item.icon className="mr-2 h-4 w-4" />
                  {!collapsed && <span>{item.title}</span>}
                </NavLink>
              </SidebarMenuButton>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
}
