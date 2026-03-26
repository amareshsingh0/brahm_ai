import { useCallback, useRef, useState, useEffect } from 'react';
import { api } from '../lib/api';
import type { ChatMessage, Source } from '../types/api';
import { useLanguageStore, LANG_TO_API } from '@/store/languageStore';
import { useKundliStore } from '@/store/kundliStore';
import { useFactSheet } from '@/hooks/useFactSheet';
import { useAuthStore } from '@/store/authStore';

function generateSessionId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

function getOrCreateSessionId(storageKey: string): string {
  const key = `brahm-session-${storageKey}`;
  const existing = localStorage.getItem(key);
  if (existing) return existing;
  const id = generateSessionId();
  localStorage.setItem(key, id);
  return id;
}

export interface BirthFormData {
  name: string;
  gender: string;
  date: string;   // YYYY-MM-DD
  time: string;   // HH:MM
  place: string;
}

export interface SaveKundaliPromptData {
  birth_date: string;
  birth_time: string;
  birth_lat: number;
  birth_lon: number;
  birth_tz: number;
  name: string;
  place: string;
  gender: string;
}

export interface UseChatOptions {
  pageContext?: string;
  pageData?: Record<string, unknown>;
  persistKey?: string;
}

export interface UseChatReturn {
  messages: ChatMessage[];
  sources: Source[];
  streaming: boolean;
  showBirthForm: boolean;
  saveKundaliPrompt: SaveKundaliPromptData | null;
  sendMessage: (text: string, extraPageData?: Record<string, unknown>) => void;
  submitBirthForm: (data: BirthFormData) => void;
  dismissSavePrompt: () => void;
  clearHistory: () => void;
}

export function useChat(options: UseChatOptions = {}): UseChatReturn {
  const { pageContext = 'general', pageData = {}, persistKey } = options;

  const storageKey = persistKey ? `brahm-chat-${persistKey}` : null;
  const userId = useAuthStore((s) => s.userId);

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
  const [showBirthForm, setShowBirthForm] = useState(false);
  const [saveKundaliPrompt, setSaveKundaliPrompt] = useState<SaveKundaliPromptData | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  const globalLang = useLanguageStore((s) => LANG_TO_API[s.lang]);

  // Auto-read user profile from store
  const birthDetails = useKundliStore((s) => s.birthDetails);
  const kundaliData = useKundliStore((s) => s.kundaliData);
  const { facts } = useFactSheet();

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

    // Auto-attach user fact-sheet if available
    if (facts.length > 0) {
      data.user_facts = facts;
    }

    return data;
  }, [pageData, birthDetails, kundaliData, facts]);

  const sendMessage = useCallback((text: string, extraPageData?: Record<string, unknown>) => {
    if (streaming) {
      abortRef.current?.abort();
    }

    setShowBirthForm(false);
    setSaveKundaliPrompt(null);

    const userMsg: ChatMessage = { role: 'user', content: text };
    const assistantMsg: ChatMessage = { role: 'assistant', content: '' };

    setMessages((prev) => [...prev, userMsg, assistantMsg]);
    setSources([]);
    setStreaming(true);

    const ctrl = new AbortController();
    abortRef.current = ctrl;

    const history = messages.slice(-8).map((m) => ({ role: m.role, content: m.content.slice(0, 2000) }));
    const sessionId = storageKey ? getOrCreateSessionId(storageKey) : generateSessionId();
    const pageData = { ...buildPageData(), ...extraPageData };

    api.streamChat(
      {
        message: text,
        history,
        language: globalLang,
        page_context: pageContext,
        page_data: pageData,
        user_id: userId ?? undefined,
        session_id: sessionId,
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
        onBirthForm: () => {
          // Remove the empty assistant message, show form instead
          setMessages((prev) => prev.slice(0, -1));
          setStreaming(false);
          setShowBirthForm(true);
        },
        onSaveKundaliPrompt: (data) => {
          setSaveKundaliPrompt(data);
          setStreaming(false);
        },
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
  }, [messages, streaming, globalLang, pageContext, buildPageData, userId, storageKey]);

  const submitBirthForm = useCallback((data: BirthFormData) => {
    setShowBirthForm(false);
    // Send structured birth data as a user message with extra page_data
    const text = `${data.name ? `Name: ${data.name}, ` : ''}DOB: ${data.date}, Time: ${data.time}, Place: ${data.place}${data.gender ? `, Gender: ${data.gender}` : ''}`;
    sendMessage(text, { birth_gender: data.gender });
  }, [sendMessage]);

  const dismissSavePrompt = useCallback(() => {
    setSaveKundaliPrompt(null);
  }, []);

  const clearHistory = useCallback(() => {
    abortRef.current?.abort();
    setMessages([]);
    setSources([]);
    setStreaming(false);
    if (storageKey) {
      // Delete the session from backend if user is logged in
      const sessionKey = `brahm-session-${storageKey}`;
      const sessionId = localStorage.getItem(sessionKey);
      if (userId && sessionId) {
        api.delete(`/api/user/chats/session/${encodeURIComponent(sessionId)}`).catch(() => {});
      }
      localStorage.removeItem(storageKey);
      // Generate a new session ID for the next conversation
      localStorage.removeItem(sessionKey);
    }
  }, [storageKey, userId]);

  return { messages, sources, streaming, showBirthForm, saveKundaliPrompt, sendMessage, submitBirthForm, dismissSavePrompt, clearHistory };
}
