/**
 * ChatHistoryPage — User's personal chat history.
 * Right-click (or long-press) a session → Pin, Archive, Rename, Delete.
 * Archived section at the bottom. Settings link in header.
 */
import { useRef, useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Trash2, MessageSquare, Sparkles, ChevronRight,
  Pin, Archive, Edit2, ArchiveRestore, ChevronDown, ChevronUp,
} from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';

// ── Types ─────────────────────────────────────────────────────────────────────

interface ChatMsg {
  role: string;
  content: string;
  created_at: string;
}

interface ChatSession {
  session_id:   string;
  page_context: string;
  last_at:      string;
  is_pinned:    boolean;
  is_archived:  boolean;
  custom_name:  string | null;
  messages:     ChatMsg[];
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const PAGE_LABELS: Record<string, string> = {
  kundali: 'Kundali', panchang: 'Panchang', compatibility: 'Compatibility',
  sky: 'Live Sky', horoscope: 'Horoscope', palmistry: 'Palmistry',
  general: 'General', chat: 'Chat', gochar: 'Gochar', prashna: 'Prashna',
  varshphal: 'Varshphal', kp: 'KP System', 'sade-sati': 'Sade Sati',
  dosha: 'Dosha', gemstones: 'Gemstones', rectification: 'Rectification',
};

function getDateLabel(iso: string) {
  const d   = new Date(iso);
  const now = new Date();
  const today     = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today.getTime() - 86400000);
  const sessionDay = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  if (sessionDay.getTime() === today.getTime())     return 'Today';
  if (sessionDay.getTime() === yesterday.getTime()) return 'Yesterday';
  const diff = today.getTime() - sessionDay.getTime();
  if (diff < 7 * 86400000) return d.toLocaleDateString('en-IN', { weekday: 'long' });
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatTime(iso: string) {
  try { return new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }); }
  catch { return ''; }
}

function groupByDate(sessions: ChatSession[]) {
  const map = new Map<string, ChatSession[]>();
  for (const s of sessions) {
    const label = getDateLabel(s.last_at);
    if (!map.has(label)) map.set(label, []);
    map.get(label)!.push(s);
  }
  return Array.from(map.entries()).map(([label, items]) => ({ label, items }));
}

function loadSessionIntoChat(session: ChatSession) {
  const storageKey = 'brahm-chat-main-chat';
  const sessionKey = 'brahm-session-brahm-chat-main-chat';
  const msgs = session.messages.slice().reverse().map((m) => ({ role: m.role, content: m.content }));
  localStorage.setItem(storageKey, JSON.stringify(msgs));
  localStorage.setItem(sessionKey, session.session_id);
}

// ── Context Menu ──────────────────────────────────────────────────────────────

interface CtxMenu {
  sessionId: string;
  x: number;
  y: number;
}

// ── Session Row ───────────────────────────────────────────────────────────────

