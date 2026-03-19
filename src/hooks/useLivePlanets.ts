/**
 * useLivePlanets — real-time sidereal planetary positions.
 *
 * Strategy:
 *  1. Fetch true positions from the API every 5 minutes (pyswisseph, accurate).
 *  2. Between fetches, interpolate per-second using average planet speeds.
 *  3. At midnight (00:00 local time) the 24-hour track resets.
 *  4. Subscribers get a new object every second → React re-renders live.
 */

import { useState, useEffect, useRef, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import type { PlanetsResponse } from "@/types/api";
import { useKundliStore } from "@/store/kundliStore";

// ── Constants ────────────────────────────────────────────────────────────────

/** Average daily motion in degrees (sidereal, tropical similar for planets) */
export const PLANET_SPEED_PER_DAY: Record<string, number> = {
  Surya:   0.9856,
  Chandra: 13.176,
  Mangal:  0.5240,
  Budh:    1.3833,
  Guru:    0.0831,
  Shukra:  1.2021,
  Shani:   0.0335,
  Rahu:   -0.0529,  // always retrograde
  Ketu:   -0.0529,  // always retrograde
};

/** Per-second motion (degrees) */
const SPEED_PER_SECOND: Record<string, number> = Object.fromEntries(
  Object.entries(PLANET_SPEED_PER_DAY).map(([k, v]) => [k, v / 86400])
);

/** Sidereal rashi index (0=Mesha … 11=Meena) */
export const RASHI_INDEX: Record<string, number> = {
  Mesha: 0, Vrishabha: 1, Mithuna: 2, Karka: 3,
  Simha: 4, Kanya: 5,    Tula: 6,    Vrischika: 7,
  Dhanu: 8, Makara: 9,   Kumbha: 10, Meena: 11,
};
export const INDEX_RASHI = Object.fromEntries(Object.entries(RASHI_INDEX).map(([k, v]) => [v, k]));

/** Combust distance (degrees) — planet is invisible when this close to Sun */
export const COMBUST_ORB: Record<string, number> = {
  Chandra: 12, Mangal: 17, Budh: 14,
  Guru: 11, Shukra: 10, Shani: 15,
};

// ── Helpers ──────────────────────────────────────────────────────────────────

export function getEclipticLon(rashi: string, degInRashi: number): number {
  return ((RASHI_INDEX[rashi] ?? 0) * 30 + degInRashi) % 360;
}

function lonToRashiDeg(lon: number): { rashi: string; degree: number } {
  const wrapped = ((lon % 360) + 360) % 360;
  const idx = Math.floor(wrapped / 30);
  return { rashi: INDEX_RASHI[idx] ?? "Mesha", degree: wrapped % 30 };
}

/** DD°MM'SS" */
export function toDMS(decimal: number): string {
  const abs = Math.abs(decimal);
  const d = Math.floor(abs);
  const m = Math.floor((abs - d) * 60);
  const s = Math.floor(((abs - d) * 60 - m) * 60);
  return `${d}°${String(m).padStart(2, "0")}'${String(s).padStart(2, "0")}"`;
}

/** Angular difference between two ecliptic longitudes (-180 to +180) */
export function angularDiff(a: number, b: number): number {
  let diff = ((a - b) % 360 + 360) % 360;
  if (diff > 180) diff -= 360;
  return diff;
}

/** Is a planet combust (too close to Sun)? */
export function isCombust(planetName: string, planetLon: number, sunLon: number): boolean {
  const orb = COMBUST_ORB[planetName];
  if (!orb) return false;
  return Math.abs(angularDiff(planetLon, sunLon)) < orb;
}

// ── Types ─────────────────────────────────────────────────────────────────────

export interface LivePlanetData {
  name:         string;
  eclipticLon:  number;     // 0–360 sidereal longitude
  rashi:        string;
  degInRashi:   number;     // 0–29.999
  nakshatra:    string;
  pada:         number;
  retro:        boolean;
  combust:      boolean;
  visible:      boolean;    // estimated sky visibility
  dms:          string;     // "15°23'45""
}

export interface LivePlanetsSnapshot {
  grahas:      Record<string, LivePlanetData>;
  lagna:       { eclipticLon: number; rashi: string; degInRashi: number; dms: string } | null;
  computedAt:  Date;        // when API last fetched
  localTime:   Date;        // current JS time (ticks every second)
  dayProgress: number;      // 0–1 fraction of current day (midnight=0, midnight=1)
  secondsToMidnight: number;
}

// ── Nakshatra from ecliptic longitude ────────────────────────────────────────

const NAKSHATRAS_27 = [
  "Ashwini","Bharani","Krittika","Rohini","Mrigashira","Ardra",
  "Punarvasu","Pushya","Ashlesha","Magha","Purva Phalguni","Uttara Phalguni",
  "Hasta","Chitra","Swati","Vishakha","Anuradha","Jyeshtha",
  "Mula","Purva Ashadha","Uttara Ashadha","Shravana","Dhanishtha",
  "Shatabhisha","Purva Bhadrapada","Uttara Bhadrapada","Revati",
];

function nakshFromLon(lon: number): { name: string; pada: number } {
  const seg = ((lon % 360) + 360) % 360 / (360 / 27);
  const idx  = Math.floor(seg) % 27;
  const pada = Math.floor((seg - Math.floor(seg)) * 4) + 1;
  return { name: NAKSHATRAS_27[idx], pada };
}

// ── Main Hook ─────────────────────────────────────────────────────────────────

export function useLivePlanets() {
  const store     = useKundliStore();
  const lat       = store.birthDetails?.latitude  ?? 28.6139;
  const lon       = store.birthDetails?.longitude ?? 77.209;
  const tz        = 5.5;

  // API fetch — truth values every 5 minutes
  const { data: apiData, dataUpdatedAt } = useQuery<PlanetsResponse>({
    queryKey: ["planets", lat, lon, tz],
    queryFn:  () => api.get<PlanetsResponse>(`/api/planets/now?lat=${lat}&lon=${lon}&tz=${tz}`),
    refetchInterval: 5 * 60 * 1000,
    staleTime:       4 * 60 * 1000,
    retry: 1,
  });

  // Base positions from last API response
  const baseRef = useRef<{
    lons:  Record<string, number>;
    retros: Record<string, boolean>;
    nakshatras: Record<string, string>;
    lagnaLon: number;
    lagnaRashi: string;
    fetchedAt: number;  // Date.now() when fetched
  } | null>(null);

  // Sync base when API data changes
  useEffect(() => {
    if (!apiData) return;
    const lons:  Record<string, number>  = {};
    const retros: Record<string, boolean> = {};
    const nakshatras: Record<string, string> = {};
    for (const [name, pos] of Object.entries(apiData.grahas)) {
      lons[name]  = getEclipticLon(pos.rashi, pos.degree);
      retros[name] = pos.retro ?? false;
      nakshatras[name] = pos.nakshatra;
    }
    const lagnaLon   = apiData.lagna ? getEclipticLon(apiData.lagna.rashi, apiData.lagna.degree) : 0;
    const lagnaRashi = apiData.lagna?.rashi ?? "Mesha";
    baseRef.current = { lons, retros, nakshatras, lagnaLon, lagnaRashi, fetchedAt: dataUpdatedAt || Date.now() };
  }, [apiData, dataUpdatedAt]);

  // Live snapshot state — updates every second
  const [snapshot, setSnapshot] = useState<LivePlanetsSnapshot | null>(null);

  const tick = useCallback(() => {
    const base = baseRef.current;
    if (!base) return;

    const now      = new Date();
    const nowMs    = now.getTime();
    const elapsed  = (nowMs - base.fetchedAt) / 1000; // seconds since last API fetch

    // Interpolate: add elapsed * per-second-speed to each planet's longitude
    const grahas: Record<string, LivePlanetData> = {};
    const sunLon = (base.lons["Surya"] ?? 0) + elapsed * SPEED_PER_SECOND["Surya"];

    for (const name of Object.keys(base.lons)) {
      const speed   = SPEED_PER_SECOND[name] ?? 0;
      const isRetro = base.retros[name] ?? false;
      // If retro, use negative speed; otherwise positive
      const effectiveSpeed = isRetro ? -Math.abs(speed) : speed;
      const lon  = ((base.lons[name] + elapsed * effectiveSpeed) % 360 + 360) % 360;
      const { rashi, degree } = lonToRashiDeg(lon);
      const { name: nksh, pada } = nakshFromLon(lon);
      const combust = isCombust(name, lon, sunLon);

      // Visibility: Sun always visible (day), others visible if not combust
      // Simplified: day/night based on hour (actual sunrise/sunset needs panchang)
      const hour   = now.getHours();
      const isDay  = hour >= 6 && hour < 18;
      let visible  = false;
      if (name === "Surya")   visible = isDay;
      else if (name === "Chandra") visible = !isDay || Math.abs(angularDiff(lon, sunLon)) > 30;
      else visible = !combust && (!isDay || Math.abs(angularDiff(lon, sunLon)) > 20);

      grahas[name] = {
        name, eclipticLon: lon,
        rashi, degInRashi: degree,
        nakshatra: nksh, pada,
        retro: isRetro, combust, visible,
        dms: toDMS(degree) + (isRetro ? " ℞" : ""),
      };
    }

    // Lagna moves ~1° per 4 minutes (360° in 24 hours sidereal day)
    // Approx: lagna moves 0.25° per minute = 0.004167°/sec
    const lagnaLon = ((base.lagnaLon + elapsed * (360 / 86400)) % 360 + 360) % 360;
    const lagnaRD  = lonToRashiDeg(lagnaLon);

    // Day progress and seconds to midnight
    const startOfDay = new Date(now);
    startOfDay.setHours(0, 0, 0, 0);
    const dayProgress = (nowMs - startOfDay.getTime()) / 86400000;
    const midnight    = new Date(now);
    midnight.setHours(24, 0, 0, 0);
    const secondsToMidnight = Math.floor((midnight.getTime() - nowMs) / 1000);

    setSnapshot({
      grahas,
      lagna: {
        eclipticLon: lagnaLon,
        rashi: lagnaRD.rashi,
        degInRashi: lagnaRD.degree,
        dms: toDMS(lagnaRD.degree),
      },
      computedAt: new Date(base.fetchedAt),
      localTime:  now,
      dayProgress,
      secondsToMidnight,
    });
  }, []);

  useEffect(() => {
    tick(); // immediate first tick
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [tick]);

  return snapshot;
}

// ── 24-hour track data ────────────────────────────────────────────────────────

/**
 * Returns the ecliptic longitude of a planet at each hour of today (00–23).
 * Uses the current position as reference and projects backward/forward using speed.
 */
export function use24hTrack(snapshot: LivePlanetsSnapshot | null) {
  if (!snapshot) return null;

  const now  = new Date();
  const startOfDay = new Date(now);
  startOfDay.setHours(0, 0, 0, 0);
  const secondsSinceMidnight = (now.getTime() - startOfDay.getTime()) / 1000;

  const track: Record<string, { hour: number; lon: number; rashi: string; degInRashi: number; dms: string }[]> = {};

  for (const [name, planet] of Object.entries(snapshot.grahas)) {
    const speed = PLANET_SPEED_PER_DAY[name] ?? 0;
    const speedPerSec = (planet.retro ? -Math.abs(speed) : speed) / 86400;
    const row: typeof track[string] = [];

    for (let hour = 0; hour <= 24; hour++) {
      const secondsOffset = hour * 3600 - secondsSinceMidnight;
      const lon = ((planet.eclipticLon + secondsOffset * speedPerSec) % 360 + 360) % 360;
      const rd  = lonToRashiDeg(lon);
      row.push({ hour, lon, rashi: rd.rashi, degInRashi: rd.degree, dms: toDMS(rd.degree) });
    }
    track[name] = row;
  }
  return track;
}
