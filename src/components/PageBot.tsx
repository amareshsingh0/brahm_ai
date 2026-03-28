/**
 * PageBot — floating AI assistant for every page.
 * Features: birth data form, save kundali prompt, follow-up loop system.
 */
import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Bot, Send, Loader2, ChevronDown, Trash2, UserCircle, X as XIcon, Plus, BookOpen, Save, CheckCircle } from 'lucide-react';
import { useChat, type BirthFormData, type SaveKundaliPromptData } from '@/hooks/useChat';
import { useFactSheet } from '@/hooks/useFactSheet';
import { useKundliStore } from '@/store/kundliStore';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

const PAGE_SUGGESTIONS: Record<string, string[]> = {
  kundali:       ['Sabse strong graha kaun hai?', 'Agle saal kaisa rahega?', 'Career ke liye best period?'],
  panchang:      ['Aaj ka din kaisa rahega?', 'Rahukaal mein kya nahi karna chahiye?', 'Aaj ki tithi ka mahatva?'],
  compatibility: ['Yeh score achha hai?', 'Nadi dosha ke upay?', 'Vivah ke liye sahi samay?'],
  sky:           ['Aaj ke graha mujhpe kaisa asar karenge?', 'Kaunsa graha vakri hai?', 'Shani ka asar kaisa rahega?'],
  palmistry:     ['Meri jeewan rekha kaisi hai?', 'Career line kya kehti hai?', 'Vivah rekha?'],
  horoscope:     ['Aaj ka din kaisa rahega?', 'Is mahine ka overview?', 'Lucky time kab hai?'],
  general:       ['Meri kundali banao', 'Aaj ka panchang?', 'Shaadi ka shubh muhurta?'],
};


interface PageBotProps {
  pageContext?: string;
  pageData?: Record<string, unknown>;
}

// ── Birth Data Form ────────────────────────────────────────────────────────────
function BirthForm({ onSubmit }: { onSubmit: (data: BirthFormData) => void }) {
  const [form, setForm] = useState<BirthFormData>({ name: '', gender: '', date: '', time: '', place: '' });
  const set = (k: keyof BirthFormData) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));
  const valid = form.date && form.time && form.place;

  return (
    <div className="px-3 py-3 space-y-2 border-t border-border/30 bg-muted/10">
      <p className="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">Apna Janam Vivran daalo</p>
      <div className="grid grid-cols-2 gap-1.5">
        <input
          value={form.name}
          onChange={set('name')}
          placeholder="Naam (optional)"
          className="col-span-2 text-xs h-8 px-2 rounded-lg bg-muted/30 border border-border/30 outline-none focus:border-primary/50"
        />
        <select
          value={form.gender}
          onChange={set('gender') as (e: React.ChangeEvent<HTMLSelectElement>) => void}
          className="text-xs h-8 px-2 rounded-lg bg-muted/30 border border-border/30 outline-none focus:border-primary/50"
        >
          <option value="">Gender</option>
          <option value="Male">Male</option>
          <option value="Female">Female</option>
          <option value="Other">Other</option>
        </select>
        <input
          value={form.date}
          onChange={set('date')}
          type="date"
          placeholder="Janam Tithi"
          className="text-xs h-8 px-2 rounded-lg bg-muted/30 border border-border/30 outline-none focus:border-primary/50"
        />
        <input
          value={form.time}
          onChange={set('time')}
          type="time"
          placeholder="Janam Samay"
          className="text-xs h-8 px-2 rounded-lg bg-muted/30 border border-border/30 outline-none focus:border-primary/50"
        />
        <input
          value={form.place}
          onChange={set('place')}
          placeholder="Janam Sthan (city)"
          className="text-xs h-8 px-2 rounded-lg bg-muted/30 border border-border/30 outline-none focus:border-primary/50"
        />
      </div>
      <Button
        size="sm"
        className="w-full h-8 text-xs"
        disabled={!valid}
        onClick={() => onSubmit(form)}
      >
        <BookOpen className="h-3 w-3 mr-1.5" />
        Kundali Banao
      </Button>
    </div>
  );
}

