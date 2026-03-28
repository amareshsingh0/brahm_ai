/**
 * PageBot — floating AI assistant for every page.
 * Features: birth data form, save kundali prompt, follow-up loop system.
 */
import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Bot, Send, Loader2, ChevronDown, Trash2, UserCircle, X as XIcon, Plus, BookOpen, Save, CheckCircle, Copy, RefreshCw } from 'lucide-react';
import { useChat, type BirthFormData, type SaveKundaliPromptData } from '@/hooks/useChat';
import { useFactSheet } from '@/hooks/useFactSheet';
import { useKundliStore } from '@/store/kundliStore';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

// ── Markdown renderer ────────────────────────────────────────────────────────
type MdBlock =
  | { type: 'heading'; text: string }
  | { type: 'para'; text: string }
  | { type: 'quote'; text: string }
  | { type: 'bullet'; text: string }
  | { type: 'numbered'; n: number; text: string }
  | { type: 'callout'; label: string; text: string }
  | { type: 'divider'; label?: string };

function parseMarkdown(raw: string): MdBlock[] {
  const lines = raw.trim().split('\n');
  const blocks: MdBlock[] = [];
  let numIdx = 0;

  for (const line of lines) {
    const t = line.trim();
    if (!t) { numIdx = 0; continue; }
    if (t.startsWith('### ')) { blocks.push({ type: 'heading', text: t.slice(4) }); continue; }
    if (t.startsWith('## '))  { blocks.push({ type: 'heading', text: t.slice(3) }); continue; }
    if (t.startsWith('# '))   { blocks.push({ type: 'heading', text: t.slice(2) }); continue; }
    if (t.startsWith('> '))   { blocks.push({ type: 'quote',   text: t.slice(2) }); continue; }
    if (t.startsWith('- ') || t.startsWith('• ') || t.startsWith('* ')) {
      blocks.push({ type: 'bullet', text: t.slice(2).trim() }); numIdx = 0; continue;
    }
    const numMatch = t.match(/^(\d+)\.\s+(.*)/);
    if (numMatch) {
      numIdx++;
      blocks.push({ type: 'numbered', n: numIdx, text: numMatch[2] }); continue;
    }
    if (t === '---' || t === '***') { blocks.push({ type: 'divider' }); continue; }
    if (/^[💡📌✨⚠️]/.test(t)) {
      blocks.push({ type: 'callout', label: 'Note', text: t.replace(/^[💡📌✨⚠️]\s*/, '') }); continue;
    }
    blocks.push({ type: 'para', text: t }); numIdx = 0;
  }
  return blocks.filter(b => b.type !== 'para' || (b as { type: 'para'; text: string }).text.trim());
}

function InlineText({ text }: { text: string }) {
  const parts = text.split(/(\*\*.*?\*\*|\*.*?\*|`.*?`)/g);
  return (
    <>
      {parts.map((p, i) => {
        if (p.startsWith('**') && p.endsWith('**'))
          return <strong key={i} className="font-semibold text-foreground">{p.slice(2, -2)}</strong>;
        if (p.startsWith('*') && p.endsWith('*'))
          return <em key={i} className="italic text-muted-foreground">{p.slice(1, -1)}</em>;
        if (p.startsWith('`') && p.endsWith('`'))
          return <code key={i} className="bg-muted/60 text-[10px] px-1 py-0.5 rounded font-mono">{p.slice(1, -1)}</code>;
        return <span key={i}>{p}</span>;
      })}
    </>
  );
}

