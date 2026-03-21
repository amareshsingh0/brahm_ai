/**
 * PageBot — floating AI assistant for every page.
 * Sends current page context + data to chat automatically.
 * User's kundali from store is always included (via useChat).
 */
import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Bot, Send, Loader2, ChevronDown, Trash2, UserCircle, X as XIcon, Plus } from 'lucide-react';
import { useChat } from '@/hooks/useChat';
import { useFactSheet } from '@/hooks/useFactSheet';
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
  const [showFacts, setShowFacts] = useState(false);
  const [factInput, setFactInput] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const factInputRef = useRef<HTMLInputElement>(null);

  const { facts, addFact, removeFact } = useFactSheet();

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
                <span className="text-xs text-muted-foreground bg-muted/40 px-1.5 py-0.5 rounded">{pageContext}</span>
              </div>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => { setShowFacts((v) => !v); setTimeout(() => factInputRef.current?.focus(), 100); }}
                  title="Meri baatein (about me)"
                  className={`p-1.5 rounded transition-colors ${showFacts ? 'bg-primary/20 text-primary' : 'hover:bg-muted/40 text-muted-foreground hover:text-foreground'}`}
                >
                  <UserCircle className="h-3.5 w-3.5" />
                </button>
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

            {/* Fact-sheet panel */}
            <AnimatePresence>
              {showFacts && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  transition={{ duration: 0.18 }}
                  className="overflow-hidden border-b border-border/30 bg-muted/10"
                >
                  <div className="px-3 py-2 space-y-1.5">
                    <p className="text-[10px] text-muted-foreground font-medium uppercase tracking-wide">Meri baatein (AI ko yaad rahega)</p>
                    <div className="flex flex-wrap gap-1">
                      {facts.map((f, i) => (
                        <span key={i} className="flex items-center gap-1 text-[10px] bg-primary/10 text-primary px-2 py-0.5 rounded-full">
                          {f}
                          <button onClick={() => removeFact(i)} className="hover:text-destructive transition-colors">
                            <XIcon className="h-2.5 w-2.5" />
                          </button>
                        </span>
                      ))}
                    </div>
                    <div className="flex gap-1">
                      <input
                        ref={factInputRef}
                        value={factInput}
                        onChange={(e) => setFactInput(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter' && factInput.trim()) { addFact(factInput); setFactInput(''); } }}
                        placeholder="e.g. 28 saal, software engineer, married"
                        className="flex-1 text-[10px] h-7 px-2 rounded bg-muted/30 border border-border/30 outline-none focus:border-primary/50"
                      />
                      <button
                        onClick={() => { if (factInput.trim()) { addFact(factInput); setFactInput(''); } }}
                        className="h-7 w-7 flex items-center justify-center rounded bg-primary/20 hover:bg-primary/30 text-primary transition-colors"
                      >
                        <Plus className="h-3 w-3" />
                      </button>
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

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

              {messages.map((msg, i) => {
                // Parse confidence tag from assistant messages
                let displayContent = msg.content;
                let confidence: 'HIGH' | 'MEDIUM' | 'LOW' | null = null;
                if (msg.role === 'assistant' && msg.content) {
                  const match = msg.content.match(/\[CONFIDENCE:\s*(HIGH|MEDIUM|LOW)\]/i);
                  if (match) {
                    confidence = match[1].toUpperCase() as 'HIGH' | 'MEDIUM' | 'LOW';
                    displayContent = msg.content.replace(/\n?\[CONFIDENCE:\s*(HIGH|MEDIUM|LOW)\]\n?/i, '').trim();
                  }
                }
                const confColor = confidence === 'HIGH' ? 'text-green-400 bg-green-400/10' : confidence === 'MEDIUM' ? 'text-yellow-400 bg-yellow-400/10' : 'text-red-400 bg-red-400/10';
                return (
                  <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                    <div className={`max-w-[85%] rounded-xl px-3 py-2 text-xs leading-relaxed whitespace-pre-wrap ${
                      msg.role === 'user'
                        ? 'bg-primary text-primary-foreground'
                        : 'bg-muted/40 text-foreground'
                    }`}>
                      {displayContent || (streaming && i === messages.length - 1 ? (
                        <Loader2 className="h-3 w-3 animate-spin" />
                      ) : '')}
                      {confidence && (
                        <span className={`inline-block mt-1.5 text-[9px] font-medium px-1.5 py-0.5 rounded ${confColor}`}>
                          ◈ {confidence} confidence
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}

              {sources.length > 0 && (
                <div className="text-xs text-muted-foreground border-t border-border/20 pt-2">
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
