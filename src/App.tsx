import { lazy, Suspense, useTransition, useEffect, useRef, useState } from "react";
import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate, useLocation, useNavigate } from "react-router-dom";
import { AppLayout } from "@/components/layout/AppLayout";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { useAuth } from "./hooks/useAuth";
import ProtectedRoute from "./components/ProtectedRoute";
import ProfileSetupModal from "@/components/ProfileSetupModal";
import { useAuthStore } from "@/store/authStore";
import { useKundliStore } from "@/store/kundliStore";

// ── Lazy pages (code-split per route) ─────────────────────────────────────────
const LandingPage  = lazy(() => import("./pages/LandingPage"));
const LoginPage    = lazy(() => import("./pages/LoginPage"));
const Dashboard    = lazy(() => import("./pages/Dashboard"));
const AIChatPage   = lazy(() => import("./pages/AIChatPage"));
const KundliPage   = lazy(() => import("./pages/KundliPage"));
const SkyPage      = lazy(() => import("./pages/SkyPage"));
const HoroscopePage = lazy(() => import("./pages/HoroscopePage"));
const ProfilePage  = lazy(() => import("./pages/ProfilePage"));
const PanchangPage = lazy(() => import("./pages/PanchangPage"));
const GrahanPage   = lazy(() => import("./pages/GrahanPage"));
const GocharPage   = lazy(() => import("./pages/GocharPage"));
const CompatibilityPage = lazy(() => import("./pages/CompatibilityPage"));
const RashiExplorer = lazy(() => import("./pages/RashiExplorer"));
const NakshatraExplorer = lazy(() => import("./pages/NakshatraExplorer"));
const RemediesPage = lazy(() => import("./pages/RemediesPage"));
const YogasPage    = lazy(() => import("./pages/YogasPage"));
const PalmistryPage = lazy(() => import("./pages/PalmistryPage"));
const VedicLibraryPage = lazy(() => import("./pages/VedicLibraryPage"));
const MantraDictionaryPage = lazy(() => import("./pages/MantraDictionaryPage"));
const GotraFinderPage = lazy(() => import("./pages/GotraFinderPage"));
const SubscriptionPage = lazy(() => import("./pages/SubscriptionPage"));
const RectificationPage = lazy(() => import("./pages/RectificationPage"));
const PrashnaPage  = lazy(() => import("./pages/PrashnaPage"));
const VarshpalPage = lazy(() => import("./pages/VarshpalPage"));
const KPPage       = lazy(() => import("./pages/KPPage"));
const DoshaPage    = lazy(() => import("./pages/DoshaPage"));
const SadeSatiPage = lazy(() => import("./pages/SadeSatiPage"));
const GemstoneRecommendationsPage = lazy(() => import("./pages/GemstoneRecommendationsPage"));
const StoriesPage  = lazy(() => import("./pages/StoriesPage"));
const ChatHistoryPage = lazy(() => import("./pages/ChatHistoryPage"));
const AdminPage    = lazy(() => import("./pages/AdminPage"));
const NotFound     = lazy(() => import("./pages/NotFound"));

// ── Thin top progress bar shown while lazy chunk loads ────────────────────────
function TopBar({ loading }: { loading: boolean }) {
  const [width, setWidth] = useState(0);
  const [visible, setVisible] = useState(false);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (loading) {
      setVisible(true);
      setWidth(15);
      timerRef.current = setInterval(() => {
        setWidth((w) => Math.min(w + (90 - w) * 0.12, 90));
      }, 80);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
      setWidth(100);
      const t = setTimeout(() => { setVisible(false); setWidth(0); }, 300);
      return () => clearTimeout(t);
    }
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [loading]);

  if (!visible) return null;
  return (
    <div
      style={{
        position: "fixed", top: 0, left: 0, height: 2, zIndex: 9999,
        width: `${width}%`,
        background: "hsl(38 80% 50%)",
        transition: loading ? "width 0.08s linear" : "width 0.25s ease",
        borderRadius: "0 2px 2px 0",
        boxShadow: "0 0 8px hsl(38 80% 50% / 0.6)",
      }}
    />
  );
}