function RichAiCard({ content }: { content: string }) {
  const blocks = parseMarkdown(content);
  const isRich = content.length > 200 || content.includes('\n') || content.includes('**') ||
    content.includes('\n- ') || content.includes('\n• ') || content.includes('\n# ');

  if (!isRich) {
    return (
      <div className="max-w-[88%] rounded-tl rounded-tr-2xl rounded-br-2xl rounded-bl-2xl bg-white border border-border/50 shadow-sm px-3 py-2.5 text-xs leading-relaxed text-foreground">
        <InlineText text={content} />
      </div>
    );
  }

  return (
    <div className="w-full rounded-xl border border-border/40 shadow-sm overflow-hidden bg-white">
      <div className="h-0.5 bg-gradient-to-r from-amber-600 via-orange-500 to-transparent" />
      <div className="flex items-center gap-1.5 px-3.5 pt-2.5 pb-1">
        <div className="w-1.5 h-1.5 rounded-sm bg-gradient-to-br from-amber-500 to-orange-600 shrink-0" />
        <span className="text-[8px] font-bold uppercase tracking-widest text-amber-700/70">Brahm AI</span>
      </div>
      <div className="px-3.5 pb-3.5 flex flex-col gap-2">
        {blocks.map((b, i) => {
          switch (b.type) {
            case 'heading':
              return (
                <div key={i} className="flex items-center gap-2">
                  <div className="w-[3px] h-4 rounded-full bg-amber-500 shrink-0" />
                  <p className="text-[13px] font-bold text-foreground leading-snug tracking-tight">
                    <InlineText text={b.text} />
                  </p>
                </div>
              );
            case 'para':
              return (
                <p key={i} className="text-[13px] leading-[1.75] text-[#1a1a1a]">
                  <InlineText text={b.text} />
                </p>
              );
            case 'quote':
              return (
                <div key={i} className="flex gap-0 rounded-lg overflow-hidden border border-amber-200/60">
                  <div className="w-[3px] bg-amber-400 shrink-0" />
                  <div className="bg-amber-50/80 px-2.5 py-2 text-[12.5px] italic leading-[1.7] text-amber-900 flex-1">
                    <InlineText text={b.text} />
                  </div>
                </div>
              );
            case 'bullet':
              return (
                <div key={i} className="flex items-start gap-2.5">
                  <span className="mt-[9px] w-[5px] h-[5px] rounded-full bg-amber-500 shrink-0" />
                  <p className="text-[13px] leading-[1.75] text-[#1a1a1a] flex-1">
                    <InlineText text={b.text} />
                  </p>
                </div>
              );
            case 'numbered':
              return (
                <div key={i} className="flex items-start gap-2">
                  <span className="text-[11px] font-bold text-amber-600 shrink-0 mt-0.5 min-w-[16px]">{b.n}.</span>
                  <p className="text-[13px] leading-[1.75] text-[#1a1a1a] flex-1">
                    <InlineText text={b.text} />
                  </p>
                </div>
              );
            case 'callout':
              return (
                <div key={i} className="flex items-start gap-2 bg-amber-50 border border-amber-200 rounded-xl px-3 py-2.5">
                  <span className="text-sm shrink-0">💡</span>
                  <p className="text-[12px] leading-[1.7] text-amber-900 font-medium flex-1">
                    <InlineText text={b.text} />
                  </p>
                </div>
              );
            case 'divider':
              return (
                <div key={i} className="flex items-center gap-2 py-1">
                  <div className="flex-1 h-px bg-border/40" />
                  <div className="flex gap-1">
                    <div className="w-1 h-1 rounded-full bg-amber-300" />
                    <div className="w-1 h-1 rounded-full bg-amber-400" />
                    <div className="w-1 h-1 rounded-full bg-amber-300" />
                  </div>
                  <div className="flex-1 h-px bg-border/40" />
                </div>
              );
            default: return null;
          }
        })}
      </div>
    </div>
  );
}

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
    sendMessage, submitBirthForm, dismissSavePrompt, clearHistory, regenerate,
  } = useChat({ pageContext, pageData, persistKey: pageContext });

  const [copiedIdx, setCopiedIdx] = useState<number | null>(null);
  const copyMessage = (text: string, idx: number) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopiedIdx(idx);
      setTimeout(() => setCopiedIdx(null), 1500);
    });
  };

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
            className="fixed inset-0 z-[60] bg-black/70"
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
                const showActions = msg.role === 'assistant' && msg.content && (msg.isComplete || !streaming);

                return (
                  <div key={i} className={`flex flex-col ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                    {msg.role === 'user' ? (
                      <div className="max-w-[85%] rounded-tl-2xl rounded-tr rounded-bl-2xl rounded-br-2xl bg-primary text-primary-foreground px-3 py-2 text-xs leading-relaxed">
                        {msg.content}
                      </div>
                    ) : msg.content ? (
                      <>
                        {msg.isComplete ? (
                          <RichAiCard content={msg.content} />
                        ) : (
                          <div className="w-full rounded-xl border border-border/40 shadow-sm bg-white overflow-hidden">
                            <div className="h-0.5 bg-gradient-to-r from-amber-600 via-orange-500 to-transparent" />
                            <div className="flex items-center gap-1.5 px-3 pt-2 pb-0.5">
                              <div className="w-1.5 h-1.5 rounded-sm bg-gradient-to-br from-amber-500 to-orange-600 shrink-0" />
                              <span className="text-[8px] font-bold uppercase tracking-widest text-amber-700/70">Brahm AI</span>
                            </div>
                            <div className="px-3 pb-3 pt-1 text-[11.5px] leading-relaxed text-[#3a3a3a] whitespace-pre-wrap">
                              {msg.content}<span className="inline-block w-0.5 h-3.5 bg-amber-500 ml-0.5 animate-pulse align-middle" />
                            </div>
                          </div>
                        )}
                      </>
                    ) : streaming && isLastAssistant ? (
                      <div className="flex items-center gap-1.5 px-3 py-2 bg-white border border-border/50 rounded-tl rounded-tr-2xl rounded-br-2xl rounded-bl-2xl shadow-sm">
                        <Loader2 className="h-3 w-3 animate-spin text-amber-600" />
                        <span className="text-[10px] text-muted-foreground">Thinking…</span>
                      </div>
                    ) : null}

                    {showActions && (
                      <div className="flex items-center gap-1 mt-1">
                        <button
                          onClick={() => copyMessage(msg.content, i)}
                          title="Copy"
                          className="flex items-center gap-1 text-[10px] text-muted-foreground hover:text-foreground px-1.5 py-0.5 rounded transition-colors"
                        >
                          {copiedIdx === i ? <CheckCircle className="h-3 w-3 text-green-500" /> : <Copy className="h-3 w-3" />}
                          {copiedIdx === i ? 'Copied' : 'Copy'}
                        </button>
                        {isLastAssistant && !streaming && (
                          <button
                            onClick={regenerate}
                            title="Regenerate"
                            className="flex items-center gap-1 text-[10px] text-muted-foreground hover:text-foreground px-1.5 py-0.5 rounded transition-colors"
                          >
                            <RefreshCw className="h-3 w-3" />
                            Regenerate
                          </button>
                        )}
                      </div>
                    )}
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
