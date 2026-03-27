import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { aFetch, preloadAll } from "@/lib/api";
import { Lock, User, Eye, EyeOff } from "lucide-react";

export default function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [key,      setKey]      = useState("");
  const [showKey,  setShowKey]  = useState(false);
  const [err,      setErr]      = useState("");
  const [loading,  setLoading]  = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !key.trim()) return;
    setErr(""); setLoading(true);

    // Encode as base64(username:secret_key)
    const token = btoa(`${username.trim()}:${key.trim()}`);
    sessionStorage.setItem("admin-key", token);

    try {
      await aFetch("/admin/stats");
      preloadAll();
      navigate("/dashboard", { replace: true });
    } catch {
      setErr("Invalid username or secret key.");
      sessionStorage.removeItem("admin-key");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-amber-50 border border-amber-200 mb-2">
            <Lock className="w-7 h-7 text-amber-700" />
          </div>
          <h1 className="text-2xl font-bold text-foreground font-display">Brahm AI Admin</h1>
          <p className="text-muted-foreground text-sm">Internal Dashboard — Authorized access only</p>
        </div>

        <form onSubmit={submit} className="space-y-4">
          {/* Username */}
          <div className="relative">
            <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Username"
              autoFocus
              autoComplete="username"
              className="w-full bg-white border border-border rounded-xl pl-10 pr-4 py-3 text-foreground placeholder:text-muted-foreground focus:outline-none focus:border-amber-400 transition-colors"
            />
          </div>

          {/* Secret Key */}
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <input
              type={showKey ? "text" : "password"}
              value={key}
              onChange={(e) => setKey(e.target.value)}
              placeholder="Secret key"
              autoComplete="current-password"
              className="w-full bg-white border border-border rounded-xl pl-10 pr-10 py-3 text-foreground placeholder:text-muted-foreground focus:outline-none focus:border-amber-400 transition-colors"
            />
            <button
              type="button"
              onClick={() => setShowKey((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              tabIndex={-1}
            >
              {showKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>

          {err && <p className="text-red-600 text-sm">{err}</p>}

          <button
            type="submit"
            disabled={loading || !username.trim() || !key.trim()}
            className="w-full py-3 rounded-xl bg-amber-600 text-white font-bold hover:bg-amber-700 disabled:opacity-40 transition-colors"
          >
            {loading ? "Verifying…" : "Enter Dashboard"}
          </button>
        </form>
      </div>
    </div>
  );
}
