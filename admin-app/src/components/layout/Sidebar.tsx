import { NavLink } from "react-router-dom";
import {
  LayoutDashboard, Users, CreditCard, MessageSquare,
  ClipboardList, UserX, ChevronLeft, ChevronRight, Activity, BadgeCheck,
} from "lucide-react";
import { cn } from "@/lib/utils";

const NAV = [
  { to: "/dashboard",        label: "Dashboard",         Icon: LayoutDashboard },
  { to: "/users",            label: "Users",             Icon: Users           },
  { to: "/payments",         label: "Payments",          Icon: CreditCard      },
  { to: "/subscriptions",    label: "Subscriptions",     Icon: BadgeCheck      },
  { to: "/chats",            label: "Chat Monitor",      Icon: MessageSquare   },
  { to: "/api-monitor",      label: "API Monitor",       Icon: Activity        },
  { to: "/logs",             label: "Admin Log",         Icon: ClipboardList   },
  { to: "/deleted-accounts", label: "Deleted Accounts",  Icon: UserX           },
];

interface SidebarProps {
  collapsed: boolean;
  onToggle:  () => void;
}

export function Sidebar({ collapsed, onToggle }: SidebarProps) {
  return (
    <aside
      className={cn(
        "h-full border-r border-border bg-white flex flex-col shrink-0 transition-all duration-200",
        collapsed ? "w-14" : "w-56",
      )}
    >
      {/* Nav links */}
      <nav className="flex-1 p-2 space-y-0.5 overflow-hidden">
        {NAV.map(({ to, label, Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                "w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors",
                isActive
                  ? "bg-amber-50 text-amber-700 font-semibold border border-amber-200"
                  : "text-muted-foreground hover:bg-muted hover:text-foreground",
              )
            }
          >
            <Icon className="w-4 h-4 shrink-0" />
            {!collapsed && <span className="truncate">{label}</span>}
          </NavLink>
        ))}
      </nav>

      {/* Collapse toggle */}
      <button
        onClick={onToggle}
        className="m-2 p-2 rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground transition-colors flex items-center justify-center"
        title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
      >
        {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
      </button>
    </aside>
  );
}
