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

const mainNav = [
  { title: "Dashboard", url: "/dashboard", icon: LayoutDashboard },
  { title: "Brahm AI Chat", url: "/chat", icon: Bot },
  { title: "My Kundli", url: "/kundli", icon: Star },
  { title: "Live Sky", url: "/sky", icon: Globe },
  { title: "Dasha Timeline", url: "/timeline", icon: Clock },
  { title: "Daily Horoscope", url: "/horoscope", icon: Sun },
];

const exploreNav = [
  { title: "Rashi Explorer", url: "/rashi", icon: Sparkles },
  { title: "Nakshatra Explorer", url: "/nakshatra", icon: Compass },
  { title: "Yogas", url: "/yogas", icon: Zap },
  { title: "Remedies", url: "/remedies", icon: Gem },
  { title: "Compatibility", url: "/compatibility", icon: Heart },
  { title: "Palmistry", url: "/palmistry", icon: Hand },
  { title: "Today", url: "/today", icon: Calendar },
  { title: "Panchang", url: "/panchang", icon: Eclipse },
  { title: "Vedic Library", url: "/library", icon: Library },
  { title: "Mantra Dictionary", url: "/mantras", icon: Music },
  { title: "Gotra Finder", url: "/gotra", icon: TreePine },
  { title: "Knowledge Base", url: "/knowledge", icon: Database },
  { title: "Stories", url: "/stories", icon: BookOpen },
];

const accountNav = [
  { title: "Profile", url: "/profile", icon: User },
  { title: "Subscription", url: "/subscription", icon: CreditCard },
];

const PLAN_BADGE: Record<string, { label: string; className: string }> = {
  free:     { label: "Free",      className: "bg-muted/40 text-muted-foreground" },
  jyotishi: { label: "Jyotishi",  className: "bg-amber-500/20 text-amber-400" },
  acharya:  { label: "Acharya",   className: "bg-purple-500/20 text-purple-400" },
};

export function AppSidebar() {
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

  return (
    <Sidebar collapsible="icon" className="border-r border-border/50">
      <SidebarContent className="star-field">
        {/* Logo */}
        <div className="flex items-center gap-3 px-4 py-6">
          <Moon className="h-8 w-8 text-primary zodiac-glow" />
          {!collapsed && (
            <span className="font-display text-lg text-primary text-glow-gold">Brahm AI</span>
          )}
        </div>

        {/* User + plan badge */}
        {!collapsed && name && (
          <div className="mx-4 mb-2 px-3 py-2 rounded-lg bg-muted/10 border border-border/20 flex items-center justify-between">
            <p className="text-xs text-foreground truncate max-w-[110px]">{name}</p>
            <span className={`text-[9px] px-1.5 py-0.5 rounded-full font-medium ${badge.className}`}>
              {badge.label}
            </span>
          </div>
        )}

        <NavGroup label="Main" items={mainNav} collapsed={collapsed} />
        <NavGroup label="Explore" items={exploreNav} collapsed={collapsed} />
        <NavGroup label="Account" items={accountNav} collapsed={collapsed} />

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
                    {!collapsed && <span>Logout</span>}
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

function NavGroup({ label, items, collapsed }: { label: string; items: typeof mainNav; collapsed: boolean }) {
  return (
    <SidebarGroup>
      <SidebarGroupLabel className="text-muted-foreground/60 text-xs uppercase tracking-widest">
        {label}
      </SidebarGroupLabel>
      <SidebarGroupContent>
        <SidebarMenu>
          {items.map((item) => (
            <SidebarMenuItem key={item.title}>
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
