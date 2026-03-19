import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { usePanchang } from "@/hooks/usePanchang";

interface Muhurta {
  event: string;
  hindi: string;
  icon: string;
  bestMonths: string;
  bestDays: string;
  bestNakshatra: string;
  avoidTithi: string;
  tips: string;
}

const muhurtaData: Muhurta[] = [
  {
    event: "Wedding (Vivah)",
    hindi: "विवाह",
    icon: "💒",
    bestMonths: "Magha, Phalguna, Vaishakha, Jyeshtha, Margashirsha",
    bestDays: "Monday, Wednesday, Thursday, Friday",
    bestNakshatra: "Rohini, Mrigashira, Magha, Uttara Phalguni, Hasta, Swati, Anuradha, Mula, Uttara Ashadha, Uttara Bhadrapada, Revati",
    avoidTithi: "Amavasya, Purnima, Rikta Tithis (4, 9, 14)",
    tips: "Avoid Bhadrapada month. Jupiter and Venus should not be combust.",
  },
  {
    event: "Griha Pravesh (Housewarming)",
    hindi: "गृह प्रवेश",
    icon: "🏠",
    bestMonths: "Magha, Phalguna, Vaishakha, Jyeshtha",
    bestDays: "Monday, Wednesday, Thursday, Friday",
    bestNakshatra: "Rohini, Mrigashira, Uttara Phalguni, Hasta, Chitra, Swati, Anuradha, Dhanishta, Shatabhisha, Uttara Bhadrapada, Revati",
    avoidTithi: "Amavasya, 4th, 9th, 14th Tithi",
    tips: "Avoid Adhik Maas. Sun should be in good sign. Prefer Shukla Paksha.",
  },
  {
    event: "Naming Ceremony (Namkaran)",
    hindi: "नामकरण",
    icon: "👶",
    bestMonths: "Any auspicious month",
    bestDays: "Monday, Wednesday, Thursday, Friday",
    bestNakshatra: "Ashwini, Rohini, Mrigashira, Punarvasu, Pushya, Hasta, Chitra, Swati, Anuradha, Shravana, Dhanishta, Revati",
    avoidTithi: "Amavasya, 4th, 8th, 9th, 14th",
    tips: "Perform on 11th or 12th day after birth. Choose name matching birth Nakshatra letter.",
  },
  {
    event: "Business Start (Vyapar Arambh)",
    hindi: "व्यापार आरंभ",
    icon: "💼",
    bestMonths: "Chaitra, Vaishakha, Jyeshtha, Kartik, Margashirsha, Magha",
    bestDays: "Wednesday, Thursday, Friday",
    bestNakshatra: "Ashwini, Rohini, Mrigashira, Punarvasu, Pushya, Hasta, Chitra, Swati, Anuradha, Shravana, Revati",
    avoidTithi: "Amavasya, 4th, 9th, 14th",
    tips: "Mercury & Jupiter should be strong. Avoid Rahu Kaal and Gulika Kaal.",
  },
  {
    event: "Vehicle Purchase",
    hindi: "वाहन खरीद",
    icon: "🚗",
    bestMonths: "Any except Adhik Maas",
    bestDays: "Tuesday, Wednesday, Thursday, Friday",
    bestNakshatra: "Ashwini, Rohini, Mrigashira, Punarvasu, Pushya, Hasta, Chitra, Swati, Anuradha, Shravana, Revati",
    avoidTithi: "Amavasya, 4th, 9th, 14th",
    tips: "Mars should be well-placed. Prefer Shukla Paksha. Tuesday good for red vehicles.",
  },
  {
    event: "Education Start (Vidyarambh)",
    hindi: "विद्यारंभ",
    icon: "📚",
    bestMonths: "Shravana, Ashwin, Kartik, Magha, Phalguna",
    bestDays: "Monday, Wednesday, Thursday, Friday",
    bestNakshatra: "Ashwini, Punarvasu, Pushya, Hasta, Chitra, Swati, Shravana, Dhanishta, Shatabhisha, Revati",
    avoidTithi: "Amavasya, Rikta Tithis",
    tips: "Jupiter and Mercury should be strong. Vasant Panchami is ideal for Vidyarambh.",
  },
  {
    event: "Travel (Yatra)",
    hindi: "यात्रा",
    icon: "✈️",
    bestMonths: "Any auspicious month",
    bestDays: "Monday, Wednesday, Thursday, Friday",
    bestNakshatra: "Ashwini, Mrigashira, Punarvasu, Pushya, Hasta, Anuradha, Shravana, Revati",
    avoidTithi: "Amavasya, 4th, 8th, 9th, 14th",
    tips: "Avoid traveling in the direction of daily Rahu Kaal. Check Hora for timing.",
  },
  {
    event: "Gold/Jewelry Purchase",
    hindi: "सोना खरीद",
    icon: "💎",
    bestMonths: "Dhanteras, Akshaya Tritiya, Dussehra, Pushya Nakshatra day",
    bestDays: "Thursday, Friday, Monday",
    bestNakshatra: "Pushya (most auspicious), Rohini, Uttara Phalguni, Hasta, Chitra, Shravana, Revati",
    avoidTithi: "Amavasya, Rikta Tithis",
    tips: "Pushya Nakshatra falling on Thursday is exceptionally auspicious for gold purchase.",
  },
];

