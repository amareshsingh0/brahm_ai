import { useTranslation } from 'react-i18next';
import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import { ArrowRight, Star, Moon, Sun, Sparkles, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

const cosmicFacts = [
  { label: "Tithi",     value: "Ekadashi", sub: "Shukla Paksha",    icon: Moon },
  { label: "Nakshatra", value: "Rohini",   sub: "Lord: Chandra",    icon: Star },
  { label: "Vara",      value: "Guruvara", sub: "Lord: Brihaspati", icon: Sun },
  { label: "Yoga",      value: "Siddha",   sub: "Auspicious",       icon: Sparkles },
];

const planColors: Record<string, string> = {
  Free:        "bg-muted/40 text-muted-foreground",
  "Jyotishi+": "bg-amber-500/20 text-amber-400",
  Acharya:     "bg-purple-500/20 text-purple-400",
};

const stats = [
  { value: "108",   label: "Yogas & Doshas detected" },
  { value: "9",     label: "Grahas tracked in real-time" },
  { value: "27",    label: "Nakshatras analyzed" },
  { value: "12",    label: "Divisional charts supported" },
];

export default function LandingPage() {
  const { t } = useTranslation();

  const features = [
    {
      icon: "🤖",
      title: `${t('appTitle')} Chat`,
      desc: "Ask any Vedic question. Powered by 1.1 million Sanskrit, Hindi, and English text chunks from 100+ sacred books.",
      plan: "Jyotishi+",
      color: "from-blue-500/10 to-purple-500/10 border-blue-500/20",
    },
    {
      icon: "⭐",
      title: "Kundali & Dashas",
      desc: "Precise birth chart with Lahiri ayanamsha. Full Vimshottari Dasha timeline, Yogas, and Lagna analysis.",
      plan: "Free",
      color: "from-gold/10 to-amber-500/10 border-amber-500/20",
    },
    {
      icon: "🌙",
      title: "Daily Panchang",
      desc: "Tithi, Nakshatra, Yoga, Karana, Vara, Choghadiya, Rahukaal, Brahma Muhurta — computed live for your city.",
      plan: "Free",
      color: "from-purple-500/10 to-pink-500/10 border-purple-500/20",
    },
    {
      icon: "❤️",
      title: "Compatibility (Kundali Milan)",
      desc: "Full 36-Guna Ashtakoot analysis. Varna, Vashya, Tara, Yoni, Graha Maitri, Gana, Bhakoot, Nadi.",
      plan: "Jyotishi+",
      color: "from-rose-500/10 to-red-500/10 border-rose-500/20",
    },
    {
      icon: "🌑",
      title: "Eclipse & Festival Calendar",
      desc: "Festivals across all regions with multi-year coverage. Grahan with Sparsha/Madhya/Moksha timings, Sutak, and conflict detection.",
      plan: "Free",
      color: "from-gray-500/10 to-slate-500/10 border-gray-500/20",
    },
    {
      icon: "📚",
      title: "Vedic Library Search",
      desc: "Semantic search over Bhagavad Gita, Upanishads, Puranas, and 100+ texts in 3 languages.",
      plan: "Acharya",
      color: "from-emerald-500/10 to-teal-500/10 border-emerald-500/20",
    },
  ];

  const steps = [
    { step: "01", title: "Login with OTP",      desc: "Enter your phone number. Get a 6-digit OTP. No password needed.",                      icon: "📱" },
    { step: "02", title: "Enter Birth Details",  desc: "Name, date, time, and city of birth. Your Kundali is generated instantly.",             icon: "🌟" },
    { step: "03", title: "Explore Your Chart",   desc: "Dashboard, Panchang, AI Chat, Compatibility — all personalized.",                       icon: "🔮" },
  ];

  const plans = [
    {
      name: "Free",     nameHi: "निःशुल्क", price: "₹0",   period: "",
      features: ["Daily Horoscope", "Today's Panchang", "Festival Calendar", "Basic Kundali", "5 AI Chats/day"],
      cta: "Start Free", highlight: false,
    },
    {
      name: "Jyotishi", nameHi: "ज्योतिषी", price: "₹199", period: "/month",
      features: ["Everything in Free", "Unlimited AI Chat", "Full Kundali + Dashas", "Compatibility Analysis", "Muhurta Finder"],
      cta: "Get Jyotishi", highlight: true,
    },
    {
      name: "Acharya",  nameHi: "आचार्य",   price: "₹499", period: "/month",
      features: ["Everything in Jyotishi", "Sanskrit Library Search", "Varshaphala (coming soon)", "Priority GPU inference", "PDF export (coming soon)"],
      cta: "Get Acharya", highlight: false,
    },
  ];

  return (
    <div className="min-h-screen bg-background text-foreground overflow-x-hidden">
      {/* ─── NAV ─── */}
      <nav className="fixed top-0 inset-x-0 z-50 flex items-center justify-between px-5 sm:px-8 py-4 glass border-b border-border/20">
        <div className="flex items-center gap-2">
          <Moon className="h-6 w-6 text-primary zodiac-glow" />
          <span className="font-display text-lg text-primary text-glow-gold">{t('appTitle')}</span>
        </div>
        <Link to="/login">
          <Button size="sm" variant="outline" className="border-primary/40 text-primary hover:bg-primary/10">
            {t('login.title') ? 'Login' : 'Login'}
          </Button>
        </Link>
      </nav>

      {/* ─── HERO ─── */}
      <section className="relative pt-28 pb-20 px-5 sm:px-8 star-field overflow-hidden">
        <div className="pointer-events-none absolute inset-0 overflow-hidden">
          <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-primary/5 blur-3xl" />
          <div className="absolute top-0 right-0 w-80 h-80 rounded-full bg-nebula/5 blur-3xl" />
          <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-[600px] h-40 rounded-full bg-cosmic/5 blur-3xl" />
        </div>

        <div className="relative z-10 max-w-4xl mx-auto text-center space-y-6">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7 }}
          >
            <p className="text-xs uppercase tracking-[0.3em] text-primary/70 mb-4">
              ॐ सर्वे भवन्तु सुखिनः
            </p>
            <h1 className="font-display text-5xl sm:text-7xl text-foreground leading-tight">
              <span className="text-glow-gold">{t('landing.hero_title')}</span>
            </h1>
            <h2 className="font-display text-2xl sm:text-3xl text-muted-foreground mt-2">
              {t('landing.hero_subtitle')}
            </h2>
          </motion.div>

          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="text-base sm:text-lg text-muted-foreground max-w-2xl mx-auto leading-relaxed"
          >
            {t('landing.hero_desc')}
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5 }}
            className="flex flex-col sm:flex-row items-center justify-center gap-3"
          >
            <Link to="/login">
              <Button size="lg" className="gap-2 px-8 font-semibold">
                {t('landing.get_started')} <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
            <Link to="/horoscope">
              <Button size="lg" variant="outline" className="gap-2 border-border/40">
                {t('horoscope.title')}
              </Button>
            </Link>
          </motion.div>
        </div>
      </section>

      {/* ─── TODAY'S PANCHANG SNAPSHOT ─── */}
      <section className="px-5 sm:px-8 py-12 max-w-4xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
        >
          <p className="text-xs uppercase tracking-widest text-primary/60 mb-4 text-center">
            {t('panchang.title')}
          </p>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {cosmicFacts.map((fact, i) => (
              <motion.div
                key={fact.label}
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.08 }}
                className="cosmic-card rounded-xl p-4 text-center"
              >
                <fact.icon className="h-5 w-5 text-primary mx-auto mb-2 zodiac-glow" />
                <p className="text-xs text-muted-foreground uppercase tracking-wider">{fact.label}</p>
                <p className="font-display text-base text-foreground mt-0.5">{fact.value}</p>
                <p className="text-xs text-muted-foreground">{fact.sub}</p>
              </motion.div>
            ))}
          </div>
          <p className="text-center text-xs text-muted-foreground/50 mt-3">
            Login to get Panchang for your city — real-time precise calculations
          </p>
        </motion.div>
      </section>

      {/* ─── STATS ─── */}
      <section className="px-5 sm:px-8 py-8 border-y border-border/20">
        <div className="max-w-4xl mx-auto grid grid-cols-2 sm:grid-cols-4 gap-6 text-center">
          {stats.map((s, i) => (
            <motion.div
              key={s.label}
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
            >
              <p className="font-display text-3xl text-primary text-glow-gold">{s.value}</p>
              <p className="text-xs text-muted-foreground mt-1">{s.label}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* ─── FEATURES ─── */}
      <section className="px-5 sm:px-8 py-16 max-w-5xl mx-auto">
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="text-center mb-10"
        >
          <p className="text-xs uppercase tracking-widest text-primary/60 mb-2">What's Inside</p>
          <h2 className="font-display text-3xl text-foreground">Complete Vedic Platform</h2>
        </motion.div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {features.map((f, i) => (
            <motion.div
              key={f.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.07 }}
              className={`cosmic-card rounded-xl p-5 bg-gradient-to-br border ${f.color} flex flex-col gap-3`}
            >
              <div className="flex items-start justify-between">
                <span className="text-3xl">{f.icon}</span>
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${planColors[f.plan]}`}>
                  {f.plan}
                </span>
              </div>
              <div>
                <h3 className="font-display text-base text-foreground">{f.title}</h3>
                <p className="text-xs text-muted-foreground mt-1 leading-relaxed">{f.desc}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </section>

      {/* ─── HOW IT WORKS ─── */}
      <section className="px-5 sm:px-8 py-16 star-field">
        <div className="max-w-3xl mx-auto text-center space-y-10">
          <motion.div initial={{ opacity: 0 }} whileInView={{ opacity: 1 }} viewport={{ once: true }}>
            <p className="text-xs uppercase tracking-widest text-primary/60 mb-2">Simple Start</p>
            <h2 className="font-display text-3xl text-foreground">Begin in 3 Steps</h2>
          </motion.div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6 text-left">
            {steps.map((item, i) => (
              <motion.div
                key={item.step}
                initial={{ opacity: 0, x: -10 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.15 }}
                className="cosmic-card rounded-xl p-5 flex flex-col gap-3"
              >
                <span className="text-3xl">{item.icon}</span>
                <p className="text-xs text-primary uppercase tracking-widest font-medium">{item.step}</p>
                <h3 className="font-display text-base text-foreground">{item.title}</h3>
                <p className="text-xs text-muted-foreground leading-relaxed">{item.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ─── PLANS PREVIEW ─── */}
      <section className="px-5 sm:px-8 py-16 max-w-4xl mx-auto">
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="text-center mb-10"
        >
          <p className="text-xs uppercase tracking-widest text-primary/60 mb-2">Pricing</p>
          <h2 className="font-display text-3xl text-foreground">Start Free, Upgrade Anytime</h2>
        </motion.div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {plans.map((plan, i) => (
            <motion.div
              key={plan.name}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className={`cosmic-card rounded-xl p-6 flex flex-col gap-4 ${plan.highlight ? "ring-1 ring-primary/50 glow-border" : ""}`}
            >
              <div>
                <p className="font-display text-xl text-foreground">{plan.name}</p>
                <p className="text-xs text-muted-foreground">{plan.nameHi}</p>
                <p className="font-display text-3xl text-primary mt-2">
                  {plan.price}<span className="text-sm font-normal text-muted-foreground">{plan.period}</span>
                </p>
              </div>
              <ul className="space-y-2 flex-1">
                {plan.features.map((f) => (
                  <li key={f} className="flex items-start gap-2 text-xs text-muted-foreground">
                    <ChevronRight className="h-3 w-3 text-primary mt-0.5 flex-shrink-0" />
                    {f}
                  </li>
                ))}
              </ul>
              <Link to="/login">
                <Button
                  className="w-full"
                  variant={plan.highlight ? "default" : "outline"}
                  size="sm"
                >
                  {plan.cta}
                </Button>
              </Link>
            </motion.div>
          ))}
        </div>
      </section>

      {/* ─── FINAL CTA ─── */}
      <section className="px-5 sm:px-8 py-20 star-field">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="max-w-2xl mx-auto text-center space-y-6"
        >
          <span className="text-5xl block animate-pulse-glow">🕉️</span>
          <h2 className="font-display text-4xl text-foreground text-glow-gold">
            Begin Your Journey
          </h2>
          <p className="text-muted-foreground">
            Join thousands exploring Vedic wisdom through AI. Free to start, no credit card required.
          </p>
          <Link to="/login">
            <Button size="lg" className="gap-2 px-10 font-semibold">
              Login with OTP <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </motion.div>
      </section>

      {/* ─── FOOTER ─── */}
      <footer className="border-t border-border/20 px-5 sm:px-8 py-8 text-center">
        <div className="flex items-center justify-center gap-2 mb-3">
          <Moon className="h-4 w-4 text-primary" />
          <span className="font-display text-sm text-primary">{t('appTitle')}</span>
        </div>
        <div className="flex items-center justify-center gap-6 text-xs text-muted-foreground">
          <Link to="/horoscope" className="hover:text-foreground transition-colors">{t('nav.horoscope')}</Link>
          <Link to="/panchang"  className="hover:text-foreground transition-colors">{t('nav.panchang')}</Link>
          <Link to="/rashi"     className="hover:text-foreground transition-colors">{t('nav.rashi')}</Link>
          <Link to="/login"     className="hover:text-foreground transition-colors">Login</Link>
        </div>
        <p className="text-xs text-muted-foreground/60 mt-4">
          Grounded in Vedic tradition · Powered by AI
        </p>
      </footer>
    </div>
  );
}
