import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { AppSidebar } from "./AppSidebar";
import { MobileDrawer } from "./MobileDrawer";
import { CosmicSky } from "@/components/CosmicSky";
import { Moon } from "lucide-react";
import { useLanguageStore } from "@/store/languageStore";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { useTranslation } from "react-i18next";
import { useEffect } from "react";

interface AppLayoutProps {
  children: React.ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const { lang } = useLanguageStore();
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
            <div className="flex items-center gap-2">
              {/* Mobile hamburger drawer */}
              <MobileDrawer />
              {/* Desktop sidebar trigger */}
              <SidebarTrigger className="mr-2 text-muted-foreground hover:text-primary transition-colors hidden md:flex" />
              <div className="flex items-center gap-2">
                <Moon className="h-5 w-5 text-primary md:hidden zodiac-glow" />
                <h1 className="font-display text-sm text-muted-foreground tracking-wider uppercase">
                  {t('appTitle')}
                </h1>
              </div>
            </div>
            {/* Language switcher in header — hide on mobile (available in drawer) */}
            <LanguageSwitcher variant="full" />
          </header>
          <main className="flex-1 overflow-x-hidden overflow-y-auto pb-6 px-3 sm:px-5 lg:px-7 pt-5">
            {children}
          </main>
        </div>
      </div>
    </SidebarProvider>
  );
}
