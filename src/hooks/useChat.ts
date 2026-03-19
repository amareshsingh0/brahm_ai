import { useCallback, useRef, useState } from 'react';
import { api } from '../lib/api';
import type { ChatMessage, Source } from '../types/api';
import { useLanguageStore, LANG_TO_API } from '@/store/languageStore';

export interface UseChatReturn {
  messages: ChatMessage[];
  sources: Source[];
  streaming: boolean;
  sendMessage: (text: string, language?: string) => void;
  clearHistory: () => void;
}

export function useChat(): UseChatReturn {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [sources, setSources] = useState<Source[]>([]);
  const [streaming, setStreaming] = useState(false);
  const abortRef = useRef<AbortController | null>(null);
  const globalLang = useLanguageStore((s) => LANG_TO_API[s.lang]);

  const sendMessage = useCallback((text: string, language?: string) => {
    const resolvedLang = language ?? globalLang;
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
      { message: text, history, language: resolvedLang },
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
  }, [messages, streaming, globalLang]);

  const clearHistory = useCallback(() => {
    abortRef.current?.abort();
    setMessages([]);
    setSources([]);
    setStreaming(false);
  }, []);

  return { messages, sources, streaming, sendMessage, clearHistory };
}
