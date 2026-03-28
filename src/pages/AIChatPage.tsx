import { useRef, useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Send, Sparkles, User, AlertTriangle, History, SquarePen,
  Pin, Archive, ArchiveRestore, Edit2, Trash2, ChevronDown, ChevronUp, X,
  Copy, Check, RefreshCw,
} from "lucide-react";
import { useChat } from "@/hooks/useChat";
import { useTranslation } from "react-i18next";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import type { ChatMessage } from "@/types/api";

// ── Types ────────────────────────────────────────────────────────────────────
interface ChatMsg   { role: string; content: string; created_at: string; }
interface ChatSession {
  session_id: string; page_context: string; last_at: string;
  is_pinned: boolean; is_archived: boolean; custom_name: string | null;
  messages: ChatMsg[];
}
interface CtxMenu { sessionId: string; x: number; y: number; }

// ── Helpers ──────────────────────────────────────────────────────────────────
function getDateLabel(iso: string) {
  const d = new Date(iso); const now = new Date();
  const today     = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today.getTime() - 86400000);
  const sd = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  if (sd.getTime() === today.getTime())     return "Today";
  if (sd.getTime() === yesterday.getTime()) return "Yesterday";
  if (today.getTime() - sd.getTime() < 7 * 86400000)
    return d.toLocaleDateString("en-IN", { weekday: "long" });
  return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}
function groupByDate(sessions: ChatSession[]) {
  const map = new Map<string, ChatSession[]>();
  for (const s of sessions) {
    const lbl = getDateLabel(s.last_at);
    if (!map.has(lbl)) map.set(lbl, []);
    map.get(lbl)!.push(s);
  }
  return Array.from(map.entries()).map(([label, items]) => ({ label, items }));
}
function sessionPreview(s: ChatSession) {
  const first = s.messages.find((m) => m.role === "user");
  return s.custom_name ?? first?.content?.slice(0, 60) ?? "…";
}

const SUGGESTIONS = [
  "What does my moon sign say about me?",
  "Explain Sade Sati and its effects",
  "What are the most powerful Yogas in Kundali?",
  "Tell me about Jupiter in the 7th house",
];

