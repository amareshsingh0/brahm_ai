import { motion } from "framer-motion";
import { KundliChart } from "@/components/charts/KundliChart";
import { Moon, Sun, Sparkles, Clock, ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { useKundliStore, dailyHoroscopes } from "@/store/kundliStore";
import cosmicBg from "@/assets/cosmic-bg.jpg";

const quickStats = [
  { icon: Moon, label: "Moon Rashi", value: "Cancer", sub: "Ashlesha Nakshatra" },
  { icon: Sun, label: "Sun Sign", value: "Leo", sub: "Magha Nakshatra" },
  { icon: Sparkles, label: "Ascendant", value: "Aries", sub: "Ashwini Nakshatra" },
  { icon: Clock, label: "Current Dasha", value: "Rahu", sub: "Until 2033" },
];

export default function Dashboard() {
  const birthDetails = useKundliStore((s) => s.birthDetails);
  const userName = birthDetails?.name || "Seeker";

  return (
    <div className="p-6 space-y-6">
      {/* Hero */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative rounded-2xl overflow-hidden"
      >
        <img src={cosmicBg} alt="Cosmic nebula background" className="absolute inset-0 w-full h-full object-cover opacity-40" />
        <div className="absolute inset-0 bg-gradient-to-r from-background via-background/80 to-transparent" />
        <div className="relative z-10 p-8 md:p-12">
          <h1 className="font-display text-3xl md:text-4xl text-foreground mb-2 text-glow-gold">
            Namaste, {userName}
          </h1>
          <p className="text-muted-foreground max-w-md mb-4">
            The stars align today with unique cosmic energy. Explore your celestial blueprint below.
          </p>
          {!birthDetails && (
            <Link
              to="/onboarding"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              Generate Your Kundli <ArrowRight className="h-4 w-4" />
            </Link>
          )}
        </div>
      </motion.div>

      {/* Quick Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {quickStats.map((stat, i) => (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 + i * 0.08 }}
            className="cosmic-card rounded-xl p-4 hover:glow-border transition-shadow"
            role="status"
            aria-label={`${stat.label}: ${stat.value}`}
          >
            <stat.icon className="h-5 w-5 text-primary mb-2" />
            <p className="text-xs text-muted-foreground">{stat.label}</p>
            <p className="font-display text-lg text-foreground">{stat.value}</p>
            <p className="text-[10px] text-muted-foreground">{stat.sub}</p>
          </motion.div>
        ))}
      </div>

      {/* Main grid */}
      <div className="grid lg:grid-cols-2 gap-6">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.4 }} className="cosmic-card rounded-xl p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-display text-lg text-foreground">Birth Chart</h2>
            <Link to="/kundli" className="text-xs text-primary hover:underline">View Full →</Link>
          </div>
          <KundliChart />
        </motion.div>

        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.5 }} className="cosmic-card rounded-xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-lg text-foreground">Today's Guidance</h2>
            <Link to="/horoscope" className="text-xs text-primary hover:underline">Full Horoscope →</Link>
          </div>
          <div className="space-y-3">
            {[
              { title: "Career", text: "Saturn's aspect brings focus. Ideal for long-term planning.", icon: "🏛️" },
              { title: "Relationships", text: "Venus transit favors harmony. Express gratitude today.", icon: "💫" },
              { title: "Health", text: "Moon in Cancer suggests rest. Nurture your inner world.", icon: "🌿" },
              { title: "Spiritual", text: "Jupiter's blessing enhances meditation practice.", icon: "🕉️" },
            ].map((item, i) => (
              <motion.div
                key={item.title}
                initial={{ opacity: 0, x: 10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.6 + i * 0.1 }}
                className="flex gap-3 bg-muted/20 rounded-lg p-3"
              >
                <span className="text-xl" role="img" aria-label={item.title}>{item.icon}</span>
                <div>
                  <p className="text-sm font-medium text-foreground">{item.title}</p>
                  <p className="text-xs text-muted-foreground">{item.text}</p>
                </div>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>

      {/* Quick Links */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        {[
          { label: "Brahm AI", url: "/chat", icon: "🤖", desc: "Ask Vedic wisdom" },
          { label: "Vedic Library", url: "/library", icon: "📚", desc: "Sacred texts & scriptures" },
          { label: "Mantras", url: "/mantras", icon: "🕉️", desc: "Sacred chants & meanings" },
          { label: "Compatibility", url: "/compatibility", icon: "❤️", desc: "Kundli matching" },
        ].map((link, i) => (
          <motion.div
            key={link.label}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.8 + i * 0.05 }}
          >
            <Link
              to={link.url}
              className="cosmic-card rounded-xl p-4 block hover:glow-border transition-shadow"
            >
              <span className="text-2xl block mb-2" role="img" aria-label={link.label}>{link.icon}</span>
              <p className="text-sm font-display text-foreground">{link.label}</p>
              <p className="text-[10px] text-muted-foreground">{link.desc}</p>
            </Link>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
