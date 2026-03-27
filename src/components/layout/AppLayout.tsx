import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { AppSidebar } from "./AppSidebar";
import { MobileDrawer } from "./MobileDrawer";
import { CosmicSky } from "@/components/CosmicSky";
import { Moon } from "lucide-react";
import { useLanguageStore } from "@/store/languageStore";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { useTranslation } from "react-i18next";
import { useEffect, useState } from "react";
import { loadLanguage } from "@/lib/i18n";
import { useAuthStore } from "@/store/authStore";
import { Avatar, ProfilePopup } from "./ProfilePopup";

interface AppLayoutProps {
  children: React.ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const { lang } = useLanguageStore();
  const { t, i18n } = useTranslation();
  const { name, plan, isLoggedIn } = useAuthStore();
  const [profileOpen, setProfileOpen] = useState(false);

  useEffect(() => {
    const code = lang.toLowerCase();
    loadLanguage(code).then(() => i18n.changeLanguage(code));
  }, [lang, i18n]);

  return (
    <SidebarProvider>
      <CosmicSky />
      <div className="min-h-screen flex w-full relative overflow-hidden" style={{ zIndex: 1 }}>
        <div className="hidden md:block">
          <AppSidebar />
        </div>
        <div className="flex-1 flex flex-col min-h-screen">
          <header className="h-14 flex items-center justify-between border-b border-border/30 px-4 glass sticky top-0 z-40">
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

            {/* Right: language + user avatar */}
            <div className="flex items-center gap-2">
              <div className="hidden md:block">
                <LanguageSwitcher variant="full" />
              </div>
              {isLoggedIn && (
                <div className="relative md:hidden">
                  <button
                    onClick={() => setProfileOpen((v) => !v)}
                    className="flex items-center gap-2 rounded-full px-2 py-1 hover:bg-muted/50 transition-colors"
                  >
                    <Avatar name={name ?? ""} size={32} />
                    <div className="hidden sm:flex flex-col items-start leading-none">
                      <span className="text-xs font-medium text-foreground max-w-[100px] truncate">{name}</span>
                      <span className="text-[10px] text-muted-foreground capitalize">{plan}</span>
                    </div>
                  </button>
                  {profileOpen && (
                    <>
                      <div className="fixed inset-0 z-40" onClick={() => setProfileOpen(false)} />
                      <div className="absolute right-0 top-full mt-2 z-50 w-72">
                        <ProfilePopup onClose={() => setProfileOpen(false)} />
                      </div>
                    </>
                  )}
                </div>
              )}
            </div>
          </header>
          <main className="flex-1 overflow-x-hidden overflow-y-auto pb-6 px-3 sm:px-5 lg:px-7 pt-5" style={{ maxWidth: "100vw" }}>
            {children}
          </main>
        </div>
      </div>
    </SidebarProvider>
  );
}
