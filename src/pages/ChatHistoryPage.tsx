/**
 * ChatHistoryPage — User's personal chat history.
 * View and delete conversations grouped by page context.
 */
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Trash2, MessageSquare, ChevronDown, ChevronUp } from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

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
  gochar: 'Gochar',
  prashna: 'Prashna',
  varshphal: 'Varshphal',
  kp: 'KP System',
  'sade-sati': 'Sade Sati',
  dosha: 'Dosha',
  gemstones: 'Gemstones',
  rectification: 'Rectification',
};

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  } catch { return iso; }
}

function SessionCard({ session, onDelete }: { session: ChatSession; onDelete: () => void }) {
  const [expanded, setExpanded] = useState(false);
  const userMsgs = session.messages.filter((m) => m.role === 'user');

  return (
    <Card className="border-border/40">
      <CardHeader className="py-3 px-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <MessageSquare className="h-4 w-4 text-amber-600" />
            <span className="text-sm font-medium">
              {PAGE_LABELS[session.page_context] ?? session.page_context}
            </span>
            <span className="text-xs text-muted-foreground bg-muted/40 px-1.5 py-0.5 rounded">
              {userMsgs.length} message{userMsgs.length !== 1 ? 's' : ''}
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-xs text-muted-foreground">{formatDate(session.last_at)}</span>
            <button
              onClick={() => setExpanded((v) => !v)}
              className="p-1 rounded hover:bg-muted/40 text-muted-foreground transition-colors"
            >
              {expanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
            </button>
            <button
              onClick={onDelete}
              className="p-1 rounded hover:bg-red-50 text-muted-foreground hover:text-red-500 transition-colors"
              title="Delete this conversation"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
        {/* Preview of first user message */}
        {!expanded && userMsgs[0] && (
          <p className="text-xs text-muted-foreground mt-1 line-clamp-1">
            {userMsgs[0].content}
          </p>
        )}
      </CardHeader>

      {expanded && (
        <CardContent className="pt-0 pb-3 px-4">
          <div className="space-y-2 max-h-64 overflow-y-auto">
            {session.messages.map((msg, i) => (
              <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[85%] text-xs px-3 py-2 rounded-xl leading-relaxed ${
                  msg.role === 'user'
                    ? 'bg-amber-600 text-white'
                    : 'bg-muted/40 text-foreground'
                }`}>
                  {msg.content}
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      )}
    </Card>
  );
}

export default function ChatHistoryPage() {
  const qc = useQueryClient();

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

  return (
    <div className="max-w-2xl mx-auto px-4 py-6 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">Chat History</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            {sessions.length} conversation{sessions.length !== 1 ? 's' : ''}
          </p>
        </div>
        {sessions.length > 0 && (
          <Button
            variant="outline"
            size="sm"
            className="text-red-500 border-red-200 hover:bg-red-50"
            onClick={() => {
              if (confirm('Saari chat history delete karein? Yeh action undo nahi ho sakta.')) {
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

      {isLoading && (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-16 rounded-xl bg-muted/30 animate-pulse" />
          ))}
        </div>
      )}

      {error && (
        <div className="text-sm text-red-500 bg-red-50 rounded-xl px-4 py-3">
          History load nahi ho payi. Please login check karein.
        </div>
      )}

      {!isLoading && sessions.length === 0 && !error && (
        <div className="text-center py-16 text-muted-foreground">
          <MessageSquare className="h-10 w-10 mx-auto mb-3 opacity-30" />
          <p className="text-sm">Abhi tak koi chat history nahi hai.</p>
          <p className="text-xs mt-1">AI se kuch poochho — sab yaad rakha jayega.</p>
        </div>
      )}

      <div className="space-y-3">
        {sessions.map((s) => (
          <SessionCard
            key={`${s.session_id}-${s.page_context}`}
            session={s}
            onDelete={() => {
              if (s.session_id) {
                deleteSession.mutate(s.session_id);
              }
            }}
          />
        ))}
      </div>
    </div>
  );
}