// ── Wrapper that keeps old content visible while new chunk loads ───────────────
function TransitionRoutes() {
  const location = useLocation();
  const [isPending, startTransition] = useTransition();
  const [displayLocation, setDisplayLocation] = useState(location);

  useEffect(() => {
    startTransition(() => {
      setDisplayLocation(location);
    });
  }, [location]);

  return (
    <>
      <TopBar loading={isPending} />
      <Routes location={displayLocation}>
        <Route
          path="/"
          element={<AuthRedirect />}
        />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/rashi"      element={<AppLayout><RashiExplorer /></AppLayout>} />
        <Route path="/nakshatra"  element={<AppLayout><NakshatraExplorer /></AppLayout>} />
        <Route path="/horoscope"  element={<AppLayout><HoroscopePage /></AppLayout>} />
        <Route path="/stories"    element={<AppLayout><StoriesPage /></AppLayout>} />
        <Route path="/grahan"     element={<Navigate to="/panchang" replace />} />
        <Route path="/calendar"   element={<Navigate to="/panchang" replace />} />
        <Route path="/panchang"   element={<AppLayout><GrahanPage /></AppLayout>} />

        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard"    element={<AppLayout><Dashboard /></AppLayout>} />
          <Route path="/onboarding"   element={<Navigate to="/kundli" replace />} />
          <Route path="/kundli"       element={<AppLayout><KundliPage /></AppLayout>} />
          <Route path="/sky"          element={<AppLayout><SkyPage /></AppLayout>} />
          <Route path="/yogas"        element={<AppLayout><YogasPage /></AppLayout>} />
          <Route path="/remedies"     element={<AppLayout><RemediesPage /></AppLayout>} />
          <Route path="/compatibility" element={<AppLayout><CompatibilityPage /></AppLayout>} />
          <Route path="/profile"      element={<AppLayout><ProfilePage /></AppLayout>} />
          <Route path="/palmistry"    element={<AppLayout><PalmistryPage /></AppLayout>} />
          <Route path="/muhurta"      element={<Navigate to="/today" replace />} />
          <Route path="/today"        element={<AppLayout><PanchangPage /></AppLayout>} />
          <Route path="/chat"         element={<AppLayout><AIChatPage /></AppLayout>} />
          <Route path="/library"      element={<AppLayout><VedicLibraryPage /></AppLayout>} />
          <Route path="/mantras"      element={<AppLayout><MantraDictionaryPage /></AppLayout>} />
          <Route path="/gotra"        element={<AppLayout><GotraFinderPage /></AppLayout>} />
          <Route path="/subscription" element={<AppLayout><SubscriptionPage /></AppLayout>} />
          <Route path="/gochar"       element={<AppLayout><GocharPage /></AppLayout>} />
          <Route path="/rectification" element={<AppLayout><RectificationPage /></AppLayout>} />
          <Route path="/prashna"      element={<AppLayout><PrashnaPage /></AppLayout>} />
          <Route path="/varshphal"    element={<AppLayout><VarshpalPage /></AppLayout>} />
          <Route path="/kp"           element={<AppLayout><KPPage /></AppLayout>} />
          <Route path="/dosha"        element={<AppLayout><DoshaPage /></AppLayout>} />
          <Route path="/sade-sati"    element={<AppLayout><SadeSatiPage /></AppLayout>} />
          <Route path="/gemstones"    element={<AppLayout><GemstoneRecommendationsPage /></AppLayout>} />
          <Route path="/chat-history" element={<AppLayout><ChatHistoryPage /></AppLayout>} />
        </Route>

        <Route path="/admin" element={<AdminPage />} />
        <Route path="*"      element={<NotFound />} />
      </Routes>
    </>
  );
}

function AuthRedirect() {
  const { isLoggedIn } = useAuth();
  return isLoggedIn ? <Navigate to="/dashboard" /> : <LandingPage />;
}

/** Shows ProfileSetupModal once after login if profile not yet saved/skipped */
function ProfileSetupGate() {
  const { isLoggedIn, profileSetupSeen } = useAuthStore();
  const { birthDetails } = useKundliStore();
  const [open, setOpen] = useState(false);

  // Close immediately if profile loads (loadProfileIntoStore completes)
  const hasBirthData = !!(birthDetails?.birthPlace);
  useEffect(() => {
    if (hasBirthData || profileSetupSeen) setOpen(false);
  }, [hasBirthData, profileSetupSeen]);

  useEffect(() => {
    if (!isLoggedIn || profileSetupSeen) return;
    // Wait 2s to give loadProfileIntoStore time to fetch /api/user and set profileSetupSeen
    const t = setTimeout(() => {
      // Re-read latest state at open time to avoid stale closure
      const { profileSetupSeen: seen } = useAuthStore.getState();
      const { birthDetails: bd } = useKundliStore.getState();
      if (!seen && !bd?.birthPlace) setOpen(true);
    }, 2000);
    return () => clearTimeout(t);
  }, [isLoggedIn, profileSetupSeen]);

  if (!open) return null;
  return <ProfileSetupModal onClose={() => setOpen(false)} />;
}

// ── Query client ──────────────────────────────────────────────────────────────
const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 5 * 60 * 1000, retry: 1 } },
});

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <ErrorBoundary>
          <Suspense fallback={null}>
            <TransitionRoutes />
            <ProfileSetupGate />
          </Suspense>
        </ErrorBoundary>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
