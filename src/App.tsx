import { lazy, Suspense } from "react";
import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AppLayout } from "@/components/layout/AppLayout";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { useAuth } from "./hooks/useAuth";
import ProtectedRoute from "./components/ProtectedRoute";

// Lazy loaded pages
const LandingPage = lazy(() => import("./pages/LandingPage"));
const LoginPage = lazy(() => import("./pages/LoginPage"));
const Dashboard = lazy(() => import("./pages/Dashboard"));
const KundliPage = lazy(() => import("./pages/KundliPage"));
const SkyPage = lazy(() => import("./pages/SkyPage"));
const TimelinePage = lazy(() => import("./pages/TimelinePage"));
const RashiExplorer = lazy(() => import("./pages/RashiExplorer"));
const StoriesPage = lazy(() => import("./pages/StoriesPage"));
const OnboardingPage = lazy(() => import("./pages/OnboardingPage"));
const NakshatraExplorer = lazy(() => import("./pages/NakshatraExplorer"));
const RemediesPage = lazy(() => import("./pages/RemediesPage"));
const YogasPage = lazy(() => import("./pages/YogasPage"));
const CompatibilityPage = lazy(() => import("./pages/CompatibilityPage"));
const HoroscopePage = lazy(() => import("./pages/HoroscopePage"));
const ProfilePage = lazy(() => import("./pages/ProfilePage"));
const PalmistryPage = lazy(() => import("./pages/PalmistryPage"));
// MuhurtaPage is now embedded inside PanchangPage — no top-level route needed
const GrahanPage = lazy(() => import("./pages/GrahanPage"));
const PanchangPage = lazy(() => import("./pages/PanchangPage"));
// CalendarPage is now embedded inside GrahanPage — no top-level route needed
const AIChatPage = lazy(() => import("./pages/AIChatPage"));
const VedicLibraryPage = lazy(() => import("./pages/VedicLibraryPage"));
const MantraDictionaryPage = lazy(() => import("./pages/MantraDictionaryPage"));
const GotraFinderPage = lazy(() => import("./pages/GotraFinderPage"));
const KnowledgeBasePage = lazy(() => import("./pages/KnowledgeBasePage"));
const SubscriptionPage = lazy(() => import("./pages/SubscriptionPage"));
const GocharPage = lazy(() => import("./pages/GocharPage"));
const NotFound = lazy(() => import("./pages/NotFound"));

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 5 * 60 * 1000, retry: 1 } },
});

function PageLoader() {
  return (
    <div className="flex items-center justify-center min-h-[50vh]">
      <div className="text-center space-y-3">
        <div className="text-4xl animate-pulse-glow">☽︎</div>
        <p className="text-xs text-muted-foreground">Consulting the stars...</p>
      </div>
    </div>
  );
}

const App = () => {
  const { isLoggedIn } = useAuth();

  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Toaster />
        <Sonner />
        <BrowserRouter>
          <ErrorBoundary>
            <Suspense fallback={<PageLoader />}>
              <Routes>
                <Route
                  path="/"
                  element={
                    isLoggedIn ? (
                      <Navigate to="/dashboard" />
                    ) : (
                      <LandingPage />
                    )
                  }
                />
                <Route path="/login" element={<LoginPage />} />
                <Route
                  path="/rashi"
                  element={
                    <AppLayout>
                      <RashiExplorer />
                    </AppLayout>
                  }
                />
                <Route
                  path="/nakshatra"
                  element={
                    <AppLayout>
                      <NakshatraExplorer />
                    </AppLayout>
                  }
                />
                <Route
                  path="/horoscope"
                  element={
                    <AppLayout>
                      <HoroscopePage />
                    </AppLayout>
                  }
                />
                <Route
                  path="/stories"
                  element={
                    <AppLayout>
                      <StoriesPage />
                    </AppLayout>
                  }
                />
                <Route path="/grahan" element={<Navigate to="/panchang" replace />} />
                <Route path="/calendar" element={<Navigate to="/panchang" replace />} />
                <Route
                  path="/panchang"
                  element={
                    <AppLayout>
                      <GrahanPage />
                    </AppLayout>
                  }
                />
                <Route element={<ProtectedRoute />}>
                  <Route
                    path="/dashboard"
                    element={
                      <AppLayout>
                        <Dashboard />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/onboarding"
                    element={
                      <AppLayout>
                        <OnboardingPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/kundli"
                    element={
                      <AppLayout>
                        <KundliPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/sky"
                    element={
                      <AppLayout>
                        <SkyPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/timeline"
                    element={
                      <AppLayout>
                        <TimelinePage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/yogas"
                    element={
                      <AppLayout>
                        <YogasPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/remedies"
                    element={
                      <AppLayout>
                        <RemediesPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/compatibility"
                    element={
                      <AppLayout>
                        <CompatibilityPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/profile"
                    element={
                      <AppLayout>
                        <ProfilePage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/palmistry"
                    element={
                      <AppLayout>
                        <PalmistryPage />
                      </AppLayout>
                    }
                  />
                  <Route path="/muhurta" element={<Navigate to="/today" replace />} />
                  <Route
                    path="/today"
                    element={
                      <AppLayout>
                        <PanchangPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/chat"
                    element={
                      <AppLayout>
                        <AIChatPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/library"
                    element={
                      <AppLayout>
                        <VedicLibraryPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/mantras"
                    element={
                      <AppLayout>
                        <MantraDictionaryPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/gotra"
                    element={
                      <AppLayout>
                        <GotraFinderPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/knowledge"
                    element={
                      <AppLayout>
                        <KnowledgeBasePage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/subscription"
                    element={
                      <AppLayout>
                        <SubscriptionPage />
                      </AppLayout>
                    }
                  />
                  <Route
                    path="/gochar"
                    element={
                      <AppLayout>
                        <GocharPage />
                      </AppLayout>
                    }
                  />
                </Route>
                <Route path="*" element={<NotFound />} />
              </Routes>
            </Suspense>
          </ErrorBoundary>
        </BrowserRouter>
      </TooltipProvider>
    </QueryClientProvider>
  );
};

export default App;
