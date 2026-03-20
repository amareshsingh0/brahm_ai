/**
 * PageBot — floating AI assistant for every page.
 * Sends current page context + data to chat automatically.
 * User's kundali from store is always included (via useChat).
 */
import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Bot, X, Send, Loader2, ChevronDown, Trash2 } from 'lucide-react';
import { useChat } from '@/hooks/useChat';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

const PAGE_SUGGESTIONS: Record<string, string[]> = {
  kundali:       ['Sabse strong graha kaun hai?', 'Agle saal kaisa rahega?', 'Career ke liye best period?'],
  panchang:      ['Aaj ka din kaisa rahega?', 'Rahukaal mein kya nahi karna chahiye?', 'Aaj ki tithi ka mahatva?'],
  compatibility: ['Yeh score achha hai?', 'Nadi dosha ke upay?', 'Vivah ke liye sahi samay?'],
  sky:           ['Aaj ke graha mujhpe kaisa asar karenge?', 'Kaunsa graha vakri hai?', 'Shani ka asar kaisa rahega?'],
  palmistry:     ['Meri jeewan rekha kaisi hai?', 'Career line kya kehti hai?', 'Vivah rekha?'],
  horoscope:     ['Aaj ka din kaisa rahega?', 'Is mahine ka overview?', 'Lucky time kab hai?'],
  general:       ['Meri kundali batao', 'Aaj ka panchang?', 'Shaadi ka shubh muhurta?'],
};

interface PageBotProps {
  pageContext?: string;
  pageData?: Record<string, unknown>;
}

export default function PageBot({ pageContext = 'general', pageData = {} }: PageBotProps) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const { messages, sources, streaming, sendMessage, clearHistory } = useChat({
    pageContext,
    pageData,
    persistKey: pageContext,
  });

  const suggestions = PAGE_SUGGESTIONS[pageContext] ?? PAGE_SUGGESTIONS.general;
  const hasMessages = messages.length > 0;

  useEffect(() => {
    if (open) {
      setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
    }
  }, [messages, open]);

  useEffect(() => {
    if (open) setTimeout(() => inputRef.current?.focus(), 100);
  }, [open]);

  const handleSend = () => {
    const text = input.trim();
    if (!text || streaming) return;
    setInput('');
    sendMessage(text);
  };

  const handleKey = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); }
  };

  return (
    <>
      {/* Floating button */}
      <AnimatePresence>
        {!open && (
          <motion.button
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            onClick={() => setOpen(true)}
            className="fixed bottom-20 right-4 md:bottom-6 md:right-6 z-50 w-12 h-12 rounded-full bg-primary shadow-lg shadow-primary/30 flex items-center justify-center hover:scale-110 transition-transform"
            aria-label="Open AI Assistant"
          >
            <Bot className="h-5 w-5 text-primary-foreground" />
          </motion.button>
        )}
      </AnimatePresence>

      {/* Chat panel */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.95 }}
            transition={{ duration: 0.2 }}
            className="fixed bottom-20 right-4 md:bottom-6 md:right-6 z-50 w-[340px] sm:w-[380px] h-[520px] flex flex-col cosmic-card rounded-2xl border border-border/40 shadow-2xl overflow-hidden"
          >
            {/* Header */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-border/30 bg-muted/20">
              <div className="flex items-center gap-2">
                <Bot className="h-4 w-4 text-primary" />
                <span className="text-sm font-medium text-primary">Brahm AI</span>
                <span className="text-[10px] text-muted-foreground bg-muted/40 px-1.5 py-0.5 rounded">{pageContext}</span>
              </div>
              <div className="flex items-center gap-1">
                {hasMessages && (
                  <button onClick={clearHistory} className="p-1.5 rounded hover:bg-muted/40 text-muted-foreground hover:text-foreground transition-colors">
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                )}
                <button onClick={() => setOpen(false)} className="p-1.5 rounded hover:bg-muted/40 text-muted-foreground hover:text-foreground transition-colors">
                  <ChevronDown className="h-4 w-4" />
                </button>
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
              {!hasMessages && (
                <div className="space-y-3">
                  <p className="text-xs text-muted-foreground text-center pt-4">Kuch poochho...</p>
                  <div className="space-y-2">
                    {suggestions.map((s) => (
                      <button
                        key={s}
                        onClick={() => sendMessage(s)}
                        className="w-full text-left text-xs px-3 py-2 rounded-lg bg-muted/30 hover:bg-primary/10 hover:text-primary border border-border/20 transition-colors"
                      >
                        {s}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {messages.map((msg, i) => (
                <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                  <div className={`max-w-[85%] rounded-xl px-3 py-2 text-xs leading-relaxed whitespace-pre-wrap ${
                    msg.role === 'user'
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-muted/40 text-foreground'
                  }`}>
                    {msg.content || (streaming && i === messages.length - 1 ? (
                      <Loader2 className="h-3 w-3 animate-spin" />
                    ) : '')}
                  </div>
                </div>
              ))}

              {sources.length > 0 && (
                <div className="text-[10px] text-muted-foreground border-t border-border/20 pt-2">
                  Sources: {sources.slice(0, 2).map(s => s.book).join(', ')}
                </div>
              )}

              <div ref={bottomRef} />
            </div>

            {/* Input */}
            <div className="px-3 py-3 border-t border-border/30 bg-muted/10">
              <div className="flex gap-2">
                <Input
                  ref={inputRef}
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKey}
                  placeholder="Ask something..."
                  className="text-xs h-9 bg-muted/20 border-border/30 flex-1"
                  disabled={streaming}
                />
                <Button size="sm" className="h-9 w-9 p-0" onClick={handleSend} disabled={!input.trim() || streaming}>
                  {streaming ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Send className="h-3.5 w-3.5" />}
                </Button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
