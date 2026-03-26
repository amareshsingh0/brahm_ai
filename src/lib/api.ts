/**
 * Central API client for Brahm AI.
 * All HTTP calls go through here — no per-file base URL.
 */

import { useAuthStore } from '@/store/authStore';

const envBaseUrl = import.meta.env.VITE_API_URL?.trim();
const BASE_URL = (envBaseUrl ?? '').replace(/\/$/, '');

function createHeaders(headers?: HeadersInit): Headers {
  const merged = new Headers(headers);
  const token = useAuthStore.getState().token;

  if (!merged.has('Content-Type')) {
    merged.set('Content-Type', 'application/json');
  }
  if (token && !merged.has('Authorization')) {
    merged.set('Authorization', `Bearer ${token}`);
  }

  return merged;
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const url = `${BASE_URL}${path}`;
  const res = await fetch(url, {
    headers: createHeaders(options?.headers),
    ...options,
  });
  if (!res.ok) {
    const detail = await res.text().catch(() => res.statusText);
    throw new Error(`API ${res.status}: ${detail}`);
  }
  return res.json() as Promise<T>;
}

export const api = {
  get<T>(path: string): Promise<T> {
    return request<T>(path);
  },

  post<T>(path: string, body: unknown): Promise<T> {
    return request<T>(path, {
      method: 'POST',
      body: JSON.stringify(body),
    });
  },

  delete<T>(path: string): Promise<T> {
    return request<T>(path, { method: 'DELETE' });
  },

  /**
   * SSE streaming for /api/chat.
   * Calls onToken for each token, onSources when sources arrive, onDone at end.
   */
  streamChat(
    body: {
      message: string;
      history: { role: string; content: string }[];
      language?: string;
      page_context?: string;
      page_data?: Record<string, unknown>;
      user_id?: string;
      session_id?: string;
    },
    callbacks: {
      onToken: (token: string) => void;
      onSources: (sources: { book: string; source: string; language: string }[]) => void;
      onDone: () => void;
      onBirthForm?: () => void;
      onSaveKundaliPrompt?: (data: Record<string, unknown>) => void;
      onError?: (err: Error) => void;
    },
    signal?: AbortSignal
  ): void {
    const url = `${BASE_URL}/api/chat`;
    fetch(url, {
      method: 'POST',
      headers: createHeaders(),
      body: JSON.stringify(body),
      signal,
    })
      .then(async (res) => {
        if (!res.ok) throw new Error(`Chat API ${res.status}`);
        const reader = res.body!.getReader();
        const decoder = new TextDecoder();
        let buf = '';
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buf += decoder.decode(value, { stream: true });
          const lines = buf.split('\n');
          buf = lines.pop() ?? '';
          for (const line of lines) {
            if (!line.startsWith('data: ')) continue;
            const json = line.slice(6).trim();
            if (!json) continue;
            try {
              const msg = JSON.parse(json);
              if (msg.type === 'token')               callbacks.onToken(msg.content);
              if (msg.type === 'sources')             callbacks.onSources(msg.sources);
              if (msg.type === 'done')                callbacks.onDone();
              if (msg.type === 'birth_form')          callbacks.onBirthForm?.();
              if (msg.type === 'save_kundali_prompt') callbacks.onSaveKundaliPrompt?.(msg.birth_data);
            } catch { /* skip malformed */ }
          }
        }
      })
      .catch((err) => {
        if (err?.name !== 'AbortError') callbacks.onError?.(err);
      });
  },
};
