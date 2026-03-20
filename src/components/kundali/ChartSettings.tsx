import { Settings2 } from "lucide-react";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { useKundliStore, type ChartStyle, type AyanamshaMode, type RahuMode, type NameLang } from "@/store/kundliStore";
const CHART_STYLES: { value: ChartStyle; label: string; desc: string }[] = [
  { value: "north", label: "North Indian", desc: "Houses rotate, rashis fixed by lagna" },
  { value: "south", label: "South Indian", desc: "Rashis fixed, lagna marked" },
  { value: "east",  label: "East Indian",  desc: "Diamond layout, Bengal style" },
  { value: "west",  label: "Western",      desc: "Circular wheel with degrees" },
];

const AYANAMSHA_OPTIONS: { value: AyanamshaMode; label: string }[] = [
  { value: "lahiri",     label: "Lahiri / Chitra Paksha" },
  { value: "raman",      label: "B.V. Raman" },
  { value: "kp",         label: "Krishnamurti (KP)" },
  { value: "true_citra", label: "True Chitrapaksha" },
];

const RAHU_OPTIONS: { value: RahuMode; label: string }[] = [
  { value: "mean", label: "Mean Rahu/Ketu" },
  { value: "true", label: "True Rahu/Ketu" },
];

const LANG_OPTIONS: { value: NameLang; label: string }[] = [
  { value: "vedic", label: "Vedic (Surya, Mesha...)" },
  { value: "en",    label: "English (Sun, Aries...)" },
];

function OptionGroup<T extends string>({
  label,
  options,
  value,
  onChange,
}: {
  label: string;
  options: { value: T; label: string; desc?: string }[];
  value: T;
  onChange: (v: T) => void;
}) {
  return (
    <div className="space-y-2">
      <p className="text-[10px] text-muted-foreground uppercase tracking-wider font-medium">{label}</p>
      <div className="space-y-1">
        {options.map((opt) => (
          <button
            key={opt.value}
            onClick={() => onChange(opt.value)}
            className={`w-full text-left px-3 py-2 rounded-lg text-xs transition-colors ${
              value === opt.value
                ? "bg-primary/15 text-primary border border-primary/30"
                : "text-muted-foreground hover:text-foreground hover:bg-muted/20 border border-transparent"
            }`}
          >
            <span className="font-medium">{opt.label}</span>
            {opt.desc && <span className="block text-[10px] text-muted-foreground/70 mt-0.5">{opt.desc}</span>}
          </button>
        ))}
      </div>
    </div>
  );
}

export function ChartSettings({ onRecalculate }: { onRecalculate?: () => void }) {
  const settings = useKundliStore((s) => s.kundaliSettings);
  const setSettings = useKundliStore((s) => s.setKundaliSettings);
  const kundaliData = useKundliStore((s) => s.kundaliData);

  const needsRecalc = (key: string) =>
    key === "ayanamsha" || key === "rahuMode";

  const handleChange = (key: string, value: string) => {
    setSettings({ [key]: value });
    if (needsRecalc(key) && kundaliData && onRecalculate) {
      // Small delay so store updates first
      setTimeout(onRecalculate, 100);
    }
  };

  return (
    <Sheet>
      <SheetTrigger asChild>
        <button className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs text-muted-foreground hover:text-foreground hover:bg-muted/30 transition-colors border border-border/20">
          <Settings2 className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">Settings</span>
        </button>
      </SheetTrigger>
      <SheetContent className="w-[320px] bg-background/95 backdrop-blur-xl border-border/30">
        <SheetHeader>
          <SheetTitle className="text-sm font-semibold">Chart Settings</SheetTitle>
        </SheetHeader>
        <div className="mt-5 space-y-5 pr-1">
          <OptionGroup
            label="Chart Style"
            options={CHART_STYLES}
            value={settings.chartStyle}
            onChange={(v) => handleChange("chartStyle", v)}
          />
          <OptionGroup
            label="Ayanamsha"
            options={AYANAMSHA_OPTIONS}
            value={settings.ayanamsha}
            onChange={(v) => handleChange("ayanamsha", v)}
          />
          <OptionGroup
            label="Rahu / Ketu Calculation"
            options={RAHU_OPTIONS}
            value={settings.rahuMode}
            onChange={(v) => handleChange("rahuMode", v)}
          />
          <OptionGroup
            label="Planet & Rashi Names"
            options={LANG_OPTIONS}
            value={settings.nameLang}
            onChange={(v) => handleChange("nameLang", v)}
          />

          {/* Info note */}
          <div className="text-[9px] text-muted-foreground/50 border-t border-border/20 pt-3 leading-relaxed">
            Changing Ayanamsha or Rahu mode will recalculate the chart.
            Chart style and name language are display-only preferences.
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}
