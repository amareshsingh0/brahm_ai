import { motion } from "framer-motion";
import { KundliChart } from "@/components/charts/KundliChart";
import { Moon, Sun, Sparkles, Clock, ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { useKundliStore } from "@/store/kundliStore";
import { useTranslation } from "react-i18next";
import cosmicBg from "@/assets/cosmic-bg.jpg";

export default function Dashboard() {
  const { t } = useTranslation();
  const birthDetails = useKundliStore((s) => s.birthDetails);
  const userName = birthDetails?.name || "Seeker";

  const quickStats = [
    { icon: Moon, label: t("dashboard.moon_rashi"), value: "Cancer", sub: "Ashlesha Nakshatra" },
    { icon: Sun, label: t("dashboard.sun_sign"), value: "Leo", sub: "Magha Nakshatra" },
    { icon: Sparkles, label: t("dashboard.ascendant"), value: "Aries", sub: "Ashwini Nakshatra" },
    { icon: Clock, label: t("dashboard.current_dasha"), value: "Rahu", sub: "Until 2033" },
  ];

  const guidance = [
    { title: t("guidance.career"), text: t("guidance.career_text"), icon: "🏛️" },
    { title: t("guidance.relationships"), text: t("guidance.relationships_text"), icon: "💫" },
    { title: t("guidance.health"), text: t("guidance.health_text"), icon: "🌿" },
    { title: t("guidance.spiritual"), text: t("guidance.spiritual_text"), icon: "🕉️" },
  ];

  const quickLinks = [
    { label: t("appTitle"), url: "/chat", icon: "🤖", desc: t("quicklinks.chat_desc") },
    { label: t("nav.library"), url: "/library", icon: "📚", desc: t("quicklinks.library_desc") },
    { label: t("nav.mantras"), url: "/mantras", icon: "🕉️", desc: t("quicklinks.mantras_desc") },
    { label: t("nav.compatibility"), url: "/compatibility", icon: "❤️", desc: t("quicklinks.compat_desc") },
  ];

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      {/* Hero */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative rounded-2xl overflow-hidden"
      >
        <img src={cosmicBg} alt="Cosmic background" className="absolute inset-0 w-full h-full object-cover opacity-60" />
        <div className="absolute inset-0 bg-gradient-to-r from-background via-background/80 to-transparent" />
        <div className="relative z-10 p-5 sm:p-8 md:p-12">
          <h1 className="font-display text-2xl sm:text-3xl md:text-4xl text-foreground mb-2 text-glow-gold">
            {t("dashboard.namaste", { name: userName })}
          </h1>
          <p className="text-muted-foreground max-w-md mb-4">{t("dashboard.subtitle")}</p>
          {!birthDetails && (
            <Link
              to="/onboarding"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              {t("dashboard.generate_cta")} <ArrowRight className="h-4 w-4" />
            </Link>
          )}
        </div>
      </motion.div>

      {/* Quick Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {quickStats.map((stat, i) => (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 + i * 0.08 }}
            className="cosmic-card rounded-xl p-4 hover:glow-border transition-shadow"
          >
            <stat.icon className="h-5 w-5 text-primary mb-2" />
            <p className="text-xs text-muted-foreground">{stat.label}</p>
            <p className="font-display text-lg text-foreground">{stat.value}</p>
            <p className="text-xs text-muted-foreground">{stat.sub}</p>
          </motion.div>
        ))}
      </div>

      {/* Main grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.4 }} className="cosmic-card rounded-xl p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-display text-lg text-foreground">{t("dashboard.birth_chart")}</h2>
            <Link to="/kundli" className="text-xs text-primary hover:underline">{t("common.view_full")}</Link>
          </div>
          <KundliChart />
        </motion.div>

        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.5 }} className="cosmic-card rounded-xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-lg text-foreground">{t("dashboard.todays_guidance")}</h2>
            <Link to="/horoscope" className="text-xs text-primary hover:underline">{t("dashboard.full_horoscope")}</Link>
          </div>
          <div className="space-y-3">
            {guidance.map((item, i) => (
              <motion.div
                key={item.title}
                initial={{ opacity: 0, x: 10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.6 + i * 0.1 }}
                className="flex gap-3 bg-muted/20 rounded-lg p-3"
              >
                <span className="text-xl">{item.icon}</span>
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
      <div className="grid grid-cols-2 sm:grid-cols-2 md:grid-cols-4 gap-3">
        {quickLinks.map((link, i) => (
          <motion.div
            key={link.url}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.8 + i * 0.05 }}
          >
            <Link to={link.url} className="cosmic-card rounded-xl p-4 block hover:glow-border transition-shadow">
              <span className="text-2xl block mb-2">{link.icon}</span>
              <p className="text-sm font-display text-foreground">{link.label}</p>
              <p className="text-xs text-muted-foreground">{link.desc}</p>
            </Link>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