function SessionRow({
  session, onContextMenu, onContinue,
}: {
  session: ChatSession;
  onContextMenu: (e: React.MouseEvent | React.TouchEvent, s: ChatSession) => void;
  onContinue: () => void;
}) {
  const userMsgs    = session.messages.filter((m) => m.role === 'user');
  const preview     = session.custom_name ?? (userMsgs[0]?.content?.slice(0, 72) ?? '…');
  const msgCount    = userMsgs.length;
  const contextLabel = PAGE_LABELS[session.page_context] ?? session.page_context;
  const canContinue  = session.page_context === 'general' || session.page_context === 'chat';

  // Long-press for mobile
  const longPressTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  return (
    <div
      className="flex items-center gap-3 px-4 py-3 hover:bg-amber-50/50 transition-colors group cursor-pointer select-none"
      onClick={canContinue ? onContinue : undefined}
      onContextMenu={(e) => { e.preventDefault(); onContextMenu(e, session); }}
      onTouchStart={(e) => {
        longPressTimer.current = setTimeout(() => onContextMenu(e, session), 500);
      }}
      onTouchEnd={() => { if (longPressTimer.current) clearTimeout(longPressTimer.current); }}
      onTouchMove={() => { if (longPressTimer.current) clearTimeout(longPressTimer.current); }}
    >
      <div className="w-9 h-9 rounded-full bg-amber-100 flex items-center justify-center flex-shrink-0 relative">
        <Sparkles className="h-4 w-4 text-amber-700" />
        {session.is_pinned && (
          <span className="absolute -top-0.5 -right-0.5 text-[10px]">📌</span>
        )}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-0.5">
          <span className="text-xs font-medium text-amber-700 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded">
            {contextLabel}
          </span>
          <span className="text-xs text-muted-foreground">{formatTime(session.last_at)}</span>
          <span className="text-xs text-muted-foreground">· {msgCount} msg{msgCount !== 1 ? 's' : ''}</span>
        </div>
        <p className="text-sm text-foreground/80 truncate">{preview}</p>
      </div>

      {canContinue && (
        <ChevronRight className="h-4 w-4 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0" />
      )}
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function ChatHistoryPage() {
  const qc             = useQueryClient();
  const navigate       = useNavigate();
  const [searchParams] = useSearchParams();

  // Fetch normal (non-archived) sessions
  const { data: normalData, isLoading, error } = useQuery({
    queryKey: ['chat-sessions'],
    queryFn:  () => api.get<{ sessions: ChatSession[] }>('/api/user/chats/sessions?include_archived=false'),
  });

  // Fetch archived sessions
  const { data: archivedData } = useQuery({
    queryKey: ['chat-sessions-archived'],
    queryFn:  () => api.get<{ sessions: ChatSession[] }>('/api/user/chats/sessions?include_archived=true'),
  });

  const sessions  = normalData?.sessions  ?? [];
  const archived  = archivedData?.sessions ?? [];

  // Context menu state
  const [ctxMenu,     setCtxMenu]     = useState<CtxMenu | null>(null);
  const [ctxSession,  setCtxSession]  = useState<ChatSession | null>(null);
  const [showArchived, setShowArchived] = useState(() => searchParams.get('archived') === '1');
  const [renaming,    setRenaming]    = useState(false);
  const [renameValue, setRenameValue] = useState('');
  const menuRef = useRef<HTMLDivElement>(null);

  // Close menu on outside click
  useEffect(() => {
    if (!ctxMenu) return;
    const handler = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setCtxMenu(null); setCtxSession(null);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [ctxMenu]);

  // Mutations
  const deleteSession = useMutation({
    mutationFn: (sessionId: string) => api.delete(`/api/user/chats/session/${encodeURIComponent(sessionId)}`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['chat-sessions'] }); qc.invalidateQueries({ queryKey: ['chat-sessions-archived'] }); },
  });

  const patchMeta = useMutation({
    mutationFn: ({ sessionId, body }: { sessionId: string; body: object }) =>
      api.patch(`/api/user/chats/session/${encodeURIComponent(sessionId)}/meta`, body),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['chat-sessions'] }); qc.invalidateQueries({ queryKey: ['chat-sessions-archived'] }); },
  });

  const deleteAll = useMutation({
    mutationFn: () => api.delete('/api/user/chats'),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['chat-sessions'] }); qc.invalidateQueries({ queryKey: ['chat-sessions-archived'] }); },
  });

  function openContextMenu(e: React.MouseEvent | React.TouchEvent, session: ChatSession) {
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    let x = 0, y = 0;
    if ('clientX' in e) { x = e.clientX; y = e.clientY; }
    else { x = rect.left + 40; y = rect.bottom; }
    setCtxMenu({ sessionId: session.session_id, x, y });
    setCtxSession(session);
  }

  function closeMenu() { setCtxMenu(null); setCtxSession(null); }

  const handleContinue = (session: ChatSession) => {
    loadSessionIntoChat(session);
    navigate('/chat');
  };

  // Group pinned + unpinned
  const pinned   = sessions.filter((s) => s.is_pinned);
  const unpinned = sessions.filter((s) => !s.is_pinned);
  const groups   = groupByDate(unpinned);

  return (
    <div className="max-w-2xl mx-auto px-4 py-6 space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">Chat History</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            {sessions.length} conversation{sessions.length !== 1 ? 's' : ''}
            {archived.length > 0 && ` · ${archived.length} archived`}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline" size="sm"
            className="text-amber-700 border-amber-200 hover:bg-amber-50"
            onClick={() => {
              localStorage.removeItem('brahm-chat-main-chat');
              localStorage.removeItem('brahm-session-brahm-chat-main-chat');
              navigate('/chat');
            }}
          >
            New Chat
          </Button>
          {sessions.length > 0 && (
            <Button
              variant="outline" size="sm"
              className="text-red-500 border-red-200 hover:bg-red-50"
              onClick={() => { if (confirm('Delete all chat history? This cannot be undone.')) deleteAll.mutate(); }}
              disabled={deleteAll.isPending}
            >
              <Trash2 className="h-3.5 w-3.5 mr-1.5" /> Delete All
            </Button>
          )}
        </div>
      </div>

      <p className="text-xs text-muted-foreground">
        Right-click (or long-press) a chat to pin, archive, rename, or delete it.
      </p>

      {/* Loading */}
      {isLoading && (
        <div className="space-y-2">
          {[1,2,3,4].map((i) => <div key={i} className="h-14 rounded-xl bg-muted/30 animate-pulse" />)}
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="text-sm text-red-500 bg-red-50 rounded-xl px-4 py-3">
          Could not load history. Please check your login.
        </div>
      )}

      {/* Empty */}
      {!isLoading && sessions.length === 0 && archived.length === 0 && !error && (
        <div className="text-center py-16 text-muted-foreground">
          <MessageSquare className="h-10 w-10 mx-auto mb-3 opacity-30" />
          <p className="text-sm">No chat history yet.</p>
          <p className="text-xs mt-1">Ask Brahm AI something — it remembers everything.</p>
          <Button size="sm" className="mt-4 bg-amber-600 hover:bg-amber-700 text-white" onClick={() => navigate('/chat')}>
            Start a conversation
          </Button>
        </div>
      )}

      {/* Pinned section */}
      {pinned.length > 0 && (
        <div>
          <div className="text-xs font-semibold text-amber-700 uppercase tracking-wide px-1 mb-1 flex items-center gap-1">
            📌 Pinned
          </div>
          <div className="bg-card border border-amber-200/60 rounded-xl overflow-hidden divide-y divide-border/30">
            {pinned.map((s) => (
              <SessionRow key={s.session_id} session={s} onContextMenu={openContextMenu} onContinue={() => handleContinue(s)} />
            ))}
          </div>
        </div>
      )}

      {/* Date-grouped */}
      {groups.map(({ label, items }) => (
        <div key={label}>
          <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wide px-1 mb-1">{label}</div>
          <div className="bg-card border border-border/40 rounded-xl overflow-hidden divide-y divide-border/30">
            {items.map((s) => (
              <SessionRow key={s.session_id} session={s} onContextMenu={openContextMenu} onContinue={() => handleContinue(s)} />
            ))}
          </div>
        </div>
      ))}

      {/* Archived section */}
      {archived.length > 0 && (
        <div>
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
                <SessionRow key={s.session_id} session={s} onContextMenu={openContextMenu} onContinue={() => handleContinue(s)} />
              ))}
            </div>
          )}
        </div>
      )}

      {/* Context menu */}
      {ctxMenu && ctxSession && (
        <div
          ref={menuRef}
          className="fixed z-50 bg-card border border-border rounded-xl shadow-xl py-1 min-w-[180px] animate-fade-in"
          style={{ left: Math.min(ctxMenu.x, window.innerWidth - 200), top: Math.min(ctxMenu.y, window.innerHeight - 220) }}
        >
          <CtxMenuItem
            icon={<Pin className="h-3.5 w-3.5" />}
            label={ctxSession.is_pinned ? 'Unpin' : 'Pin'}
            onClick={() => {
              patchMeta.mutate({ sessionId: ctxSession.session_id, body: { action: ctxSession.is_pinned ? 'unpin' : 'pin' } });
              closeMenu();
            }}
          />
          <CtxMenuItem
            icon={<Edit2 className="h-3.5 w-3.5" />}
            label="Rename"
            onClick={() => {
              setRenameValue(ctxSession.custom_name ?? ctxSession.messages.find((m) => m.role === 'user')?.content?.slice(0, 60) ?? '');
              setRenaming(true);
              closeMenu();
            }}
          />
          {ctxSession.is_archived ? (
            <CtxMenuItem
              icon={<ArchiveRestore className="h-3.5 w-3.5" />}
              label="Unarchive"
              onClick={() => { patchMeta.mutate({ sessionId: ctxSession.session_id, body: { action: 'unarchive' } }); closeMenu(); }}
            />
          ) : (
            <CtxMenuItem
              icon={<Archive className="h-3.5 w-3.5" />}
              label="Archive"
              onClick={() => { patchMeta.mutate({ sessionId: ctxSession.session_id, body: { action: 'archive' } }); closeMenu(); }}
            />
          )}
          <div className="h-px bg-border mx-2 my-1" />
          <CtxMenuItem
            icon={<Trash2 className="h-3.5 w-3.5" />}
            label="Delete"
            danger
            onClick={() => {
              if (confirm('Delete this chat?')) deleteSession.mutate(ctxSession.session_id);
              closeMenu();
            }}
          />
        </div>
      )}

      {/* Rename modal */}
      {renaming && ctxSession && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30">
          <div className="bg-card border border-border rounded-2xl shadow-xl p-6 w-full max-w-sm space-y-4">
            <h3 className="font-semibold text-base">Rename Chat</h3>
            <input
              autoFocus
              className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-500"
              value={renameValue}
              onChange={(e) => setRenameValue(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && renameValue.trim()) {
                  patchMeta.mutate({ sessionId: ctxSession.session_id, body: { action: 'rename', name: renameValue.trim() } });
                  setRenaming(false);
                }
              }}
            />
            <div className="flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => setRenaming(false)}>Cancel</Button>
              <Button size="sm" className="bg-amber-600 hover:bg-amber-700 text-white"
                disabled={!renameValue.trim()}
                onClick={() => {
                  patchMeta.mutate({ sessionId: ctxSession.session_id, body: { action: 'rename', name: renameValue.trim() } });
                  setRenaming(false);
                }}
              >
                Save
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function CtxMenuItem({ icon, label, danger, onClick }: { icon: React.ReactNode; label: string; danger?: boolean; onClick: () => void }) {
  return (
    <button
      className={`w-full flex items-center gap-2.5 px-4 py-2.5 text-sm hover:bg-muted transition-colors ${danger ? 'text-red-500 hover:bg-red-50' : 'text-foreground'}`}
      onClick={onClick}
    >
      {icon} {label}
    </button>
  );
}
