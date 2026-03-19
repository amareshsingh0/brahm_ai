import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronLeft, ChevronRight } from "lucide-react";

const stories = [
  {
    title: "The Legend of Ashwini Kumar",
    slides: [
      { heading: "Origin", text: "Twin divine horsemen, sons of Surya, the Sun God. They embody speed, healing, and the dawn of new beginnings.", emoji: "🐎" },
      { heading: "The Healing Touch", text: "Known as the physicians of the Devas, they restored youth to the aged sage Chyavana and healed countless celestial beings.", emoji: "✨" },
      { heading: "Astrological Meaning", text: "Ashwini Nakshatra grants its natives initiative, healing abilities, and swift action. Those born under it are pioneers.", emoji: "⭐" },
    ],
  },
  {
    title: "Saturn's Discipline",
    slides: [
      { heading: "The Taskmaster", text: "Saturn, son of Surya, carries the weight of karma. His slow orbit teaches patience and rewards perseverance.", emoji: "🪐" },
      { heading: "Shani's Justice", text: "Legend says even the Gods feared Shani's gaze. His lessons are harsh but fair — building character through challenge.", emoji: "⚖️" },
      { heading: "Sade Sati", text: "The 7.5-year transit of Saturn over your Moon sign. A transformative period of growth through endurance.", emoji: "🔄" },
    ],
  },
  {
    title: "Rahu & Ketu: The Shadow",
    slides: [
      { heading: "The Churning", text: "During Samudra Manthan, a demon disguised as a Deva drank the nectar of immortality. Vishnu severed his head.", emoji: "🌑" },
      { heading: "Eternal Eclipse", text: "The head became Rahu, the tail became Ketu. They chase the Sun and Moon, causing eclipses when they catch them.", emoji: "🌒" },
      { heading: "Karmic Axis", text: "Rahu represents worldly desires and obsession. Ketu represents spiritual liberation and past-life wisdom.", emoji: "🔮" },
    ],
  },
];

export default function StoriesPage() {
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
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">Cosmic Stories</h1>
        <p className="text-sm text-muted-foreground">Ancient tales behind the stars</p>
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
              <ChevronLeft className="h-4 w-4" /> Previous
            </button>
            <span className="text-xs text-muted-foreground">
              {slideIndex + 1} / {story.slides.length}
            </span>
            <button
              onClick={nextSlide}
              className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors"
            >
              Next <ChevronRight className="h-4 w-4" />
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
