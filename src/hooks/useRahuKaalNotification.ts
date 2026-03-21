/**
 * useRahuKaalNotification
 * Requests browser notification permission and schedules alerts:
 *   - 5 min before Rahu Kaal starts
 *   - At Rahu Kaal start
 * Works while the page/tab is open (no service worker needed).
 */
import { useEffect, useRef } from "react";

function parseTime(timeStr: string, baseDate?: Date): Date | null {
  // e.g. "14:30" or "2:30 PM"
  if (!timeStr) return null;
  const d = baseDate ? new Date(baseDate) : new Date();
  // Try HH:MM format
  const hm = timeStr.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?$/);
  if (hm) {
    d.setHours(Number(hm[1]), Number(hm[2]), Number(hm[3] ?? 0), 0);
    return d;
  }
  // Try "2:30 PM" format
  const ampm = timeStr.match(/^(\d{1,2}):(\d{2})\s*(AM|PM)$/i);
  if (ampm) {
    let h = Number(ampm[1]);
    const m = Number(ampm[2]);
    const period = ampm[3].toUpperCase();
    if (period === "PM" && h !== 12) h += 12;
    if (period === "AM" && h === 12) h = 0;
    d.setHours(h, m, 0, 0);
    return d;
  }
  return null;
}

function scheduleNotification(title: string, body: string, atTime: Date): number {
  const delay = atTime.getTime() - Date.now();
  if (delay < 0 || delay > 8 * 60 * 60 * 1000) return -1; // skip if past or >8h away
  return window.setTimeout(() => {
    if (Notification.permission === "granted") {
      new Notification(title, {
        body,
        icon: "/favicon.ico",
        tag: "brahm-ai-rahu",
      });
    }
  }, delay);
}

export function useRahuKaalNotification(rahuStart?: string, rahuEnd?: string) {
  const timerIds = useRef<number[]>([]);

  useEffect(() => {
    if (!rahuStart || !rahuEnd) return;
    if (!("Notification" in window)) return;

    async function setup() {
      let permission = Notification.permission;
      if (permission === "default") {
        permission = await Notification.requestPermission();
      }
      if (permission !== "granted") return;

      // Clear any existing timers
      timerIds.current.forEach(id => clearTimeout(id));
      timerIds.current = [];

      const startDt = parseTime(rahuStart!);
      if (!startDt) return;

      // 5-min warning
      const warnDt = new Date(startDt.getTime() - 5 * 60 * 1000);
      const warnId = scheduleNotification(
        "⚠️ Rahu Kaal in 5 minutes",
        `Rahu Kaal starts at ${rahuStart}. Avoid starting new work or important tasks.`,
        warnDt,
      );
      if (warnId !== -1) timerIds.current.push(warnId);

      // At start
      const startId = scheduleNotification(
        "🔴 Rahu Kaal has started",
        `Rahu Kaal: ${rahuStart} – ${rahuEnd}. Avoid new beginnings until it ends.`,
        startDt,
      );
      if (startId !== -1) timerIds.current.push(startId);
    }

    setup();
    return () => {
      timerIds.current.forEach(id => clearTimeout(id));
    };
  }, [rahuStart, rahuEnd]);
}
