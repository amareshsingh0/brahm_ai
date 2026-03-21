import { useRef, useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Send, Bot, User, Sparkles, BookOpen, Loader2 } from "lucide-react";
import { useChat } from "@/hooks/useChat";

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  sources?: { book: string; page: number }[];
  timestamp: Date;
}

const suggestedQueries = [
  { label: "Bhagavad Gita", query: "What does Krishna say about Dharma in the Bhagavad Gita?" },
  { label: "Vedic Astrology", query: "Explain the significance of Nakshatras in Vedic astrology" },
  { label: "Karma Theory", query: "What is the concept of Karma according to Vedic texts?" },
  { label: "Meditation", query: "What do the Upanishads say about meditation and self-realization?" },
  { label: "Kundali Yoga", query: "What are the most powerful yogas in a Kundali?" },
  { label: "Sanskrit Mantras", query: "Explain the meaning of Gayatri Mantra in detail" },
];

const mockResponses: Record<string, { answer: string; sources: { book: string; page: number }[] }> = {
  default: {
    answer:
      "Namaste! I am Brahm AI, your guide to Vedic wisdom. I draw knowledge from ancient Sanskrit texts, the Vedas, Upanishads, Bhagavad Gita, Jyotish Shastras, and thousands of scholarly works.\n\nI can help you with:\n- **Vedic Philosophy** — Dharma, Karma, Moksha, Atman\n- **Jyotish Shastra** — Kundali analysis, planetary effects, remedies\n- **Sanskrit Texts** — Translations and interpretations\n- **Hindu Calendar** — Panchang, Muhurta, Tithi calculations\n- **Mantras & Rituals** — Meanings and proper recitation\n\nAsk me anything about the vast ocean of Vedic knowledge!",
    sources: [],
  },
  dharma: {
    answer:
      'In the Bhagavad Gita (Chapter 2, Verse 7), Arjuna surrenders to Krishna asking for guidance on Dharma:\n\n**"कार्पण्यदोषोपहतस्वभावः पृच्छामि त्वां धर्मसम्मूढचेताः"**\n\n*"My heart is overpowered by weakness. My mind is confused about Dharma. I ask You: tell me decisively what is good for me."*\n\nKrishna responds that Dharma is not about mere duty — it is about righteous action aligned with cosmic order (Rita). In Chapter 3, He introduces **Nishkama Karma** — selfless action without attachment to results.\n\nThe Mahabharata defines Dharma as: **"धर्मो रक्षति रक्षितः"** — Dharma protects those who protect Dharma.\n\nKey aspects of Dharma from the Gita:\n1. **Svadharma** — One\'s own natural duty (3.35)\n2. **Samanya Dharma** — Universal ethics: non-violence, truth, compassion\n3. **Varnashrama Dharma** — Duties based on stage of life\n4. **Yuddha Dharma** — Righteous conduct even in conflict',
    sources: [
      { book: "Bhagavad Gita", page: 47 },
      { book: "Mahabharata - Shanti Parva", page: 231 },
      { book: "Dharma Sutras", page: 12 },
    ],
  },
  nakshatra: {
    answer:
      'The **27 Nakshatras** (lunar mansions) form the backbone of Vedic astrology, far more precise than the 12 Rashis.\n\nEach Nakshatra spans **13°20\'** of the zodiac and is ruled by a specific deity and planet:\n\n**Key Nakshatras and their significance:**\n\n1. **Ashwini** (0°-13°20\' Aries) — Ruled by Ashwini Kumaras, the divine physicians. Bestows healing ability and swift action.\n\n2. **Rohini** (10°-23°20\' Taurus) — Moon\'s favorite. Ruled by Brahma. Grants beauty, creativity, and material abundance.\n\n3. **Pushya** (3°20\'-16°40\' Cancer) — The most auspicious Nakshatra. Ruled by Brihaspati. Excellent for starting any venture.\n\n4. **Revati** (16°40\'-30° Pisces) — The final Nakshatra. Ruled by Pushan. Represents completion and spiritual journey.\n\nThe **Nakshatra system** is used for:\n- Birth chart analysis (Janma Nakshatra)\n- Compatibility matching (Ashta Koot)\n- Muhurta selection\n- Dasha calculation (Vimshottari)',
    sources: [
      { book: "Brihat Parashara Hora Shastra", page: 89 },
      { book: "Jataka Parijata", page: 156 },
      { book: "Muhurta Chintamani", page: 34 },
    ],
  },
  karma: {
    answer:
      'The concept of **Karma** (कर्म) is one of the most profound doctrines in Vedic philosophy.\n\n**From the Bhagavad Gita (4.17):**\n**"कर्मणो ह्यपि बोद्धव्यं बोद्धव्यं च विकर्मणः। अकर्मणश्च बोद्धव्यं गहना कर्मणो गतिः॥"**\n\n*"One must understand the nature of action, wrong action, and inaction. The path of action is deep and mysterious."*\n\n**Three Types of Karma:**\n1. **Sanchita Karma** — Accumulated karma from all past lives (the total account)\n2. **Prarabdha Karma** — The portion of Sanchita that is active in this life (your current destiny)\n3. **Kriyamana Karma** — New karma being created now by your present actions\n\n**From the Upanishads:**\nThe Brihadaranyaka Upanishad (4.4.5) states: *"You are what your deep, driving desire is. As your desire is, so is your will. As your will is, so is your deed. As your deed is, so is your destiny."*\n\nThis creates the cycle: **Desire → Action → Result → Impression → Desire**\n\nThe Gita\'s solution is **Nishkama Karma** — acting without attachment to fruits, breaking the cycle.',
    sources: [
      { book: "Bhagavad Gita", page: 92 },
      { book: "Brihadaranyaka Upanishad", page: 178 },
      { book: "Yoga Sutras of Patanjali", page: 45 },
    ],
  },
  meditation: {
    answer:
      'The Upanishads are the primary source of meditation teachings in Vedic literature.\n\n**Mandukya Upanishad — The Four States of Consciousness:**\n1. **Jagrat** (Waking) — Awareness of external world\n2. **Svapna** (Dream) — Internal mental activity\n3. **Sushupti** (Deep Sleep) — Beyond mind, pure rest\n4. **Turiya** (The Fourth) — Pure consciousness, the goal of meditation\n\n**"प्रणवो ह्यपरं ब्रह्म"** — *"Om is indeed the Supreme Brahman"* (Mandukya 1)\n\n**Katha Upanishad on Meditation (2.3.10-11):**\n*"When the five senses along with the mind are still, and the intellect does not stir — that is called the highest state. This steady control of the senses is called Yoga."*\n\n**Practical Meditation from Shvetashvatara Upanishad:**\n- Sit in a clean, quiet place\n- Keep body straight (head, neck, chest aligned)\n- Control the senses\n- Meditate on Om in the heart\n- The Self reveals itself like fire from friction sticks\n\n**Dhyana (ध्यान)** in the Yoga Sutras:\nPatanjali defines it as **"तत्र प्रत्ययैकतानता ध्यानम्"** — continuous flow of awareness toward one object.',
    sources: [
      { book: "Mandukya Upanishad", page: 3 },
      { book: "Katha Upanishad", page: 67 },
      { book: "Shvetashvatara Upanishad", page: 28 },
      { book: "Yoga Sutras of Patanjali", page: 51 },
    ],
  },
  yoga: {
    answer:
      'In Vedic astrology, **Yogas** are specific planetary combinations that shape destiny.\n\n**Most Powerful Benefic Yogas:**\n\n1. **Raja Yoga** — Lord of Kendra + Lord of Trikona in conjunction or mutual aspect. Grants power, authority, and success.\n\n2. **Gajakesari Yoga** — Jupiter in Kendra from Moon. One of the most celebrated yogas. Bestows wisdom, fame, and lasting prosperity.\n\n3. **Dhana Yoga** — Lords of 2nd and 11th houses connected. Indicates wealth accumulation.\n\n4. **Pancha Mahapurusha Yogas** (5 Great Person Yogas):\n   - **Hamsa** (Jupiter in own/exalted in Kendra) — Spiritual wisdom\n   - **Malavya** (Venus) — Beauty, luxury, arts\n   - **Ruchaka** (Mars) — Courage, leadership\n   - **Bhadra** (Mercury) — Intelligence, communication\n   - **Shasha** (Saturn) — Discipline, authority\n\n5. **Budhaditya Yoga** — Sun-Mercury conjunction. Sharp intellect.\n\n**Important:** A yoga\'s strength depends on:\n- Dignity of planets (exalted vs debilitated)\n- House placement\n- Aspects from benefics/malefics\n- Dasha activation period',
    sources: [
      { book: "Brihat Parashara Hora Shastra", page: 201 },
      { book: "Phaladeepika", page: 78 },
      { book: "Jataka Parijata", page: 134 },
    ],
  },
  gayatri: {
    answer:
      'The **Gayatri Mantra** is the most sacred mantra in Vedic tradition, from the Rig Veda (3.62.10).\n\n**ॐ भूर्भुवः स्वः\nतत्सवितुर्वरेण्यं\nभर्गो देवस्य धीमहि\nधियो यो नः प्रचोदयात्॥**\n\n**Word-by-word meaning:**\n- **ॐ (Om)** — The primordial sound, essence of all\n- **भूः (Bhur)** — Earth/Physical plane\n- **भुवः (Bhuvah)** — Atmosphere/Mental plane\n- **स्वः (Svah)** — Heaven/Spiritual plane\n- **तत् (Tat)** — That (Supreme Reality)\n- **सवितुः (Savitur)** — Of the Sun/Divine Light\n- **वरेण्यं (Varenyam)** — Most excellent, worthy of worship\n- **भर्गो (Bhargo)** — Radiance, spiritual effulgence\n- **देवस्य (Devasya)** — Of the Divine\n- **धीमहि (Dhimahi)** — We meditate upon\n- **धियो (Dhiyo)** — Intellect, understanding\n- **यो (Yo)** — Who\n- **नः (Nah)** — Our\n- **प्रचोदयात् (Prachodayat)** — May inspire/illuminate\n\n**Full Translation:**\n*"We meditate upon the glorious radiance of the Divine Sun. May that Supreme Light illuminate our intellect."*\n\n**Best time to chant:** Sandhya Kaal (dawn, noon, dusk) — 108 times with a Rudraksha mala.',
    sources: [
      { book: "Rig Veda (Mandala 3)", page: 62 },
      { book: "Chandogya Upanishad", page: 15 },
      { book: "Manusmriti", page: 44 },
    ],
  },
};