const panchangElements = [
  { name: "Tithi", hindi: "तिथि", desc: "Lunar day (Shukla/Krishna Paksha 1-15)", icon: "🌙" },
  { name: "Nakshatra", hindi: "नक्षत्र", desc: "Lunar mansion (27 Nakshatras)", icon: "⭐" },
  { name: "Yoga", hindi: "योग", desc: "Luni-solar combination (27 Yogas)", icon: "🔮" },
  { name: "Karana", hindi: "करण", desc: "Half of a Tithi (11 Karanas)", icon: "📐" },
  { name: "Vara", hindi: "वार", desc: "Day of the week (7 Varas)", icon: "📅" },
];

const rahuKaal = [
  { day: "Monday", time: "7:30 AM – 9:00 AM", avoid: "New beginnings" },
  { day: "Tuesday", time: "3:00 PM – 4:30 PM", avoid: "Travel, Surgery" },
  { day: "Wednesday", time: "12:00 PM – 1:30 PM", avoid: "Financial deals" },
  { day: "Thursday", time: "1:30 PM – 3:00 PM", avoid: "Education start" },
  { day: "Friday", time: "10:30 AM – 12:00 PM", avoid: "Marriages" },
  { day: "Saturday", time: "9:00 AM – 10:30 AM", avoid: "Auspicious work" },
  { day: "Sunday", time: "4:30 PM – 6:00 PM", avoid: "Important tasks" },
];

