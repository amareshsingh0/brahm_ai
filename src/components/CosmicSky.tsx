import { useEffect, useRef } from "react";

interface Star {
  x: number;
  y: number;
  size: number;
  baseOpacity: number;
  opacity: number;
  twinkleSpeed: number;
  twinklePhase: number;
  r: number; g: number; b: number;
  rising: boolean;
  riseSpeed: number;
  fadeIn: number;       // 0→1 fade-in progress (for rising stars that wrap)
}

export function CosmicSky() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animId: number;
    let stars: Star[] = [];

    function makeColor(): [number, number, number] {
      const roll = Math.random();
      if (roll < 0.72) return [210, 220, 240];    // cold blue-white (most common)
      if (roll < 0.87) return [240, 230, 210];    // warm white
      if (roll < 0.94) return [220, 155, 50];     // antique gold — divine
      if (roll < 0.98) return [200, 190, 255];    // pale lavender
      return [140, 80, 220];                       // mystic purple — rare
    }

    function makeStar(canvas: HTMLCanvasElement, randomY = true): Star {
      const [r, g, b] = makeColor();
      const rising = Math.random() < 0.25;
      const size = Math.random() < 0.06
        ? Math.random() * 1.4 + 1.6          // bright large star (rare)
        : Math.random() < 0.25
          ? Math.random() * 0.8 + 0.9        // medium
          : Math.random() * 0.55 + 0.2;      // dim tiny (most)

      const baseOpacity = size > 1.6
        ? Math.random() * 0.4 + 0.55         // bright stars: higher floor
        : Math.random() * 0.45 + 0.08;       // dim stars: wider range

      return {
        x: Math.random() * canvas.width,
        y: randomY ? Math.random() * canvas.height : canvas.height + Math.random() * 60,
        size,
        baseOpacity,
        opacity: baseOpacity,
        twinkleSpeed: Math.random() * 0.012 + 0.002,
        twinklePhase: Math.random() * Math.PI * 2,
        r, g, b,
        rising,
        riseSpeed: rising ? Math.random() * 0.06 + 0.01 : 0,
        fadeIn: randomY ? 1 : 0,
      };
    }

    function resize() {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
      // Density: ~1 star per 4800px² for a rich but not cluttered sky
      const count = Math.min(Math.floor((canvas.width * canvas.height) / 4800), 340);
      stars = Array.from({ length: count }, () => makeStar(canvas, true));
    }

    function draw() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      // ── Subtle nebula glow in upper third ──
      const nebula = ctx.createRadialGradient(
        canvas.width * 0.62, canvas.height * 0.22, 0,
        canvas.width * 0.62, canvas.height * 0.22, canvas.width * 0.38,
      );
      nebula.addColorStop(0,   "hsla(263,55%,35%,0.045)");
      nebula.addColorStop(0.5, "hsla(225,55%,20%,0.025)");
      nebula.addColorStop(1,   "transparent");
      ctx.fillStyle = nebula;
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // A second softer nebula near lower-left for depth
      const nebula2 = ctx.createRadialGradient(
        canvas.width * 0.18, canvas.height * 0.68, 0,
        canvas.width * 0.18, canvas.height * 0.68, canvas.width * 0.28,
      );
      nebula2.addColorStop(0,   "hsla(38,80%,25%,0.03)");
      nebula2.addColorStop(1,   "transparent");
      ctx.fillStyle = nebula2;
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      for (let i = stars.length - 1; i >= 0; i--) {
        const s = stars[i];

        // Twinkle
        s.twinklePhase += s.twinkleSpeed;
        // Use a sum of two sines for more organic shimmer
        const shimmer = 0.5 + 0.35 * Math.sin(s.twinklePhase)
                             + 0.15 * Math.sin(s.twinklePhase * 2.7 + 1.3);
        s.opacity = s.baseOpacity * shimmer * s.fadeIn;

        // Rise
        if (s.rising) {
          s.y -= s.riseSpeed;
          if (s.y < -s.size * 4) {
            // Respawn at bottom
            stars[i] = makeStar(canvas, false);
            continue;
          }
          // Fade in near bottom
          if (s.fadeIn < 1) {
            s.fadeIn = Math.min(1, s.fadeIn + 0.008);
          }
        }

        if (s.opacity <= 0.01) continue;

        // ── Draw star ──
        ctx.save();

        // Glow halo for medium+ stars
        if (s.size > 0.85) {
          const glowR = s.size * (s.size > 1.6 ? 5 : 3.2);
          const grd = ctx.createRadialGradient(s.x, s.y, 0, s.x, s.y, glowR);
          const alpha = s.opacity * (s.size > 1.6 ? 0.7 : 0.45);
          grd.addColorStop(0,   `rgba(${s.r},${s.g},${s.b},${alpha})`);
          grd.addColorStop(0.35,`rgba(${s.r},${s.g},${s.b},${alpha * 0.3})`);
          grd.addColorStop(1,   "rgba(0,0,0,0)");
          ctx.fillStyle = grd;
          ctx.beginPath();
          ctx.arc(s.x, s.y, glowR, 0, Math.PI * 2);
          ctx.fill();
        }

        // Cross-diffraction spike for the brightest stars (size > 2)
        if (s.size > 2.0) {
          const spikeLen = s.size * 6;
          const spikeAlpha = s.opacity * 0.25;
          ctx.strokeStyle = `rgba(${s.r},${s.g},${s.b},${spikeAlpha})`;
          ctx.lineWidth = 0.5;
          ctx.beginPath();
          ctx.moveTo(s.x - spikeLen, s.y);
          ctx.lineTo(s.x + spikeLen, s.y);
          ctx.stroke();
          ctx.beginPath();
          ctx.moveTo(s.x, s.y - spikeLen);
          ctx.lineTo(s.x, s.y + spikeLen);
          ctx.stroke();
        }

        // Core dot
        ctx.globalAlpha = Math.min(s.opacity, 1);
        ctx.fillStyle = `rgb(${s.r},${s.g},${s.b})`;
        ctx.beginPath();
        ctx.arc(s.x, s.y, s.size, 0, Math.PI * 2);
        ctx.fill();

        ctx.restore();
      }

      animId = requestAnimationFrame(draw);
    }

    resize();
    draw();

    window.addEventListener("resize", resize);
    return () => {
      cancelAnimationFrame(animId);
      window.removeEventListener("resize", resize);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      aria-hidden="true"
      className="fixed inset-0 pointer-events-none"
      style={{ zIndex: 0, opacity: 0.92 }}
    />
  );
}
