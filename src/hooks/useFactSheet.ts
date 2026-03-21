/**
 * useFactSheet — compact "about me" facts stored in localStorage.
 * Facts are short strings the user adds manually (e.g. "I'm 28, married, software engineer").
 * These are sent with every chat request so AI has personal context.
 */
import { useState, useCallback } from 'react';

const STORAGE_KEY = 'brahm-factsheet';
const MAX_FACTS = 8;

function load(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function save(facts: string[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(facts));
  } catch { /* quota */ }
}

export function useFactSheet() {
  const [facts, setFacts] = useState<string[]>(load);

  const addFact = useCallback((fact: string) => {
    const trimmed = fact.trim();
    if (!trimmed) return;
    setFacts((prev) => {
      const next = [...prev.filter((f) => f !== trimmed), trimmed].slice(-MAX_FACTS);
      save(next);
      return next;
    });
  }, []);

  const removeFact = useCallback((index: number) => {
    setFacts((prev) => {
      const next = prev.filter((_, i) => i !== index);
      save(next);
      return next;
    });
  }, []);

  const clearFacts = useCallback(() => {
    save([]);
    setFacts([]);
  }, []);

  return { facts, addFact, removeFact, clearFacts };
}
