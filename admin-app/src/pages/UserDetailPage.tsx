import { useCallback, useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, ShieldOff, ShieldCheck, Trash2, RefreshCw } from "lucide-react";
import { aFetch } from "@/lib/api";
import { STATUS_CLS } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { Loader } from "@/components/ui/Loader";
import { ActionBtn } from "@/components/ui/Loader";
import { Pagination } from "@/components/ui/Pagination";
import type { UserDetail, ChatMsg, KundaliEntry, PalmEntry, PaymentRow, LoginEntry } from "@/lib/types";
import { ProfileTab }  from "@/user-detail-tabs/ProfileTab";
import { ChatsTab }    from "@/user-detail-tabs/ChatsTab";
import { KundalisTab } from "@/user-detail-tabs/KundalisTab";
import { PalmistryTab }from "@/user-detail-tabs/PalmistryTab";
import { PaymentsTab } from "@/user-detail-tabs/PaymentsTab";
import { UsageTab }    from "@/user-detail-tabs/UsageTab";
import { LoginsTab }   from "@/user-detail-tabs/LoginsTab";

type Tab = "profile" | "chats" | "kundalis" | "palmistry" | "payments" | "usage" | "logins";
const TABS: { id: Tab; label: string }[] = [
  { id: "profile",   label: "Profile"    },
  { id: "chats",     label: "Chats"      },
  { id: "kundalis",  label: "Kundalis"   },
  { id: "palmistry", label: "Palmistry"  },
  { id: "payments",  label: "Payments"   },
  { id: "usage",     label: "Usage"      },
  { id: "logins",    label: "Logins"     },
];