function getResponse(query: string) {
  const q = query.toLowerCase();
  if (q.includes("dharma") || q.includes("krishna") || q.includes("gita")) return mockResponses.dharma;
  if (q.includes("nakshatra") || q.includes("lunar")) return mockResponses.nakshatra;
  if (q.includes("karma")) return mockResponses.karma;
  if (q.includes("meditation") || q.includes("upanishad") || q.includes("dhyana")) return mockResponses.meditation;
  if (q.includes("yoga") || q.includes("kundali") || q.includes("kundli")) return mockResponses.yoga;
  if (q.includes("gayatri") || q.includes("mantra")) return mockResponses.gayatri;
  return mockResponses.default;
}

export default function AIChatPage() {
  const { messages: chatMessages, sources, streaming, sendMessage: sendChatMessage } = useChat();
  const [input, setInput] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  // Prepend welcome message
  const welcomeMsg: Message = {
    id: "welcome",
    role: "assistant",
    content: "Namaste! I am Brahm AI — your guide to Vedic wisdom.\nAsk me about Dharma, Karma, Mantras, Jyotish, Upanishads, and more.",
    timestamp: new Date(),
  };
  const messages: Message[] = [
    welcomeMsg,
    ...chatMessages.map((m, i) => ({
      id: String(i),
      role: m.role as "user" | "assistant",
      content: m.content,
      sources: undefined,
      timestamp: new Date(),
    })),
  ];

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [chatMessages, streaming]);

  const sendMessage = (text: string) => {
    if (!text.trim() || streaming) return;
    setInput("");
    sendChatMessage(text.trim());
  };

  const isTyping = streaming;

  return (
    <div className="flex flex-col h-[calc(100vh-4rem)]">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className="flex-shrink-0 pb-4">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-primary/10">
            <Bot className="h-6 w-6 text-primary" />
          </div>
          <div>
            <h1 className="font-display text-2xl text-primary text-glow-gold">Brahm AI</h1>
            <p className="text-xs text-muted-foreground">
              {/* GPU RAG Engine — Sanskrit / Hindi / English Knowledge Base */}

            </p>
          </div>
          <Badge variant="secondary" className="ml-auto text-xs">
            Vedic Assistant
          </Badge>
        </div>
      </motion.div>

      {/* Messages */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto space-y-4 pr-2 pb-4 scrollbar-thin">
        <AnimatePresence initial={false}>
          {messages.map((msg) => (
            <motion.div
              key={msg.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className={`flex gap-3 ${msg.role === "user" ? "justify-end" : ""}`}
            >
              {msg.role === "assistant" && (
                <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
                  <Sparkles className="h-4 w-4 text-primary" />
                </div>
              )}
              <div className={`max-w-[80%] ${msg.role === "user" ? "order-first" : ""}`}>
                <Card className={`${msg.role === "user" ? "bg-primary text-primary-foreground border-primary" : "glass border-border/30"}`}>
                  <CardContent className="p-4 text-sm whitespace-pre-line leading-relaxed">
                    {msg.content.split(/(\*\*[^*]+\*\*)/).map((part, i) =>
                      part.startsWith("**") && part.endsWith("**") ? (
                        <strong key={i} className={msg.role === "user" ? "text-primary-foreground" : "text-foreground"}>
                          {part.slice(2, -2)}
                        </strong>
                      ) : (
                        <span key={i}>{part}</span>
                      ),
                    )}
                  </CardContent>
                </Card>
                {msg.role === "assistant" && sources.length > 0 && msg.id === String(chatMessages.length - 1) && (
                  <div className="flex flex-wrap gap-1.5 mt-2 ml-1">
                    <BookOpen className="h-3 w-3 text-muted-foreground mt-0.5" />
                    {sources.map((src, i) => (
                      <Badge key={i} variant="outline" className="text-xs font-normal">
                        {src.book}
                      </Badge>
                    ))}
                  </div>
                )}
              </div>
              {msg.role === "user" && (
                <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-muted flex items-center justify-center">
                  <User className="h-4 w-4 text-muted-foreground" />
                </div>
              )}
            </motion.div>
          ))}
        </AnimatePresence>

        {isTyping && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex gap-3">
            <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
              <Sparkles className="h-4 w-4 text-primary" />
            </div>
            <Card className="glass border-border/30">
              <CardContent className="p-4 flex items-center gap-2">
                <Loader2 className="h-4 w-4 animate-spin text-primary" />
                <span className="text-sm text-muted-foreground">Searching knowledge base...</span>
              </CardContent>
            </Card>
          </motion.div>
        )}
      </div>

      {/* Suggestions */}
      {messages.length <= 1 && (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} className="flex-shrink-0 pb-3">
          <p className="text-xs text-muted-foreground mb-2">Try asking about:</p>
          <div className="flex flex-wrap gap-2">
            {suggestedQueries.map((sq) => (
              <Button
                key={sq.label}
                variant="outline"
                size="sm"
                className="text-xs h-7 border-border/50 hover:bg-primary/10 hover:text-primary"
                onClick={() => sendMessage(sq.query)}
              >
                {sq.label}
              </Button>
            ))}
          </div>
        </motion.div>
      )}

      {/* Input */}
      <div className="flex-shrink-0 pt-2 border-t border-border/30">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            sendMessage(input);
          }}
          className="flex gap-2"
        >
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask about Vedic wisdom, Jyotish, Mantras, Dharma..."
            className="flex-1 bg-muted/30 border-border/40"
            disabled={isTyping}
          />
          <Button type="submit" size="icon" disabled={!input.trim() || isTyping} className="shrink-0">
            <Send className="h-4 w-4" />
          </Button>
        </form>
        <p className="text-xs text-muted-foreground mt-1.5 text-center">
          {/* Powered by Qwen 2.5-7B + FAISS Hybrid RAG — 100k+ Sanskrit, Hindi & English texts */}
        </p>
      </div>
    </div>
  );
}
