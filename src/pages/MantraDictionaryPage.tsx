import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Search, Volume2, ChevronDown, ChevronUp } from "lucide-react";
import { useSearch } from "@/hooks/useSearch";
import { useTranslation } from "react-i18next";

const MANTRA_STATIC = [
  {
    id: 1, name: "Gayatri Mantra", deity: "Savitri (Sun)", source: "Rig Veda 3.62.10",
    sanskrit: "ॐ भूर्भुवः स्वः\nतत्सवितुर्वरेण्यं\nभर्गो देवस्य धीमहि\nधियो यो नः प्रचोदयात्॥",
    transliteration: "Om Bhur Bhuvah Svah\nTat Savitur Varenyam\nBhargo Devasya Dhimahi\nDhiyo Yo Nah Prachodayat",
    meaningKey: "data.mantras.gayatri_meaning", benefitsKey: "data.mantras.gayatri_benefits",
    chantCount: 108, category: "Universal",
  },
  {
    id: 2, name: "Mahamrityunjaya Mantra", deity: "Lord Shiva", source: "Rig Veda 7.59.12",
    sanskrit: "ॐ त्र्यम्बकं यजामहे\nसुगन्धिं पुष्टिवर्धनम्।\nउर्वारुकमिव बन्धनान्\nमृत्योर्मुक्षीय मामृतात्॥",
    transliteration: "Om Tryambakam Yajamahe\nSugandhim Pushtivardhanam\nUrvarukamiva Bandhanan\nMrityor Mukshiya Maamritat",
    meaningKey: "data.mantras.mahamrityunjaya_meaning", benefitsKey: "data.mantras.mahamrityunjaya_benefits",
    chantCount: 108, category: "Protection",
  },
  {
    id: 3, name: "Om Namah Shivaya", deity: "Lord Shiva", source: "Krishna Yajur Veda (Shri Rudram)",
    sanskrit: "ॐ नमः शिवाय",
    transliteration: "Om Namah Shivaya",
    meaningKey: "data.mantras.om_namah_shivaya_meaning", benefitsKey: "data.mantras.om_namah_shivaya_benefits",
    chantCount: 108, category: "Devotion",
  },
  {
    id: 4, name: "Vishnu Sahasranama", deity: "Lord Vishnu", source: "Mahabharata - Anushasana Parva",
    sanskrit: "ॐ विश्वं विष्णुर्वषट्कारो\nभूतभव्यभवत्प्रभुः।\nभूतकृद्भूतभृद्भावो\nभूतात्मा भूतभावनः॥",
    transliteration: "Om Vishvam Vishnur Vashatkaro\nBhuta-Bhavya-Bhavat-Prabhuh\nBhutakrid Bhutabhrid Bhavo\nBhutatma Bhutabhavanah",
    meaningKey: "data.mantras.vishnu_sahasranama_meaning", benefitsKey: "data.mantras.vishnu_sahasranama_benefits",
    chantCount: 1, category: "Devotion",
  },
  {
    id: 5, name: "Hanuman Chalisa", deity: "Lord Hanuman", source: "Tulsidas (16th century)",
    sanskrit: "श्रीगुरु चरन सरोज रज\nनिज मनु मुकुरु सुधारि।\nबरनउँ रघुबर बिमल जसु\nजो दायकु फल चारि॥",
    transliteration: "Shri Guru Charan Saroj Raj\nNij Manu Mukuru Sudhari\nBaranau Raghubar Bimal Jasu\nJo Dayaku Phal Chari",
    meaningKey: "data.mantras.hanuman_chalisa_meaning", benefitsKey: "data.mantras.hanuman_chalisa_benefits",
    chantCount: 7, category: "Protection",
  },
  {
    id: 6, name: "Lakshmi Mantra", deity: "Goddess Lakshmi", source: "Sri Sukta (Rig Veda)",
    sanskrit: "ॐ श्रीं ह्रीं श्रीं\nकमले कमलालये\nप्रसीद प्रसीद\nॐ श्रीं ह्रीं श्रीं\nमहालक्ष्म्यै नमः॥",
    transliteration: "Om Shreem Hreem Shreem\nKamale Kamalalaye\nPrasida Prasida\nOm Shreem Hreem Shreem\nMahalakshmyai Namah",
    meaningKey: "data.mantras.lakshmi_meaning", benefitsKey: "data.mantras.lakshmi_benefits",
    chantCount: 108, category: "Prosperity",
  },
  {
    id: 7, name: "Saraswati Mantra", deity: "Goddess Saraswati", source: "Saraswati Stotram",
    sanskrit: "ॐ ऐं सरस्वत्यै नमः\n\nया कुन्देन्दुतुषारहारधवला\nया शुभ्रवस्त्रावृता।\nया वीणावरदण्डमण्डितकरा\nया श्वेतपद्मासना॥",
    transliteration: "Om Aim Saraswatyai Namah\n\nYa Kundendu Tushara Hara Dhavala\nYa Shubhra Vastravruta\nYa Veena Vara Danda Mandita Kara\nYa Shveta Padmasana",
    meaningKey: "data.mantras.saraswati_meaning", benefitsKey: "data.mantras.saraswati_benefits",
    chantCount: 108, category: "Knowledge",
  },
  {
    id: 8, name: "Ganesh Mantra", deity: "Lord Ganesha", source: "Ganapati Atharvashirsha",
    sanskrit: "ॐ गं गणपतये नमः\n\nवक्रतुण्ड महाकाय\nसूर्यकोटि समप्रभ।\nनिर्विघ्नं कुरु मे देव\nसर्वकार्येषु सर्वदा॥",
    transliteration: "Om Gam Ganapataye Namah\n\nVakratunda Mahakaya\nSuryakoti Samaprabha\nNirvighnam Kuru Me Deva\nSarvakaryeshu Sarvada",
    meaningKey: "data.mantras.ganesh_meaning", benefitsKey: "data.mantras.ganesh_benefits",
    chantCount: 108, category: "Universal",
  },
  {
    id: 9, name: "Navgraha Mantra", deity: "Nine Planets", source: "Navagraha Stotram",
    sanskrit: "ॐ सूर्याय नमः। ॐ चन्द्राय नमः।\nॐ मङ्गलाय नमः। ॐ बुधाय नमः।\nॐ बृहस्पतये नमः। ॐ शुक्राय नमः।\nॐ शनैश्चराय नमः।\nॐ राहवे नमः। ॐ केतवे नमः।",
    transliteration: "Om Suryaya Namah. Om Chandraya Namah.\nOm Mangalaya Namah. Om Budhaya Namah.\nOm Brihaspataye Namah. Om Shukraya Namah.\nOm Shanaischaraya Namah.\nOm Rahave Namah. Om Ketave Namah.",
    meaningKey: "data.mantras.navgraha_meaning", benefitsKey: "data.mantras.navgraha_benefits",
    chantCount: 9, category: "Astrology",
  },
  {
    id: 10, name: "Shanti Mantra", deity: "Universal Peace", source: "Brihadaranyaka Upanishad",
    sanskrit: "ॐ सह नाववतु। सह नौ भुनक्तु।\nसह वीर्यं करवावहै।\nतेजस्वि नावधीतमस्तु\nमा विद्विषावहै।\nॐ शान्तिः शान्तिः शान्तिः॥",
    transliteration: "Om Saha Navavatu. Saha Nau Bhunaktu.\nSaha Viryam Karavavahai.\nTejasvi Navadhitamastu\nMa Vidvishavahai.\nOm Shantih Shantih Shantih",
    meaningKey: "data.mantras.shanti_meaning", benefitsKey: "data.mantras.shanti_benefits",
    chantCount: 3, category: "Universal",
  },
];