export default function UserDetailPage() {
  const { id }     = useParams<{ id: string }>();
  const navigate   = useNavigate();
  const [tab, setTab] = useState<Tab>("profile");

  // ── User detail ──────────────────────────────────────────────────────────────
  const [user,    setUser]    = useState<UserDetail | null>(null);
  const [loading, setLoading] = useState(true);

  // ── Per-tab data ─────────────────────────────────────────────────────────────
  const [chats,     setChats]     = useState<ChatMsg[]>([]);
  const [chatPage,  setChatPage]  = useState(1);
  const [chatPages, setChatPages] = useState(1);
  const [chatCtx,   setChatCtx]   = useState("");
  const [chatLoad,  setChatLoad]  = useState(false);

  const [kundalis,  setKundalis]  = useState<KundaliEntry[]>([]);
  const [palms,     setPalms]     = useState<PalmEntry[]>([]);
  const [payments,  setPayments]  = useState<PaymentRow[]>([]);
  const [logins,    setLogins]    = useState<LoginEntry[]>([]);

  const [actioning, setActioning] = useState(false);

  // ── Load ALL data in parallel on mount ───────────────────────────────────────
  useEffect(() => {
    if (!id) return;
    setLoading(true);
    Promise.all([
      aFetch<UserDetail>(`/admin/users/${id}`),
      aFetch<{ items: ChatMsg[]; page: number; pages: number }>(`/admin/users/${id}/chats?page=1&limit=30`),
      aFetch<KundaliEntry[]>(`/admin/users/${id}/kundalis`),
      aFetch<PalmEntry[]>(`/admin/users/${id}/palms`),
      aFetch<PaymentRow[]>(`/admin/users/${id}/payments`),
      aFetch<LoginEntry[]>(`/admin/users/${id}/logins`),
    ]).then(([userData, chatsData, kundData, palmData, payData, loginData]) => {
      setUser(userData);
      const cd = chatsData as { items: ChatMsg[]; page: number; pages: number };
      setChats(cd.items ?? []);
      setChatPage(cd.page ?? 1);
      setChatPages(cd.pages ?? 1);
      setKundalis(kundData as KundaliEntry[]);
      setPalms(palmData as PalmEntry[]);
      setPayments(payData as PaymentRow[]);
      setLogins(loginData as LoginEntry[]);
    }).finally(() => setLoading(false));
  }, [id]);

  // ── Reload chats (pagination / ctx filter) ────────────────────────────────────
  const loadChats = useCallback(async (p: number, ctx: string) => {
    if (!id) return;
    setChatLoad(true);
    const q = ctx ? `&ctx=${ctx}` : "";
    const d = await aFetch<{ items: ChatMsg[]; page: number; pages: number }>(
      `/admin/users/${id}/chats?page=${p}&limit=30${q}`
    ).finally(() => setChatLoad(false));
    setChats(d.items ?? []);
    setChatPage(d.page ?? 1);
    setChatPages(d.pages ?? 1);
  }, [id]);

  // ── Actions ───────────────────────────────────────────────────────────────────
  async function doAction(action: string, payload?: Record<string, unknown>) {
    if (actioning) return;
    setActioning(true);
    try {
      await aFetch(`/admin/users/${id}/action`, {
        method: "POST",
        body:   JSON.stringify({ action, ...payload }),
      });
      const u = await aFetch<UserDetail>(`/admin/users/${id}`);
      setUser(u);
    } finally {
      setActioning(false);
    }
  }

  async function flagChat(msgId: string) {
    await aFetch(`/admin/chats/${msgId}/flag`, { method: "POST" });
    setChats((prev) => prev.map((m) => m.id === msgId ? { ...m, flagged: true } : m));
  }

  if (loading) return (
    <div className="flex items-center justify-center h-64"><Loader /></div>
  );
  if (!user) return (
    <div className="text-muted-foreground text-sm p-8">User not found.</div>
  );

  return (
    <div className="space-y-5 animate-fade-in">
      {/* ── Header ─────────────────────────────────────────────────────────────── */}
      <div className="flex items-start gap-4 flex-wrap">
        <button onClick={() => navigate("/users")}
          className="flex items-center gap-1.5 text-muted-foreground hover:text-foreground text-sm transition-colors mt-0.5">
          <ArrowLeft size={15} /> Back
        </button>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="text-xl font-bold text-foreground font-display truncate">
              {user.name ?? user.phone ?? user.id}
            </h1>
            <Badge text={user.status} cls={STATUS_CLS[user.status] ?? "bg-muted text-muted-foreground"} />
            <Badge text={user.role}   cls="bg-muted text-muted-foreground" />
            <Badge text={user.plan}   cls="bg-amber-50 text-amber-700" />
          </div>
          <p className="text-muted-foreground text-xs mt-0.5 font-mono">{user.id}</p>
        </div>

        {/* ── Action buttons ────────────────────────────────────────────────────── */}
        <div className="flex items-center gap-2 flex-wrap">
          {user.status === "active" && (
            <ActionBtn
              icon={<ShieldOff size={13} />}
              label="Suspend"
              cls="border border-yellow-300 text-yellow-700 hover:bg-yellow-50"
              loading={actioning}
              onClick={() => doAction("suspend")}
            />
          )}
          {user.status === "suspended" && (
            <ActionBtn
              icon={<ShieldCheck size={13} />}
              label="Reactivate"
              cls="border border-emerald-300 text-emerald-700 hover:bg-emerald-50"
              loading={actioning}
              onClick={() => doAction("reactivate")}
            />
          )}
          {user.status !== "banned" && (
            <ActionBtn
              icon={<ShieldOff size={13} />}
              label="Ban"
              cls="border border-red-300 text-red-600 hover:bg-red-50"
              loading={actioning}
              onClick={() => {
                if (confirm(`Ban ${user.name ?? user.id}?`)) doAction("ban");
              }}
            />
          )}
          <ActionBtn
            icon={<RefreshCw size={13} />}
            label="Reset Key"
            cls="border border-border text-muted-foreground hover:bg-muted"
            loading={actioning}
            onClick={() => {
              if (confirm("Reset this user's API key?")) doAction("reset_key");
            }}
          />
          <ActionBtn
            icon={<Trash2 size={13} />}
            label="Delete"
            cls="border border-red-200 text-red-500 hover:bg-red-50"
            loading={actioning}
            onClick={() => {
              if (confirm(`Permanently delete ${user.name ?? user.id}? This cannot be undone.`)) doAction("delete");
            }}
          />
        </div>
      </div>

      {/* ── Tabs ─────────────────────────────────────────────────────────────────── */}
      <div className="flex gap-1 flex-wrap border-b border-border/60 pb-0">
        {TABS.map((t) => (
          <button key={t.id} onClick={() => setTab(t.id)}
            className={`px-4 py-2 text-xs font-medium transition-colors rounded-t -mb-px ${
              tab === t.id
                ? "bg-background border border-border/60 border-b-background text-amber-700"
                : "text-muted-foreground hover:text-foreground"
            }`}>
            {t.label}
          </button>
        ))}
      </div>

      {/* ── Tab panels ───────────────────────────────────────────────────────────── */}
      <div className="pt-1">
        {tab === "profile"   && <ProfileTab   user={user} />}
        {tab === "chats"     && (
          <>
            <ChatsTab
              chats={chats}
              loading={chatLoad}
              page={chatPage}
              pages={chatPages}
              ctx={chatCtx}
              onCtxChange={(c) => { setChatCtx(c); loadChats(1, c); }}
              onPage={(p) => loadChats(p, chatCtx)}
              onFlag={flagChat}
            />
            <Pagination page={chatPage} pages={chatPages} onChange={(p) => loadChats(p, chatCtx)} />
          </>
        )}
        {tab === "kundalis"  && <KundalisTab  items={kundalis} loading={false} />}
        {tab === "palmistry" && <PalmistryTab items={palms}    loading={false} />}
        {tab === "payments"  && <PaymentsTab  items={payments} loading={false} />}
        {tab === "usage"     && <UsageTab     items={user.usage_today ?? []} loading={false} />}
        {tab === "logins"    && <LoginsTab    items={logins}    loading={loginLoad} />}
      </div>
    </div>
  );
}
