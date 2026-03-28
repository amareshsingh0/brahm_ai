import { useEffect, useRef, useState } from "react";
import { ChevronUp } from "lucide-react";
import { cn } from "@/lib/utils";

export function ScrollToTopButton({ scrollRef }: { scrollRef: React.RefObject<HTMLElement | null> }) {
  const [visible, setVisible] = useState(false);
  const lastScrollTop = useRef(0);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const onScroll = () => {
      const current = el.scrollTop;
      const scrollingUp = current < lastScrollTop.current;
      setVisible(scrollingUp && current > 200);
      lastScrollTop.current = current;
    };
    el.addEventListener("scroll", onScroll, { passive: true });
    return () => el.removeEventListener("scroll", onScroll);
  }, [scrollRef]);

  const scrollToTop = () => scrollRef.current?.scrollTo({ top: 0, behavior: "smooth" });

  return (
    <button
      onClick={scrollToTop}
      aria-label="Scroll to top"
      className={cn(
        "fixed bottom-20 right-4 z-50 flex h-10 w-10 items-center justify-center rounded-full shadow-lg transition-all duration-300",
        "bg-amber-600 text-white hover:bg-amber-700 active:scale-95",
        "md:bottom-6 md:right-6",
        visible ? "opacity-100 translate-y-0 pointer-events-auto" : "opacity-0 translate-y-4 pointer-events-none",
      )}
    >
      <ChevronUp className="h-5 w-5" />
    </button>
  );
}
