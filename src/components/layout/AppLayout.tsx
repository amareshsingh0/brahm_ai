import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { AppSidebar } from "./AppSidebar";
import { MobileDrawer } from "./MobileDrawer";
import { CosmicSky } from "@/components/CosmicSky";
import { Moon } from "lucide-react";
import { useLanguageStore } from "@/store/languageStore";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { useTranslation } from "react-i18next";
import { useEffect, useRef } from "react";
import { loadLanguage } from "@/lib/i18n";
import { ScrollToTopButton } from "@/components/ui/ScrollToTopButton";
import { useLocation } from "react-router-dom";
import PageBot from "@/components/PageBot";
import { usePageBotStore } from "@/store/pageBotStore";

interface AppLayoutProps {
  children: React.ReactNode;
}

// Routes where PageBot should NOT appear
const NO_BOT_ROUTES = new Set([
  '/dashboard', '/today', '/stories', '/library',
  '/profile', '/billing', '/chat', '/chat-history',
]);

// Routes where the global header should be hidden (page has its own header/topbar)
const NO_HEADER_ROUTES = new Set(['/chat']);

export function AppLayout({ children }: AppLayoutProps) {
  const { lang } = useLanguageStore();
  const { t, i18n } = useTranslation();
  const mainRef = useRef<HTMLElement | null>(null);
  const location = useLocation();
  const { context, data } = usePageBotStore();
  const showBot    = !NO_BOT_ROUTES.has(location.pathname);
  const showHeader = !NO_HEADER_ROUTES.has(location.pathname);

  useEffect(() => {
    const code = lang.toLowerCase();
    loadLanguage(code).then(() => i18n.changeLanguage(code));
  }, [lang, i18n]);

  return (
    <SidebarProvider>
      <CosmicSky />
      <div className="h-screen flex w-full relative overflow-hidden" style={{ zIndex: 1 }}>
        <div className="hidden md:block h-full">
          <AppSidebar />
        </div>
        <div className="flex-1 flex flex-col h-full overflow-hidden">
          {showHeader && <header className="h-14 flex-shrink-0 flex items-center justify-between border-b border-border/30 px-4 glass z-40">
            {/* Left: hamburger (mobile) / sidebar trigger (desktop) + logo */}
            <div className="flex items-center gap-2">
              <MobileDrawer />
              <SidebarTrigger className="mr-1 text-muted-foreground hover:text-primary transition-colors hidden md:flex" />
              <div className="flex items-center gap-2">
                <Moon className="h-5 w-5 text-primary md:hidden zodiac-glow" />
                <h1 className="font-display text-sm font-semibold text-foreground tracking-wider uppercase">
                  {t('appTitle')}
                </h1>
              </div>
            </div>

            {/* Right: language switcher */}
            <div className="flex items-center gap-2">
              <div className="hidden md:block">
                <LanguageSwitcher variant="full" />
              </div>
            </div>
          </header>}
          <main ref={mainRef} className="flex-1 overflow-x-hidden overflow-y-auto pb-6 px-3 sm:px-5 lg:px-7 pt-5" style={{ maxWidth: "100vw" }}>
            {children}
          </main>
        </div>
      </div>
      <ScrollToTopButton scrollRef={mainRef} />
      {showBot && <PageBot pageContext={context} pageData={data} />}
    </SidebarProvider>
  );
}
