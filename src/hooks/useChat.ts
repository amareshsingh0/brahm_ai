import { useCallback, useRef, useState, useEffect } from 'react';
import { api } from '../lib/api';
import type { ChatMessage, Source } from '../types/api';
import { useLanguageStore, LANG_TO_API } from '@/store/languageStore';
import { useKundliStore } from '@/store/kundliStore';

export interface UseChatOptions {
  pageContext?: string;
  pageData?: Record<string, unknown>;
  persistKey?: string; // localStorage key for history persistence
}

export interface UseChatReturn {
  messages: ChatMessage[];
  sources: Source[];
  streaming: boolean;
  sendMessage: (text: string) => void;
  clearHistory: () => void;
}

export function useChat(options: UseChatOptions = {}): UseChatReturn {
  const { pageContext = 'general', pageData = {}, persistKey } = options;

  const storageKey = persistKey ? `brahm-chat-${persistKey}` : null;

  const loadInitialMessages = (): ChatMessage[] => {
    if (!storageKey) return [];
    try {
      const saved = localStorage.getItem(storageKey);
      return saved ? JSON.parse(saved) : [];
    } catch { return []; }
  };

  const [messages, setMessages] = useState<ChatMessage[]>(loadInitialMessages);
  const [sources, setSources] = useState<Source[]>([]);
  const [streaming, setStreaming] = useState(false);
  const abortRef = useRef<AbortController | null>(null);
  const globalLang = useLanguageStore((s) => LANG_TO_API[s.lang]);

  // Auto-read user profile from store
  const birthDetails = useKundliStore((s) => s.birthDetails);
  const kundaliData = useKundliStore((s) => s.kundaliData);

  // Persist messages to localStorage when they change
  useEffect(() => {
    if (!storageKey || messages.length === 0) return;
    try {
      // Keep last 30 messages only
      localStorage.setItem(storageKey, JSON.stringify(messages.slice(-30)));
    } catch { /* quota exceeded — ignore */ }
  }, [messages, storageKey]);

  const buildPageData = useCallback(() => {
    const data: Record<string, unknown> = { ...pageData };

    // Auto-attach user birth data from store (profile)
    if (birthDetails?.dateOfBirth && birthDetails?.lat) {
      data.user_birth_data = {
        birth_date: birthDetails.dateOfBirth,
        birth_time: birthDetails.timeOfBirth,
        birth_lat: birthDetails.lat,
        birth_lon: birthDetails.lon,
        birth_tz: birthDetails.tz ?? 5.5,
        name: birthDetails.name,
        place: birthDetails.birthPlace,
      };
    }

    // Auto-attach kundali if available
    if (kundaliData) {
      data.kundali_raw = kundaliData;
    }

    return data;
  }, [pageData, birthDetails, kundaliData]);

  const sendMessage = useCallback((text: string) => {
    if (streaming) {
      abortRef.current?.abort();
    }

    const userMsg: ChatMessage = { role: 'user', content: text };
    const assistantMsg: ChatMessage = { role: 'assistant', content: '' };

    setMessages((prev) => [...prev, userMsg, assistantMsg]);
    setSources([]);
    setStreaming(true);

    const ctrl = new AbortController();
    abortRef.current = ctrl;

    const history = messages.map((m) => ({ role: m.role, content: m.content }));

    api.streamChat(
      {
        message: text,
        history,
        language: globalLang,
        page_context: pageContext,
        page_data: buildPageData(),
      },
      {
        onToken: (token) => {
          setMessages((prev) => {
            const updated = [...prev];
            const last = updated[updated.length - 1];
            updated[updated.length - 1] = { ...last, content: last.content + token };
            return updated;
          });
        },
        onSources: (srcs) => setSources(srcs),
        onDone: () => setStreaming(false),
        onError: (err) => {
          setStreaming(false);
          setMessages((prev) => {
            const updated = [...prev];
            updated[updated.length - 1] = { role: 'assistant', content: `Error: ${err.message}` };
            return updated;
          });
        },
      },
      ctrl.signal
    );
  }, [messages, streaming, globalLang, pageContext, buildPageData]);

  const clearHistory = useCallback(() => {
    abortRef.current?.abort();
    setMessages([]);
    setSources([]);
    setStreaming(false);
    if (storageKey) localStorage.removeItem(storageKey);
  }, [storageKey]);

  return { messages, sources, streaming, sendMessage, clearHistory };
}
