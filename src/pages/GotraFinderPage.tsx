import { useState } from "react";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Search, Users, BookOpen, TreePine } from "lucide-react";

const gotras = [
  {
    name: "Bharadwaj",
    sanskrit: "भारद्वाज",
    rishi: "Maharshi Bharadwaj",
    veda: "Rig Veda",
    pravara: ["Angirasa", "Barhaspatya", "Bharadwaja"],
    deity: "Brihaspati (Jupiter)",
    description:
      "One of the most common gotras. Rishi Bharadwaj authored many hymns of the Rig Veda and was a great teacher of Ayurveda and warfare. Father of Dronacharya. The gotra encompasses knowledge, teaching, and martial skill.",
    notableDescendants: ["Dronacharya", "Ashwatthama"],
    relatedGotras: ["Angirasa", "Garg"],
  },
  {
    name: "Kashyap",
    sanskrit: "कश्यप",
    rishi: "Maharshi Kashyap",
    veda: "All Four Vedas",
    pravara: ["Kashyapa", "Avatsara", "Naidhruva"],
    deity: "Prajapati",
    description:
      "Considered the father of all living beings — Devas, Asuras, Nagas, and humans are all descendants of Kashyap. One of the Saptarishis. His name means 'one who drinks light.' The most universal of all gotras.",
    notableDescendants: ["Surya (Sun God)", "Vamana Avatar"],
    relatedGotras: ["Vatsya", "Sandilya"],
  },
  {
    name: "Vatsa",
    sanskrit: "वत्स",
    rishi: "Maharshi Vatsa",
    veda: "Rig Veda",
    pravara: ["Bhargava", "Chyavana", "Apnavana", "Aurva", "Jamadagnya"],
    deity: "Agni",
    description:
      "Descended from Rishi Vatsa of the Bhrigu lineage. Known for deep devotion and fire rituals. The five-rishi pravara indicates a very ancient and respected lineage among Brahmins.",
    notableDescendants: ["Sage Vatsa", "Various Vedic scholars"],
    relatedGotras: ["Bhrigu", "Jamadagni"],
  },
  {
    name: "Gautam",
    sanskrit: "गौतम",
    rishi: "Maharshi Gautam",
    veda: "Sama Veda",
    pravara: ["Angirasa", "Ayasya", "Gautama"],
    deity: "Indra",
    description:
      "Founded by Rishi Gautam, one of the Saptarishis. He is credited with writing the Nyaya Sutras (logic). Known for his intense tapas and establishment of ashram at River Godavari. Wife: Ahalya.",
    notableDescendants: ["Shatananda", "Gautama Buddha (some traditions)"],
    relatedGotras: ["Angirasa", "Bharadwaj"],
  },
  {
    name: "Agastya",
    sanskrit: "अगस्त्य",
    rishi: "Maharshi Agastya",
    veda: "Rig Veda",
    pravara: ["Agastya", "Dardhanchi", "Idhmavaha"],
    deity: "Mitra-Varuna",
    description:
      "The great sage who brought Vedic civilization to South India. Known as 'Agastya — the one who humbled the Vindhya mountain.' Father of Tamil grammar (Agattiyam). Pioneer of Siddha medicine.",
    notableDescendants: ["Southern Brahmin lineages"],
    relatedGotras: ["Pulastya", "Idhmavaha"],
  },
  {
    name: "Vasishtha",
    sanskrit: "वसिष्ठ",
    rishi: "Maharshi Vasishtha",
    veda: "Rig Veda (Mandala 7)",
    pravara: ["Vasishtha", "Indrapramada", "Abharadvasava"],
    deity: "Varuna & Mitra",
    description:
      "One of the Saptarishis and Rajguru (royal priest) of the Surya Vansha (Solar Dynasty). Owner of Kamadhenu (wish-fulfilling cow). Had famous rivalry with Vishwamitra. Author of Yoga Vasishtha.",
    notableDescendants: ["Parashara", "Vyasa (grandson)"],
    relatedGotras: ["Parashara", "Shakti"],
  },
  {
    name: "Vishwamitra",
    sanskrit: "विश्वामित्र",
    rishi: "Maharshi Vishwamitra",
    veda: "Rig Veda (Mandala 3)",
    pravara: ["Vaishvamitra", "Devarata", "Audala"],
    deity: "Savitri (Sun)",
    description:
      "Originally a Kshatriya king who became a Brahmarshi through immense tapas. Discovered the Gayatri Mantra. Created a parallel heaven (Trishanku). His story shows that spiritual status is earned, not inherited.",
    notableDescendants: ["Shunahshepa (adopted)", "Madhucchandas"],
    relatedGotras: ["Kaushika", "Devarata"],
  },
  {
    name: "Atri",
    sanskrit: "अत्रि",
    rishi: "Maharshi Atri",
    veda: "Rig Veda (Mandala 5)",
    pravara: ["Atreya", "Archananasa", "Shyavashva"],
    deity: "Brahma, Vishnu, Shiva (Trimurti)",
    description:
      "One of the Saptarishis, born from Brahma's mind. Father of Dattatreya (incarnation of the Trimurti), Chandra (Moon), and Sage Durvasa. His wife Anasuya was the epitome of chastity.",
    notableDescendants: ["Dattatreya", "Chandra (Moon)", "Durvasa"],
    relatedGotras: ["Mudgala", "Archananasa"],
  },
  {
    name: "Jamadagni",
    sanskrit: "जमदग्नि",
    rishi: "Maharshi Jamadagni",
    veda: "Rig Veda",
    pravara: ["Bhargava", "Chyavana", "Apnavana", "Aurva", "Jamadagnya"],
    deity: "Agni",
    description:
      "Of the Bhrigu lineage. Father of Parashurama (6th avatar of Vishnu). Possessed the divine cow Surabhi. Known for his mastery of weapons and Vedic knowledge. His ashram was a center of learning.",
    notableDescendants: ["Parashurama"],
    relatedGotras: ["Bhrigu", "Vatsa"],
  },
  {
    name: "Sandilya",
    sanskrit: "शाण्डिल्य",
    rishi: "Maharshi Sandilya",
    veda: "Chandogya Upanishad",
    pravara: ["Kashyapa", "Avatsara", "Sandilya"],
    deity: "Brahman",
    description:
      "Author of the Sandilya Vidya in the Chandogya Upanishad — the meditation on Brahman as 'all this is Brahman.' Also authored the Sandilya Bhakti Sutras. Known for the teaching: 'This Self in my heart is Brahman.'",
    notableDescendants: ["Various Vedic scholar lineages"],
    relatedGotras: ["Kashyap", "Kaushika"],
  },
];

