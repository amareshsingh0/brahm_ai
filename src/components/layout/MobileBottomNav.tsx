import { NavLink as RouterNavLink, useLocation } from "react-router-dom";
import { LayoutDashboard, Star, Bot, Clock, User } from "lucide-react";
import { useTranslation } from "react-i18next";

const mobileNavItems = [
  { id: "dashboard", url: "/dashboard", icon: LayoutDashboard },
  { id: "chat", url: "/chat", icon: Bot },
  { id: "kundli", url: "/kundli", icon: Star },
  { id: "timeline", url: "/timeline", icon: Clock },
  { id: "profile", url: "/profile", icon: User },
];

export function MobileBottomNav() {
  const location = useLocation();
  const { t } = useTranslation();

  return (
    <nav
      className="md:hidden fixed bottom-0 left-0 right-0 z-50 glass border-t border-border/30"
      role="navigation"
      aria-label="Mobile navigation"
    >
      <div className="flex items-center justify-around h-14">
        {mobileNavItems.map((item) => {
          const isActive = item.url === "/dashboard"
            ? location.pathname === "/dashboard"
            : location.pathname.startsWith(item.url);
          return (
            <RouterNavLink
              key={item.url}
              to={item.url}
              className={`flex flex-col items-center gap-0.5 px-3 py-1.5 text-xs transition-colors ${
                isActive ? "text-primary" : "text-muted-foreground"
              }`}
              aria-label={t(`nav.${item.id}`)}
            >
              <item.icon className={`h-5 w-5 ${isActive ? "zodiac-glow" : ""}`} />
              <span>{t(`nav.${item.id}`)}</span>
            </RouterNavLink>
          );
        })}
      </div>
    </nav>
  );
}
