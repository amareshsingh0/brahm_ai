import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { AppSidebar } from "./AppSidebar";
import { MobileBottomNav } from "./MobileBottomNav";
import { CosmicSky } from "@/components/CosmicSky";
import { Moon, Languages } from "lucide-react";
import { useLanguageStore, LANG_LABELS } from "@/store/languageStore";
import { useTranslation } from "react-i18next";
import { useEffect } from "react";

interface AppLayoutProps {
  children: React.ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const { lang, cycleLang } = useLanguageStore();
  const { t, i18n } = useTranslation();

  useEffect(() => {
    i18n.changeLanguage(lang.toLowerCase());
  }, [lang, i18n]);

  return (
    <SidebarProvider>
      <CosmicSky />
      <div className="min-h-screen flex w-full relative" style={{ zIndex: 1 }}>
        <div className="hidden md:block">
          <AppSidebar />
        </div>
        <div className="flex-1 flex flex-col min-h-screen">
          <header className="h-14 flex items-center justify-between border-b border-border/30 px-4 glass sticky top-0 z-40">
            <div className="flex items-center">
              <SidebarTrigger className="mr-4 text-muted-foreground hover:text-primary transition-colors hidden md:flex" />
              <div className="flex items-center gap-2">
                <Moon className="h-5 w-5 text-primary md:hidden zodiac-glow" />
                <h1 className="font-display text-sm text-muted-foreground tracking-wider uppercase">
                  {t('appTitle')}
                </h1>
              </div>
            </div>
            <button
              onClick={cycleLang}
              className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-medium bg-muted/30 border border-border/40 hover:bg-muted/50 transition-colors"
              title={`Language: ${LANG_LABELS[lang]}`}
            >
              <Languages className="h-3.5 w-3.5 text-primary" />
              <span>{LANG_LABELS[lang]}</span>
            </button>
          </header>
          <main className="flex-1 overflow-auto pb-20 md:pb-6 px-3 sm:px-5 lg:px-7 pt-5">
            {children}
          </main>
        </div>
        <MobileBottomNav />
      </div>
    </SidebarProvider>
  );
}