// ── Save Kundali Prompt ────────────────────────────────────────────────────────
function SaveKundaliCard({
  data,
  onConfirm,
  onDismiss,
}: {
  data: SaveKundaliPromptData;
  onConfirm: () => void;
  onDismiss: () => void;
}) {
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const { setBirthDetails, setKundaliData } = useKundliStore();

  const handleSave = async () => {
    setSaving(true);
    try {
      const resp = await api.post<Record<string, unknown>>('/api/kundali', {
        date: data.birth_date,
        time: data.birth_time,
        lat: data.birth_lat,
        lon: data.birth_lon,
        tz: data.birth_tz,
        name: data.name,
        place: data.place,
      });
      setBirthDetails({
        name: data.name,
        dateOfBirth: data.birth_date,
        timeOfBirth: data.birth_time,
        birthPlace: data.place,
        lat: data.birth_lat,
        lon: data.birth_lon,
        tz: data.birth_tz,
      });
      setKundaliData(resp as Parameters<typeof setKundaliData>[0]);
      setSaved(true);
      setTimeout(onDismiss, 1800);
    } catch {
      setSaving(false);
    }
  };

  return (
    <div className="mx-3 mb-2 rounded-xl border border-primary/20 bg-primary/5 px-3 py-2.5 space-y-2">
      {saved ? (
        <div className="flex items-center gap-2 text-xs text-green-600">
          <CheckCircle className="h-3.5 w-3.5" />
          Kundali save ho gayi! "My Kundali" mein dekh sakte ho.
        </div>
      ) : (
        <>
          <p className="text-xs text-foreground font-medium">
            🌟 Kya aap is kundali ko <b>My Kundali</b> mein save karna chahoge?
          </p>
          <p className="text-[10px] text-muted-foreground">
            {data.name && `${data.name} · `}{data.birth_date} · {data.birth_time} · {data.place}
          </p>
          <div className="flex gap-2">
            <Button size="sm" className="h-7 text-xs flex-1" onClick={handleSave} disabled={saving}>
              {saving ? <Loader2 className="h-3 w-3 animate-spin" /> : <><Save className="h-3 w-3 mr-1" /> Haan, save karo</>}
            </Button>
            <button onClick={onDismiss} className="text-xs text-muted-foreground hover:text-foreground px-2">
              Nahi
            </button>
          </div>
        </>
      )}
    </div>
  );
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

  const {
    messages, sources, streaming,
    showBirthForm, saveKundaliPrompt,
    sendMessage, submitBirthForm, dismissSavePrompt, clearHistory,
  } = useChat({ pageContext, pageData, persistKey: pageContext });

  const suggestions = PAGE_SUGGESTIONS[pageContext] ?? PAGE_SUGGESTIONS.general;
  const hasMessages = messages.length > 0;

  useEffect(() => {
    if (open) {
      setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
    }
  }, [messages, open, showBirthForm, saveKundaliPrompt]);

  useEffect(() => {
    if (open) {
      setTimeout(() => inputRef.current?.focus(), 100);
    }
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
            className="fixed bottom-20 right-4 md:bottom-6 md:right-6 z-[55] w-12 h-12 rounded-full bg-primary shadow-lg shadow-primary/30 flex items-center justify-center hover:scale-110 transition-transform"
            aria-label="Open AI Assistant"
          >
            <Bot className="h-5 w-5 text-primary-foreground" />
          </motion.button>
        )}
      </AnimatePresence>

      {/* Bottom sheet backdrop */}
      <AnimatePresence>
        {open && (
          <motion.div
            key="backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-[60] bg-black/40"
            onClick={() => setOpen(false)}
          />
        )}
      </AnimatePresence>

      {/* Chat panel — bottom sheet */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ y: "100%" }}
            animate={{ y: 0 }}
            exit={{ y: "100%" }}
            transition={{ type: "spring", damping: 30, stiffness: 300 }}
            className="fixed bottom-0 left-0 right-0 md:left-auto md:right-6 md:w-[420px] z-[70] flex flex-col bg-background rounded-t-2xl md:rounded-2xl border-t md:border border-x border-border/40 shadow-2xl overflow-hidden"
            style={{ maxHeight: "85dvh" }}
          >
            {/* Drag handle */}
            <div className="flex justify-center pt-3 pb-1 shrink-0">
              <div className="w-10 h-1 rounded-full bg-border/60" />
            </div>

            {/* Header */}
            <div className="flex items-center justify-between px-4 py-2.5 border-b border-border/30 bg-muted/20 shrink-0">
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
                  className="overflow-hidden border-b border-border/30 bg-muted/10 shrink-0"
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
            <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3 min-h-[200px]">
              {!hasMessages && !showBirthForm && (
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
                const isLastAssistant = msg.role === 'assistant' && i === messages.length - 1;
                const showFollowups = msg.isComplete && (msg.followUps?.length ?? 0) > 0;

                return (
                  <div key={i} className={`flex flex-col ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                    <div className={`max-w-[85%] rounded-xl px-3 py-2 text-xs leading-relaxed whitespace-pre-wrap ${
                      msg.role === 'user' ? 'bg-primary text-primary-foreground' : 'bg-muted/40 text-foreground'
                    }`}>
                      {msg.content || (streaming && isLastAssistant ? <Loader2 className="h-3 w-3 animate-spin" /> : '')}
                    </div>
                    {showFollowups && (
                      <div className="flex flex-wrap gap-1.5 mt-1.5 max-w-[90%]">
                        {msg.followUps!.map((q, qi) => (
                          <button
                            key={qi}
                            onClick={() => sendMessage(q)}
                            className="text-[10px] px-2.5 py-1 rounded-full bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200 hover:border-amber-300 transition-all leading-none"
                          >
                            {q}
                          </button>
                        ))}
                      </div>
                    )}
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

            {/* Save Kundali Prompt — above input */}
            {saveKundaliPrompt && (
              <SaveKundaliCard data={saveKundaliPrompt} onConfirm={() => {}} onDismiss={dismissSavePrompt} />
            )}

            {/* Birth Form OR Input */}
            {showBirthForm ? (
              <BirthForm onSubmit={submitBirthForm} />
            ) : (
              <div className="px-3 py-3 border-t border-border/30 bg-muted/10 shrink-0">
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
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
