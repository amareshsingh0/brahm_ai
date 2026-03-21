import { useState } from "react";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Search, BookOpen, ScrollText, Flame, Feather, Loader2 } from "lucide-react";
import { useSearch } from "@/hooks/useSearch";

const categories = [
  {
    id: "vedas",
    label: "Vedas",
    icon: Flame,
    description: "The foundational scriptures of Sanatan Dharma — Shruti (that which is heard)",
    texts: [
      {
        name: "Rig Veda",
        sanskrit: "ऋग्वेद",
        books: 10,
        hymns: 1028,
        language: "Vedic Sanskrit",
        description:
          "The oldest and most important Veda. Contains hymns (Suktas) praising Agni, Indra, Varuna, and other deities. The Gayatri Mantra originates from Mandala 3. Foundation of all Vedic knowledge.",
        keyTopics: ["Cosmology", "Agni Sukta", "Purusha Sukta", "Nasadiya Sukta", "Gayatri Mantra"],
      },
      {
        name: "Yajur Veda",
        sanskrit: "यजुर्वेद",
        books: 40,
        hymns: 1975,
        language: "Vedic Sanskrit",
        description:
          "The Veda of rituals and liturgy. Divided into Shukla (White) and Krishna (Black) recensions. Contains mantras and instructions for performing Yajnas (fire rituals). Includes the famous Isha Upanishad.",
        keyTopics: ["Yajna Vidhi", "Rudram", "Chamakam", "Shatarudriya", "Ritual Formulas"],
      },
      {
        name: "Sama Veda",
        sanskrit: "सामवेद",
        books: 2,
        hymns: 1549,
        language: "Vedic Sanskrit",
        description:
          "The Veda of melodies and chants. Most verses derived from Rig Veda but set to musical notation (Saman). Krishna in the Gita says: 'Among the Vedas, I am the Sama Veda.' Origin of Indian classical music.",
        keyTopics: ["Musical Notation", "Udgitha", "Jaiminiya Sama", "Kauthuma", "Ranayaniya"],
      },
      {
        name: "Atharva Veda",
        sanskrit: "अथर्ववेद",
        books: 20,
        hymns: 730,
        language: "Vedic Sanskrit",
        description:
          "The Veda of everyday knowledge. Contains hymns for healing, protection, and daily life. Covers medicine (Ayurveda roots), architecture, mathematics, and philosophical speculation. Named after sage Atharvan.",
        keyTopics: ["Healing Mantras", "Prithvi Sukta", "Marriage Hymns", "Ayurveda Origins", "Protection Mantras"],
      },
    ],
  },
  {
    id: "upanishads",
    label: "Upanishads",
    icon: ScrollText,
    description: "Philosophical texts exploring Brahman, Atman, and the nature of reality — Vedanta",
    texts: [
      {
        name: "Isha Upanishad",
        sanskrit: "ईशोपनिषद्",
        verses: 18,
        veda: "Shukla Yajur Veda",
        description:
          "The shortest yet most profound Upanishad. Opens with 'Ishavasyam idam sarvam' — All this is pervaded by the Lord. Teaches the unity of action and knowledge, and the nature of the Self.",
        keyTopics: ["Ishvara", "Karma-Jnana Synthesis", "Vidya-Avidya"],
      },
      {
        name: "Kena Upanishad",
        sanskrit: "केनोपनिषद्",
        verses: 35,
        veda: "Sama Veda",
        description:
          "Asks 'By whom (Kena) is the mind directed?' Explores the power behind all senses and cognition. Contains the story of Brahman humbling the Devas — showing that true power is beyond the gods.",
        keyTopics: ["Source of Consciousness", "Uma Haimavati", "Beyond the Senses"],
      },
      {
        name: "Katha Upanishad",
        sanskrit: "कठोपनिषद्",
        verses: 119,
        veda: "Krishna Yajur Veda",
        description:
          "The dialogue between young Nachiketa and Yama (Death). Teaches about the Self (Atman) that is beyond death. Contains the chariot metaphor — body is chariot, intellect is charioteer, mind is reins.",
        keyTopics: ["Nachiketa", "Atman", "Chariot Metaphor", "Shreyas vs Preyas"],
      },
      {
        name: "Mandukya Upanishad",
        sanskrit: "माण्डूक्योपनिषद्",
        verses: 12,
        veda: "Atharva Veda",
        description:
          "Only 12 verses but considered sufficient for liberation. Analyzes Om (AUM) and the four states of consciousness: Waking, Dream, Deep Sleep, and Turiya (the transcendent Fourth).",
        keyTopics: ["AUM Analysis", "Four States", "Turiya", "Consciousness"],
      },
      {
        name: "Chandogya Upanishad",
        sanskrit: "छान्दोग्योपनिषद्",
        chapters: 8,
        veda: "Sama Veda",
        description:
          "One of the largest Upanishads. Contains the famous teaching 'Tat Tvam Asi' (Thou art That) — Uddalaka teaching his son Shvetaketu. Also covers Udgitha meditation and the Panchagni Vidya.",
        keyTopics: ["Tat Tvam Asi", "Udgitha", "Panchagni Vidya", "Bhuma Vidya"],
      },
      {
        name: "Brihadaranyaka Upanishad",
        sanskrit: "बृहदारण्यकोपनिषद्",
        chapters: 6,
        veda: "Shukla Yajur Veda",
        description:
          "The largest and oldest Upanishad. Contains Yajnavalkya's teachings to Maitreyi and King Janaka. Introduces 'Neti Neti' (not this, not this) as the method to know Brahman.",
        keyTopics: ["Neti Neti", "Yajnavalkya-Maitreyi", "Madhu Vidya", "Karma Theory"],
      },
    ],
  },
  {
    id: "gita",
    label: "Bhagavad Gita",
    icon: Feather,
    description: "The Song of God — Krishna's 700 verses of wisdom to Arjuna on the battlefield",
    texts: [
      {
        name: "Ch 1-3: The Crisis & Karma Yoga",
        sanskrit: "अर्जुनविषादयोग / सांख्ययोग / कर्मयोग",
        verses: 126,
        description:
          "Arjuna's despair on the battlefield. Krishna's teaching on the eternal Self, the distinction between body and soul, and the path of selfless action (Nishkama Karma).",
        keyTopics: ["Arjuna's Despair", "Immortality of Atman", "Nishkama Karma", "Svadharma"],
      },
      {
        name: "Ch 4-6: Knowledge & Meditation",
        sanskrit: "ज्ञानकर्मसन्यासयोग / कर्मसन्यासयोग / ध्यानयोग",
        verses: 131,
        description:
          "The yoga of knowledge and renunciation. Krishna reveals He incarnates in every age. Teaches meditation technique, equanimity, and the nature of a true Yogi.",
        keyTopics: ["Divine Incarnation", "Jnana Yoga", "Dhyana Technique", "Sthitaprajna"],
      },
      {
        name: "Ch 7-9: Divine Knowledge & Royal Secret",
        sanskrit: "ज्ञानविज्ञानयोग / अक्षरब्रह्मयोग / राजविद्याराजगुह्ययोग",
        verses: 101,
        description:
          "Krishna reveals His divine nature as the source of all existence. The supreme secret: total devotion. 'Whatever you do, offer it to Me.'",
        keyTopics: ["Prakriti & Purusha", "Akshara Brahman", "Bhakti", "Raja Vidya"],
      },
      {
        name: "Ch 10-12: Divine Glory & Devotion",
        sanskrit: "विभूतियोग / विश्वरूपदर्शनयोग / भक्तियोग",
        verses: 117,
        description:
          "Krishna's divine manifestations (Vibhuti). The cosmic vision (Vishvarupa) — Arjuna sees the entire universe within Krishna. The path of pure devotion declared supreme.",
        keyTopics: ["Vibhuti", "Vishvarupa Darshan", "Bhakti Yoga", "The Cosmic Form"],
      },
      {
        name: "Ch 13-15: Field, Gunas & Supreme Self",
        sanskrit: "क्षेत्रक्षेत्रज्ञविभागयोग / गुणत्रयविभागयोग / पुरुषोत्तमयोग",
        verses: 100,
        description:
          "The body as Field and the Self as Knower. The three Gunas (Sattva, Rajas, Tamas) and how to transcend them. The metaphor of the inverted Ashvattha tree.",
        keyTopics: ["Kshetra-Kshetrajna", "Three Gunas", "Purushottama", "Ashvattha Tree"],
      },
      {
        name: "Ch 16-18: Liberation & Surrender",
        sanskrit: "दैवासुरसम्पद्विभागयोग / श्रद्धात्रयविभागयोग / मोक्षसन्यासयोग",
        verses: 125,
        description:
          "Divine vs demoniac qualities. Three types of faith, food, and worship. The grand conclusion: 'Abandon all dharmas and surrender to Me alone. I shall liberate you from all sins.'",
        keyTopics: ["Daivi-Asuri Sampad", "Shraddha Traya", "Moksha", "Sarva-dharman Parityajya"],
      },
    ],
  },
  {
    id: "dharma",
    label: "Dharma Texts",
    icon: BookOpen,
    description: "Smriti texts — Itihasas, Puranas, Dharma Shastras, and philosophical treatises",
    texts: [
      {
        name: "Mahabharata",
        sanskrit: "महाभारत",
        verses: 100000,
        author: "Vyasa",
        description:
          "The world's longest epic. Contains the Bhagavad Gita, the story of the Kuru dynasty, and the great war at Kurukshetra. 'What is found here may be found elsewhere; what is not here is nowhere.'",
        keyTopics: ["Kurukshetra War", "Dharma", "Bhishma's Teachings", "Vidura Niti"],
      },
      {
        name: "Ramayana",
        sanskrit: "रामायण",
        verses: 24000,
        author: "Valmiki",
        description:
          "The epic of Lord Rama — the ideal king, son, husband, and warrior. Teaches Maryada (righteousness through example). Seven Kandas from Bala to Uttara.",
        keyTopics: ["Rama's Exile", "Sita's Devotion", "Hanuman's Service", "Dharma Rajya"],
      },
      {
        name: "Yoga Sutras",
        sanskrit: "योगसूत्राणि",
        verses: 196,
        author: "Patanjali",
        description:
          "The definitive text on Yoga philosophy and practice. 'Yogash Chitta Vritti Nirodhah' — Yoga is the cessation of mind fluctuations. Covers Ashtanga (8 limbs) system.",
        keyTopics: ["Ashtanga Yoga", "Samadhi", "Klesha", "Ishvara Pranidhana", "Kaivalya"],
      },
      {
        name: "Brahma Sutras",
        sanskrit: "ब्रह्मसूत्राणि",
        verses: 555,
        author: "Badarayana",
        description:
          "Systematic philosophical treatise analyzing Upanishadic teachings. One of the Prasthana Traya (triple foundation) along with Upanishads and Gita. Basis of all Vedanta schools.",
        keyTopics: ["Brahman", "Jiva", "Maya", "Moksha", "Vedanta Schools"],
      },
      {
        name: "Arthashastra",
        sanskrit: "अर्थशास्त्र",
        chapters: 150,
        author: "Kautilya (Chanakya)",
        description:
          "Ancient treatise on statecraft, economic policy, and military strategy. Covers governance, law, taxation, diplomacy, and espionage. Practical wisdom for leadership.",
        keyTopics: ["Rajya Shastra", "Danda Niti", "Kosha", "Foreign Policy", "Administration"],
      },
      {
        name: "Manusmriti",
        sanskrit: "मनुस्मृति",
        verses: 2685,
        author: "Manu",
        description:
          "Dharma Shastra covering law, duty, and social conduct. Discusses Varnashrama Dharma, Samskaras (life rituals), and moral codes. Must be read in historical-philosophical context.",
        keyTopics: ["Dharma", "Samskaras", "Raja Dharma", "Prayaschitta", "Achara"],
      },
    ],
  },
];

