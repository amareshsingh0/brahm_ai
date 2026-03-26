import { useRef, useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Send, Sparkles, User, AlertTriangle } from "lucide-react";
import { useChat } from "@/hooks/useChat";
import { useTranslation } from "react-i18next";
import type { ChatMessage } from "@/types/api";

const SUGGESTIONS = [
  "What does my moon sign say about me?",
  "Explain Sade Sati and its effects",
  "What are the most powerful Yogas in Kundali?",
  "Tell me about Jupiter in the 7th house",
];

export default function AIChatPage() {
  const { t } = useTranslation();
  const { messages: chatMessages, streaming, sendMessage: sendChatMessage } = useChat({
    pageContext: "chat",
    persistKey: "main-chat",
  });
  const [input, setInput] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [chatMessages, streaming]);

  const send = (text: string) => {
    if (!text.trim() || streaming) return;
    setInput("");
    sendChatMessage(text.trim());
  };

  // Typing indicator: streaming but last message is still empty
  const showTyping = streaming && chatMessages.length > 0 && chatMessages[chatMessages.length - 1].content === "";
  // Visible messages: skip empty in-progress assistant bubble (replaced by typing dots)
  const visibleMessages = chatMessages.filter(
    (m) => !(m.role === "assistant" && m.content === "" && streaming)
  );

  return (
    <div className="flex flex-col h-[calc(100vh-8rem)] sm:h-[calc(100vh-4rem)]">

      {/* ── Header ─────────────────────────────────────────────────── */}
      <div className="flex-shrink-0 mb-3">
        <div className="flex items-center gap-3 mb-2">
          <div className="w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center flex-shrink-0">
            <Sparkles className="h-5 w-5 text-amber-700" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <h1 className="font-semibold text-lg text-foreground leading-none">Brahm AI</h1>
              <span className="text-[10px] font-semibold tracking-wide bg-green-50 text-green-700 border border-green-200 rounded px-1.5 py-0.5 leading-none">ONLINE</span>
            </div>
            <p className="text-xs text-muted-foreground mt-0.5">Vedic Astrology Assistant</p>
          </div>
        </div>
        {/* Disclaimer */}
        <div className="flex items-center gap-2 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
          <AlertTriangle className="h-3.5 w-3.5 text-amber-600 flex-shrink-0" />
          <p className="text-[11px] text-amber-800">AI can make mistakes • Please verify important information</p>
        </div>
      </div>

      {/* ── Messages ───────────────────────────────────────────────── */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto space-y-3 pb-4 scrollbar-thin">
        {visibleMessages.length === 0 && !streaming && (
          <EmptyState onSuggestion={send} />
        )}

        <AnimatePresence initial={false}>
          {visibleMessages.map((msg, i) => (
            <motion.div key={i} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.2 }}>
              <MessageBubble msg={msg} onFollowUp={send} />
            </motion.div>
          ))}
        </AnimatePresence>

        {showTyping && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-end gap-2">
            <AiAvatar />
            <div className="bg-white border border-border rounded-2xl rounded-bl-sm px-4 py-3 shadow-sm">
              <div className="flex gap-1 items-center h-4">
                {[0, 1, 2].map((i) => (
                  <motion.span
                    key={i}
                    className="w-1.5 h-1.5 rounded-full bg-muted-foreground block"
                    animate={{ y: [0, -4, 0] }}
                    transition={{ duration: 0.7, repeat: Infinity, delay: i * 0.15 }}
                  />
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </div>

      {/* ── Input bar ──────────────────────────────────────────────── */}
      <div className="flex-shrink-0 pt-3 border-t border-border">
        <form
          onSubmit={(e) => { e.preventDefault(); send(input); }}
          className="flex gap-2 items-end"
        >
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); send(input); }
            }}
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
    </div>
  );
}

// ── AI Avatar ───────────────────────────────────────────────────────────────
function AiAvatar() {
  return (
    <div className="w-7 h-7 rounded-full bg-amber-100 flex items-center justify-center flex-shrink-0 self-end mb-0.5">
      <Sparkles className="h-3.5 w-3.5 text-amber-700" />
    </div>
  );
}

// ── User Avatar ─────────────────────────────────────────────────────────────
function UserAvatar() {
  return (
    <div className="w-7 h-7 rounded-full bg-muted flex items-center justify-center flex-shrink-0 self-end mb-0.5">
      <User className="h-3.5 w-3.5 text-muted-foreground" />
    </div>
  );
}

// ── Message Bubble ──────────────────────────────────────────────────────────
function MessageBubble({ msg, onFollowUp }: { msg: ChatMessage; onFollowUp: (q: string) => void }) {
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
        {/* Follow-up chips */}
        {msg.isComplete && msg.followUps && msg.followUps.length > 0 && (
          <div className="mt-2 flex flex-col gap-1.5 max-w-[85%]">
            {msg.followUps.map((q, i) => (
              <button
                key={i}
                onClick={() => onFollowUp(q)}
                className="text-left text-xs px-3 py-2 rounded-xl border border-amber-200 bg-amber-50 text-amber-800 hover:bg-amber-100 transition-colors font-medium"
              >
                {q}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ── Inline markdown: **bold** support ───────────────────────────────────────
function MessageContent({ content }: { content: string }) {
  const parts = content.split(/(\*\*[^*]+\*\*)/g);
  return (
    <>
      {parts.map((part, i) =>
        part.startsWith("**") && part.endsWith("**") ? (
          <strong key={i} className="font-semibold text-foreground">{part.slice(2, -2)}</strong>
        ) : (
          <span key={i}>{part}</span>
        )
      )}
    </>
  );
}

// ── Empty state ─────────────────────────────────────────────────────────────
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
          <button
            key={s}
            onClick={() => onSuggestion(s)}
            className="text-left text-sm px-4 py-2.5 rounded-xl border border-amber-200 bg-amber-50 text-amber-800 hover:bg-amber-100 transition-colors"
          >
            {s}
          </button>
        ))}
      </div>
    </div>
  );
}
