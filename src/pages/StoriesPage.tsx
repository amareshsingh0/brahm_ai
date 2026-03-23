import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useTranslation } from "react-i18next";

export default function StoriesPage() {
  const { t } = useTranslation();

  const stories = [
    {
      title: t("stories.ashwini_title"),
      slides: [
        { heading: t("stories.ashwini_s1_heading"), text: t("stories.ashwini_s1_text"), emoji: "🐎" },
        { heading: t("stories.ashwini_s2_heading"), text: t("stories.ashwini_s2_text"), emoji: "✨" },
        { heading: t("stories.ashwini_s3_heading"), text: t("stories.ashwini_s3_text"), emoji: "⭐" },
      ],
    },
    {
      title: t("stories.saturn_title"),
      slides: [
        { heading: t("stories.saturn_s1_heading"), text: t("stories.saturn_s1_text"), emoji: "🪐" },
        { heading: t("stories.saturn_s2_heading"), text: t("stories.saturn_s2_text"), emoji: "⚖️" },
        { heading: t("stories.saturn_s3_heading"), text: t("stories.saturn_s3_text"), emoji: "🔄" },
      ],
    },
    {
      title: t("stories.rahu_ketu_title"),
      slides: [
        { heading: t("stories.rahu_ketu_s1_heading"), text: t("stories.rahu_ketu_s1_text"), emoji: "🌑" },
        { heading: t("stories.rahu_ketu_s2_heading"), text: t("stories.rahu_ketu_s2_text"), emoji: "🌒" },
        { heading: t("stories.rahu_ketu_s3_heading"), text: t("stories.rahu_ketu_s3_text"), emoji: "🔮" },
      ],
    },
  ];

  const [storyIndex, setStoryIndex] = useState(0);
  const [slideIndex, setSlideIndex] = useState(0);

  const story = stories[storyIndex];
  const slide = story.slides[slideIndex];

  const nextSlide = () => {
    if (slideIndex < story.slides.length - 1) {
      setSlideIndex((s) => s + 1);
    } else if (storyIndex < stories.length - 1) {
      setStoryIndex((s) => s + 1);
      setSlideIndex(0);
    }
  };

  const prevSlide = () => {
    if (slideIndex > 0) {
      setSlideIndex((s) => s - 1);
    } else if (storyIndex > 0) {
      setStoryIndex((s) => s - 1);
      setSlideIndex(stories[storyIndex - 1].slides.length - 1);
    }
  };

  return (
    <div className="p-6 space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">{t("stories.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("stories.subtitle")}</p>
      </motion.div>

      {/* Story viewer */}
      <div className="max-w-lg mx-auto">
        <div className="cosmic-card rounded-2xl overflow-hidden">
          {/* Progress bar */}
          <div className="flex gap-1 p-3">
            {story.slides.map((_, i) => (
              <div key={i} className="flex-1 h-0.5 rounded-full bg-muted/30 overflow-hidden">
                <motion.div
                  className="h-full bg-primary"
                  initial={{ width: 0 }}
                  animate={{ width: i <= slideIndex ? "100%" : "0%" }}
                  transition={{ duration: 0.3 }}
                />
              </div>
            ))}
          </div>

          {/* Content */}
          <div className="px-8 py-12 min-h-[320px] flex flex-col items-center justify-center text-center star-field">
            <AnimatePresence mode="wait">
              <motion.div
                key={`${storyIndex}-${slideIndex}`}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{ duration: 0.3 }}
                className="space-y-4"
              >
                <span className="text-5xl">{slide.emoji}</span>
                <p className="text-xs text-primary uppercase tracking-widest">{story.title}</p>
                <h2 className="font-display text-2xl text-foreground">{slide.heading}</h2>
                <p className="text-sm text-muted-foreground leading-relaxed max-w-sm">{slide.text}</p>
              </motion.div>
            </AnimatePresence>
          </div>

          {/* Controls */}
          <div className="flex items-center justify-between p-4 border-t border-border/20">
            <button
              onClick={prevSlide}
              className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors"
            >
              <ChevronLeft className="h-4 w-4" /> {t("stories.previous")}
            </button>
            <span className="text-xs text-muted-foreground">
              {slideIndex + 1} / {story.slides.length}
            </span>
            <button
              onClick={nextSlide}
              className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors"
            >
              {t("stories.next")} <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>

        {/* Story selector */}
        <div className="flex gap-2 mt-4 justify-center">
          {stories.map((s, i) => (
            <button
              key={s.title}
              onClick={() => { setStoryIndex(i); setSlideIndex(0); }}
              className={`text-xs px-3 py-1.5 rounded-full transition-colors ${
                i === storyIndex ? "bg-primary/20 text-primary" : "text-muted-foreground hover:text-foreground bg-muted/20"
              }`}
            >
              {s.title}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
