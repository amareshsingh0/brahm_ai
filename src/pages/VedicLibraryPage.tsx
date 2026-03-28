import { useState } from "react";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Search, BookOpen, ScrollText, Flame, Feather } from "lucide-react";
import { useTranslation } from "react-i18next";

// Categories are defined as a function so they can use translations
function getCategories(t: (key: string) => string) {
  return [
  {
    id: "vedas",
    label: t("data.library.vedas_label"),
    icon: Flame,
    description: t("data.library.vedas_desc"),
    texts: [
      {
        name: "Rig Veda",
        sanskrit: "ऋग्वेद",
        books: 10,
        hymns: 1028,
        language: "Vedic Sanskrit",
        description: t("data.library.rig_veda_desc"),
        keyTopics: t("data.library.rig_veda_topics").split(", "),
      },
      {
        name: "Yajur Veda",
        sanskrit: "यजुर्वेद",
        books: 40,
        hymns: 1975,
        language: "Vedic Sanskrit",
        description: t("data.library.yajur_veda_desc"),
        keyTopics: t("data.library.yajur_veda_topics").split(", "),
      },
      {
        name: "Sama Veda",
        sanskrit: "सामवेद",
        books: 2,
        hymns: 1549,
        language: "Vedic Sanskrit",
        description: t("data.library.sama_veda_desc"),
        keyTopics: t("data.library.sama_veda_topics").split(", "),
      },
      {
        name: "Atharva Veda",
        sanskrit: "अथर्ववेद",
        books: 20,
        hymns: 730,
        language: "Vedic Sanskrit",
        description: t("data.library.atharva_veda_desc"),
        keyTopics: t("data.library.atharva_veda_topics").split(", "),
      },
    ],
  },
  {
    id: "upanishads",
    label: t("data.library.upanishads_label"),
    icon: ScrollText,
    description: t("data.library.upanishads_desc"),
    texts: [
      {
        name: "Isha Upanishad",
        sanskrit: "ईशोपनिषद्",
        verses: 18,
        veda: "Shukla Yajur Veda",
        description: t("data.library.isha_upanishad_desc"),
        keyTopics: t("data.library.isha_upanishad_topics").split(", "),
      },
      {
        name: "Kena Upanishad",
        sanskrit: "केनोपनिषद्",
        verses: 35,
        veda: "Sama Veda",
        description: t("data.library.kena_upanishad_desc"),
        keyTopics: t("data.library.kena_upanishad_topics").split(", "),
      },
      {
        name: "Katha Upanishad",
        sanskrit: "कठोपनिषद्",
        verses: 119,
        veda: "Krishna Yajur Veda",
        description: t("data.library.katha_upanishad_desc"),
        keyTopics: t("data.library.katha_upanishad_topics").split(", "),
      },
      {
        name: "Mandukya Upanishad",
        sanskrit: "माण्डूक्योपनिषद्",
        verses: 12,
        veda: "Atharva Veda",
        description: t("data.library.mandukya_upanishad_desc"),
        keyTopics: t("data.library.mandukya_upanishad_topics").split(", "),
      },
      {
        name: "Chandogya Upanishad",
        sanskrit: "छान्दोग्योपनिषद्",
        chapters: 8,
        veda: "Sama Veda",
        description: t("data.library.chandogya_upanishad_desc"),
        keyTopics: t("data.library.chandogya_upanishad_topics").split(", "),
      },
      {
        name: "Brihadaranyaka Upanishad",
        sanskrit: "बृहदारण्यकोपनिषद्",
        chapters: 6,
        veda: "Shukla Yajur Veda",
        description: t("data.library.brihadaranyaka_upanishad_desc"),
        keyTopics: t("data.library.brihadaranyaka_upanishad_topics").split(", "),
      },
    ],
  },
  {
    id: "gita",
    label: t("data.library.gita_label"),
    icon: Feather,
    description: t("data.library.gita_desc"),
    texts: [
      {
        name: "Ch 1-3: The Crisis & Karma Yoga",
        sanskrit: "अर्जुनविषादयोग / सांख्ययोग / कर्मयोग",
        verses: 126,
        description: t("data.library.gita_ch1_3_desc"),
        keyTopics: t("data.library.gita_ch1_3_topics").split(", "),
      },
      {
        name: "Ch 4-6: Knowledge & Meditation",
        sanskrit: "ज्ञानकर्मसन्यासयोग / कर्मसन्यासयोग / ध्यानयोग",
        verses: 131,
        description: t("data.library.gita_ch4_6_desc"),
        keyTopics: t("data.library.gita_ch4_6_topics").split(", "),
      },
      {
        name: "Ch 7-9: Divine Knowledge & Royal Secret",
        sanskrit: "ज्ञानविज्ञानयोग / अक्षरब्रह्मयोग / राजविद्याराजगुह्ययोग",
        verses: 101,
        description: t("data.library.gita_ch7_9_desc"),
        keyTopics: t("data.library.gita_ch7_9_topics").split(", "),
      },
      {
        name: "Ch 10-12: Divine Glory & Devotion",
        sanskrit: "विभूतियोग / विश्वरूपदर्शनयोग / भक्तियोग",
        verses: 117,
        description: t("data.library.gita_ch10_12_desc"),
        keyTopics: t("data.library.gita_ch10_12_topics").split(", "),
      },
      {
        name: "Ch 13-15: Field, Gunas & Supreme Self",
        sanskrit: "क्षेत्रक्षेत्रज्ञविभागयोग / गुणत्रयविभागयोग / पुरुषोत्तमयोग",
        verses: 100,
        description: t("data.library.gita_ch13_15_desc"),
        keyTopics: t("data.library.gita_ch13_15_topics").split(", "),
      },
      {
        name: "Ch 16-18: Liberation & Surrender",
        sanskrit: "दैवासुरसम्पद्विभागयोग / श्रद्धात्रयविभागयोग / मोक्षसन्यासयोग",
        verses: 125,
        description: t("data.library.gita_ch16_18_desc"),
        keyTopics: t("data.library.gita_ch16_18_topics").split(", "),
      },
    ],
  },
  {
    id: "dharma",
    label: t("data.library.dharma_label"),
    icon: BookOpen,
    description: t("data.library.dharma_desc"),
    texts: [
      {
        name: "Mahabharata",
        sanskrit: "महाभारत",
        verses: 100000,
        author: "Vyasa",
        description: t("data.library.mahabharata_desc"),
        keyTopics: t("data.library.mahabharata_topics").split(", "),
      },
      {
        name: "Ramayana",
        sanskrit: "रामायण",
        verses: 24000,
        author: "Valmiki",
        description: t("data.library.ramayana_desc"),
        keyTopics: t("data.library.ramayana_topics").split(", "),
      },
      {
        name: "Yoga Sutras",
        sanskrit: "योगसूत्राणि",
        verses: 196,
        author: "Patanjali",
        description: t("data.library.yoga_sutras_desc"),
        keyTopics: t("data.library.yoga_sutras_topics").split(", "),
      },
      {
        name: "Brahma Sutras",
        sanskrit: "ब्रह्मसूत्राणि",
        verses: 555,
        author: "Badarayana",
        description: t("data.library.brahma_sutras_desc"),
        keyTopics: t("data.library.brahma_sutras_topics").split(", "),
      },
      {
        name: "Arthashastra",
        sanskrit: "अर्थशास्त्र",
        chapters: 150,
        author: "Kautilya (Chanakya)",
        description: t("data.library.arthashastra_desc"),
        keyTopics: t("data.library.arthashastra_topics").split(", "),
      },
      {
        name: "Manusmriti",
        sanskrit: "मनुस्मृति",
        verses: 2685,
        author: "Manu",
        description: t("data.library.manusmriti_desc"),
        keyTopics: t("data.library.manusmriti_topics").split(", "),
      },
    ],
  },
];
}

export default function VedicLibraryPage() {
  const { t } = useTranslation();
  const [search, setSearch] = useState("");
  const categories = getCategories(t);

  return (
    <div className="space-y-6 p-4 md:p-6">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-3xl text-primary text-glow-gold">{t("library.title")}</h1>
      </motion.div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t("library.search_placeholder")}
          className="pl-9 bg-muted/30 border-border/40"
        />
      </div>

      <Tabs defaultValue="vedas">
        <TabsList className="w-full justify-start overflow-x-auto flex-nowrap">
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