export default function VedicLibraryPage() {
  const [search, setSearch] = useState("");
  const { data: ragResults, isFetching: ragLoading } = useSearch(search);

  return (
    <div className="space-y-6">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-3xl text-primary text-glow-gold">Vedic Library</h1>
        <p className="text-muted-foreground mt-1">
          Explore the complete repository of Vedic & Sanskrit texts — Shruti, Smriti, Itihasas & Shastras
        </p>
      </motion.div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search texts, topics, or keywords..."
          className="pl-9 bg-muted/30 border-border/40"
        />
      </div>

      {/* RAG search results from FAISS knowledge base */}
      {search.length > 2 && (
        <div className="space-y-2">
          <p className="text-xs text-muted-foreground flex items-center gap-1">
            {ragLoading ? <Loader2 className="h-3 w-3 animate-spin" /> : <Search className="h-3 w-3" />}
            Knowledge base results for "{search}"
          </p>
          {ragResults?.results.map((r, i) => (
            <motion.div key={i} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.04 }}>
              <Card className="glass border-border/30">
                <CardContent className="pt-4 pb-3">
                  <p className="text-xs text-muted-foreground mb-1">{r.source} · {r.language}</p>
                  <p className="text-sm text-foreground">{r.text}</p>
                </CardContent>
              </Card>
            </motion.div>
          ))}
        </div>
      )}

      <Tabs defaultValue="vedas">
        <TabsList className="w-full justify-start overflow-x-auto">
          {categories.map((cat) => (
            <TabsTrigger key={cat.id} value={cat.id} className="gap-1.5">
              <cat.icon className="h-3.5 w-3.5" />
              {cat.label}
            </TabsTrigger>
          ))}
        </TabsList>

        {categories.map((cat) => (
          <TabsContent key={cat.id} value={cat.id} className="space-y-4 mt-4">
            <p className="text-sm text-muted-foreground">{cat.description}</p>
            <div className="grid gap-4">
              {cat.texts
                .filter(
                  (t) =>
                    !search ||
                    t.name.toLowerCase().includes(search.toLowerCase()) ||
                    t.description.toLowerCase().includes(search.toLowerCase()) ||
                    t.keyTopics.some((k) => k.toLowerCase().includes(search.toLowerCase())),
                )
                .map((text, i) => (
                  <motion.div key={text.name} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.06 }}>
                    <Card className="glass border-border/30 hover:glow-border transition-shadow">
                      <CardHeader className="pb-2">
                        <div className="flex items-start justify-between">
                          <div>
                            <CardTitle className="text-lg flex items-center gap-2">
                              {text.name}
                              <span className="text-sm font-normal text-primary">{text.sanskrit}</span>
                            </CardTitle>
                            {"author" in text && (
                              <p className="text-xs text-muted-foreground mt-0.5">by {(text as any).author}</p>
                            )}
                          </div>
                          <div className="flex gap-2">
                            {"verses" in text && (
                              <Badge variant="secondary" className="text-xs">
                                {(text as any).verses.toLocaleString()} verses
                              </Badge>
                            )}
                            {"hymns" in text && (
                              <Badge variant="secondary" className="text-xs">
                                {(text as any).hymns.toLocaleString()} hymns
                              </Badge>
                            )}
                            {"chapters" in text && (
                              <Badge variant="secondary" className="text-xs">
                                {(text as any).chapters} chapters
                              </Badge>
                            )}
                            {"books" in text && (
                              <Badge variant="outline" className="text-xs">
                                {(text as any).books} books
                              </Badge>
                            )}
                          </div>
                        </div>
                      </CardHeader>
                      <CardContent>
                        <p className="text-sm text-muted-foreground leading-relaxed">{text.description}</p>
                        <div className="flex flex-wrap gap-1.5 mt-3">
                          {text.keyTopics.map((topic) => (
                            <Badge key={topic} variant="outline" className="text-xs font-normal border-primary/30 text-primary">
                              {topic}
                            </Badge>
                          ))}
                        </div>
                      </CardContent>
                    </Card>
                  </motion.div>
                ))}
            </div>
          </TabsContent>
        ))}
      </Tabs>
    </div>
  );
}