// ── Main Page ────────────────────────────────────────────────────────────────
export default function AIChatPage() {
  const { t } = useTranslation();
  const { messages: chatMessages, streaming, sendMessage: sendChatMessage } = useChat({
    pageContext: "chat",
    persistKey:  "main-chat",
  });
  const [input,       setInput]       = useState("");
  const [showHistory, setShowHistory] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [chatMessages, streaming]);

  const send = (text: string) => {
    if (!text.trim() || streaming) return;
    setInput("");
    sendChatMessage(text.trim());
  };

  const startNewChat = () => {
    localStorage.removeItem("brahm-chat-main-chat");
    localStorage.removeItem("brahm-session-brahm-chat-main-chat");
    window.location.reload();
  };

  const showTyping = streaming && chatMessages.length > 0 && chatMessages[chatMessages.length - 1].content === "";
  const visibleMessages = chatMessages.filter(
    (m) => !(m.role === "assistant" && m.content === "" && streaming)
  );

  return (
    <div className="flex flex-col h-[calc(100vh-8rem)] sm:h-[calc(100vh-4rem)]">

      {/* ── Top bar: disclaimer + action buttons ─────────────────────── */}
      <div className="flex-shrink-0 mb-3 flex items-center gap-2">
        <div className="flex-1 flex items-center gap-2 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
          <AlertTriangle className="h-3.5 w-3.5 text-amber-600 flex-shrink-0" />
          <p className="text-[11px] text-amber-800">AI can make mistakes • Please verify important information</p>
        </div>
        <button
          onClick={() => setShowHistory(true)}
          title="Chat History"
          className="p-2 rounded-lg border border-border hover:bg-muted/50 text-muted-foreground hover:text-foreground transition-colors flex-shrink-0"
        >
          <History className="h-4 w-4" />
        </button>
        <button
          onClick={startNewChat}
          title="New Chat"
          className="p-2 rounded-lg border border-amber-200 hover:bg-amber-50 text-amber-700 transition-colors flex-shrink-0"
        >
          <SquarePen className="h-4 w-4" />
        </button>
      </div>

      {/* ── Messages ─────────────────────────────────────────────────── */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto space-y-3 pb-4 scrollbar-thin">
        {visibleMessages.length === 0 && !streaming && (
          <EmptyState onSuggestion={send} />
        )}
        <AnimatePresence initial={false}>
          {visibleMessages.map((msg, i) => (
            <motion.div key={i} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }}>
              <MessageBubble
                msg={msg}
                onFollowUp={send}
                onRegenerate={i === visibleMessages.length - 1 && msg.role === "assistant" ? () => {
                  const lastUser = [...visibleMessages].reverse().find((m) => m.role === "user");
                  if (lastUser) send(lastUser.content);
                } : undefined}
              />
            </motion.div>
          ))}
        </AnimatePresence>
        {showTyping && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-end gap-2">
            <AiAvatar />
            <div className="bg-white border border-border rounded-2xl rounded-bl-sm px-4 py-3 shadow-sm">
              <div className="flex gap-1 items-center h-4">
                {[0, 1, 2].map((i) => (
                  <motion.span key={i} className="w-1.5 h-1.5 rounded-full bg-muted-foreground block"
                    animate={{ y: [0, -4, 0] }} transition={{ duration: 0.7, repeat: Infinity, delay: i * 0.15 }} />
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </div>

      {/* ── Input bar ────────────────────────────────────────────────── */}
      <div className="flex-shrink-0 pt-3 border-t border-border">
        <form onSubmit={(e) => { e.preventDefault(); send(input); }} className="flex gap-2 items-end">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); send(input); } }}
            placeholder="Message Brahm AI..."
            rows={1}
            disabled={streaming}
            className="flex-1 resize-none rounded-2xl border border-border bg-muted/30 px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:border-amber-400 focus:ring-1 focus:ring-amber-400/30 disabled:opacity-50 transition-colors min-h-[42px] max-h-[120px] overflow-y-auto"
            style={{ scrollbarWidth: "none" }}
          />
          <button
            type="submit"
            disabled={!input.trim() || streaming}
            className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 transition-colors disabled:bg-border disabled:cursor-not-allowed bg-amber-600 hover:bg-amber-700 text-white"
          >
            <Send className="h-4 w-4" />
          </button>
        </form>
      </div>

      {/* ── History Drawer ────────────────────────────────────────────── */}
      <AnimatePresence>
        {showHistory && (
          <HistoryDrawer onClose={() => setShowHistory(false)} onNewChat={startNewChat} />
        )}
      </AnimatePresence>
    </div>
  );
}

