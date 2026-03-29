import { useState } from "react";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Search, Users, BookOpen, TreePine } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useRegisterPageBot } from "@/hooks/useRegisterPageBot";

const GOTRA_STATIC = [
  { name: "Bharadwaj", sanskrit: "भारद्वाज", rishi: "Maharshi Bharadwaj", rishiDev: "महर्षि भारद्वाज", veda: "Rig Veda", pravara: ["Angirasa", "Barhaspatya", "Bharadwaja"], pravaraD: ["अंगिरस", "बार्हस्पत्य", "भारद्वाज"], deity: "Brihaspati (Jupiter)", deityKey: "data.gotra.deity_bharadwaj", descKey: "data.gotra.bharadwaj_desc", notableDescendants: ["Dronacharya", "Ashwatthama"], notableD: ["द्रोणाचार्य", "अश्वत्थामा"], relatedGotras: ["Angirasa", "Garg"], relatedD: ["अंगिरस", "गर्ग"] },
  { name: "Kashyap", sanskrit: "कश्यप", rishi: "Maharshi Kashyap", rishiDev: "महर्षि कश्यप", veda: "All Four Vedas", pravara: ["Kashyapa", "Avatsara", "Naidhruva"], pravaraD: ["कश्यप", "अवत्सर", "नैध्रुव"], deity: "Prajapati", deityKey: "data.gotra.deity_kashyap", descKey: "data.gotra.kashyap_desc", notableDescendants: ["Surya (Sun God)", "Vamana Avatar"], notableD: ["सूर्य (सूर्यदेव)", "वामन अवतार"], relatedGotras: ["Vatsya", "Sandilya"], relatedD: ["वात्स्य", "शाण्डिल्य"] },
  { name: "Vatsa", sanskrit: "वत्स", rishi: "Maharshi Vatsa", rishiDev: "महर्षि वत्स", veda: "Rig Veda", pravara: ["Bhargava", "Chyavana", "Apnavana", "Aurva", "Jamadagnya"], pravaraD: ["भार्गव", "च्यवन", "आप्नवान", "और्व", "जामदग्न्य"], deity: "Agni", deityKey: "data.gotra.deity_vatsa", descKey: "data.gotra.vatsa_desc", notableDescendants: ["Sage Vatsa", "Various Vedic scholars"], notableD: ["ऋषि वत्स", "विभिन्न वैदिक विद्वान"], relatedGotras: ["Bhrigu", "Jamadagni"], relatedD: ["भृगु", "जमदग्नि"] },
  { name: "Gautam", sanskrit: "गौतम", rishi: "Maharshi Gautam", rishiDev: "महर्षि गौतम", veda: "Sama Veda", pravara: ["Angirasa", "Ayasya", "Gautama"], pravaraD: ["अंगिरस", "आयास्य", "गौतम"], deity: "Indra", deityKey: "data.gotra.deity_gautam", descKey: "data.gotra.gautam_desc", notableDescendants: ["Shatananda", "Gautama Buddha (some traditions)"], notableD: ["शतानन्द", "गौतम बुद्ध (कुछ परंपराओं में)"], relatedGotras: ["Angirasa", "Bharadwaj"], relatedD: ["अंगिरस", "भारद्वाज"] },
  { name: "Agastya", sanskrit: "अगस्त्य", rishi: "Maharshi Agastya", rishiDev: "महर्षि अगस्त्य", veda: "Rig Veda", pravara: ["Agastya", "Dardhanchi", "Idhmavaha"], pravaraD: ["अगस्त्य", "दर्धञ्च", "इध्मवाह"], deity: "Mitra-Varuna", deityKey: "data.gotra.deity_agastya", descKey: "data.gotra.agastya_desc", notableDescendants: ["Southern Brahmin lineages"], notableD: ["दक्षिण ब्राह्मण वंश"], relatedGotras: ["Pulastya", "Idhmavaha"], relatedD: ["पुलस्त्य", "इध्मवाह"] },
  { name: "Vasishtha", sanskrit: "वसिष्ठ", rishi: "Maharshi Vasishtha", rishiDev: "महर्षि वसिष्ठ", veda: "Rig Veda (Mandala 7)", pravara: ["Vasishtha", "Indrapramada", "Abharadvasava"], pravaraD: ["वसिष्ठ", "इन्द्रप्रमद", "अभरद्वसव"], deity: "Varuna & Mitra", deityKey: "data.gotra.deity_vasishtha", descKey: "data.gotra.vasishtha_desc", notableDescendants: ["Parashara", "Vyasa (grandson)"], notableD: ["पराशर", "व्यास (पौत्र)"], relatedGotras: ["Parashara", "Shakti"], relatedD: ["पराशर", "शक्ति"] },
  { name: "Vishwamitra", sanskrit: "विश्वामित्र", rishi: "Maharshi Vishwamitra", rishiDev: "महर्षि विश्वामित्र", veda: "Rig Veda (Mandala 3)", pravara: ["Vaishvamitra", "Devarata", "Audala"], pravaraD: ["वैश्वामित्र", "देवरात", "और्दाल"], deity: "Savitri (Sun)", deityKey: "data.gotra.deity_vishwamitra", descKey: "data.gotra.vishwamitra_desc", notableDescendants: ["Shunahshepa (adopted)", "Madhucchandas"], notableD: ["शुनःशेप (दत्तक)", "मधुच्छन्दस्"], relatedGotras: ["Kaushika", "Devarata"], relatedD: ["कौशिक", "देवरात"] },
  { name: "Atri", sanskrit: "अत्रि", rishi: "Maharshi Atri", rishiDev: "महर्षि अत्रि", veda: "Rig Veda (Mandala 5)", pravara: ["Atreya", "Archananasa", "Shyavashva"], pravaraD: ["आत्रेय", "अर्चनानस", "श्यावाश्व"], deity: "Brahma, Vishnu, Shiva (Trimurti)", deityKey: "data.gotra.deity_atri", descKey: "data.gotra.atri_desc", notableDescendants: ["Dattatreya", "Chandra (Moon)", "Durvasa"], notableD: ["दत्तात्रेय", "चन्द्र (चन्द्रमा)", "दुर्वासा"], relatedGotras: ["Mudgala", "Archananasa"], relatedD: ["मुद्गल", "अर्चनानस"] },
  { name: "Jamadagni", sanskrit: "जमदग्नि", rishi: "Maharshi Jamadagni", rishiDev: "महर्षि जमदग्नि", veda: "Rig Veda", pravara: ["Bhargava", "Chyavana", "Apnavana", "Aurva", "Jamadagnya"], pravaraD: ["भार्गव", "च्यवन", "आप्नवान", "और्व", "जामदग्न्य"], deity: "Agni", deityKey: "data.gotra.deity_jamadagni", descKey: "data.gotra.jamadagni_desc", notableDescendants: ["Parashurama"], notableD: ["परशुराम"], relatedGotras: ["Bhrigu", "Vatsa"], relatedD: ["भृगु", "वत्स"] },
  { name: "Sandilya", sanskrit: "शाण्डिल्य", rishi: "Maharshi Sandilya", rishiDev: "महर्षि शाण्डिल्य", veda: "Chandogya Upanishad", pravara: ["Kashyapa", "Avatsara", "Sandilya"], pravaraD: ["कश्यप", "अवत्सर", "शाण्डिल्य"], deity: "Brahman", deityKey: "data.gotra.deity_sandilya", descKey: "data.gotra.sandilya_desc", notableDescendants: ["Various Vedic scholar lineages"], notableD: ["विभिन्न वैदिक विद्वान वंश"], relatedGotras: ["Kashyap", "Kaushika"], relatedD: ["कश्यप", "कौशिक"] },
];

