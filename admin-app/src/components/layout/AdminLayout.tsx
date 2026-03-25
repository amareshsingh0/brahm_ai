import { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import { Header } from "./Header";
import { Sidebar } from "./Sidebar";
import { aFetch, preloadAll } from "@/lib/api";

export function AdminLayout() {
  const navigate   = useNavigate();
  const [ready,     setReady]     = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    const key = sessionStorage.getItem("admin-key");
    if (!key) { navigate("/login", { replace: true }); return; }

    // Verify key is still valid + warm cache
    aFetch("/admin/stats")
      .then(() => { setReady(true); preloadAll(); })
      .catch(() => { sessionStorage.removeItem("admin-key"); navigate("/login", { replace: true }); });
  }, [navigate]);

  if (!ready) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="w-6 h-6 rounded-full border-2 border-amber-200 border-t-amber-600 animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <Header />
      <div className="flex flex-1 overflow-hidden">
        {/* Desktop sidebar */}
        <div className="hidden sm:flex h-[calc(100vh-3.5rem)] sticky top-14">
          <Sidebar collapsed={collapsed} onToggle={() => setCollapsed((c) => !c)} />
        </div>

        {/* Main content */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 pb-20 sm:pb-6">
          <Outlet />
        </main>
      </div>

      {/* Mobile bottom nav */}
      <MobileNav />
    </div>
  );
}

function MobileNav() {
  const items = [
    { to: "/dashboard", label: "Dashboard", icon: "📊" },
    { to: "/users",     label: "Users",     icon: "👥" },
    { to: "/payments",  label: "Payments",  icon: "💳" },
    { to: "/chats",     label: "Chats",     icon: "💬" },
    { to: "/logs",      label: "Log",       icon: "📋" },
  ];

  return (
    <div className="sm:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-border flex z-40 shadow-md">
      {items.map(({ to, label, icon }) => (
        <a
          key={to}
          href={`/admin${to}`}
          className="flex-1 py-3 flex flex-col items-center gap-0.5 text-[10px] text-muted-foreground"
        >
          <span className="text-base">{icon}</span>
          {label}
        </a>
      ))}
    </div>
  );
}
