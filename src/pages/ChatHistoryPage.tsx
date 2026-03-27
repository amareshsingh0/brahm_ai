/**
 * ChatHistoryPage — User's personal chat history.
 * Date-grouped sessions, continue a past chat, per-session delete.
 */
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Trash2, MessageSquare, Sparkles, ChevronRight, PlusCircle } from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';

interface ChatMsg {
  role: string;
  content: string;
  created_at: string;
}

interface ChatSession {
  session_id: string;
  page_context: string;
  last_at: string;
  messages: ChatMsg[];
}

const PAGE_LABELS: Record<string, string> = {
  kundali: 'Kundali',
  panchang: 'Panchang',
  compatibility: 'Compatibility',
  sky: 'Live Sky',
  horoscope: 'Horoscope',
  palmistry: 'Palmistry',
  general: 'General',
  chat: 'Chat',
  gochar: 'Gochar',
  prashna: 'Prashna',
  varshphal: 'Varshphal',
  kp: 'KP System',
  'sade-sati': 'Sade Sati',
  dosha: 'Dosha',
  gemstones: 'Gemstones',
  rectification: 'Rectification',
};

function getDateLabel(iso: string): string {
  const d = new Date(iso);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today.getTime() - 86400000);
  const sessionDay = new Date(d.getFullYear(), d.getMonth(), d.getDate());

  if (sessionDay.getTime() === today.getTime()) return 'Today';
  if (sessionDay.getTime() === yesterday.getTime()) return 'Yesterday';

  const diff = today.getTime() - sessionDay.getTime();
  if (diff < 7 * 86400000) {
    return d.toLocaleDateString('en-IN', { weekday: 'long' });
  }
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatTime(iso: string) {
  try {
    return new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' });
  } catch { return ''; }
}

function groupByDate(sessions: ChatSession[]): { label: string; items: ChatSession[] }[] {
  const map = new Map<string, ChatSession[]>();
  for (const s of sessions) {
    const label = getDateLabel(s.last_at);
    if (!map.has(label)) map.set(label, []);
    map.get(label)!.push(s);
  }
  return Array.from(map.entries()).map(([label, items]) => ({ label, items }));
}

// Load a past session into localStorage so AIChatPage can continue it
function loadSessionIntoChat(session: ChatSession) {
  const storageKey = 'brahm-chat-main-chat';
  const sessionKey = 'brahm-session-brahm-chat-main-chat';

  // API returns newest-first; reverse to oldest-first for chat display
  const msgs = session.messages
    .slice()
    .reverse()
    .map((m) => ({ role: m.role, content: m.content }));

  localStorage.setItem(storageKey, JSON.stringify(msgs));
  localStorage.setItem(sessionKey, session.session_id);
}

