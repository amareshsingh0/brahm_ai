import { useState } from "react";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Search, Users, BookOpen, TreePine } from "lucide-react";
import { useTranslation } from "react-i18next";

const GOTRA_STATIC = [
  { name: "Bharadwaj", sanskrit: "भारद्वाज", rishi: "Maharshi Bharadwaj", veda: "Rig Veda", pravara: ["Angirasa", "Barhaspatya", "Bharadwaja"], deity: "Brihaspati (Jupiter)", descKey: "data.gotra.bharadwaj_desc", notableDescendants: ["Dronacharya", "Ashwatthama"], relatedGotras: ["Angirasa", "Garg"] },
  { name: "Kashyap", sanskrit: "कश्यप", rishi: "Maharshi Kashyap", veda: "All Four Vedas", pravara: ["Kashyapa", "Avatsara", "Naidhruva"], deity: "Prajapati", descKey: "data.gotra.kashyap_desc", notableDescendants: ["Surya (Sun God)", "Vamana Avatar"], relatedGotras: ["Vatsya", "Sandilya"] },
  { name: "Vatsa", sanskrit: "वत्स", rishi: "Maharshi Vatsa", veda: "Rig Veda", pravara: ["Bhargava", "Chyavana", "Apnavana", "Aurva", "Jamadagnya"], deity: "Agni", descKey: "data.gotra.vatsa_desc", notableDescendants: ["Sage Vatsa", "Various Vedic scholars"], relatedGotras: ["Bhrigu", "Jamadagni"] },
  { name: "Gautam", sanskrit: "गौतम", rishi: "Maharshi Gautam", veda: "Sama Veda", pravara: ["Angirasa", "Ayasya", "Gautama"], deity: "Indra", descKey: "data.gotra.gautam_desc", notableDescendants: ["Shatananda", "Gautama Buddha (some traditions)"], relatedGotras: ["Angirasa", "Bharadwaj"] },
  { name: "Agastya", sanskrit: "अगस्त्य", rishi: "Maharshi Agastya", veda: "Rig Veda", pravara: ["Agastya", "Dardhanchi", "Idhmavaha"], deity: "Mitra-Varuna", descKey: "data.gotra.agastya_desc", notableDescendants: ["Southern Brahmin lineages"], relatedGotras: ["Pulastya", "Idhmavaha"] },
  { name: "Vasishtha", sanskrit: "वसिष्ठ", rishi: "Maharshi Vasishtha", veda: "Rig Veda (Mandala 7)", pravara: ["Vasishtha", "Indrapramada", "Abharadvasava"], deity: "Varuna & Mitra", descKey: "data.gotra.vasishtha_desc", notableDescendants: ["Parashara", "Vyasa (grandson)"], relatedGotras: ["Parashara", "Shakti"] },
  { name: "Vishwamitra", sanskrit: "विश्वामित्र", rishi: "Maharshi Vishwamitra", veda: "Rig Veda (Mandala 3)", pravara: ["Vaishvamitra", "Devarata", "Audala"], deity: "Savitri (Sun)", descKey: "data.gotra.vishwamitra_desc", notableDescendants: ["Shunahshepa (adopted)", "Madhucchandas"], relatedGotras: ["Kaushika", "Devarata"] },
  { name: "Atri", sanskrit: "अत्रि", rishi: "Maharshi Atri", veda: "Rig Veda (Mandala 5)", pravara: ["Atreya", "Archananasa", "Shyavashva"], deity: "Brahma, Vishnu, Shiva (Trimurti)", descKey: "data.gotra.atri_desc", notableDescendants: ["Dattatreya", "Chandra (Moon)", "Durvasa"], relatedGotras: ["Mudgala", "Archananasa"] },
  { name: "Jamadagni", sanskrit: "जमदग्नि", rishi: "Maharshi Jamadagni", veda: "Rig Veda", pravara: ["Bhargava", "Chyavana", "Apnavana", "Aurva", "Jamadagnya"], deity: "Agni", descKey: "data.gotra.jamadagni_desc", notableDescendants: ["Parashurama"], relatedGotras: ["Bhrigu", "Vatsa"] },
  { name: "Sandilya", sanskrit: "शाण्डिल्य", rishi: "Maharshi Sandilya", veda: "Chandogya Upanishad", pravara: ["Kashyapa", "Avatsara", "Sandilya"], deity: "Brahman", descKey: "data.gotra.sandilya_desc", notableDescendants: ["Various Vedic scholar lineages"], relatedGotras: ["Kashyap", "Kaushika"] },
];

const RULES_STATIC = [
  { titleKey: "data.gotra.same_gotra_title", descKey: "data.gotra.same_gotra_desc", type: "avoid" as const },
  { titleKey: "data.gotra.sapinda_title", descKey: "data.gotra.sapinda_desc", type: "avoid" as const },
  { titleKey: "data.gotra.pravara_title", descKey: "data.gotra.pravara_desc", type: "avoid" as const },
  { titleKey: "data.gotra.diff_gotra_title", descKey: "data.gotra.diff_gotra_desc", type: "good" as const },
];

export default function GotraFinderPage() {
  const { t } = useTranslation();
  const [search, setSearch] = useState("");

  const gotras = GOTRA_STATIC.map((g) => ({ ...g, description: t(g.descKey) }));
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
                      Founded by {gotra.rishi} — {gotra.veda}
                    </p>
                  </div>
                  <Badge variant="secondary" className="text-xs">{gotra.deity}</Badge>
                </div>

                <p className="text-sm text-muted-foreground leading-relaxed mb-3">{gotra.description}</p>

                <div className="grid sm:grid-cols-3 gap-3">
                  <div>
                    <p className="text-xs uppercase tracking-wider text-muted-foreground mb-1">{t("gotra.pravara_label")}</p>
                    <div className="flex flex-wrap gap-1">
                      {gotra.pravara.map((p) => (
                        <Badge key={p} variant="outline" className="text-xs border-primary/30 text-primary">{p}</Badge>
                      ))}
                    </div>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-wider text-muted-foreground mb-1">{t("gotra.notable_label")}</p>
                    <div className="flex flex-wrap gap-1">
                      {gotra.notableDescendants.map((d) => (
                        <Badge key={d} variant="outline" className="text-xs">{d}</Badge>
                      ))}
                    </div>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-wider text-muted-foreground mb-1">{t("gotra.related_label")}</p>
                    <div className="flex flex-wrap gap-1">
                      {gotra.relatedGotras.map((r) => (
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