const rules = [
  { title: "Same Gotra Marriage", desc: "Marriage within the same gotra is traditionally prohibited as they share a common ancestor (Sagotra vivah nishedh).", type: "avoid" as const },
  { title: "Sapinda Restriction", desc: "Marriage is avoided if related within 7 generations on father's side and 5 on mother's side.", type: "avoid" as const },
  { title: "Pravara Matching", desc: "Families with matching pravaras (ancestor sages) should avoid intermarriage even if gotras differ.", type: "avoid" as const },
  { title: "Different Gotra Match", desc: "Marriage between different gotras is considered auspicious and promotes genetic diversity.", type: "good" as const },
];

export default function GotraFinderPage() {
  const [search, setSearch] = useState("");

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
        <h1 className="font-display text-3xl text-primary text-glow-gold">Gotra Finder</h1>
        <p className="text-muted-foreground mt-1">
          Explore Vedic lineages — Saptarishis, Pravaras, and ancestral heritage
        </p>
      </motion.div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search gotra, rishi, or lineage..."
          className="pl-9 bg-muted/30 border-border/40"
        />
      </div>

      {/* Marriage Rules */}
      <Card className="glass border-border/30">
        <CardHeader className="pb-2">
          <CardTitle className="text-base flex items-center gap-2">
            <Users className="h-4 w-4 text-primary" />
            Gotra & Marriage — Vedic Guidelines
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid sm:grid-cols-2 gap-2">
            {rules.map((rule) => (
              <div key={rule.title} className={`rounded-lg p-3 text-sm ${rule.type === "avoid" ? "bg-destructive/5 border border-destructive/20" : "bg-green-500/5 border border-green-500/20"}`}>
                <p className="font-medium text-xs mb-0.5">{rule.title}</p>
                <p className="text-[11px] text-muted-foreground">{rule.desc}</p>
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
                  <Badge variant="secondary" className="text-[10px]">{gotra.deity}</Badge>
                </div>

                <p className="text-sm text-muted-foreground leading-relaxed mb-3">{gotra.description}</p>

                <div className="grid sm:grid-cols-3 gap-3">
                  <div>
                    <p className="text-[10px] uppercase tracking-wider text-muted-foreground mb-1">Pravara (Ancestor Sages)</p>
                    <div className="flex flex-wrap gap-1">
                      {gotra.pravara.map((p) => (
                        <Badge key={p} variant="outline" className="text-[10px] border-primary/30 text-primary">{p}</Badge>
                      ))}
                    </div>
                  </div>
                  <div>
                    <p className="text-[10px] uppercase tracking-wider text-muted-foreground mb-1">Notable Descendants</p>
                    <div className="flex flex-wrap gap-1">
                      {gotra.notableDescendants.map((d) => (
                        <Badge key={d} variant="outline" className="text-[10px]">{d}</Badge>
                      ))}
                    </div>
                  </div>
                  <div>
                    <p className="text-[10px] uppercase tracking-wider text-muted-foreground mb-1">Related Gotras</p>
                    <div className="flex flex-wrap gap-1">
                      {gotra.relatedGotras.map((r) => (
                        <Badge key={r} variant="outline" className="text-[10px]">{r}</Badge>
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