function SessionRow({
  session,
  onDelete,
  onContinue,
}: {
  session: ChatSession;
  onDelete: () => void;
  onContinue: () => void;
}) {
  const userMsgs = session.messages.filter((m) => m.role === 'user');
  const preview = userMsgs[0]?.content?.slice(0, 72) ?? '…';
  const msgCount = userMsgs.length;
  const contextLabel = PAGE_LABELS[session.page_context] ?? session.page_context;
  const canContinue = session.page_context === 'general' || session.page_context === 'chat';

  return (
    <div
      className="flex items-center gap-3 px-4 py-3 hover:bg-amber-50/50 transition-colors group cursor-pointer"
      onClick={canContinue ? onContinue : undefined}
    >
      {/* Avatar */}
      <div className="w-9 h-9 rounded-full bg-amber-100 flex items-center justify-center flex-shrink-0">
        <Sparkles className="h-4 w-4 text-amber-700" />
      </div>

      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-0.5">
          <span className="text-xs font-medium text-amber-700 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded">
            {contextLabel}
          </span>
          <span className="text-xs text-muted-foreground">{formatTime(session.last_at)}</span>
          <span className="text-xs text-muted-foreground">
            · {msgCount} msg{msgCount !== 1 ? 's' : ''}
          </span>
        </div>
        <p className="text-sm text-foreground/80 truncate">{preview}</p>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
        <button
          onClick={(e) => { e.stopPropagation(); onDelete(); }}
          className="p-1.5 rounded-lg hover:bg-red-50 text-muted-foreground hover:text-red-500 transition-colors"
          title="Delete"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </button>
        {canContinue && (
          <button
            onClick={(e) => { e.stopPropagation(); onContinue(); }}
            className="p-1.5 rounded-lg hover:bg-amber-100 text-muted-foreground hover:text-amber-700 transition-colors"
            title="Continue chat"
          >
            <ChevronRight className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
    </div>
  );
}

export default function ChatHistoryPage() {
  const qc = useQueryClient();
  const navigate = useNavigate();

  const { data, isLoading, error } = useQuery({
    queryKey: ['chat-sessions'],
    queryFn: () => api.get<{ sessions: ChatSession[] }>('/api/user/chats/sessions'),
  });

  const deleteSession = useMutation({
    mutationFn: (sessionId: string) =>
      api.delete(`/api/user/chats/session/${encodeURIComponent(sessionId)}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['chat-sessions'] }),
  });

  const deleteAll = useMutation({
    mutationFn: () => api.delete('/api/user/chats'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['chat-sessions'] }),
  });

  const sessions = data?.sessions ?? [];
  const groups = groupByDate(sessions);

  const handleContinue = (session: ChatSession) => {
    loadSessionIntoChat(session);
    navigate('/chat');
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-6 space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">Chat History</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            {sessions.length} conversation{sessions.length !== 1 ? 's' : ''}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            className="text-amber-700 border-amber-200 hover:bg-amber-50"
            onClick={() => {
              localStorage.removeItem('brahm-chat-main-chat');
              localStorage.removeItem('brahm-session-brahm-chat-main-chat');
              navigate('/chat');
            }}
          >
            <PlusCircle className="h-3.5 w-3.5 mr-1.5" />
            New Chat
          </Button>
          {sessions.length > 0 && (
            <Button
              variant="outline"
              size="sm"
              className="text-red-500 border-red-200 hover:bg-red-50"
              onClick={() => {
                if (confirm('Delete all chat history? This cannot be undone.')) {
                  deleteAll.mutate();
                }
              }}
              disabled={deleteAll.isPending}
            >
              <Trash2 className="h-3.5 w-3.5 mr-1.5" />
              Delete All
            </Button>
          )}
        </div>
      </div>

      {/* Loading skeleton */}
      {isLoading && (
        <div className="space-y-2">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-14 rounded-xl bg-muted/30 animate-pulse" />
          ))}
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="text-sm text-red-500 bg-red-50 rounded-xl px-4 py-3">
          Could not load history. Please check your login.
        </div>
      )}

      {/* Empty state */}
      {!isLoading && sessions.length === 0 && !error && (
        <div className="text-center py-16 text-muted-foreground">
          <MessageSquare className="h-10 w-10 mx-auto mb-3 opacity-30" />
          <p className="text-sm">No chat history yet.</p>
          <p className="text-xs mt-1">Ask Brahm AI something — it remembers everything.</p>
          <Button
            size="sm"
            className="mt-4 bg-amber-600 hover:bg-amber-700 text-white"
            onClick={() => navigate('/chat')}
          >
            Start a conversation
          </Button>
        </div>
      )}

      {/* Date-grouped sessions */}
      {groups.map(({ label, items }) => (
        <div key={label}>
          <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wide px-1 mb-1">
            {label}
          </div>
          <div className="bg-card border border-border/40 rounded-xl overflow-hidden divide-y divide-border/30">
            {items.map((s) => (
              <SessionRow
                key={`${s.session_id}-${s.page_context}`}
                session={s}
                onDelete={() => {
                  if (s.session_id) deleteSession.mutate(s.session_id);
                }}
                onContinue={() => handleContinue(s)}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