const categoryColors: Record<string, string> = {
  Universal: "bg-blue-500/10 text-blue-400 border-blue-500/30",
  Protection: "bg-red-500/10 text-red-400 border-red-500/30",
  Devotion: "bg-purple-500/10 text-purple-400 border-purple-500/30",
  Prosperity: "bg-yellow-500/10 text-yellow-400 border-yellow-500/30",
  Knowledge: "bg-cyan-500/10 text-cyan-400 border-cyan-500/30",
  Astrology: "bg-orange-500/10 text-orange-400 border-orange-500/30",
};

export default function MantraDictionaryPage() {
  const { t } = useTranslation();
  const [search, setSearch] = useState("");
  const { data: ragResults } = useSearch(search);
  const [expanded, setExpanded] = useState<number | null>(null);
  const [filter, setFilter] = useState<string | null>(null);

  // Resolve translations at render time
  const mantras = MANTRA_STATIC.map((m) => ({
    ...m,
    meaning: t(m.meaningKey),
    benefits: t(m.benefitsKey).split(", "),
  }));

  const allCategories = [...new Set(mantras.map((m) => m.category))];

  const filtered = mantras.filter((m) => {
    const matchSearch =
      !search ||
      m.name.toLowerCase().includes(search.toLowerCase()) ||
      m.deity.toLowerCase().includes(search.toLowerCase()) ||
      m.meaning.toLowerCase().includes(search.toLowerCase());
    const matchCategory = !filter || m.category === filter;
    return matchSearch && matchCategory;
  });

  return (
    <div className="space-y-6">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-3xl text-primary text-glow-gold">{t("mantras.title")}</h1>
        <p className="text-muted-foreground mt-1">{t("mantras.subtitle")}</p>
      </motion.div>

      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={t("mantras.search_placeholder")}
            className="pl-9 bg-muted/30 border-border/40"
          />
        </div>
        <div className="flex flex-wrap gap-1.5">
          <Badge
            variant={!filter ? "default" : "outline"}
            className="cursor-pointer text-xs"
            onClick={() => setFilter(null)}
          >
            {t("mantras.all")}
          </Badge>
          {allCategories.map((cat) => (
            <Badge
              key={cat}
              variant={filter === cat ? "default" : "outline"}
              className="cursor-pointer text-xs"
              onClick={() => setFilter(filter === cat ? null : cat)}
            >
              {cat}
            </Badge>
          ))}
        </div>
      </div>

      <div className="space-y-3">
        {filtered.map((mantra, i) => (
          <motion.div key={mantra.id} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.04 }}>
            <Card
              className="glass border-border/30 hover:glow-border transition-all cursor-pointer"
              onClick={() => setExpanded(expanded === mantra.id ? null : mantra.id)}
            >
              <CardContent className="p-4">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="font-display text-base">{mantra.name}</h3>
                      <Badge variant="outline" className={`text-xs ${categoryColors[mantra.category] || ""}`}>
                        {mantra.category}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      {mantra.deity} — {mantra.source}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="secondary" className="text-xs">
                      {mantra.chantCount}x
                    </Badge>
                    {expanded === mantra.id ? (
                      <ChevronUp className="h-4 w-4 text-muted-foreground" />
                    ) : (
                      <ChevronDown className="h-4 w-4 text-muted-foreground" />
                    )}
                  </div>
                </div>

                <AnimatePresence>
                  {expanded === mantra.id && (
                    <motion.div
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: "auto" }}
                      exit={{ opacity: 0, height: 0 }}
                      className="overflow-hidden"
                    >
                      <div className="mt-4 space-y-4">
                        {/* Sanskrit */}
                        <div className="bg-primary/5 border border-primary/20 rounded-lg p-4">
                          <div className="flex items-center justify-between mb-2">
                            <p className="text-xs uppercase tracking-wider text-primary">Sanskrit</p>
                            <Volume2 className="h-3.5 w-3.5 text-muted-foreground" />
                          </div>
                          <p className="text-lg leading-relaxed whitespace-pre-line font-serif">{mantra.sanskrit}</p>
                        </div>

                        {/* Transliteration */}
                        <div className="bg-muted/20 rounded-lg p-4">
                          <p className="text-xs uppercase tracking-wider text-muted-foreground mb-2">{t("mantras.transliteration")}</p>
                          <p className="text-sm italic whitespace-pre-line">{mantra.transliteration}</p>
                        </div>

                        {/* Meaning */}
                        <div>
                          <p className="text-xs uppercase tracking-wider text-muted-foreground mb-1">{t("mantras.meaning")}</p>
                          <p className="text-sm text-muted-foreground leading-relaxed">{mantra.meaning}</p>
                        </div>

                        {/* Benefits */}
                        <div>
                          <p className="text-xs uppercase tracking-wider text-muted-foreground mb-2">{t("mantras.benefits")}</p>
                          <div className="grid sm:grid-cols-2 gap-1.5">
                            {mantra.benefits.map((b) => (
                              <div key={b} className="flex items-center gap-2 text-xs">
                                <div className="w-1.5 h-1.5 rounded-full bg-primary shrink-0" />
                                {b}
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </CardContent>
            </Card>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