export default function MuhurtaPage() {
  const d = new Date();
  const today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  const { data: panchang } = usePanchang({ date: today });

  return (
    <div className="space-y-6">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-3xl text-primary text-glow-gold">🕉️ Shubh Muhurta</h1>
        <p className="text-muted-foreground mt-1">Auspicious times for important life events — based on Vedic Panchang</p>
      </motion.div>

      {/* Today's live muhurta */}
      {panchang && (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
          <Card className="glass border-primary/30 bg-primary/5">
            <CardHeader className="pb-2">
              <CardTitle className="text-base flex items-center gap-2">
                <span>⚡</span> Today's Muhurta — {panchang.vara.name}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid sm:grid-cols-3 gap-3 text-sm">
                <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-3 text-center">
                  <p className="text-[10px] text-muted-foreground uppercase tracking-wider mb-1">✨ Abhijit Muhurta</p>
                  <p className="font-mono font-bold text-emerald-400">{panchang.abhijit_muhurta.start} – {panchang.abhijit_muhurta.end}</p>
                  <p className="text-[10px] text-muted-foreground mt-1">Best daily muhurta</p>
                </div>
                <div className="bg-red-500/10 border border-red-500/20 rounded-xl p-3 text-center">
                  <p className="text-[10px] text-muted-foreground uppercase tracking-wider mb-1">⚠️ Rahu Kaal</p>
                  <p className="font-mono font-bold text-red-400">{panchang.rahukaal.start} – {panchang.rahukaal.end}</p>
                  <p className="text-[10px] text-muted-foreground mt-1">Avoid new beginnings</p>
                </div>
                <div className="bg-muted/20 border border-border/30 rounded-xl p-3 text-center">
                  <p className="text-[10px] text-muted-foreground uppercase tracking-wider mb-1">📅 Tithi</p>
                  <p className="font-medium text-foreground">{panchang.tithi.paksha} {panchang.tithi.name}</p>
                  <p className="text-[10px] text-muted-foreground mt-1">Nakshatra: {panchang.nakshatra.name}</p>
                </div>
              </div>
              <div className={`mt-3 rounded-lg p-2.5 text-xs text-center ${panchang.yoga.is_auspicious ? "bg-emerald-500/10 text-emerald-400" : "bg-amber-500/10 text-amber-400"}`}>
                {panchang.yoga.name} Yoga — {panchang.yoga.is_auspicious ? "Auspicious day for muhurta activities" : "Take extra care with timing today"}
              </div>
            </CardContent>
          </Card>
        </motion.div>
      )}

      <Tabs defaultValue="muhurta" className="w-full">
        <TabsList className="glass">
          <TabsTrigger value="muhurta">Auspicious Times</TabsTrigger>
          <TabsTrigger value="panchang">Panchang Elements</TabsTrigger>
          <TabsTrigger value="rahukaal">Rahu Kaal</TabsTrigger>
        </TabsList>

        <TabsContent value="muhurta" className="space-y-4 mt-4">
          {muhurtaData.map((m, i) => (
            <motion.div key={m.event} initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.06 }}>
              <Card className="glass border-border/30">
                <CardHeader className="pb-2">
                  <CardTitle className="flex items-center gap-2 text-lg">
                    <span className="text-2xl">{m.icon}</span>
                    {m.event}
                    <span className="text-sm text-muted-foreground font-normal">({m.hindi})</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="grid sm:grid-cols-2 gap-3 text-sm">
                    <div className="bg-muted/30 rounded-lg p-3">
                      <p className="text-xs text-muted-foreground mb-1">Best Months</p>
                      <p>{m.bestMonths}</p>
                    </div>
                    <div className="bg-muted/30 rounded-lg p-3">
                      <p className="text-xs text-muted-foreground mb-1">Best Days</p>
                      <p>{m.bestDays}</p>
                    </div>
                    <div className="bg-muted/30 rounded-lg p-3">
                      <p className="text-xs text-muted-foreground mb-1">Best Nakshatras</p>
                      <p>{m.bestNakshatra}</p>
                    </div>
                    <div className="bg-muted/30 rounded-lg p-3">
                      <p className="text-xs text-muted-foreground mb-1">Avoid Tithis</p>
                      <p>{m.avoidTithi}</p>
                    </div>
                  </div>
                  <div className="bg-primary/10 border border-primary/20 rounded-lg p-3">
                    <p className="text-xs text-primary mb-1">💡 Tip</p>
                    <p className="text-sm">{m.tips}</p>
                  </div>
                </CardContent>
              </Card>
            </motion.div>
          ))}
        </TabsContent>

        <TabsContent value="panchang" className="mt-4">
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {panchangElements.map((el, i) => (
              <motion.div key={el.name} initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: i * 0.08 }}>
                <Card className="glass border-border/30 h-full">
                  <CardContent className="pt-5 space-y-2 text-center">
                    <span className="text-4xl">{el.icon}</span>
                    <h3 className="font-display text-lg">{el.name}</h3>
                    <p className="text-sm text-muted-foreground">{el.hindi}</p>
                    <p className="text-xs text-muted-foreground">{el.desc}</p>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="rahukaal" className="mt-4 space-y-4">
          {/* Today's real Rahu Kaal from API */}
          {panchang && (
            <Card className="glass border-red-500/30 bg-red-500/5">
              <CardHeader className="pb-2">
                <CardTitle className="text-base text-red-400">⚠️ Today's Rahu Kaal — {panchang.vara.name}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center justify-between bg-red-500/10 rounded-xl p-4">
                  <div>
                    <p className="text-xs text-muted-foreground mb-1">Exact time for your location</p>
                    <p className="font-mono text-2xl font-bold text-red-400">{panchang.rahukaal.start} – {panchang.rahukaal.end}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-muted-foreground">Sunrise: {panchang.sunrise}</p>
                    <p className="text-xs text-muted-foreground">Sunset: {panchang.sunset}</p>
                  </div>
                </div>
                <p className="text-xs text-muted-foreground mt-2">Avoid: Starting new work, signing contracts, travel, marriages, surgery during this period.</p>
              </CardContent>
            </Card>
          )}

          {/* Vedic rule for Rahu Kaal */}
          <Card className="glass border-border/30">
            <CardHeader className="pb-2">
              <CardTitle className="text-base">📖 How Rahu Kaal is Calculated</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <p className="text-muted-foreground">Rahu Kaal = 1/8th of the day between sunrise and sunset. The order varies by weekday:</p>
              <div className="grid grid-cols-2 gap-2">
                {[
                  { day: "Sunday",    order: "8th", fraction: "4:30 PM area" },
                  { day: "Monday",    order: "2nd", fraction: "7:30 AM area" },
                  { day: "Tuesday",   order: "7th", fraction: "3:00 PM area" },
                  { day: "Wednesday", order: "5th", fraction: "12:00 PM area" },
                  { day: "Thursday",  order: "6th", fraction: "1:30 PM area" },
                  { day: "Friday",    order: "3rd", fraction: "10:30 AM area" },
                  { day: "Saturday",  order: "4th", fraction: "9:00 AM area" },
                ].map((r) => (
                  <div key={r.day} className="flex items-center justify-between bg-muted/20 rounded-lg px-3 py-2">
                    <span className="text-xs font-medium">{r.day}</span>
                    <span className="text-xs text-muted-foreground">{r.order} part · ~{r.fraction}</span>
                  </div>
                ))}
              </div>
              <p className="text-[10px] text-muted-foreground">Exact times depend on your location's sunrise — use Panchang page for precise timings.</p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
