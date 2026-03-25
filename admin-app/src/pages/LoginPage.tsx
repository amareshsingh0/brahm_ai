import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { aFetch, preloadAll } from "@/lib/api";
import { Lock } from "lucide-react";

export default function LoginPage() {
  const navigate = useNavigate();
  const [key,     setKey]     = useState("");
  const [err,     setErr]     = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErr(""); setLoading(true);
    sessionStorage.setItem("admin-key", key);
    try {
      await aFetch("/admin/stats");
      preloadAll();
      navigate("/dashboard", { replace: true });
    } catch {
      setErr("Invalid admin key.");
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
          <input
            type="password"
            value={key}
            onChange={(e) => setKey(e.target.value)}
            placeholder="Admin secret key"
            autoFocus
            className="w-full bg-white border border-border rounded-xl px-4 py-3 text-foreground placeholder:text-muted-foreground focus:outline-none focus:border-amber-400 transition-colors"
          />
          {err && <p className="text-red-600 text-sm">{err}</p>}
          <button
            type="submit"
            disabled={loading || !key}
            className="w-full py-3 rounded-xl bg-amber-600 text-white font-bold hover:bg-amber-700 disabled:opacity-40 transition-colors"
          >
            {loading ? "Verifying…" : "Enter Dashboard"}
          </button>
        </form>
      </div>
    </div>
  );
}
