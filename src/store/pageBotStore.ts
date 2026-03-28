import { create } from 'zustand';

interface PageBotState {
  context: string;
  data: Record<string, unknown>;
  setPageBot: (context: string, data: Record<string, unknown>) => void;
  clearPageBot: () => void;
}

export const usePageBotStore = create<PageBotState>((set) => ({
  context: 'general',
  data: {},
  setPageBot: (context, data) => set({ context, data }),
  clearPageBot: () => set({ context: 'general', data: {} }),
}));