// ── History Drawer ────────────────────────────────────────────────────────────
function HistoryDrawer({ onClose, onNewChat }: { onClose: () => void; onNewChat: () => void }) {
  const qc = useQueryClient();

  const { data: normalData,   isLoading } = useQuery({
    queryKey: ["chat-sessions"],
    queryFn:  () => api.get<{ sessions: ChatSession[] }>("/api/user/chats/sessions?include_archived=false"),
  });
  const { data: archivedData } = useQuery({
    queryKey: ["chat-sessions-archived"],
    queryFn:  () => api.get<{ sessions: ChatSession[] }>("/api/user/chats/sessions?include_archived=true"),
  });

  const sessions = normalData?.sessions  ?? [];
  const archived = archivedData?.sessions ?? [];

  const [ctxMenu,      setCtxMenu]      = useState<CtxMenu | null>(null);
  const [ctxSession,   setCtxSession]   = useState<ChatSession | null>(null);
  const [showArchived, setShowArchived] = useState(false);
  const [renaming,     setRenaming]     = useState(false);
  const [renameValue,  setRenameValue]  = useState("");
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!ctxMenu) return;
    const handler = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setCtxMenu(null); setCtxSession(null);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [ctxMenu]);

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ["chat-sessions"] });
    qc.invalidateQueries({ queryKey: ["chat-sessions-archived"] });
  };

  const deleteSession = useMutation({
    mutationFn: (id: string) => api.delete(`/api/user/chats/session/${encodeURIComponent(id)}`),
    onSuccess: invalidate,
  });
  const patchMeta = useMutation({
    mutationFn: ({ sessionId, body }: { sessionId: string; body: object }) =>
      api.patch(`/api/user/chats/session/${encodeURIComponent(sessionId)}/meta`, body),
    onSuccess: invalidate,
  });
  const deleteAll = useMutation({
    mutationFn: () => api.delete("/api/user/chats"),
    onSuccess: invalidate,
  });

  function openCtx(e: React.MouseEvent | React.TouchEvent, s: ChatSession) {
    e.preventDefault();
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const x = "clientX" in e ? e.clientX : rect.left + 40;
    const y = "clientY" in e ? e.clientY : rect.bottom;
    setCtxMenu({ sessionId: s.session_id, x, y });
    setCtxSession(s);
  }
  function closeCtx() { setCtxMenu(null); setCtxSession(null); }

  function loadSession(s: ChatSession) {
    const msgs = s.messages.slice().reverse().map((m) => ({ role: m.role, content: m.content }));
    localStorage.setItem("brahm-chat-main-chat", JSON.stringify(msgs));
    localStorage.setItem("brahm-session-brahm-chat-main-chat", s.session_id);
    onClose();
    window.location.reload();
  }

  const pinned   = sessions.filter((s) => s.is_pinned);
  const unpinned = sessions.filter((s) => !s.is_pinned);
  const groups   = groupByDate(unpinned);

  return (
    <>
      {/* Backdrop */}
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
        className="fixed inset-0 z-40 bg-black/30"
        onClick={onClose}
      />

      {/* Drawer */}
      <motion.div
        initial={{ x: "100%" }} animate={{ x: 0 }} exit={{ x: "100%" }}
        transition={{ type: "spring", stiffness: 320, damping: 32 }}
        className="fixed right-0 top-0 bottom-0 z-50 w-full max-w-sm bg-background border-l border-border shadow-2xl flex flex-col"
      >
        {/* Drawer header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-border flex-shrink-0">
          <div>
            <h2 className="font-semibold text-base">Chat History</h2>
            <p className="text-xs text-muted-foreground">
              {sessions.length} conversation{sessions.length !== 1 ? "s" : ""}
              {archived.length > 0 && ` · ${archived.length} archived`}
            </p>
          </div>
          <div className="flex items-center gap-1">
            <button
              onClick={() => { onNewChat(); onClose(); }}
              title="New Chat"
              className="p-2 rounded-lg hover:bg-muted text-amber-700 transition-colors"
            >
              <SquarePen className="h-4 w-4" />
            </button>
            {sessions.length > 0 && (
              <button
                onClick={() => { if (confirm("Delete all chat history? This cannot be undone.")) deleteAll.mutate(); }}
                title="Delete All"
                className="p-2 rounded-lg hover:bg-red-50 text-red-400 hover:text-red-500 transition-colors"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            )}
            <button onClick={onClose} className="p-2 rounded-lg hover:bg-muted text-muted-foreground transition-colors">
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        <p className="text-xs text-muted-foreground px-4 pt-2 pb-1">Right-click (or long-press) to pin, archive, rename, or delete.</p>

        {/* Scrollable list */}
        <div className="flex-1 overflow-y-auto">
          {isLoading && (
            <div className="space-y-2 p-4">
              {[1,2,3,4].map((i) => <div key={i} className="h-14 rounded-xl bg-muted/30 animate-pulse" />)}
            </div>
          )}

          {!isLoading && sessions.length === 0 && archived.length === 0 && (
            <div className="flex flex-col items-center justify-center h-full text-center text-muted-foreground py-16 px-4">
              <History className="h-10 w-10 mx-auto mb-3 opacity-20" />
              <p className="text-sm">No chat history yet.</p>
            </div>
          )}

          {/* Pinned */}
          {pinned.length > 0 && (
            <div className="px-3 pt-3">
              <p className="text-xs font-semibold text-amber-700 uppercase tracking-wide px-1 mb-1 flex items-center gap-1">📌 Pinned</p>
              <div className="bg-card border border-amber-200/60 rounded-xl overflow-hidden divide-y divide-border/30">
                {pinned.map((s) => (
                  <HistoryRow key={s.session_id} session={s} onContextMenu={(e) => openCtx(e, s)} onContinue={() => loadSession(s)} />
                ))}
              </div>
            </div>
          )}

          {/* Date-grouped */}
          {groups.map(({ label, items }) => (
            <div key={label} className="px-3 pt-3">
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide px-1 mb-1">{label}</p>
              <div className="bg-card border border-border/40 rounded-xl overflow-hidden divide-y divide-border/30">
                {items.map((s) => (
                  <HistoryRow key={s.session_id} session={s} onContextMenu={(e) => openCtx(e, s)} onContinue={() => loadSession(s)} />
                ))}
              </div>
            </div>
          ))}

          {/* Archived */}
          {archived.length > 0 && (
            <div className="px-3 pt-3 pb-4">
              <button
                className="w-full flex items-center gap-2 px-1 py-2 text-xs font-semibold text-muted-foreground uppercase tracking-wide hover:text-foreground transition-colors"
                onClick={() => setShowArchived(!showArchived)}
              >
                {showArchived ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                <Archive className="h-3.5 w-3.5" />
                Archived ({archived.length})
              </button>
              {showArchived && (
                <div className="bg-card border border-border/40 rounded-xl overflow-hidden divide-y divide-border/30 opacity-80">
                  {archived.map((s) => (
                    <HistoryRow key={s.session_id} session={s} onContextMenu={(e) => openCtx(e, s)} onContinue={() => loadSession(s)} />
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </motion.div>

      {/* Context menu */}
      {ctxMenu && ctxSession && (
        <div
          ref={menuRef}
          className="fixed z-[60] bg-card border border-border rounded-xl shadow-xl py-1 min-w-[180px]"
          style={{ left: Math.min(ctxMenu.x, window.innerWidth - 200), top: Math.min(ctxMenu.y, window.innerHeight - 220) }}
        >
          <CtxItem icon={<Pin className="h-3.5 w-3.5" />} label={ctxSession.is_pinned ? "Unpin" : "Pin"}
            onClick={() => { patchMeta.mutate({ sessionId: ctxSession.session_id, body: { action: ctxSession.is_pinned ? "unpin" : "pin" } }); closeCtx(); }} />
          <CtxItem icon={<Edit2 className="h-3.5 w-3.5" />} label="Rename"
            onClick={() => {
              const first = ctxSession.messages.find((m) => m.role === "user");
              setRenameValue(ctxSession.custom_name ?? first?.content?.slice(0, 60) ?? "");
              setRenaming(true); closeCtx();
            }} />
          {ctxSession.is_archived
            ? <CtxItem icon={<ArchiveRestore className="h-3.5 w-3.5" />} label="Unarchive"
                onClick={() => { patchMeta.mutate({ sessionId: ctxSession.session_id, body: { action: "unarchive" } }); closeCtx(); }} />
            : <CtxItem icon={<Archive className="h-3.5 w-3.5" />} label="Archive"
                onClick={() => { patchMeta.mutate({ sessionId: ctxSession.session_id, body: { action: "archive" } }); closeCtx(); }} />
          }
          <div className="h-px bg-border mx-2 my-1" />
          <CtxItem icon={<Trash2 className="h-3.5 w-3.5" />} label="Delete" danger
            onClick={() => { if (confirm("Delete this chat?")) deleteSession.mutate(ctxSession.session_id); closeCtx(); }} />
        </div>
      )}

      {/* Rename modal */}
      {renaming && ctxSession && (
        <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/40">
          <div className="bg-card border border-border rounded-2xl shadow-xl p-6 w-full max-w-sm space-y-4 mx-4">
            <h3 className="font-semibold text-base">Rename Chat</h3>
            <input
              autoFocus
              className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-500"
              value={renameValue}
              onChange={(e) => setRenameValue(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && renameValue.trim()) {
                  patchMeta.mutate({ sessionId: ctxSession.session_id, body: { action: "rename", name: renameValue.trim() } });
                  setRenaming(false);
                }
              }}
            />
            <div className="flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => setRenaming(false)}>Cancel</Button>
              <Button size="sm" className="bg-amber-600 hover:bg-amber-700 text-white"
                disabled={!renameValue.trim()}
                onClick={() => {
                  patchMeta.mutate({ sessionId: ctxSession.session_id, body: { action: "rename", name: renameValue.trim() } });
                  setRenaming(false);
                }}
              >Save</Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

// ── History Row ───────────────────────────────────────────────────────────────
function HistoryRow({ session, onContextMenu, onContinue }: {
  session: ChatSession;
  onContextMenu: (e: React.MouseEvent | React.TouchEvent) => void;
  onContinue: () => void;
}) {
  const longPressTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const preview = sessionPreview(session);
  const time = (() => { try { return new Date(session.last_at).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }); } catch { return ""; } })();

  return (
    <div
      className="flex items-center gap-3 px-3 py-2.5 hover:bg-amber-50/50 transition-colors cursor-pointer select-none"
      onClick={onContinue}
      onContextMenu={onContextMenu}
      onTouchStart={(e) => { longPressTimer.current = setTimeout(() => onContextMenu(e), 500); }}
      onTouchEnd={() => { if (longPressTimer.current) clearTimeout(longPressTimer.current); }}
      onTouchMove={() => { if (longPressTimer.current) clearTimeout(longPressTimer.current); }}
    >
      <div className="w-8 h-8 rounded-full bg-amber-100 flex items-center justify-center flex-shrink-0 relative">
        <Sparkles className="h-3.5 w-3.5 text-amber-700" />
        {session.is_pinned && <span className="absolute -top-0.5 -right-0.5 text-[9px]">📌</span>}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5 mb-0.5">
          <span className="text-[10px] font-medium text-amber-700 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded capitalize">
            {session.page_context}
          </span>
          <span className="text-[10px] text-muted-foreground">{time}</span>
        </div>
        <p className="text-xs text-foreground/80 truncate">{preview}</p>
      </div>
    </div>
  );
}

// ── Ctx menu item ─────────────────────────────────────────────────────────────
function CtxItem({ icon, label, danger, onClick }: { icon: React.ReactNode; label: string; danger?: boolean; onClick: () => void }) {
  return (
    <button
      className={`w-full flex items-center gap-2.5 px-4 py-2.5 text-sm hover:bg-muted transition-colors ${danger ? "text-red-500 hover:bg-red-50" : "text-foreground"}`}
      onClick={onClick}
    >
      {icon} {label}
    </button>
  );
}

// ── AI Avatar ─────────────────────────────────────────────────────────────────
function AiAvatar() {
  return (
    <div className="w-7 h-7 rounded-full bg-amber-100 flex items-center justify-center flex-shrink-0 self-end mb-0.5">
      <Sparkles className="h-3.5 w-3.5 text-amber-700" />
    </div>
  );
}

// ── User Avatar ───────────────────────────────────────────────────────────────
function UserAvatar() {
  return (
    <div className="w-7 h-7 rounded-full bg-muted flex items-center justify-center flex-shrink-0 self-end mb-0.5">
      <User className="h-3.5 w-3.5 text-muted-foreground" />
    </div>
  );
}

// ── Message Bubble ────────────────────────────────────────────────────────────
function MessageBubble({ msg, onFollowUp, onRegenerate }: {
  msg: ChatMessage;
  onFollowUp: (q: string) => void;
  onRegenerate?: () => void;
}) {
  const [copied, setCopied] = useState(false);

  const copy = () => {
    navigator.clipboard.writeText(msg.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  if (msg.role === "user") {
    return (
      <div className="flex justify-end items-end gap-2">
        <div className="max-w-[80%] bg-amber-600 text-white rounded-2xl rounded-br-sm px-4 py-2.5 text-sm leading-relaxed shadow-sm">
          {msg.content}
        </div>
        <UserAvatar />
      </div>
    );
  }
  return (
    <div className="flex items-end gap-2">
      <AiAvatar />
      <div className="flex-1 min-w-0">
        <div className="max-w-[85%] bg-white border border-border rounded-2xl rounded-bl-sm px-4 py-2.5 text-sm text-foreground leading-relaxed shadow-sm whitespace-pre-wrap">
          <MessageContent content={msg.content} />
        </div>
        {/* Action buttons — show when message is complete */}
        {msg.isComplete && msg.content && (
          <div className="flex items-center gap-1 mt-1.5 max-w-[85%]">
            <button
              onClick={copy}
              title="Copy"
              className="flex items-center gap-1 px-2 py-1 rounded-lg text-[11px] text-muted-foreground hover:text-foreground hover:bg-muted/60 transition-colors"
            >
              {copied ? <Check className="h-3 w-3 text-green-600" /> : <Copy className="h-3 w-3" />}
              {copied ? "Copied" : "Copy"}
            </button>
            {onRegenerate && (
              <button
                onClick={onRegenerate}
                title="Regenerate"
                className="flex items-center gap-1 px-2 py-1 rounded-lg text-[11px] text-muted-foreground hover:text-foreground hover:bg-muted/60 transition-colors"
              >
                <RefreshCw className="h-3 w-3" />
                Regenerate
              </button>
            )}
          </div>
        )}
        {msg.isComplete && msg.followUps && msg.followUps.length > 0 && (
          <div className="mt-2 flex flex-col gap-1.5 max-w-[85%]">
            {msg.followUps.map((q, i) => (
              <button key={i} onClick={() => onFollowUp(q)}
                className="text-left text-xs px-3 py-2 rounded-xl border border-amber-200 bg-amber-50 text-amber-800 hover:bg-amber-100 transition-colors font-medium">
                {q}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ── Inline markdown ───────────────────────────────────────────────────────────
function MessageContent({ content }: { content: string }) {
  const parts = content.split(/(\*\*[^*]+\*\*)/g);
  return (
    <>
      {parts.map((part, i) =>
        part.startsWith("**") && part.endsWith("**")
          ? <strong key={i} className="font-semibold text-foreground">{part.slice(2, -2)}</strong>
          : <span key={i}>{part}</span>
      )}
    </>
  );
}

// ── Empty state ───────────────────────────────────────────────────────────────
function EmptyState({ onSuggestion }: { onSuggestion: (q: string) => void }) {
  return (
    <div className="flex flex-col items-center text-center pt-10 pb-6 gap-4">
      <div className="w-16 h-16 rounded-full bg-amber-100 flex items-center justify-center">
        <span className="text-3xl">🔮</span>
      </div>
      <div>
        <p className="font-semibold text-foreground text-lg">Brahm AI</p>
        <p className="text-sm text-muted-foreground mt-1">Your Vedic astrology guide.<br />Ask about planets, kundali, doshas &amp; more.</p>
      </div>
      <div className="flex flex-col gap-2 w-full max-w-sm mt-2">
        {SUGGESTIONS.map((s) => (
          <button key={s} onClick={() => onSuggestion(s)}
            className="text-left text-sm px-4 py-2.5 rounded-xl border border-amber-200 bg-amber-50 text-amber-800 hover:bg-amber-100 transition-colors">
            {s}
          </button>
        ))}
      </div>
    </div>
  );
}
