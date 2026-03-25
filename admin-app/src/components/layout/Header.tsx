import { Settings, LogOut } from "lucide-react";
import { useNavigate } from "react-router-dom";

export function Header() {
  const navigate = useNavigate();

  const handleLogout = () => {
    sessionStorage.removeItem("admin-key");
    navigate("/login");
  };

  return (
    <header className="h-14 border-b border-amber-100 bg-white px-5 flex items-center gap-3 shrink-0 shadow-sm">
      {/* Logo */}
      <div className="flex items-center gap-2">
        <Settings className="w-5 h-5 text-amber-700" />
        <span className="font-bold text-foreground font-display">Brahm AI</span>
        <span className="text-xs px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-200 font-medium">
          Admin
        </span>
      </div>

      <div className="ml-auto flex items-center gap-3">
        <button
          onClick={handleLogout}
          className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg bg-muted text-muted-foreground hover:bg-border hover:text-foreground border border-border transition-colors"
        >
          <LogOut className="w-3.5 h-3.5" />
          Logout
        </button>
      </div>
    </header>
  );
}