const RULES_STATIC = [
  { titleKey: "data.gotra.same_gotra_title", descKey: "data.gotra.same_gotra_desc", type: "avoid" as const },
  { titleKey: "data.gotra.sapinda_title", descKey: "data.gotra.sapinda_desc", type: "avoid" as const },
  { titleKey: "data.gotra.pravara_title", descKey: "data.gotra.pravara_desc", type: "avoid" as const },
  { titleKey: "data.gotra.diff_gotra_title", descKey: "data.gotra.diff_gotra_desc", type: "good" as const },
];

export default function GotraFinderPage() {
  const { t, i18n } = useTranslation();
  useRegisterPageBot('gotra', {});
  const [search, setSearch] = useState("");
  const isEn = i18n.language === "en";

  const gotras = GOTRA_STATIC.map((g) => ({
    ...g,
    description: t(g.descKey),
    deityLabel: t(g.deityKey, { defaultValue: g.deity }),
    rishiLabel: isEn ? g.rishi : g.rishiDev,
    pravaraList: isEn ? g.pravara : g.pravaraD,
    notableList: isEn ? g.notableDescendants : g.notableD,
    relatedList: isEn ? g.relatedGotras : g.relatedD,
  }));
  const rules = RULES_STATIC.map((r) => ({ title: t(r.titleKey), desc: t(r.descKey), type: r.type }));

  const filtered = gotras.filter(
    (g) =>
      !search ||
      g.name.toLowerCase().includes(search.toLowerCase()) ||
      g.rishi.toLowerCase().includes(search.toLowerCase()) ||
      g.description.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div className="space-y-6">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-3xl text-primary text-glow-gold">{t("gotra.title")}</h1>
        <p className="text-muted-foreground mt-1">{t("gotra.subtitle")}</p>
      </motion.div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t("gotra.search_placeholder")}
          className="pl-9 bg-muted/30 border-border/40"
        />
      </div>

      {/* Marriage Rules */}
      <Card className="glass border-border/30">
        <CardHeader className="pb-2">
          <CardTitle className="text-base flex items-center gap-2">
            <Users className="h-4 w-4 text-primary" />
            {t("gotra.marriage_rules")}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid sm:grid-cols-2 gap-2">
            {rules.map((rule) => (
              <div key={rule.title} className={`rounded-lg p-3 text-sm ${rule.type === "avoid" ? "bg-destructive/5 border border-destructive/20" : "bg-green-500/5 border border-green-500/20"}`}>
                <p className="font-medium text-xs mb-0.5">{rule.title}</p>
                <p className="text-xs text-muted-foreground">{rule.desc}</p>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Gotra Cards */}
      <div className="grid gap-4">
        {filtered.map((gotra, i) => (
          <motion.div key={gotra.name} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.05 }}>
            <Card className="glass border-border/30 hover:glow-border transition-shadow">
              <CardContent className="p-5">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h3 className="font-display text-xl flex items-center gap-2">
                      <TreePine className="h-5 w-5 text-primary" />
                      {gotra.name}
                      <span className="text-base text-primary font-normal">{gotra.sanskrit}</span>
                    </h3>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      {t("gotra.founded_by", { rishi: gotra.rishiLabel, veda: gotra.veda })}
                    </p>
                  </div>
                  <Badge variant="secondary" className="text-xs">{gotra.deityLabel}</Badge>
                </div>

                <p className="text-sm text-muted-foreground leading-relaxed mb-3">{gotra.description}</p>

                <div className="grid sm:grid-cols-3 gap-3">
                  <div>
                    <p className="text-xs uppercase tracking-wider text-muted-foreground mb-1">{t("gotra.pravara_label")}</p>
                    <div className="flex flex-wrap gap-1">
                      {gotra.pravaraList.map((p) => (
                        <Badge key={p} variant="outline" className="text-xs border-primary/30 text-primary">{p}</Badge>
                      ))}
                    </div>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-wider text-muted-foreground mb-1">{t("gotra.notable_label")}</p>
                    <div className="flex flex-wrap gap-1">
                      {gotra.notableList.map((d) => (
                        <Badge key={d} variant="outline" className="text-xs">{d}</Badge>
                      ))}
                    </div>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-wider text-muted-foreground mb-1">{t("gotra.related_label")}</p>
                    <div className="flex flex-wrap gap-1">
                      {gotra.relatedList.map((r) => (
                        <Badge key={r} variant="outline" className="text-xs">{r}</Badge>
                      ))}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
