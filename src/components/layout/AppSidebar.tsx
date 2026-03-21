import { useTranslation } from 'react-i18next';
import { LayoutDashboard, Star, Globe, Clock, Sparkles, BookOpen, Moon, Heart, Gem, Sun, Compass, User, Zap, Hand, Eclipse, Calendar, Bot, Library, Music, TreePine, Database, LogOut, CreditCard } from "lucide-react";
import { NavLink } from "@/components/NavLink";
import { useLocation, useNavigate } from "react-router-dom";
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";
import { useAuth } from "@/hooks/useAuth";
import { useAuthStore } from "@/store/authStore";

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
  { key: "compatibility", url: "/compatibility", icon: Heart },
  { key: "palmistry", url: "/palmistry", icon: Hand },
  { key: "gochar", url: "/gochar", icon: Moon },
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

export function AppSidebar() {
  const { t } = useTranslation();
  const { state } = useSidebar();
  const collapsed = state === "collapsed";
  const { logout } = useAuth();
  const navigate = useNavigate();
  const plan = useAuthStore((s) => s.plan);
  const name = useAuthStore((s) => s.name);
  const badge = PLAN_BADGE[plan] ?? PLAN_BADGE.free;

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  const mainNav = mainNavItems.map(i => ({ ...i, title: t(`nav.${i.key}`) }));
  const exploreNav = exploreNavItems.map(i => ({ ...i, title: t(`nav.${i.key}`) }));
  const accountNav = accountNavItems.map(i => ({ ...i, title: t(`nav.${i.key}`) }));

  return (
    <Sidebar collapsible="icon" className="border-r border-border/50">
      <SidebarContent className="star-field">
        {/* Logo */}
        <div className="flex items-center gap-3 px-4 py-6">
          <Moon className="h-8 w-8 text-primary zodiac-glow" />
          {!collapsed && (
            <span className="font-display text-lg text-primary text-glow-gold">{t('appTitle')}</span>
          )}
        </div>

        {/* User + plan badge */}
        {!collapsed && name && (
          <div className="mx-4 mb-2 px-3 py-2 rounded-lg bg-muted/10 border border-border/20 flex items-center justify-between">
            <p className="text-xs text-foreground truncate max-w-[110px]">{name}</p>
            <span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${badge.className}`}>
              {badge.label}
            </span>
          </div>
        )}

        <NavGroup label={t('nav_group.main')} items={mainNav} collapsed={collapsed} />
        <NavGroup label={t('nav_group.explore')} items={exploreNav} collapsed={collapsed} />
        <NavGroup label={t('nav_group.account')} items={accountNav} collapsed={collapsed} />

        {/* Logout */}
        <SidebarGroup className="mt-auto pb-4">
          <SidebarGroupContent>
            <SidebarMenu>
              <SidebarMenuItem>
                <SidebarMenuButton asChild>
                  <button
                    onClick={handleLogout}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm text-muted-foreground hover:text-destructive transition-colors rounded-md"
                  >
                    <LogOut className="h-4 w-4" />
                    {!collapsed && <span>{t('nav.logout')}</span>}
                  </button>
                </SidebarMenuButton>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  );
}

function NavGroup({ label, items, collapsed }: { label: string; items: { title: string; url: string; icon: React.ElementType }[]; collapsed: boolean }) {
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

