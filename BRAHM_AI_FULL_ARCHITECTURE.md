# Brahm AI — Full Architecture Document
# Last Updated: 2026-03-27 (v7.0 — API Versioning + Admin Subscriptions + Chat Monitor Upgrade + Auth Upgrade)

---

## 1. SYSTEM OVERVIEW

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         BRAHM AI PLATFORM v5.0                            │
│              Web (React Vite) + Android (Java 17) + iOS (Swift 5.9)       │
│                                                                            │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐        │
│  │   REACT WEBSITE  │  │  ANDROID APP     │  │   iOS APP        │        │
│  │  (Vite + React)  │  │  Java 17 + MVVM  │  │  Swift 5.9 SwiftUI       │
│  │  brahmasmi.      │  │  Retrofit2 +     │  │  URLSession +    │        │
│  │  bimoraai.com    │  │  OkHttp SSE      │  │  async/await SSE │        │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘        │
│           │   HTTP/SSE          │   HTTP/SSE           │   HTTP/SSE       │
│           └────────────────────┬┘──────────────────────┘                  │
│                                │                                            │
│                                ▼                                            │
│                  ┌─────────────────────────────┐                           │
│                  │       FASTAPI BACKEND         │                          │
│                  │   brahmasmi.bimoraai.com/api  │                          │
│                  │   (Google Cloud VM — GPU)     │                          │
│                  └──────────────┬───────────────┘                          │
│                                 │                                           │
│               ┌─────────────────▼──────────────────┐                      │
│               │         AI + CALC LAYER (v5.0)       │                     │
│               │   Two-Pass: Gemini 2.5 Flash          │                    │
│               │   RAG Pipeline (FAISS 1.1M chunks)    │                    │
│               │   BM25 + Cross-Encoder Reranker        │                   │
│               │   pyswisseph + Swiss Ephemeris         │                   │
│               │   kundali/panchang/kp/prashna etc.     │                   │
│               └──────────────┬─────────────────────┘                      │
│                              │                                              │
│               ┌──────────────▼───────────────┐                             │
│               │          DATA LAYER            │                            │
│               │  SQLite (dev) / PostgreSQL (prod)                           │
│               │  users, subscriptions, sessions │                           │
│               └───────────────────────────────┘                            │
│                                                                             │
│  EXTERNAL SERVICES:                                                         │
│  • Google Cloud VM: g2-standard-32, 32 vCPU, 128GB RAM, NVIDIA L4 24GB    │
│  • Cashfree (payments/subscriptions)                                        │
│  • MSG91 / Firebase (OTP SMS + FCM push notifications)                     │
│  • APNs (iOS push notifications)                                            │
│  • Gemini 2.5 Flash API (google-genai SDK)                                 │
└──────────────────────────────────────────────────────────────────────────┘
```

### Platform Summary
| Platform | Tech | Distribution | Status |
|----------|------|--------------|--------|
| **Website** | React 18 + Vite + TypeScript | brahmasmi.bimoraai.com | ✅ Live |
| **Admin App** | React 18 + Vite (separate build) | api.brahmasmi.bimoraai.com/admin | ✅ Live |
| **Android** | Java 17 LTS + MVVM + Retrofit2 | Google Play Store | 🔜 Phase 1-4 |
| **iOS** | Swift 5.9 + SwiftUI + URLSession | Apple App Store | 🔜 Phase 5-6 |

All platforms share the **same FastAPI backend** — zero backend changes needed for mobile.

### Domain Separation
| Domain | Purpose | Serves |
|--------|---------|--------|
| `brahmasmi.bimoraai.com` | User-facing web app | React app (`/var/www/brahm-ai/`) |
| `brahmasmi.bimoraai.com/admin` | **Returns 404** | Admin blocked from user domain |
| `api.brahmasmi.bimoraai.com/api` | FastAPI backend | Proxied to port 8000 |
| `api.brahmasmi.bimoraai.com/admin` | Admin panel | Separate React app (`/var/www/brahm-admin/`) |
| `api.brahmasmi.bimoraai.com/docs` | Swagger UI | FastAPI docs |

**VM:** Google Cloud g2-standard-32 | 32 vCPU | 128 GB RAM | NVIDIA L4 24GB GPU
**IP:** 34.135.70.190 | Cost: ~$0.60/hr | OS: Debian 12 | CUDA 13.2

---

## 2. COMPLETE FILE TREE

```
C:\desktop\Brahm AI\              (local project root)
│
├── BRAHM_AI_FULL_ARCHITECTURE.md  ← THIS FILE
├── PROGRESS.md
├── VM_SETUP.md
├── capacitor.config.ts             ← NEW: Capacitor mobile app config
├── package.json                    (React + Capacitor deps)
├── vite.config.ts                  (proxy /api → port 8000)
├── tailwind.config.ts              (dark cosmic theme)
├── index.html                      (React entry)
├── .env.local                      (VITE_API_URL=http://34.135.70.190:8000)
│
├── admin-app/                      ← Separate Admin React App (deployed to /var/www/brahm-admin/)
│   ├── package.json                (react, react-router-dom, recharts, lucide-react, tailwindcss)
│   ├── vite.config.ts              (base: '/admin/', proxy /api → port 8000, port 5174)
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   └── src/
│       ├── main.tsx                React entry point
│       ├── App.tsx                 BrowserRouter basename="/admin" + all routes
│       ├── index.css               CSS vars (same tokens as main app)
│       ├── lib/
│       │   ├── types.ts            All admin TypeScript interfaces
│       │   ├── api.ts              aFetch + 5-min GET cache + preloadAll()
│       │   └── utils.ts            cn, fmt, fmtInr, PLAN_CLS, STATUS_CLS, PAY_CLS, ACTION_CLS
│       ├── components/
│       │   ├── layout/
│       │   │   ├── AdminLayout.tsx  Auth guard + sidebar + header + mobile bottom nav
│       │   │   ├── Sidebar.tsx      Collapsible (w-56/w-14), NavLink active state
│       │   │   └── Header.tsx       Logo + Admin badge + logout
│       │   └── ui/
│       │       ├── StatCard.tsx     Icon + label + value + optional trend
│       │       ├── DataTable.tsx    Generic reusable table (Column<T> + rows + loading)
│       │       ├── Pagination.tsx
│       │       ├── Badge.tsx
│       │       └── Loader.tsx       Loader + Empty + ActionBtn
│       ├── pages/
│       │   ├── LoginPage.tsx        Username + Secret Key → btoa(user:key) → sessionStorage → preloadAll
│       │   ├── DashboardPage.tsx    Stats grid + revenue (Top Endpoints moved to ApiMonitorPage)
│       │   ├── UsersPage.tsx        Paginated table → navigate /users/:id on click
│       │   ├── UserDetailPage.tsx   useParams(:id) + 7 sub-tabs + action buttons
│       │   ├── PaymentsPage.tsx     Revenue StatCards + filter + refund action
│       │   ├── SubscriptionsPage.tsx Full subscription list: summary metrics, plan chart, extend/cancel/grant
│       │   ├── ChatsPage.tsx        Conversations grouped by session_id; user+AI turn pairs; confidence badges
│       │   ├── ApiMonitorPage.tsx   Top endpoints, latency, errors, status dist, timeline (today/7d/30d)
│       │   ├── DeletedAccountsPage.tsx 30-day grace window review
│       │   └── LogsPage.tsx         Admin action log (DataTable + ACTION_CLS)
│       └── user-detail-tabs/
│           ├── ProfileTab.tsx       User fields grid
│           ├── ChatsTab.tsx         Context filter + flag button + pagination
│           ├── KundalisTab.tsx      Expand → raw JSON toggle
│           ├── PalmistryTab.tsx     Lines found chips
│           ├── PaymentsTab.tsx      Payment history with status badge
│           ├── UsageTab.tsx         Bar chart (today's feature counts)
│           └── LoginsTab.tsx        Login history (success/fail + IP + device)
│
├── api/                            ← FastAPI backend (on VM: ~/books/api/)
│   ├── main.py                     FastAPI app, CORS, lifespan
│   ├── config.py                   ALL constants (single source of truth)
│   ├── dependencies.py             Shared G{} state, Depends() injection
│   ├── requirements.txt            fastapi uvicorn sse-starlette pydantic
│   │
│   ├── models/
│   │   ├── __init__.py
│   │   ├── common.py               BirthDetails, Coordinates
│   │   ├── chat.py                 ChatRequest, Source, ChatMessage
│   │   ├── kundali.py              KundaliRequest, KundaliResponse, GrahaData
│   │   ├── panchang.py             PanchangRequest, PanchangResponse
│   │   ├── compatibility.py        CompatibilityRequest, CompatibilityResponse
│   │   ├── user.py                 UserProfile, UserSettings
│   │   ├── auth.py                 ← NEW: OTPRequest, OTPVerify, TokenResponse, RegisterRequest
│   │   └── subscription.py        ← NEW: Plan, SubscriptionStatus, CashfreeOrder
│   │
│   ├── services/
│   │   ├── __init__.py
│   │   ├── rag_service.py          load_all(), hybrid_search(), generate_stream()
│   │   ├── kundali_service.py      calc_kundali() → KundaliResponse dict
│   │   ├── panchang_service.py     Panchang class wrapper → PanchangResponse dict
│   │   ├── festival_service.py     get_festival_calendar() → 53 festivals/year
│   │   ├── calendar_service.py     monthly Panchang calendar with festival lookup
│   │   ├── horoscope_service.py    static JSON + future LLM generation
│   │   ├── muhurta_service.py      auspicious timing logic
│   │   ├── auth_service.py        ← NEW: OTP send/verify, JWT create/decode
│   │   └── cashfree_service.py    ← NEW: create order, verify webhook, manage subscription
│   │
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── chat.py                 POST /api/chat (SSE stream)
│   │   ├── kundali.py              POST /api/kundali
│   │   ├── panchang.py             GET  /api/panchang
│   │   ├── compatibility.py        POST /api/compatibility
│   │   ├── search.py               GET  /api/search
│   │   ├── planets.py              GET  /api/planets/now
│   │   ├── muhurta.py              GET  /api/muhurta
│   │   ├── grahan.py               GET  /api/grahan
│   │   ├── horoscope.py            GET  /api/horoscope/{rashi}
│   │   ├── calendar.py             GET  /api/calendar/month
│   │   ├── user.py                 GET/PATCH /api/user/me
│   │   ├── cities.py               GET  /api/cities
│   │   ├── auth.py                ← NEW: POST /api/auth/send-otp, /api/auth/verify-otp, /api/auth/logout
│   │   └── subscription.py        ← NEW: GET /api/subscription/plans, POST /api/subscription/checkout, POST /api/subscription/webhook
│   │
│   ├── middleware/
│   │   └── auth_middleware.py     ← NEW: JWT decode → request.state.user_id
│   │
│   └── data/
│       ├── cities.json             SINGLE SOURCE: 40 cities + lat/lon/tz
│       ├── static_horoscopes.json  fallback horoscopes per rashi
│       ├── subscription_plans.json ← NEW: plan definitions (Free, Jyotishi, Acharya)
│       └── users.db                SQLite (dev) / PostgreSQL (prod)
│
├── src/                            ← React frontend
│   ├── main.tsx                    React entry point
│   ├── App.tsx                     Routes + QueryClientProvider + AuthGuard
│   ├── index.css                   Global styles (dark cosmic theme)
│   ├── i18n.ts                    ← NEW: react-i18next setup (en/hi/sa)
│   │
│   ├── locales/                   ← NEW: Translation files
│   │   ├── en/
│   │   │   └── translation.json    English UI strings
│   │   ├── hi/
│   │   │   └── translation.json    Hindi UI strings (हिन्दी)
│   │   └── sa/
│   │       └── translation.json    Sanskrit labels (for purists)
│   │
│   ├── types/
│   │   └── api.ts                  ALL TypeScript interfaces (matches API exactly)
│   │
│   ├── lib/
│   │   ├── utils.ts                cn() helper
│   │   ├── api.ts                  Central API client (get/post/streamChat) + auth header
│   │   └── cities.ts               Fetch+cache from /api/cities
│   │
│   ├── hooks/
│   │   ├── use-mobile.tsx
│   │   ├── use-toast.ts
│   │   ├── useChat.ts              SSE streaming chat
│   │   ├── useKundali.ts           POST /api/kundali mutation
│   │   ├── usePanchang.ts          GET /api/panchang query
│   │   ├── useCompatibility.ts     POST /api/compatibility mutation
│   │   ├── useHoroscope.ts         GET /api/horoscope/{rashi} query
│   │   ├── useSearch.ts            GET /api/search query
│   │   ├── usePlanets.ts           GET /api/planets/now query
│   │   ├── useGrahan.ts            GET /api/grahan query
│   │   ├── useMuhurta.ts           GET /api/muhurta query
│   │   ├── useUser.ts              GET/PATCH /api/user/me
│   │   ├── useAuth.ts             ← NEW: sendOtp, verifyOtp, logout, isLoggedIn
│   │   └── useSubscription.ts     ← NEW: plans, currentPlan, checkout, webhookStatus
│   │
│   ├── store/
│   │   ├── kundliStore.ts          Zustand (kundaliData: KundaliResponse)
│   │   └── authStore.ts           ← NEW: Zustand (token, userId, name, phone, plan)
│   │
│   ├── components/
│   │   ├── ErrorBoundary.tsx
│   │   ├── NavLink.tsx
│   │   ├── ProtectedRoute.tsx     ← NEW: redirects to /login if not authenticated
│   │   ├── PlanGate.tsx           ← NEW: wraps premium features, shows upgrade CTA
│   │   ├── LanguageSwitcher.tsx   ← NEW: EN / हिं / संस्कृत toggle (header)
│   │   ├── layout/
│   │   │   ├── AppLayout.tsx       (existing — no change)
│   │   │   ├── AppSidebar.tsx      (existing — add logout + plan badge)
│   │   │   └── MobileBottomNav.tsx (existing — no change)
│   │   ├── cards/
│   │   │   ├── PlanetInfoPanel.tsx
│   │   │   └── RashiCard.tsx
│   │   ├── charts/
│   │   │   ├── KundliChart.tsx
│   │   │   └── DashaTimeline.tsx
│   │   └── ui/                     50+ shadcn components
│   │
│   ├── pages/
│   │   ├── Index.tsx               route redirect → /dashboard or /landing
│   │   ├── LandingPage.tsx         first screen for unauthenticated users
│   │   ├── LoginPage.tsx           phone OTP + optional Google OAuth
│   │   ├── OnboardingPage.tsx      birth details setup + city search → kundliStore
│   │   ├── SubscriptionPage.tsx    plan cards + Cashfree checkout
│   │   │
│   │   ├── Dashboard.tsx           home dashboard — quick stats + chart preview
│   │   ├── KundliPage.tsx          7-tab Kundali (Chart/Planets/Dashas/Yogas/Alerts/Shadbala/Navamsha)
│   │   ├── AIChatPage.tsx          SSE streaming AI chat (Jyotishi+ plan)
│   │   ├── PanchangPage.tsx        today's panchang + muhurta tabs [route: /today]
│   │   ├── MuhurtaPage.tsx         auspicious timing finder (POST /api/muhurta/activity)
│   │   ├── HoroscopePage.tsx       daily rashi horoscope (GET /api/horoscope/{rashi})
│   │   ├── GocharPage.tsx          planetary transits + AV scoring (GET+POST /api/gochar)
│   │   ├── CompatibilityPage.tsx   kundali milan + nakshatra compatibility (POST /api/compatibility)
│   │   ├── GrahanPage.tsx          eclipse calendar + festival calendar
│   │   ├── CalendarPage.tsx        monthly panchang calendar (embedded in GrahanPage)
│   │   ├── SkyPage.tsx             live sky — current planet positions (GET /api/planets/now)
│   │   │
│   │   ├── TimelinePage.tsx        dasha timeline chart (reads kundaliData.dashas from store)
│   │   ├── YogasPage.tsx           yoga analysis cards (reads kundaliData.yogas from store)
│   │   ├── RemediesPage.tsx        yoga remedies (mantra/gem/deity) — reads kundaliData
│   │   │
│   │   ├── SadeSatiPage.tsx        sade sati calculator (GET /api/sade-sati)
│   │   ├── DoshaPage.tsx           manglik + dosha analysis (GET /api/dosha)
│   │   ├── GemstoneRecommendationsPage.tsx  gemstone recommendations (GET /api/gemstones)
│   │   ├── KPPage.tsx              KP system sub-lords table (POST /api/kp)
│   │   ├── PrashnaPage.tsx         prashna kundali — horary chart (POST /api/prashna)
│   │   ├── VarshpalPage.tsx        varshphal solar return chart (POST /api/varshphal)
│   │   ├── RectificationPage.tsx   birth time rectification (POST /api/rectification)
│   │   ├── PalmistryPage.tsx       camera palm reading — Gemini Vision (POST /api/palmistry)
│   │   │
│   │   ├── VedicLibraryPage.tsx    vedic text search (GET /api/search)
│   │   ├── MantraDictionaryPage.tsx mantra dictionary search (GET /api/search)
│   │   ├── KnowledgeBasePage.tsx   knowledge base search (GET /api/search)
│   │   ├── ProfilePage.tsx         user profile + birth details + plan info
│   │   │
│   │   ├── RashiExplorer.tsx       rashi info explorer (STATIC)
│   │   ├── NakshatraExplorer.tsx   nakshatra info explorer (STATIC)
│   │   ├── StoriesPage.tsx         vedic stories (STATIC)
│   │   ├── GotraFinderPage.tsx     gotra finder tool (STATIC)
│   │   └── NotFound.tsx            404 page (STATIC)
│   │
│   └── ingestion/                  Python OCR pipeline modules
│
│
├── android/                       ← Native Android App (Java 17 + MVVM)
│   └── app/
│       ├── src/main/
│       │   ├── java/com/bimoraai/brahm/
│       │   │   ├── api/
│       │   │   │   ├── ApiClient.java          ← Retrofit2 setup (base URL, auth header)
│       │   │   │   ├── ApiService.java         ← all API endpoint interfaces
│       │   │   │   └── SseManager.java         ← OkHttp EventSource for AI Chat SSE
│       │   │   ├── model/
│       │   │   │   ├── KundaliData.java        ← mirrors KundaliResponse from backend
│       │   │   │   ├── PanchangData.java
│       │   │   │   ├── ChatMessage.java
│       │   │   │   └── UserProfile.java
│       │   │   ├── repository/
│       │   │   │   ├── KundaliRepository.java
│       │   │   │   ├── ChatRepository.java
│       │   │   │   └── PanchangRepository.java
│       │   │   ├── viewmodel/
│       │   │   │   ├── KundaliViewModel.java
│       │   │   │   ├── ChatViewModel.java
│       │   │   │   └── PanchangViewModel.java
│       │   │   ├── ui/
│       │   │   │   ├── auth/
│       │   │   │   │   ├── LoginActivity.java       ← OTP phone login
│       │   │   │   │   └── OnboardingActivity.java  ← name, DOB, time, city
│       │   │   │   ├── main/
│       │   │   │   │   └── MainActivity.java        ← bottom nav host (5 tabs)
│       │   │   │   ├── home/HomeFragment.java       ← Dashboard
│       │   │   │   ├── kundali/
│       │   │   │   │   ├── KundaliFragment.java     ← 7-tab Kundali
│       │   │   │   │   └── KundaliChartView.java    ← custom Canvas wheel
│       │   │   │   ├── chat/ChatFragment.java       ← AI Chat (SSE streaming)
│       │   │   │   ├── today/TodayFragment.java     ← Panchang
│       │   │   │   ├── profile/ProfileFragment.java
│       │   │   │   └── secondary/
│       │   │   │       ├── GocharActivity.java
│       │   │   │       ├── CompatibilityActivity.java
│       │   │   │       ├── MuhurtaActivity.java
│       │   │   │       ├── SadeSatiActivity.java
│       │   │   │       ├── DoshaActivity.java
│       │   │   │       ├── GemstoneActivity.java
│       │   │   │       ├── KPActivity.java
│       │   │   │       ├── PrashnaActivity.java
│       │   │   │       ├── VarshpalActivity.java
│       │   │   │       ├── RectificationActivity.java
│       │   │   │       └── PalmistryActivity.java   ← camera + Gemini Vision
│       │   │   └── utils/
│       │   │       ├── PrefsHelper.java             ← SharedPreferences wrapper
│       │   │       └── DateUtils.java
│       │   └── res/
│       │       ├── layout/                          ← XML layouts (Material Design 3)
│       │       ├── navigation/nav_graph.xml         ← Jetpack Navigation
│       │       ├── values/                          ← colors, strings, themes
│       │       └── drawable/                        ← icons, assets
│       ├── build.gradle                             ← Retrofit2, OkHttp, MPAndroidChart etc.
│       └── google-services.json                     ← Firebase config (FCM push)
│
├── ios/                           ← Native iOS App (Swift 5.9 + SwiftUI)
│   └── BrahmAI/
│       ├── App/
│       │   ├── BrahmAIApp.swift                    ← @main entry point
│       │   └── ContentView.swift                   ← NavigationStack root
│       ├── Network/
│       │   ├── APIClient.swift                     ← URLSession base client
│       │   ├── APIEndpoints.swift                  ← all endpoint definitions
│       │   └── SSEStream.swift                     ← URLSession bytes stream for AI Chat
│       ├── Models/
│       │   ├── KundaliData.swift
│       │   ├── PanchangData.swift
│       │   ├── ChatMessage.swift
│       │   └── UserProfile.swift
│       ├── Views/
│       │   ├── Auth/
│       │   │   ├── LoginView.swift                 ← OTP phone login
│       │   │   └── OnboardingView.swift            ← birth details setup
│       │   ├── Main/
│       │   │   └── MainTabView.swift               ← TabView (5 tabs)
│       │   ├── Home/HomeView.swift                 ← Dashboard
│       │   ├── Kundali/
│       │   │   ├── KundaliView.swift               ← 7-tab Kundali
│       │   │   └── KundaliChartView.swift          ← Swift Charts + Canvas wheel
│       │   ├── Chat/ChatView.swift                 ← AI Chat (SSE streaming)
│       │   ├── Today/TodayView.swift               ← Panchang
│       │   ├── Profile/ProfileView.swift
│       │   └── Secondary/
│       │       ├── GocharView.swift
│       │       ├── CompatibilityView.swift
│       │       ├── MuhurtaView.swift
│       │       ├── SadeSatiView.swift
│       │       ├── DoshaView.swift
│       │       ├── GemstoneView.swift
│       │       ├── KPView.swift
│       │       ├── PrashnaView.swift
│       │       ├── VarshpalView.swift
│       │       ├── RectificationView.swift
│       │       └── PalmistryView.swift             ← camera + Gemini Vision
│       ├── ViewModels/
│       │   ├── KundaliViewModel.swift
│       │   ├── ChatViewModel.swift
│       │   └── PanchangViewModel.swift
│       └── Utils/
│           ├── KeychainHelper.swift                ← secure token storage
│           └── DateUtils.swift
│
└── scripts/
    ├── 08_gradio_kundali.py        Gradio UI (kept, port 7860)
    └── ...
```

---

## 3. ALL API ENDPOINTS

### Base URL: `http://34.135.70.190:8000`

#### Auth Endpoints (NEW)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/send-otp` | — | Send OTP to phone (MSG91/Firebase) |
| POST | `/api/auth/verify-otp` | — | Verify OTP → returns JWT access token |
| POST | `/api/auth/google` | — | Google OAuth token exchange → JWT |
| POST | `/api/auth/logout` | Bearer JWT | Invalidate token |
| POST | `/api/auth/refresh` | Refresh token | Get new access token |

#### Subscription Endpoints (NEW)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/subscription/plans` | — | List all plans (Free/Jyotishi/Acharya) |
| GET | `/api/subscription/status` | Bearer JWT | Current user's plan + expiry |
| POST | `/api/subscription/checkout` | Bearer JWT | Create Cashfree order → payment URL |
| POST | `/api/subscription/webhook` | Cashfree sig | Payment confirmed → activate plan |
| POST | `/api/subscription/cancel` | Bearer JWT | Cancel subscription |

#### User Endpoints (UPDATED)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/user/me` | Bearer JWT | Full profile (name, birth, plan, settings) |
| PATCH | `/api/user/me` | Bearer JWT | Update profile / preferences / language |
| DELETE | `/api/user/me` | Bearer JWT | Delete account (GDPR) |

#### API Versioning (v7.0)
All Jyotish feature routers are mounted at **both** `/api/v1/...` (versioned) and `/api/...` (legacy alias):
```python
# api/main.py
for router, prefix, tags in _JYOTISH_ROUTERS:
    app.include_router(router, prefix=f"/api/v1{prefix}", tags=tags)
    app.include_router(router, prefix=f"/api{prefix}",    tags=tags)  # legacy
# Admin + Auth stay unversioned at /api/admin and /api/auth
```
Mobile clients can migrate to `/api/v1/...` at their own pace. Swagger: `/api/docs`.

#### Core Feature Endpoints (unchanged — available at both `/api/...` and `/api/v1/...`)
| Method | Endpoint | Auth | Description | Speed |
|--------|----------|------|-------------|-------|
| POST | `/api/chat` | Bearer JWT (Jyotishi+) | RAG chat SSE stream | ~14s first |
| POST | `/api/kundali` | Bearer JWT | Birth chart | <1s |
| GET | `/api/panchang` | Bearer JWT | Today's almanac | <1s |
| POST | `/api/compatibility` | Bearer JWT | Guna matching | <2s |
| GET | `/api/search` | Bearer JWT | Semantic search | <0.5s |
| GET | `/api/planets/now` | Bearer JWT | Current planets | <1s |
| GET | `/api/muhurta` | Bearer JWT | Auspicious timing | <1s |
| GET | `/api/grahan` | — | Eclipse calendar (public) | <0.5s |
| GET | `/api/festivals` | — | Festival calendar (public) | <1s |
| GET | `/api/calendar/month` | Bearer JWT | Monthly Panchang | <1s |
| GET | `/api/horoscope/{rashi}` | — | Daily prediction (public) | <0.5s |
| GET | `/api/cities` | — | City lookup (public) | <0.1s |

#### Admin Endpoints (X-Admin-Key header auth — `btoa("username:secret_key")`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/stats` | MAU, DAU, revenue, subscriptions summary |
| GET | `/api/admin/users` | Paginated users list; batch-enriched (4 IN queries, no N+1) |
| GET | `/api/admin/users/{id}` | Full user profile + subscription + usage_today |
| PATCH | `/api/admin/users/{id}` | suspend / reactivate / ban / unban (status field only) |
| POST | `/api/admin/users/{id}/action` | grant_plan / extend_sub / cancel_sub / clear_chats / delete |
| GET | `/api/admin/users/{id}/chats` | Paginated chat messages (ctx filter) |
| GET | `/api/admin/users/{id}/kundalis` | Kundali history |
| GET | `/api/admin/users/{id}/palms` | Palmistry history |
| GET | `/api/admin/users/{id}/payments` | Payment history |
| GET | `/api/admin/users/{id}/logins` | Login history |
| POST | `/api/admin/chats/{id}/flag` | Flag a chat message |
| GET | `/api/admin/payments` | All payments + revenue stats |
| POST | `/api/admin/payments/{id}/refund` | Issue refund |
| GET | `/api/admin/revenue` | Revenue breakdown (today/month/total) |
| GET | `/api/admin/chats` | Recent chats; batch-enriched (no N+1 HTTP/2 drops) |
| GET | `/api/admin/chats/flagged` | Flagged chats |
| GET | `/api/admin/chats/analytics` | Top questions + context distribution |
| GET | `/api/admin/api-stats` | Top endpoints, latency, errors, timeline (period=today/7d/30d) |
| GET | `/api/admin/subscriptions` | All subscriptions: user enriched, days_left, summary metrics |
| GET | `/api/admin/deleted-accounts` | 30-day grace window accounts |
| GET | `/api/admin/logs` | Admin action log |

---

## 4. AUTH SYSTEM

### 4.1 Auth Flow (Phone OTP — Primary)
```
Mobile/Web                 FastAPI                  MSG91/Firebase
    │                         │                           │
    │── POST /auth/send-otp ──▶│                           │
    │   { phone: "+919876543210" }                         │
    │                         │── Send OTP SMS ──────────▶│
    │                         │◀─ OTP sent ───────────────│
    │◀─ { sent: true } ───────│                           │
    │                         │                           │
    │   [User enters 6-digit OTP]                         │
    │                         │                           │
    │── POST /auth/verify-otp ▶│                           │
    │   { phone, otp: "123456" }                          │
    │                         │── Verify OTP ────────────▶│
    │                         │◀─ valid/invalid ──────────│
    │                         │                           │
    │                         │── Upsert user in DB       │
    │                         │── Generate JWT (7 days)   │
    │                         │── Generate refresh (30d)  │
    │                         │                           │
    │◀─ { access_token,       │                           │
    │     refresh_token,      │                           │
    │     user: {...} }       │                           │
    │                         │                           │
    │── Store tokens          │                           │
    │   Web: localStorage     │                           │
    │   Android: Encrypted    │                           │
    │     SharedPreferences   │                           │
    │   iOS: Keychain         │                           │
```

### 4.2 JWT Structure
```json
{
  "sub": "usr_a1b2c3d4",
  "phone": "+919876543210",
  "name": "Ramesh Sharma",
  "plan": "jyotishi",
  "role": "user",
  "iat": 1742000000,
  "exp": 1742604800
}
```

### 4.3 Backend JWT Implementation
```python
# api/services/auth_service.py

SECRET_KEY = os.getenv("JWT_SECRET")      # 64-char random string in .env
ALGORITHM  = "HS256"
ACCESS_TOKEN_EXPIRE  = timedelta(days=7)
REFRESH_TOKEN_EXPIRE = timedelta(days=30)

def create_access_token(user: UserDB) -> str:
    payload = {
        "sub":   user.id,
        "phone": user.phone,
        "name":  user.name,
        "plan":  user.plan,          # "free" | "jyotishi" | "acharya"
        "role":  user.role,          # "user" | "admin"
        "exp":   datetime.utcnow() + ACCESS_TOKEN_EXPIRE
    }
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)

def verify_token(token: str) -> dict:
    return jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
```

### 4.4 Dependency Injection (Protect any route)
```python
# api/dependencies.py

from fastapi.security import HTTPBearer
from api.services.auth_service import verify_token

security = HTTPBearer()

def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    payload = verify_token(token)          # raises 401 if invalid/expired
    return payload                         # dict: sub, phone, name, plan, role

def require_plan(min_plan: str):
    """Usage: Depends(require_plan("jyotishi"))"""
    plan_order = {"free": 0, "jyotishi": 1, "acharya": 2}
    def checker(user = Depends(get_current_user)):
        if plan_order[user["plan"]] < plan_order[min_plan]:
            raise HTTPException(403, f"Requires {min_plan} plan")
        return user
    return checker
```

### 4.5 Frontend Auth Store (Zustand)
```typescript
// src/store/authStore.ts

interface AuthState {
  token:     string | null
  userId:    string | null
  name:      string | null
  phone:     string | null
  plan:      "free" | "jyotishi" | "acharya"
  isLoggedIn: boolean

  setAuth: (token: string, user: UserInfo) => void
  logout:  () => void
}

// Persisted to localStorage (web) / EncryptedSharedPreferences (Android) / Keychain (iOS)
```

---

## 5. MOBILE APP ARCHITECTURE (Native Java + Swift)

### 5.1 Mobile API Flow

#### AI Chat — SSE Streaming
```
Android (OkHttp EventSource)          iOS (URLSession bytes stream)
        │                                         │
        │── POST /api/chat ───────────────────────┤
        │   { "message": "...", "birth_data": {} }│
        │                                         │
        │◀── data: {"type":"chunk","content":"..."} ─▶│
        │◀── data: {"type":"chunk","content":"..."} ─▶│
        │◀── data: {"type":"done","sources":[...]} ─▶│
        │                                         │
        │  [Android] OkHttpClient.newEventSource()    │
        │  → EventSourceListener.onEvent()            │
        │  → LiveData.postValue() → UI update         │
        │                                             │
        │  [iOS] URLSession.bytes(for:request)        │
        │  → AsyncThrowingStream → @Observable update │
```

#### All Other Requests — REST
```
Android (Retrofit2)                   iOS (URLSession + async/await)

@GET("api/kundali")                   func fetchKundali() async throws
Call<KundaliResponse> getKundali(     → URLSession.data(for: request)
  @Query("date") String date, ...)    → JSONDecoder().decode(...)

→ Retrofit callback → ViewModel       → await → ViewModel @Published
→ LiveData.observe → Fragment UI      → @Observable → SwiftUI auto-update
```

### 5.2 Android — Java 17 MVVM Architecture

```
Activity/Fragment (View)
    │ observe LiveData
    ▼
ViewModel (state, logic)
    │ calls
    ▼
Repository (data layer)
    │ calls
    ▼
ApiService (Retrofit interface) / SseManager (OkHttp)
    │ HTTP/SSE
    ▼
FastAPI Backend (brahmasmi.bimoraai.com/api)
```

**Key dependencies (build.gradle):**
```groovy
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.squareup.okhttp3:okhttp-sse:4.12.0'      // AI Chat SSE
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'    // charts
implementation 'com.github.bumptech.glide:glide:4.16.0'      // images
implementation 'androidx.navigation:navigation-fragment:2.7.7'
implementation 'com.google.firebase:firebase-messaging:23.4.1' // FCM push
```

**Token storage (Android):**
```java
// PrefsHelper.java
EncryptedSharedPreferences.create(
    "brahm_secure_prefs",
    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
);
```

### 5.3 iOS — Swift 5.9 SwiftUI Architecture

```
SwiftUI View (body)
    │ @Bindable / @Observable
    ▼
@Observable ViewModel
    │ async/await calls
    ▼
APIClient (URLSession)
    │ HTTP / URLSession.bytes (SSE)
    ▼
FastAPI Backend (brahmasmi.bimoraai.com/api)
```

**Key stack (Swift Package Manager):**
```swift
// No external heavy deps — use Swift stdlib
// URLSession for HTTP + SSE (built-in)
// Swift Charts for charts (built-in, iOS 16+)
// SwiftUI NavigationStack (iOS 16+)
// Keychain via Security framework (built-in)
// Firebase (optional — FCM + Analytics)
```

**Token storage (iOS):**
```swift
// KeychainHelper.swift
SecItemAdd([
    kSecClass: kSecClassGenericPassword,
    kSecAttrAccount: "brahm_access_token",
    kSecValueData: tokenData
] as CFDictionary, nil)
```

### 5.4 All App Screens

#### Bottom Navigation (5 main tabs)
| Tab | Android | iOS | Backend |
|-----|---------|-----|---------|
| Home | HomeFragment | HomeView | Dashboard data |
| Kundali | KundaliFragment (7 tabs) | KundaliView | POST /api/kundali |
| Chat | ChatFragment (SSE) | ChatView (SSE) | POST /api/chat |
| Today | TodayFragment | TodayView | GET /api/panchang |
| Profile | ProfileFragment | ProfileView | GET /api/user/me |

#### Secondary Screens
| Screen | Android Activity | iOS View | Backend Endpoint |
|--------|-----------------|---------|-----------------|
| Gochar | GocharActivity | GocharView | GET /api/gochar + POST /api/gochar/analyze |
| Compatibility | CompatibilityActivity | CompatibilityView | POST /api/compatibility |
| Muhurta | MuhurtaActivity | MuhurtaView | POST /api/muhurta/activity |
| Horoscope | HoroscopeActivity | HoroscopeView | GET /api/horoscope/{rashi} |
| Sade Sati | SadeSatiActivity | SadeSatiView | GET /api/sade-sati |
| Dosha | DoshaActivity | DoshaView | GET /api/dosha |
| Gemstone | GemstoneActivity | GemstoneView | GET /api/gemstones |
| KP System | KPActivity | KPView | POST /api/kp |
| Prashna | PrashnaActivity | PrashnaView | POST /api/prashna |
| Varshphal | VarshpalActivity | VarshpalView | POST /api/varshphal |
| Rectification | RectificationActivity | RectificationView | POST /api/rectification |
| Palmistry | PalmistryActivity | PalmistryView | POST /api/palmistry (camera + Gemini Vision) |
| Remedies | RemediesActivity | RemediesView | Kundali store (local) |
| Timeline | TimelineActivity | TimelineView | Kundali store (local) |
| Yogas | YogasActivity | YogasView | Kundali store (local) |
| Grahan | GrahanActivity | GrahanView | GET /api/grahan |
| Rashi Explorer | RashiActivity | RashiView | Static |
| Nakshatra Explorer | NakshatraActivity | NakshatraView | Static |
| Vedic Library | LibraryActivity | LibraryView | GET /api/search |
| Mantra Dictionary | MantraActivity | MantraView | GET /api/search |
| Subscription | SubscriptionActivity | SubscriptionView | GET /api/subscription/plans |

### 5.5 Push Notifications

#### Android — Firebase Cloud Messaging (FCM)
```
Backend (scheduler) ──▶ FCM API ──▶ Android Device
  • Daily Rahu Kaal alert (time-based)
  • Daily horoscope (morning)
  • Custom planetary alerts
```

#### iOS — APNs via Firebase
```
Backend (scheduler) ──▶ Firebase ──▶ APNs ──▶ iOS Device
  Same notification types as Android
  Uses same Firebase project (add iOS app)
```

### 5.6 Design System (Mobile)
```
Theme:          Dark (match website)
Primary:        #7C3AED  (purple)
Background:     #09090B  (zinc-950)
Surface:        #18181B  (zinc-900)
Text:           #FAFAFA  (zinc-50)
Accent:         #F59E0B  (amber — star-gold)
Font:           Poppins (downloaded as .ttf asset)
Corner Radius:  12dp (Android) / 12pt (iOS)
Min SDK:        Android 8.0 (API 26) — 95%+ devices
Min iOS:        iOS 16 (SwiftUI NavigationStack + Swift Charts)
```

### 5.7 Phase Plan

| Phase | Goal | Platform | Duration |
|-------|------|----------|----------|
| 1 | Foundation + Auth + Core Screens | Android | Week 1 |
| 2 | AI Chat + Full Kundali | Android | Week 2 |
| 3 | All Secondary Screens | Android | Week 3 |
| 4 | Polish + Play Store | Android | Week 4 |
| 5 | iOS Port (SwiftUI) | iOS | Week 5-6 |

**Package name:** `com.bimoraai.brahm`
**Bundle ID (iOS):** `com.bimoraai.brahm`

---

## 6. SUBSCRIPTION SYSTEM (CASHFREE)

### 6.1 Plan Definitions
```json
[
  {
    "id": "free",
    "name": "Free",
    "name_hi": "निःशुल्क",
    "price_monthly": 0,
    "price_yearly": 0,
    "features": [
      "Daily Horoscope",
      "Today's Panchang",
      "Festival Calendar",
      "Eclipse Calendar",
      "Basic Kundali (view only)",
      "5 AI Chat messages/day"
    ],
    "limits": { "ai_chat_daily": 5, "kundali_saves": 1 }
  },
  {
    "id": "jyotishi",
    "name": "Jyotishi",
    "name_hi": "ज्योतिषी",
    "price_monthly": 199,
    "price_yearly": 1499,
    "currency": "INR",
    "features": [
      "Everything in Free",
      "Unlimited AI Chat",
      "Full Kundali + Dasha Timeline",
      "Compatibility Analysis",
      "Muhurta Finder",
      "Vedic Library Search",
      "Save unlimited charts"
    ],
    "limits": { "ai_chat_daily": -1, "kundali_saves": -1 }
  },
  {
    "id": "acharya",
    "name": "Acharya",
    "name_hi": "आचार्य",
    "price_monthly": 499,
    "price_yearly": 3999,
    "currency": "INR",
    "features": [
      "Everything in Jyotishi",
      "Sanskrit text search (1.1M chunks)",
      "Varshaphala (Solar Return)",
      "Prashna (Horary) — coming soon",
      "Priority GPU inference",
      "Export PDF reports"
    ],
    "limits": { "ai_chat_daily": -1, "kundali_saves": -1, "priority_queue": true }
  }
]
```

### 5.2 Cashfree Payment Flow
```
Frontend                   FastAPI                    Cashfree
    │                         │                           │
    │── POST /subscription    │                           │
    │     /checkout           │                           │
    │   { plan: "jyotishi",   │                           │
    │     period: "monthly" } │                           │
    │                         │── Create Order ──────────▶│
    │                         │   (amount, customer info) │
    │                         │◀─ { order_id,             │
    │                         │     payment_session_id }  │
    │◀─ { payment_session_id, │                           │
    │     order_id }          │                           │
    │                         │                           │
    │── Open Cashfree         │                           │
    │   Checkout (JS SDK)     │                           │
    │   or redirect URL       │                           │
    │                         │                           │
    │   [User pays UPI/Card/  │                           │
    │    Netbanking/Wallet]    │                           │
    │                         │                           │
    │                         │◀─ Webhook POST            │
    │                         │   /subscription/webhook   │
    │                         │   { order_id, status:     │
    │                         │     "PAID", user_id }     │
    │                         │── Verify signature        │
    │                         │── Activate plan in DB     │
    │                         │── Update user JWT scope   │
    │                         │                           │
    │── Poll /subscription    │                           │
    │     /status (30s)       │                           │
    │◀─ { plan: "jyotishi",   │                           │
    │     expires_at: "..." } │                           │
    │── Update authStore      │                           │
    │── Unlock features       │                           │
```

### 5.3 Cashfree Integration Code Pattern
```python
# api/services/cashfree_service.py

import hmac, hashlib, requests

CASHFREE_APP_ID  = os.getenv("CASHFREE_APP_ID")
CASHFREE_SECRET  = os.getenv("CASHFREE_SECRET")
CASHFREE_ENV     = "PROD"   # or "TEST"
BASE_URL         = "https://api.cashfree.com/pg"  # TEST: sandbox.cashfree.com

def create_order(user_id: str, amount: int, plan: str, period: str) -> dict:
    order_id = f"brahm_{user_id}_{int(time.time())}"
    payload  = {
        "order_id":       order_id,
        "order_amount":   amount,
        "order_currency": "INR",
        "customer_details": { "customer_id": user_id, ... },
        "order_meta": {
            "return_url": f"https://brahmai.app/subscription/status?order_id={order_id}",
            "notify_url": "https://34.135.70.190:8000/api/subscription/webhook"
        }
    }
    resp = requests.post(f"{BASE_URL}/orders", json=payload,
                         headers={"x-client-id": CASHFREE_APP_ID,
                                  "x-client-secret": CASHFREE_SECRET,
                                  "x-api-version": "2023-08-01"})
    return resp.json()   # contains payment_session_id

def verify_webhook_signature(raw_body: bytes, received_sig: str) -> bool:
    computed = hmac.new(CASHFREE_SECRET.encode(), raw_body, hashlib.sha256).hexdigest()
    return hmac.compare_digest(computed, received_sig)
```

---

## 7. MULTILINGUAL SUPPORT

### 6.1 Strategy: Two Levels
```
Level 1: UI Language (react-i18next)
    → All labels, navigation, buttons, descriptions
    → Languages: English (default), Hindi (हिन्दी), Sanskrit (संस्कृत)
    → User picks in settings → stored in user profile + localStorage

Level 2: Content Language (already done — RAG)
    → AI Chat answers in user's preferred language
    → Search results in Sanskrit/Hindi/English mixed
    → Horoscope, panchang terms shown in both script + English
```

### 6.2 i18n Setup
```typescript
// src/i18n.ts
import i18n from "i18next"
import { initReactI18next } from "react-i18next"
import en from "./locales/en/translation.json"
import hi from "./locales/hi/translation.json"
import sa from "./locales/sa/translation.json"

i18n.use(initReactI18next).init({
  resources: { en: { translation: en }, hi: { translation: hi }, sa: { translation: sa } },
  lng: localStorage.getItem("brahm_lang") || "en",
  fallbackLng: "en",
  interpolation: { escapeValue: false }
})
```

### 6.3 Translation File Structure
```json
// locales/hi/translation.json  (sample)
{
  "nav": {
    "dashboard":   "डैशबोर्ड",
    "kundli":      "मेरी कुंडली",
    "chat":        "ब्रह्म AI चैट",
    "panchang":    "पंचांग",
    "horoscope":   "राशिफल",
    "compatibility":"कुंडली मिलान",
    "library":     "वैदिक पुस्तकालय",
    "profile":     "प्रोफाइल"
  },
  "panchang": {
    "tithi":       "तिथि",
    "nakshatra":   "नक्षत्र",
    "yoga":        "योग",
    "karana":      "करण",
    "vara":        "वार",
    "sunrise":     "सूर्योदय",
    "sunset":      "सूर्यास्त",
    "rahukaal":    "राहुकाल"
  },
  "subscription": {
    "upgrade":     "अपग्रेड करें",
    "your_plan":   "आपका प्लान",
    "free":        "निःशुल्क",
    "premium":     "प्रीमियम"
  }
}
```

### 6.4 AI Chat Language Routing
```python
# api/routers/chat.py — send language preference with every request
# Frontend sends: { "message": "...", "language": "hi" | "en" | "sa" | "all" }
# RAG prompt prefix changes:
LANG_PROMPTS = {
    "hi": "कृपया हिंदी में उत्तर दें।",
    "sa": "संस्कृतभाषायाम् उत्तरं देहि।",
    "en": "Please answer in English.",
    "all": ""   # Qwen uses source language naturally
}
```

---

## 8. ~~CAPACITOR~~ — REPLACED BY NATIVE APPS

> **Decision (2026-03-21):** Capacitor approach discarded. We are building fully native apps:
> - **Android:** Java 17 + MVVM + Retrofit2 (see Section 5)
> - **iOS:** Swift 5.9 + SwiftUI + URLSession (see Section 5)
>
> Reasons: Better performance (60/120fps), full native API access, camera for Palmistry,
> no framework dependency risk, best App Store / Play Store acceptance.
> All details are in **Section 5: MOBILE APP ARCHITECTURE**.

---

## 8. FIRST SCREEN & USER JOURNEY

### 8.1 First Screen Decision Tree
```
App/Web opens
      │
      ▼
Check JWT in secure storage
      │
      ├── Token valid + plan loaded
      │         │
      │         ▼
      │   DASHBOARD (personalized)
      │   "Namaste, Ramesh 🙏"
      │   Today's cosmic snapshot:
      │     • Tithi: Ekadashi (ends 14:32)
      │     • Nakshatra: Rohini (your birth nakshatra!)
      │     • Current Dasha: Shani (ends 2031)
      │     • Sunrise: 6:18 AM | Rahukaal: 3-4:30 PM
      │
      ├── Token expired / no token
      │         │
      │         ▼
      │   LANDING PAGE (unauthenticated)
      │   (see 8.2 below)
      │
      └── Token valid but onboarding incomplete
                │
                ▼
          ONBOARDING PAGE
          (collect birth details → generate kundali)
```

### 8.2 Landing Page (First Screen — Unauthenticated)
```
┌─────────────────────────────────────────────────────────────┐
│  🌌  BRAHM AI                                    [Login]    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   [ Animated starfield / Om symbol / cosmic mandala ]       │
│                                                              │
│   ब्रह्म AI                                                   │
│   Your Personal Vedic Guide                                  │
│                                                              │
│   [ TODAY'S COSMIC SNAPSHOT — public ]                       │
│   ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐           │
│   │Ekadashi│  │Rohini  │  │Sukla   │  │Brahma  │           │
│   │ Tithi  │  │Nakshtra│  │Paksha  │  │Muhurta │           │
│   │ 11th   │  │        │  │        │  │5:42 AM │           │
│   └────────┘  └────────┘  └────────┘  └────────┘           │
│                                                              │
│   [ Feature highlights ]                                     │
│   • AI Chat with 1.1M Sanskrit/Hindi texts                  │
│   • Complete Kundali & Dasha analysis                        │
│   • Real-time Panchang for your city                        │
│   • 36-Guna Kundali matching                                 │
│                                                              │
│   ┌──────────────────────────────────────────┐              │
│   │   🚀  Get Started Free — Login with OTP  │              │
│   └──────────────────────────────────────────┘              │
│                                                              │
│   [ Testimonials / Trust badges ]                           │
│   "10,000+ charts generated" | "Sanskrit RAG: 792K chunks" │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 8.3 Login Page
```
┌─────────────────────────────────────────────────────────────┐
│  ← Back                    ब्रह्म AI                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   Sign in / Register                                         │
│   (New users auto-registered on first OTP verify)           │
│                                                              │
│   ┌──────────────────────────────┐                          │
│   │  +91  │  9876543210          │ ← phone input            │
│   └──────────────────────────────┘                          │
│                                                              │
│   [ Send OTP ]                                               │
│                                                              │
│   ─────────────── or ───────────────                         │
│                                                              │
│   [ G  Continue with Google ]   ← OAuth (optional)          │
│                                                              │
│   By continuing you agree to our Terms & Privacy Policy     │
│                                                              │
│   ─── After OTP sent ───                                    │
│   Enter 6-digit OTP sent to +91 98765 43210                 │
│   ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐                           │
│   │  │ │  │ │  │ │  │ │  │ │  │ ← OTP boxes                │
│   └──┘ └──┘ └──┘ └──┘ └──┘ └──┘                           │
│   [ Verify & Continue ]   [ Resend in 30s ]                 │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 8.4 Post-Login Flow
```
First login → Onboarding (name + birth details) → Kundali generated → Dashboard
Return login → Dashboard (personalized, kundali pre-loaded from DB)
```

---

## 9. USER DATA MODEL & RETRIEVAL

### 9.1 Database Schema (SQLite dev / PostgreSQL prod)
```sql
-- Users table
CREATE TABLE users (
  id            TEXT PRIMARY KEY,        -- "usr_a1b2c3d4" (uuid prefix)
  phone         TEXT UNIQUE NOT NULL,
  email         TEXT UNIQUE,
  name          TEXT,
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  last_login    DATETIME,
  role          TEXT DEFAULT 'user',     -- 'user' | 'admin'
  lang_pref     TEXT DEFAULT 'en',       -- 'en' | 'hi' | 'sa'
  city          TEXT,
  lat           REAL,
  lon           REAL,
  tz            REAL,
  birth_date    TEXT,
  birth_time    TEXT,
  birth_city    TEXT,
  birth_lat     REAL,
  birth_lon     REAL,
  birth_tz      REAL,
  kundali_json  TEXT,                    -- cached KundaliResponse JSON
  notif_panchang  BOOLEAN DEFAULT TRUE,
  notif_grahan    BOOLEAN DEFAULT TRUE,
  notif_festivals BOOLEAN DEFAULT FALSE,
  fcm_token       TEXT                   -- for push notifications
);

-- Subscriptions table
CREATE TABLE subscriptions (
  id              TEXT PRIMARY KEY,
  user_id         TEXT REFERENCES users(id),
  plan            TEXT NOT NULL,         -- 'free' | 'jyotishi' | 'acharya'
  period          TEXT,                  -- 'monthly' | 'yearly'
  status          TEXT DEFAULT 'active', -- 'active' | 'cancelled' | 'expired'
  cashfree_order_id TEXT,
  started_at      DATETIME,
  expires_at      DATETIME,
  cancelled_at    DATETIME
);

-- Usage tracking (for free tier limits)
CREATE TABLE usage_log (
  user_id     TEXT,
  feature     TEXT,                      -- 'ai_chat' | 'kundali' | 'search'
  used_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  metadata    TEXT                       -- JSON blob
);
```

### 9.2 Get Particular User's Details (Admin)

**Via API (admin token):**
```bash
# Get specific user by ID
curl -H "Authorization: Bearer ADMIN_JWT" \
  http://34.135.70.190:8000/api/admin/users/usr_a1b2c3d4

# Search users by phone
curl -H "Authorization: Bearer ADMIN_JWT" \
  "http://34.135.70.190:8000/api/admin/users?phone=+919876543210"

# Get user's subscription history
curl -H "Authorization: Bearer ADMIN_JWT" \
  http://34.135.70.190:8000/api/admin/users/usr_a1b2c3d4/subscriptions

# Get user's usage stats
curl -H "Authorization: Bearer ADMIN_JWT" \
  http://34.135.70.190:8000/api/admin/users/usr_a1b2c3d4/usage
```

**Via SQLite (direct VM access):**
```bash
# On VM: ~/books/api/data/users.db
sqlite3 ~/books/api/data/users.db

# Get user by phone
SELECT id, name, phone, plan, created_at FROM users WHERE phone = '+919876543210';

# Get all premium users
SELECT u.name, u.phone, s.plan, s.expires_at
FROM users u JOIN subscriptions s ON u.id = s.user_id
WHERE s.status = 'active' AND s.plan != 'free';

# Get today's new signups
SELECT name, phone, created_at FROM users
WHERE date(created_at) = date('now');

# Get MAU
SELECT COUNT(DISTINCT user_id) as mau
FROM usage_log
WHERE used_at >= date('now', '-30 days');
```

### 9.3 /api/user/me Response (Full Profile)
```json
{
  "id":           "usr_a1b2c3d4",
  "name":         "Ramesh Sharma",
  "phone":        "+919876543210",
  "lang_pref":    "hi",
  "location": {
    "city": "New Delhi",
    "lat":  28.6139,
    "lon":  77.209,
    "tz":   5.5
  },
  "birth_details": {
    "date":  "1990-05-15",
    "time":  "10:30",
    "city":  "Varanasi",
    "lat":   25.317,
    "lon":   82.973,
    "tz":    5.5
  },
  "kundali_cached": { ...KundaliResponse... },
  "subscription": {
    "plan":       "jyotishi",
    "status":     "active",
    "started_at": "2026-03-01",
    "expires_at": "2026-04-01",
    "period":     "monthly"
  },
  "usage_today": {
    "ai_chat": 12,
    "limit":   -1
  },
  "notifications": {
    "panchang":  true,
    "grahan":    true,
    "festivals": false
  },
  "created_at":   "2026-01-15T10:30:00",
  "last_login":   "2026-03-18T08:22:00"
}
```

---

## 10. ROUTES & SIDEBAR NAVIGATION (UPDATED)

| URL | Sidebar Label | Component | Auth Required | Notes |
|-----|--------------|-----------|---------------|-------|
| `/` | — | Index.tsx → LandingPage | ✗ | Redirects to /dashboard if logged in |
| `/login` | — | LoginPage | ✗ | Phone OTP form |
| `/onboarding` | — | OnboardingPage | ✓ | Birth details setup after first login |
| `/dashboard` | Dashboard | Dashboard | ✓ | Personalized daily snapshot |
| `/chat` | Brahm AI Chat | AIChatPage | ✓ Jyotishi+ | SSE streaming AI |
| `/kundli` | My Kundli | KundliPage | ✓ | 7 tabs: Chart/Planets/Dashas/Yogas/Alerts/Shadbala/Navamsha |
| `/sky` | Live Sky | SkyPage | ✓ | Current planet positions |
| `/timeline` | Dasha Timeline | TimelinePage | ✓ | Dasha timeline chart |
| `/yogas` | Yogas | YogasPage | ✓ | Yoga analysis cards |
| `/remedies` | Remedies | RemediesPage | ✓ | Yoga remedies (mantra/gem/deity) |
| `/horoscope` | Daily Horoscope | HoroscopePage | ✗ | Public |
| `/rashi` | Rashi Explorer | RashiExplorer | ✗ | Public — static |
| `/nakshatra` | Nakshatra Explorer | NakshatraExplorer | ✗ | Public — static |
| `/compatibility` | Compatibility | CompatibilityPage | ✓ | Kundali milan + nakshatra tab |
| `/palmistry` | Palmistry | PalmistryPage | ✓ | Camera + Gemini Vision (POST /api/palmistry) |
| `/gochar` | Gochar | GocharPage | ✓ | Planetary transits + AV scoring |
| `/today` | Today | PanchangPage | ✓ | Tab 1: Panchang · Tab 2: Muhurta |
| `/panchang` | Panchang | GrahanPage | ✗ | Public — Calendar / Festivals / Eclipses |
| `/grahan` | — | → redirect /panchang | ✗ | Redirect alias |
| `/calendar` | — | → redirect /panchang | ✗ | Redirect alias |
| `/muhurta` | — | → redirect /today | ✓ | Redirect alias |
| `/sade-sati` | Sade Sati | SadeSatiPage | ✓ | Sade Sati calculator |
| `/dosha` | Dosha | DoshaPage | ✓ | Manglik + dosha analysis |
| `/gemstones` | Gemstones | GemstoneRecommendationsPage | ✓ | Gemstone recommendations |
| `/kp` | KP System | KPPage | ✓ | KP sub-lords table |
| `/prashna` | Prashna | PrashnaPage | ✓ | Prashna (horary) kundali |
| `/varshphal` | Varshphal | VarshpalPage | ✓ | Varshphal solar return |
| `/rectification` | Rectification | RectificationPage | ✓ | Birth time rectification |
| `/library` | Vedic Library | VedicLibraryPage | ✓ Acharya | Sanskrit text search |
| `/mantras` | Mantra Dictionary | MantraDictionaryPage | ✓ | Mantra search |
| `/knowledge` | Knowledge Base | KnowledgeBasePage | ✓ | Knowledge base search |
| `/stories` | Stories | StoriesPage | ✗ | Public — static |
| `/gotra` | Gotra Finder | GotraFinderPage | ✓ | |
| `/profile` | Profile | ProfilePage | ✓ | Account + birth details + plan |
| `/subscription` | — | SubscriptionPage | ✓ | Plan selection + Cashfree checkout |
| `*` | — | NotFound | ✗ | 404 |

---

## 11. PAGE → API → HOOK MAPPING

| Page | API Endpoint | Hook / Direct call | Auth |
|------|-------------|-------------------|------|
| **LandingPage** | — | — | ✗ |
| **LoginPage** | POST /api/auth/send-otp + /verify-otp | useAuth | ✗ |
| **OnboardingPage** | POST /api/kundali | useKundali | ✓ |
| **Dashboard** | Zustand store + GET /api/panchang | usePanchang | ✓ |
| **AIChatPage** | POST /api/chat (SSE stream) | useChat | ✓ |
| **KundliPage** | Zustand store (kundaliData) | — | ✓ |
| **TimelinePage** | Zustand store (kundaliData.dashas) | — | ✓ |
| **YogasPage** | Zustand store (kundaliData.yogas) | — | ✓ |
| **RemediesPage** | Zustand store (kundaliData) | — | ✓ |
| **PanchangPage** | GET /api/panchang + POST /api/muhurta/activity | usePanchang | ✓ |
| **GrahanPage** | GET /api/grahan + /api/festivals + /api/calendar/month | useGrahan | ✗ |
| **HoroscopePage** | GET /api/horoscope/{rashi} | useHoroscope | ✗ |
| **GocharPage** | GET /api/gochar + POST /api/gochar/analyze | api.get / api.post | ✓ |
| **CompatibilityPage** | POST /api/compatibility | useCompatibility | ✓ |
| **SadeSatiPage** | GET /api/sade-sati | api.get | ✓ |
| **DoshaPage** | GET /api/dosha | api.get | ✓ |
| **GemstoneRecommendationsPage** | GET /api/gemstones | api.get | ✓ |
| **KPPage** | POST /api/kp | api.post | ✓ |
| **PrashnaPage** | POST /api/prashna | api.post | ✓ |
| **VarshpalPage** | POST /api/varshphal | api.post | ✓ |
| **RectificationPage** | POST /api/rectification | api.post | ✓ |
| **PalmistryPage** | POST /api/palmistry (multipart image) | api.post | ✓ |
| **SkyPage** | GET /api/planets/now | usePlanets | ✓ |
| **VedicLibraryPage** | GET /api/search | useSearch | ✓ Acharya |
| **MantraDictionaryPage** | GET /api/search | useSearch | ✓ |
| **KnowledgeBasePage** | GET /api/search | useSearch | ✓ |
| **ProfilePage** | GET+PATCH /api/user/me | useUser | ✓ |
| **SubscriptionPage** | GET /api/subscription/plans + POST /checkout | useSubscription | ✓ |
| **RashiExplorer** | — | — | ✗ (static) |
| **NakshatraExplorer** | — | — | ✗ (static) |
| **StoriesPage** | — | — | ✗ (static) |
| **GotraFinderPage** | — | — | ✓ (static) |

---

## 12. DATA FLOW DIAGRAMS

### 12.1 Auth + First Load Flow
```
App starts
      │
      ▼
secureStore.get("brahm_token")
      │
      ├── token exists
      │         │
      │         ▼
      │   verify JWT expiry (client-side decode)
      │         │
      │         ├── valid → setAuth(token, payload) → navigate("/dashboard")
      │         │
      │         └── expired → POST /api/auth/refresh
      │                           → new token → setAuth → navigate("/dashboard")
      │
      └── no token → navigate("/") → LandingPage
```

### 12.2 Subscription + Feature Gate Flow
```
User clicks "Unlimited AI Chat" (Jyotishi feature)
      │
      ▼
PlanGate checks authStore.plan
      │
      ├── plan is "jyotishi" or "acharya"
      │         → render feature normally
      │
      └── plan is "free"
                │
                ▼
          Show upgrade modal:
          "Unlock unlimited chat with Jyotishi plan"
          [ ₹199/month ]  [ ₹1499/year ]
          [ Upgrade Now ]
                │
                ▼
          navigate("/subscription")
                │
                ▼
          User selects plan → POST /subscription/checkout
                │
                ▼
          { payment_session_id } → Cashfree.checkout(session_id)
                │
                ▼
          [Cashfree payment UI — UPI / Card]
                │
                ▼
          Webhook → activate plan → user polling → authStore.plan = "jyotishi"
```

### 12.3 Chat Flow (SSE Streaming)
```
[same as before, but now includes:]
├── Auth header: "Authorization: Bearer {token}"
├── Usage check: require_plan("jyotishi") OR (plan=="free" AND daily_count < 5)
└── Language routing: prompt prefix based on user.lang_pref
```

### 12.4 Kundali Persistence Flow
```
OnboardingPage → POST /api/kundali
      │
      ▼
FastAPI calculates kundali
      │
      ├── Returns KundaliResponse
      │
      ├── Saves kundali_json to users table (DB cache)
      │
      ▼
Frontend: setKundaliData(response) → Zustand store

Return visit:
GET /api/user/me → includes kundali_cached
      │
      ▼
setKundaliData(user.kundali_cached) → instantly populated
(no re-calculation needed)
```

---

## 13. BACKEND DEPENDENCY TREE (UPDATED)

```
main.py
  └── imports: config, dependencies, 13 routers

config.py             (constants: JWT_SECRET, CASHFREE_*, MSG91_*)

dependencies.py
  └── get_current_user()      → JWT verify → user dict
  └── require_plan()          → plan-level gate
  └── get_admin_user()        → role=="admin" check
  └── get_rag_state()         → GPU models

routers/auth.py
  └── services/auth_service   → OTP send/verify, JWT create
  └── DB: users table

routers/subscription.py
  └── services/cashfree_service → create order, verify webhook
  └── DB: subscriptions table

routers/user.py
  └── DB: users table (full profile CRUD)

routers/admin.py (NEW)
  └── DB: users + subscriptions + usage_log

routers/chat.py
  └── services/rag_service, dependencies (require_plan)
  └── Usage: usage_log INSERT

[all other routers unchanged — add Depends(get_current_user) to each]
```

---

## 14. FRONTEND DEPENDENCY TREE (UPDATED)

```
src/main.tsx
  └── i18n.ts                        ← NEW: loads before App
  └── App.tsx
        └── QueryClientProvider
              └── AuthProvider (authStore init)
                    └── router
                          ├── / → LandingPage (public)
                          ├── /login → LoginPage (public)
                          ├── /horoscope → HoroscopePage (public)
                          ├── /panchang → GrahanPage (public)
                          ├── [ProtectedRoute]
                          │     ├── /dashboard → Dashboard
                          │     ├── /kundli → KundliPage
                          │     └── ...all auth-required routes
                          └── [PlanGate wrappers inside pages]

src/store/authStore.ts
  └── zustand + persist
  └── uses: src/lib/storage.ts (web: localStorage | app: Capacitor Preferences)

src/hooks/useAuth.ts
  └── sendOtp(): POST /api/auth/send-otp
  └── verifyOtp(): POST /api/auth/verify-otp → setAuth()
  └── logout(): clear store + secure storage

src/hooks/useSubscription.ts
  └── plans(): GET /api/subscription/plans
  └── status(): GET /api/subscription/status
  └── checkout(): POST /api/subscription/checkout → open Cashfree

src/lib/api.ts (UPDATED)
  └── all requests: adds Authorization: Bearer {token} from authStore
  └── on 401: attempt token refresh → retry → redirect /login

src/components/ProtectedRoute.tsx
  └── checks authStore.isLoggedIn → redirect /login if false

src/components/PlanGate.tsx
  └── checks authStore.plan against required plan → show upgrade CTA
```

---

## 15. NO-DUPLICATE RULES

| Data | Single Source of Truth | What Was Eliminated |
|------|----------------------|---------------------|
| City list + lat/lon/tz | `api/data/cities.json` via GET /api/cities | No per-file hardcoded cities |
| API base URL | `.env.local` VITE_API_URL | No per-file hardcoded URLs |
| Kundali calc | `api/services/kundali_service.py` | Extracted from scripts once |
| Panchang calc | `scripts/panchang.py` Panchang class | panchang_service.py imports it |
| TypeScript types | `src/types/api.ts` | No local re-definitions per page |
| Auth token | `authStore.ts` + `secureStore` abstraction | Not in two places |
| Plan definitions | `api/data/subscription_plans.json` | Not hardcoded in frontend OR backend |
| i18n strings | `src/locales/*/translation.json` | No hardcoded UI strings in components |
| JWT secret | `.env` on VM only | Never in frontend, never in git |

---

## 16. REACT QUERY CACHE STRATEGY

| Endpoint | staleTime | Auth |
|----------|-----------|------|
| /api/kundali | Infinity | User's birth chart never changes |
| /api/panchang | 5 min | Tithi changes every ~1.5 days |
| /api/horoscope | 1 hour | Daily content |
| /api/planets/now | 2 min, refetch 5 min | Planets move |
| /api/search | 30 sec | User-driven |
| /api/grahan | 1 day | Annual events |
| /api/muhurta | 10 min | Date-specific |
| /api/cities | Infinity | Never changes |
| /api/user/me | 1 min | Profile may update |
| /api/subscription/status | 30 sec | Payment can activate anytime |
| /api/subscription/plans | 1 hour | Plan prices change rarely |

---

## 17. DEPLOYMENT ARCHITECTURE (UPDATED v6.0)

```
VM: 34.134.231.111  (updated IP)
│
├── Port 8000 — FastAPI (systemd: brahm-api)
│   cmd: uvicorn api.main:app --host 0.0.0.0 --port 8000 --workers 1
│   NEVER use --reload (breaks SSE)
│
├── /var/www/brahm-ai/         ← User-facing React app (pnpm run build → dist/)
│
└── /var/www/brahm-admin/      ← Admin React app (admin-app/dist/)

nginx: /etc/nginx/sites-available/brahm-ai  (user domain)
  server_name brahmasmi.bimoraai.com;
  location /  → root /var/www/brahm-ai; try_files ...
  location /admin { return 404; }        ← BLOCKED on user domain
  location /docs  { return 404; }
  location /api   → proxy_pass http://127.0.0.1:8000;
  add_header Cross-Origin-Opener-Policy "same-origin-allow-popups";

nginx: /etc/nginx/sites-available/brahm-api  (admin/api domain)
  server_name api.brahmasmi.bimoraai.com;
  location /api  → proxy_pass http://127.0.0.1:8000; (SSE: proxy_buffering off)
  location /admin → alias /var/www/brahm-admin; try_files $uri $uri/ /admin/index.html;
  location ~ ^/(docs|redoc|openapi.json|health) → proxy_pass http://127.0.0.1:8000;
  location /  → return 404;  (no full site on api domain)

Deploy commands:
  # Backend
  cd ~/books && git pull origin main && sudo systemctl restart brahm-api

  # User frontend
  cd ~/books && git pull origin main && pnpm run build && sudo cp -r dist/* /var/www/brahm-ai/

  # Admin app
  cd ~/books/admin-app && pnpm install && pnpm run build
  sudo mkdir -p /var/www/brahm-admin && sudo cp -r dist/* /var/www/brahm-admin/
  sudo nginx -t && sudo systemctl reload nginx

GCloud Firewall:
  brahm-api: tcp:8000

Environment vars (systemd service file — NOT /etc/environment):
  /etc/systemd/system/brahm-api.service → Environment= lines
  After editing: sudo systemctl daemon-reload && sudo systemctl restart brahm-api

  KEY vars: JWT_SECRET, CASHFREE_APP_ID, CASHFREE_SECRET, MSG91_AUTH_KEY,
            ADMIN_KEY (for X-Admin-Key header auth), GOOGLE_CLIENT_ID

App Store:
  Bundle ID: ai.brahm.app
  Deployment: Java/Swift → Android Studio/Xcode → Play Console / App Store Connect
```

**VRAM Budget (NVIDIA L4 24GB):**
```
Qwen2.5-7B 4-bit nf4:          6.3 GB
Embedding MiniLM-L12-v2:       0.5 GB
Cross-Encoder reranker:        0.1 GB
─────────────────────────────────────
Total used:                    6.9 GB
Available buffer:             17.1 GB
```

---

## 18. SECURITY (Phase 2 — Auth Enabled)

- **JWT RS256 or HS256** with 64-char secret in VM env (never in code/git)
- **OTP rate limit**: 3 OTP requests per phone per 10 minutes
- **Token refresh**: 7-day access token, 30-day refresh (revocable in DB)
- **Cashfree webhook**: verify HMAC-SHA256 signature before activating plan
- **CORS**: allow `brahmai.app`, `localhost:8080`, Capacitor origins only
- **Admin routes**: separate `role='admin'` check (not just plan level)
- **Free tier abuse**: rate limit per user_id, not per IP
- **No secrets on client**: Cashfree keys only on server; JWT secret only on VM

---

## 19. ADDING NEW FEATURES

### New Plan-Gated Feature
```
1. api/data/subscription_plans.json → add feature to plan definition
2. api/routers/your_router.py       → add Depends(require_plan("jyotishi"))
3. src/components/PlanGate.tsx      → wrap frontend component with <PlanGate plan="jyotishi">
```

### New Jyotish calculation (e.g., Varshaphala)
```
1. api/services/varshaphala_service.py
2. api/models/varshaphala.py
3. api/routers/varshaphala.py  → with Depends(require_plan("acharya"))
4. api/main.py                 → include_router (1 line)
5. src/types/api.ts            → interface VarshaphalaResponse
6. src/hooks/useVarshaphala.ts → useMutation hook
7. Wire into page
```

### New language UI strings
```
1. src/locales/en/translation.json → add English key
2. src/locales/hi/translation.json → add Hindi translation
3. Use in component: const { t } = useTranslation(); t("key.subkey")
```

### New push notification type
```
1. api/routers/notifications.py → cron logic + FCM send
2. users table: add notif_* column
3. ProfilePage: add toggle
```

---

## 20. TECH STACK SUMMARY (UPDATED)

### Web Frontend
| Layer | Technology | Notes |
|-------|-----------|-------|
| Framework | React 18.3.1 | |
| Language | TypeScript 5.x | |
| Build | Vite + SWC | |
| UI | shadcn/ui + Radix UI | |
| Styling | TailwindCSS 3.4.17 | |
| Animations | Framer Motion 10.18.0 | |
| State | Zustand 4.5.7 | |
| Server State | React Query 5.83.0 | |
| Routing | React Router 6.30.1 | |
| i18n | react-i18next | EN/HI/SA |
| Auth | Clerk React SDK | Phone OTP, JWT |
| Storage | localStorage + Zustand | birth details, state |
| Payments | Cashfree JS SDK | In-app checkout |
| Error Tracking | Sentry React SDK | Crashes + replays |
| Testing | Vitest + Playwright | Unit + E2E |

### Android App
| Layer | Technology | Notes |
|-------|-----------|-------|
| Language | Java 17 LTS | Records, sealed classes, text blocks |
| IDE | Android Studio Hedgehog+ | |
| UI | XML Layouts + Material Design 3 | |
| HTTP | Retrofit2 + OkHttp3 | All REST calls |
| SSE (AI Chat) | OkHttp EventSource | Streaming responses |
| Charts | MPAndroidChart + custom Canvas | Kundali wheel |
| Images | Glide | |
| Storage | EncryptedSharedPreferences + Room DB | Secure token |
| Navigation | Jetpack Navigation Component | |
| Async | ExecutorService + LiveData | MVVM pattern |
| Push | Firebase Cloud Messaging (FCM) | Daily alerts |
| Min SDK | API 26 (Android 8.0) | 95%+ device coverage |

### iOS App
| Layer | Technology | Notes |
|-------|-----------|-------|
| Language | Swift 5.9 | async/await, @Observable |
| IDE | Xcode 15+ (Mac required) | |
| UI | SwiftUI | NavigationStack |
| HTTP | URLSession + async/await | All REST calls |
| SSE (AI Chat) | URLSession bytes stream | Streaming responses |
| Charts | Swift Charts + custom Canvas | Kundali wheel |
| Storage | UserDefaults + Keychain + CoreData | Secure token |
| Push | APNs via Firebase | Daily alerts |
| Min iOS | iOS 16 | SwiftUI NavigationStack |

### Backend
| Layer | Technology | Notes |
|-------|-----------|-------|
| API | FastAPI | Pydantic v2 |
| Server | Uvicorn | workers=1 |
| Auth | Clerk (→ Auth0 at scale) | Phone OTP + JWT |
| DB | Supabase (PostgreSQL) | + RLS + Realtime |
| Payments | Cashfree Payments API | Webhooks |
| LLM | Gemini 2.5 Flash | google-genai SDK |
| Embedding | paraphrase-multilingual-MiniLM-L12-v2 | 384 dim |
| Reranker | cross-encoder/ms-marco-MiniLM-L-6-v2 | |
| Vector DB | FAISS HNSW | 1.1M chunks |
| Keyword | rank_bm25 | |
| Astronomy | pyswisseph | |
| Error Tracking | Sentry | Frontend + Backend |
| Metrics | Prometheus + Grafana | VM port 9090/3000 |
| Process | systemd (brahm-api) | |

---

## 21. VERIFICATION COMMANDS

```bash
# Health check
curl http://34.135.70.190:8000/health

# Auth test
curl -X POST http://34.135.70.190:8000/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"phone": "+919876543210"}'

# Plans (public)
curl http://34.135.70.190:8000/api/subscription/plans | python3 -m json.tool

# User profile (needs JWT)
export TOKEN="eyJ..."
curl -H "Authorization: Bearer $TOKEN" \
  http://34.135.70.190:8000/api/user/me | python3 -m json.tool

# Admin: get all users (needs admin JWT)
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://34.135.70.190:8000/api/admin/users?limit=10"

# Kundali (needs JWT)
curl -X POST http://34.135.70.190:8000/api/kundali \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"date":"2003-04-14","time":"20:30","lat":24.6917,"lon":83.0818,"tz":5.5}'

# FastAPI docs
open http://34.135.70.190:8000/docs
```

---

*Generated: 2026-03-21 | Brahm AI v5.0 — Web + Android (Java 17) + iOS (Swift 5.9) + AI Engine*

---

## 22. DATABASE — SUPABASE

### Decision
**Supabase** replaces raw SQLite/PostgreSQL plan.
- PostgreSQL under the hood (same schema, no changes needed)
- Built-in Auth (used as DB only — Clerk handles auth UI/OTP)
- Realtime subscriptions (can push plan activation to frontend instantly)
- Row Level Security (RLS) for data isolation
- Dashboard for admin queries (replaces manual sqlite3 CLI)
- Free tier: 500MB DB + 2GB bandwidth — enough for launch
- Upgrade path: Pro ($25/mo) → scale as needed

### Supabase Schema (same as Section 9.1, now on Supabase)
```sql
-- All tables stay identical to Section 9.1
-- users, subscriptions, usage_log

-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE usage_log ENABLE ROW LEVEL SECURITY;

-- Users can only read/update their own row
CREATE POLICY "user_self_access" ON users
  FOR ALL USING (id = current_setting('request.jwt.claims', true)::json->>'sub');

-- Subscriptions: user reads own, admin reads all
CREATE POLICY "user_own_subscription" ON subscriptions
  FOR SELECT USING (user_id = current_setting('request.jwt.claims', true)::json->>'sub');
```

### FastAPI Integration
```python
# api/services/db.py
from supabase import create_client, Client

SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY")  # service role — full access

supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# Usage examples:
# Insert user:
supabase.table("users").insert({"id": uid, "phone": phone, ...}).execute()

# Get user:
result = supabase.table("users").select("*").eq("phone", phone).single().execute()

# Update kundali cache:
supabase.table("users").update({"kundali_json": json.dumps(kundali)}).eq("id", uid).execute()

# Log usage:
supabase.table("usage_log").insert({"user_id": uid, "feature": "ai_chat"}).execute()
```

### Environment Variables (VM)
```
SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_SERVICE_ROLE_KEY=eyJ...  (server only — never expose to frontend)
SUPABASE_ANON_KEY=eyJ...          (safe for frontend if needed)
```

### Migration from SQLite → Supabase
```
1. pnpm add @supabase/supabase-js (frontend, if needed)
2. pip install supabase (backend)
3. Run schema SQL in Supabase SQL editor
4. Replace sqlite3 calls in api/services/ with supabase client calls
5. Move SUPABASE_URL + keys to VM /etc/environment
6. Test: POST /api/auth/send-otp → user row created in Supabase
```

---

## 23. AUTHENTICATION — CLERK (→ AUTH0 AT SCALE)

### Phase Strategy
| Phase | Tool | When | Why |
|-------|------|------|-----|
| **Launch → 10k MAU** | **Clerk** | Now | Best DX, phone OTP built-in, React SDK, generous free tier |
| **Scale → 50k+ MAU** | **Auth0** | When Clerk free tier exhausted | Enterprise SLA, multi-tenant, advanced rules |

### Why Clerk Over Custom JWT (Now)
- Phone OTP (India numbers) built-in — no MSG91 setup needed
- Pre-built `<SignIn>`, `<SignUp>`, `<UserButton>` React components
- JWT automatically attached to requests via `useAuth()` hook
- 10,000 MAU free → perfect for launch
- Webhooks: `user.created`, `user.updated` → sync to Supabase users table
- Takes 1 day to integrate vs 1 week for custom JWT

### Clerk Integration
```typescript
// src/main.tsx
import { ClerkProvider } from '@clerk/clerk-react'

const PUBLISHABLE_KEY = import.meta.env.VITE_CLERK_PUBLISHABLE_KEY

<ClerkProvider publishableKey={PUBLISHABLE_KEY}>
  <App />
</ClerkProvider>
```

```typescript
// src/hooks/useAuth.ts — replaces custom JWT hook
import { useAuth, useUser } from '@clerk/clerk-react'

export function useBrahmAuth() {
  const { getToken, isSignedIn } = useAuth()
  const { user } = useUser()

  // Get JWT for API calls
  const token = await getToken()  // auto-refreshed by Clerk

  return { token, isSignedIn, userId: user?.id, phone: user?.phoneNumbers[0]?.phoneNumber }
}
```

```typescript
// src/lib/api.ts — auto-inject Clerk JWT
import { useAuth } from '@clerk/clerk-react'

// In API client:
const token = await getToken()
headers: { Authorization: `Bearer ${token}` }
```

```python
# api/middleware/auth_middleware.py — verify Clerk JWT
import jwt
from clerk_backend_api import Clerk

clerk = Clerk(bearer_auth=os.getenv("CLERK_SECRET_KEY"))

async def verify_clerk_token(token: str) -> dict:
    # Clerk JWT verified with Clerk's JWKS endpoint
    payload = jwt.decode(token, options={"verify_signature": False})  # Clerk handles sig
    # Or use: clerk.sessions.verify_token(token)
    return {"sub": payload["sub"], "phone": payload.get("phone_number")}
```

### Clerk → Supabase Sync (Webhook)
```python
# api/routers/webhooks.py
# POST /api/webhooks/clerk — called by Clerk on user.created
@router.post("/webhooks/clerk")
async def clerk_webhook(request: Request):
    payload = await request.json()
    if payload["type"] == "user.created":
        user_data = payload["data"]
        phone = user_data["phone_numbers"][0]["phone_number"]
        uid = user_data["id"]  # Clerk user ID
        # Upsert into Supabase
        supabase.table("users").upsert({
            "id": uid, "phone": phone,
            "name": f"{user_data.get('first_name', '')} {user_data.get('last_name', '')}".strip()
        }).execute()
```

### Migration to Auth0 (When Ready)
```
1. Export users from Clerk → import to Auth0
2. Replace ClerkProvider with Auth0Provider
3. Replace useAuth() with useAuth0()
4. Backend: verify Auth0 JWT (RS256) instead of Clerk
5. Zero DB changes (Supabase users table stays same)
```

### Environment Variables
```
# Frontend (.env.local)
VITE_CLERK_PUBLISHABLE_KEY=pk_live_...

# Backend (VM /etc/environment)
CLERK_SECRET_KEY=sk_live_...
```

---

## 24. TESTING STRATEGY

### Test Pyramid
```
         ┌─────────────┐
         │  E2E Tests  │  ← Playwright (5-10 critical flows)
         │  (slow, few)│
         ├─────────────┤
         │  API Tests  │  ← Postman / Newman (all endpoints)
         │  (medium)   │
         ├─────────────┤
         │ Unit Tests  │  ← Jest + Vitest (fast, many)
         │ (fast, many)│
         └─────────────┘
```

### 24.1 Unit Tests — Jest (Frontend) + pytest (Backend)

#### Frontend: Vitest + React Testing Library
```bash
# Install
pnpm add -D vitest @testing-library/react @testing-library/jest-dom jsdom

# vite.config.ts — add test config
test: {
  environment: 'jsdom',
  globals: true,
  setupFiles: ['./src/test/setup.ts']
}
```

```typescript
// src/hooks/__tests__/useKundali.test.ts
import { renderHook } from '@testing-library/react'
import { useKundliStore } from '@/store/kundliStore'

test('kundali store initializes empty', () => {
  const { result } = renderHook(() => useKundliStore())
  expect(result.current.kundaliData).toBeNull()
})
```

```typescript
// src/components/__tests__/KundliChart.test.tsx
test('KundliChart renders SVG', () => {
  render(<KundliChart />)
  expect(screen.getByRole('img')).toBeInTheDocument()  // SVG
})
```

#### Backend: pytest + httpx
```bash
pip install pytest pytest-asyncio httpx

# Run tests
pytest api/tests/ -v
```

```python
# api/tests/test_kundali.py
from httpx import AsyncClient
from api.main import app

@pytest.mark.asyncio
async def test_kundali_endpoint():
    async with AsyncClient(app=app, base_url="http://test") as client:
        resp = await client.post("/api/kundali", json={
            "date": "1990-05-15", "time": "10:30",
            "lat": 25.317, "lon": 82.973, "tz": 5.5
        })
    assert resp.status_code == 200
    data = resp.json()
    assert "lagna" in data
    assert "grahas" in data
    assert data["lagna"]["rashi"] in ["Mesha", "Vrishabha", "Mithuna", "Karka",
                                       "Simha", "Kanya", "Tula", "Vrishchika",
                                       "Dhanu", "Makara", "Kumbha", "Meena"]

@pytest.mark.asyncio
async def test_panchang_endpoint():
    async with AsyncClient(app=app, base_url="http://test") as client:
        resp = await client.get("/api/panchang?lat=28.6139&lon=77.209&tz=5.5")
    assert resp.status_code == 200
    assert "tithi" in resp.json()
```

### 24.2 API Tests — Postman + Newman (CI)

```
Collection: Brahm AI API Tests
├── Auth
│   ├── POST /api/auth/send-otp → 200, sent: true
│   └── POST /api/auth/verify-otp → 200, access_token present
├── Kundali
│   ├── POST /api/kundali (valid) → 200, lagna.rashi present
│   └── POST /api/kundali (missing fields) → 422 Unprocessable
├── Panchang
│   └── GET /api/panchang → 200, tithi + nakshatra present
├── Chat
│   └── POST /api/chat → 200, SSE stream starts
├── Subscription
│   ├── GET /api/subscription/plans → 3 plans returned
│   └── POST /api/subscription/checkout → payment_session_id present
└── Admin
    └── GET /api/admin/stats → 200, mau + revenue
```

```bash
# Run via Newman (CLI — use in CI/CD)
pip install newman  # or npm install -g newman
newman run brahm-ai-api-tests.postman_collection.json \
  --environment brahm-ai-prod.postman_environment.json \
  --reporters cli,json \
  --reporter-json-export results.json
```

### 24.3 E2E Tests — Playwright

**Why Playwright over Cypress:**
- Better SSE/streaming support (AI chat testing)
- Faster parallel execution
- Mobile viewport testing (critical for Brahm AI mobile)
- No iframe restrictions (Cashfree checkout testing)

```bash
pnpm add -D @playwright/test
npx playwright install chromium firefox webkit
```

```typescript
// tests/e2e/auth.spec.ts
import { test, expect } from '@playwright/test'

test('login with OTP flow', async ({ page }) => {
  await page.goto('/')
  await page.click('text=Get Started Free')
  await page.fill('[placeholder*="phone"]', '9876543210')
  await page.click('text=Send OTP')
  await expect(page.locator('text=Enter 6-digit OTP')).toBeVisible()
})

test('kundali page loads chart', async ({ page }) => {
  // login first (use test account)
  await page.goto('/kundli')
  await expect(page.locator('svg')).toBeVisible()  // chart renders
  await expect(page.locator('text=Lagna')).toBeVisible()
})

test('AI chat sends and receives message', async ({ page }) => {
  await page.goto('/chat')
  await page.fill('[placeholder="Ask something..."]', 'Meri kundali batao')
  await page.press('[placeholder="Ask something..."]', 'Enter')
  // Wait for streaming response
  await expect(page.locator('.ai-response')).toBeVisible({ timeout: 30000 })
})

test('mobile hamburger menu opens', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })  // iPhone
  await page.goto('/dashboard')
  await page.click('[aria-label="Open navigation"]')
  await expect(page.locator('text=My Kundali')).toBeVisible()
})
```

```bash
# Run E2E tests
npx playwright test
npx playwright test --headed                    # visible browser
npx playwright test --project=Mobile-Chrome    # mobile viewport
```

### 24.4 Test Coverage Targets
| Layer | Tool | Target Coverage |
|-------|------|----------------|
| Backend services | pytest | 80% (kundali_service, panchang_service, auth_service) |
| API endpoints | pytest + Postman | 100% of endpoints have at least 1 happy-path test |
| React hooks | Vitest | 70% (useChat, useKundali, useAuth) |
| E2E flows | Playwright | 5 critical paths: login, kundali, chat, subscription, mobile nav |

### 24.5 CI/CD Pipeline (GitHub Actions)
```yaml
# .github/workflows/test.yml
name: Tests
on: [push, pull_request]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: pip install -r api/requirements.txt pytest pytest-asyncio httpx
      - run: pytest api/tests/ -v

  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: pnpm install && pnpm test --run

  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: pnpm install && npx playwright install --with-deps chromium
      - run: npx playwright test --project=chromium
```

---

## 25. MONITORING & LOGGING

### Stack
| Tool | Purpose | Where |
|------|---------|-------|
| **Sentry** | Error tracking (frontend + backend) | Cloud (sentry.io) |
| **Prometheus** | Metrics collection (FastAPI) | VM: port 9090 |
| **Grafana** | Dashboards (on top of Prometheus) | VM: port 3000 |
| **Python logging** | Structured backend logs | VM: `/var/log/brahm-api/` |

### 25.1 Sentry — Error Tracking

#### Frontend (React)
```bash
pnpm add @sentry/react
```

```typescript
// src/main.tsx
import * as Sentry from "@sentry/react"

Sentry.init({
  dsn: import.meta.env.VITE_SENTRY_DSN,
  environment: import.meta.env.MODE,       // "development" | "production"
  tracesSampleRate: 0.1,                   // 10% of requests traced
  integrations: [
    Sentry.browserTracingIntegration(),
    Sentry.replayIntegration({ maskAllText: false }),  // Session replay
  ],
  replaysOnErrorSampleRate: 1.0,           // 100% of errors get replay
})

// Wrap app
<Sentry.ErrorBoundary fallback={<ErrorPage />}>
  <App />
</Sentry.ErrorBoundary>
```

#### Backend (FastAPI)
```bash
pip install sentry-sdk[fastapi]
```

```python
# api/main.py
import sentry_sdk
from sentry_sdk.integrations.fastapi import FastApiIntegration

sentry_sdk.init(
    dsn=os.getenv("SENTRY_DSN"),
    environment=os.getenv("ENV", "production"),
    traces_sample_rate=0.1,
    integrations=[FastApiIntegration()],
    # Capture user context from JWT
    before_send=lambda event, hint: event
)

# In routes — capture user context:
sentry_sdk.set_user({"id": user["sub"], "phone": user["phone"]})
```

**What Sentry catches:**
- React component crashes → full stack trace + session replay
- FastAPI 500 errors → Python traceback + request context
- Unhandled promise rejections (SSE failures, API timeouts)
- Slow transactions (if kundali calc > 2s)

#### Environment Variables
```
# Frontend (.env.local)
VITE_SENTRY_DSN=https://xxx@sentry.io/yyy

# Backend (VM)
SENTRY_DSN=https://xxx@sentry.io/yyy
ENV=production
```

### 25.2 Prometheus — Metrics Collection

```bash
pip install prometheus-fastapi-instrumentator
```

```python
# api/main.py
from prometheus_fastapi_instrumentator import Instrumentator

app = FastAPI()
Instrumentator().instrument(app).expose(app)
# Auto-exposes: GET /metrics (Prometheus scrape endpoint)
```

**Auto-collected metrics:**
```
http_requests_total{method, handler, status}     — request counts per endpoint
http_request_duration_seconds{handler}           — latency histogram
http_requests_in_progress{handler}               — concurrent requests
```

**Custom metrics to add:**
```python
from prometheus_client import Counter, Histogram, Gauge

kundali_calculations = Counter("brahm_kundali_total", "Total kundali calculations")
ai_chat_tokens = Counter("brahm_ai_tokens_total", "Total AI tokens used")
active_users = Gauge("brahm_active_users", "Users active in last 5 min")
rag_latency = Histogram("brahm_rag_seconds", "RAG pipeline latency")

# In route handlers:
kundali_calculations.inc()
with rag_latency.time():
    result = await rag_pipeline(query)
```

**Prometheus config (`/etc/prometheus/prometheus.yml` on VM):**
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: brahm-api
    static_configs:
      - targets: ['localhost:8000']   # FastAPI /metrics endpoint
```

```bash
# Install + start Prometheus on VM
wget https://github.com/prometheus/prometheus/releases/download/.../prometheus-*.tar.gz
tar xf prometheus-*.tar.gz
./prometheus --config.file=prometheus.yml &
# Access: http://34.135.70.190:9090
```

### 25.3 Grafana — Dashboards

```bash
# Install Grafana on VM
sudo apt-get install -y grafana
sudo systemctl start grafana-server
sudo systemctl enable grafana-server
# Access: http://34.135.70.190:3000 (admin/admin default)
```

**Add Prometheus data source:**
```
Settings → Data Sources → Add → Prometheus
URL: http://localhost:9090
```

**Key Dashboards to create:**

| Dashboard | Panels |
|-----------|--------|
| **API Health** | RPS, error rate, p95 latency per endpoint |
| **AI Engine** | Kundali calc/min, chat requests/min, RAG latency, token usage |
| **Users** | New signups/day, MAU, plan distribution (Free/Jyotishi/Acharya) |
| **Revenue** | Cashfree payments/day, MRR, plan upgrades |
| **VM** | CPU %, RAM %, GPU VRAM usage |

**Useful PromQL queries:**
```promql
# Error rate (last 5 min)
rate(http_requests_total{status=~"5.."}[5m])

# p95 API latency
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Kundali calculations per hour
increase(brahm_kundali_total[1h])

# Active AI chat sessions
brahm_active_users
```

### 25.4 Structured Python Logging

```python
# api/logging_config.py
import logging, json
from datetime import datetime

class JSONFormatter(logging.Formatter):
    def format(self, record):
        return json.dumps({
            "ts":      datetime.utcnow().isoformat(),
            "level":   record.levelname,
            "module":  record.module,
            "msg":     record.getMessage(),
            "user_id": getattr(record, "user_id", None),
            "endpoint": getattr(record, "endpoint", None),
        })

logging.basicConfig(
    level=logging.INFO,
    handlers=[
        logging.StreamHandler(),                              # → journalctl
        logging.FileHandler("/var/log/brahm-api/app.log"),   # → file
    ]
)
```

```python
# Usage in routes:
logger = logging.getLogger(__name__)
logger.info("kundali_calculated", extra={"user_id": uid, "endpoint": "/api/kundali"})
logger.error("cashfree_webhook_failed", extra={"order_id": order_id, "error": str(e)})
```

**Log rotation (logrotate config on VM):**
```
/var/log/brahm-api/*.log {
    daily
    rotate 30
    compress
    missingok
    notifempty
    postrotate
        systemctl reload brahm-api
    endscript
}
```

### 25.5 Alerting (Grafana Alerts)

| Alert | Condition | Action |
|-------|-----------|--------|
| High error rate | 5xx > 5% for 5 min | Email + Slack DM |
| API down | No scrape for 2 min | Email immediately |
| Slow kundali | p95 > 3s for 10 min | Log warning |
| VRAM near full | GPU > 22GB | Email warning |
| DB connection fail | Supabase errors > 10/min | Email + auto-retry |

```
Grafana → Alerting → Contact points → Add email / Slack webhook
```

### 25.6 Setup Order
```
1. Sentry (30 min) — signup sentry.io, add DSN to frontend + backend
2. Python logging (1 hr) — structured logs to /var/log/brahm-api/
3. Prometheus (1 hr) — install on VM, add instrumentator to FastAPI
4. Grafana (2 hr) — install on VM, connect Prometheus, build 3 dashboards
5. Alerts (30 min) — set up email alerts for critical conditions
```

---

## 26. USER DATA STORAGE — COMPLETE MAP

### 26.1 Where Every Piece of User Data Lives

| Data | Storage | Why | Retention |
|------|---------|-----|-----------|
| User profile (name, phone, birth details) | Supabase `users` table | Persistent, synced across devices | Forever |
| Kundali JSON (calculated chart) | Supabase `users.kundali_json` column | Cached — no recalc on return | Forever |
| Active subscription + plan | Supabase `subscriptions` table | Source of truth for feature gates | Until cancelled/expired |
| Payment history | Supabase `subscriptions` + `payment_log` table | Audit trail, receipts | Forever |
| Feature usage counts (AI chat/day) | Supabase `usage_log` table | Free tier enforcement | 90 days rolling |
| **Chat history (all conversations)** | **Supabase `chat_messages` table** | Persistent across devices, admin visible | 1 year |
| Chat history (current session cache) | Browser `localStorage` (`brahm_chat_{pageContext}`) | Fast load, no API call needed | Until cleared |
| Birth details (quick access) | Zustand `kundliStore` + `localStorage` | Instant availability on every page | Until logout |
| Auth token (JWT) | `localStorage` (web) / Keychain (iOS) / EncryptedSharedPrefs (Android) | Clerk-managed | 7 days |
| UI preferences (language, theme) | `localStorage` + Supabase `users.lang_pref` | Instant + cross-device | Forever |
| Fact-sheet ("meri baatein") | `localStorage` (`brahm_facts`) | Personal AI context | Until cleared |
| FCM push token | Supabase `users.fcm_token` | Push notification delivery | Until app uninstall |
| Session replays / error events | Sentry cloud | Debugging only | 30 days |
| API metrics | Prometheus (VM) | Performance monitoring | 30 days |

---

### 26.2 Chat History — Full Design

#### Why store chat history in DB (not just localStorage)?
- User switches device (phone → laptop) — history should sync
- Admin can see conversations for support/moderation
- AI can reference past sessions ("pichli baar tune kaha tha...")
- Analytics: what questions users ask most
- Required for any "history" feature in the app

#### Supabase Schema — `chat_messages` table
```sql
CREATE TABLE chat_messages (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      TEXT REFERENCES users(id) ON DELETE CASCADE,
  session_id   TEXT NOT NULL,        -- groups messages into one conversation
  page_context TEXT DEFAULT 'general', -- 'kundali' | 'panchang' | 'sky' | 'general' etc.
  role         TEXT NOT NULL,        -- 'user' | 'assistant'
  content      TEXT NOT NULL,
  confidence   TEXT,                 -- 'HIGH' | 'MEDIUM' | 'LOW' (from AI tag)
  sources      JSONB,                -- [{book, chunk_id}] from RAG
  tokens_used  INTEGER,              -- Gemini token count (for billing/analytics)
  created_at   TIMESTAMPTZ DEFAULT now()
);

-- Indexes for fast queries
CREATE INDEX idx_chat_user_id ON chat_messages(user_id);
CREATE INDEX idx_chat_session ON chat_messages(session_id);
CREATE INDEX idx_chat_created ON chat_messages(created_at DESC);

-- RLS: users see only their own messages; admins see all
ALTER TABLE chat_messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "user_own_messages" ON chat_messages
  FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "admin_all_messages" ON chat_messages
  FOR ALL USING (
    EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'admin')
  );
```

#### session_id Strategy
```
session_id = "{user_id}_{page_context}_{date}"
Example:    "usr_a1b2c3_kundali_2026-03-24"

→ One session per page per day
→ Groups related messages together in admin view
→ Easy to clear one page's history without affecting others
```

#### FastAPI — Save Chat Messages
```python
# api/routers/chat.py — after streaming completes, save to DB

async def save_chat_message(user_id: str, session_id: str, page_context: str,
                             role: str, content: str, confidence: str = None,
                             sources: list = None, tokens: int = None):
    supabase.table("chat_messages").insert({
        "user_id":      user_id,
        "session_id":   session_id,
        "page_context": page_context,
        "role":         role,
        "content":      content,
        "confidence":   confidence,
        "sources":      sources or [],
        "tokens_used":  tokens,
    }).execute()

# In /api/chat SSE handler:
# 1. Save user message immediately
await save_chat_message(uid, session_id, page_context, "user", user_message)

# 2. Stream AI response... collect full response
full_response = ""
async for chunk in gemini_stream:
    full_response += chunk
    yield chunk

# 3. Save AI response after stream ends
await save_chat_message(uid, session_id, page_context, "assistant",
                        full_response, confidence=extracted_confidence,
                        sources=rag_sources, tokens=token_count)
```

#### Frontend — Load Chat History from DB
```typescript
// src/hooks/useChat.ts — on mount, load history from API

// New endpoint: GET /api/chat/history?page_context=kundali&limit=20
const loadHistory = async () => {
  const res = await api.get(`/chat/history?page_context=${pageContext}&limit=20`)
  if (res.messages?.length) {
    setMessages(res.messages)
    // Also update localStorage cache
    localStorage.setItem(`brahm_chat_${pageContext}`, JSON.stringify(res.messages))
  } else {
    // Fallback to localStorage if API fails/slow
    const cached = localStorage.getItem(`brahm_chat_${pageContext}`)
    if (cached) setMessages(JSON.parse(cached))
  }
}
```

```python
# api/routers/chat.py — new history endpoint
@router.get("/chat/history")
async def get_chat_history(
    page_context: str = "general",
    limit: int = 20,
    user = Depends(get_current_user)
):
    result = supabase.table("chat_messages") \
        .select("role, content, confidence, sources, created_at") \
        .eq("user_id", user["sub"]) \
        .eq("page_context", page_context) \
        .order("created_at", desc=False) \
        .limit(limit) \
        .execute()
    return {"messages": result.data}
```

#### Chat History Retention Policy
```
- Keep last 1 year of messages per user
- Scheduled job (weekly): DELETE FROM chat_messages WHERE created_at < now() - interval '1 year'
- On user account delete: CASCADE deletes all messages automatically
- User can clear their own history: DELETE WHERE user_id = ? AND page_context = ?
```

---

### 26.3 Updated Supabase Schema — Complete

```sql
-- ── USERS ──────────────────────────────────────────────────────
CREATE TABLE users (
  id              TEXT PRIMARY KEY,        -- Clerk user ID (user_xxxxx)
  phone           TEXT UNIQUE,
  email           TEXT UNIQUE,
  name            TEXT,
  role            TEXT DEFAULT 'user',     -- 'user' | 'admin'
  lang_pref       TEXT DEFAULT 'en',
  city            TEXT,
  lat             REAL, lon REAL, tz REAL,
  birth_date      TEXT, birth_time TEXT,
  birth_city      TEXT,
  birth_lat       REAL, birth_lon REAL, birth_tz REAL,
  kundali_json    TEXT,                    -- cached KundaliResponse
  notif_panchang  BOOLEAN DEFAULT TRUE,
  notif_grahan    BOOLEAN DEFAULT TRUE,
  notif_festivals BOOLEAN DEFAULT FALSE,
  fcm_token       TEXT,
  created_at      TIMESTAMPTZ DEFAULT now(),
  last_login      TIMESTAMPTZ
);

-- ── SUBSCRIPTIONS ───────────────────────────────────────────────
CREATE TABLE subscriptions (
  id                TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
  user_id           TEXT REFERENCES users(id) ON DELETE CASCADE,
  plan              TEXT NOT NULL,         -- 'free' | 'jyotishi' | 'acharya'
  period            TEXT,                  -- 'monthly' | 'yearly'
  status            TEXT DEFAULT 'active', -- 'active' | 'cancelled' | 'expired'
  cashfree_order_id TEXT,
  amount_paid       INTEGER,               -- in paise (₹199 = 19900)
  started_at        TIMESTAMPTZ,
  expires_at        TIMESTAMPTZ,
  cancelled_at      TIMESTAMPTZ
);

-- ── USAGE LOG ───────────────────────────────────────────────────
CREATE TABLE usage_log (
  id          BIGSERIAL PRIMARY KEY,
  user_id     TEXT REFERENCES users(id) ON DELETE CASCADE,
  feature     TEXT,                        -- 'ai_chat' | 'kundali' | 'search' | 'palmistry'
  metadata    JSONB,                       -- {page_context, tokens, response_ms}
  used_at     TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_usage_user_date ON usage_log(user_id, used_at DESC);

-- ── CHAT MESSAGES ───────────────────────────────────────────────
CREATE TABLE chat_messages (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      TEXT REFERENCES users(id) ON DELETE CASCADE,
  session_id   TEXT NOT NULL,
  page_context TEXT DEFAULT 'general',
  role         TEXT NOT NULL,              -- 'user' | 'assistant'
  content      TEXT NOT NULL,
  confidence   TEXT,                       -- 'HIGH' | 'MEDIUM' | 'LOW'
  sources      JSONB,                      -- [{book, chunk_id}]
  tokens_used  INTEGER,
  created_at   TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_chat_user_id ON chat_messages(user_id);
CREATE INDEX idx_chat_session  ON chat_messages(session_id);
CREATE INDEX idx_chat_created  ON chat_messages(created_at DESC);

-- ── SAVED KUNDALIS ──────────────────────────────────────────────
-- For Jyotishi+ users saving multiple charts (clients, family)
CREATE TABLE saved_kundalis (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      TEXT REFERENCES users(id) ON DELETE CASCADE,
  label        TEXT,                       -- "My chart" | "Mom" | "Client - Ramesh"
  birth_date   TEXT, birth_time TEXT,
  birth_city   TEXT,
  birth_lat    REAL, birth_lon REAL, birth_tz REAL,
  kundali_json TEXT,                       -- KundaliResponse JSON
  created_at   TIMESTAMPTZ DEFAULT now()
);
```

---

## 27. ADMIN PANEL — COMPLETE SYSTEM

### 27.1 Complete Data Tracked Per User

Every action a user takes is stored and visible to admin:

| Action | Table | What's Stored |
|--------|-------|--------------|
| Signed up | `users` | phone, name, device, signup time, IP |
| Login | `users.last_login` + `login_log` | timestamp, device, IP, success/fail |
| Generated Kundali | `kundali_log` | birth details used, result JSON, calc time |
| AI Chat message | `chat_messages` | full message, AI reply, confidence, tokens, page context |
| Palm reading | `palmistry_log` | image hash, AI result JSON, timestamp |
| Paid subscription | `subscriptions` + `payment_log` | plan, amount, Cashfree order ID, status |
| Payment failed | `payment_log` | reason, attempt count |
| Subscription cancelled | `subscriptions.cancelled_at` | reason if provided |
| Used feature | `usage_log` | feature name, count per day |
| Saved Kundali | `saved_kundalis` | label, birth details, kundali JSON |
| Account deleted | `deleted_accounts` | soft-delete audit trail |

---

### 27.2 Complete Supabase Schema (All Tables)

```sql
-- ── USERS ─────────────────────────────────────────────────────────────────
CREATE TABLE users (
  id              TEXT PRIMARY KEY,        -- Clerk user_xxx ID
  phone           TEXT UNIQUE,
  email           TEXT UNIQUE,
  name            TEXT,
  role            TEXT DEFAULT 'user',     -- 'user' | 'admin' | 'banned'
  status          TEXT DEFAULT 'active',   -- 'active' | 'suspended' | 'deleted'
  lang_pref       TEXT DEFAULT 'en',
  city            TEXT,
  lat             REAL, lon REAL, tz REAL,
  birth_date      TEXT, birth_time TEXT,
  birth_city      TEXT,
  birth_lat       REAL, birth_lon REAL, birth_tz REAL,
  kundali_json    TEXT,                    -- cached active KundaliResponse
  notif_panchang  BOOLEAN DEFAULT TRUE,
  notif_grahan    BOOLEAN DEFAULT TRUE,
  notif_festivals BOOLEAN DEFAULT FALSE,
  fcm_token       TEXT,
  signup_ip       TEXT,
  signup_device   TEXT,                    -- 'web' | 'android' | 'ios'
  created_at      TIMESTAMPTZ DEFAULT now(),
  last_login      TIMESTAMPTZ,
  deleted_at      TIMESTAMPTZ             -- soft delete
);

-- ── LOGIN LOG ─────────────────────────────────────────────────────────────
CREATE TABLE login_log (
  id         BIGSERIAL PRIMARY KEY,
  user_id    TEXT REFERENCES users(id) ON DELETE CASCADE,
  phone      TEXT,                         -- in case user not found
  ip         TEXT,
  device     TEXT,                         -- 'web' | 'android' | 'ios'
  user_agent TEXT,
  success    BOOLEAN,
  fail_reason TEXT,                        -- 'wrong_otp' | 'expired_otp' | 'banned'
  logged_at  TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_login_user ON login_log(user_id, logged_at DESC);

-- ── SUBSCRIPTIONS ─────────────────────────────────────────────────────────
CREATE TABLE subscriptions (
  id                TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
  user_id           TEXT REFERENCES users(id) ON DELETE CASCADE,
  plan              TEXT NOT NULL,         -- 'jyotishi' | 'acharya'
  period            TEXT,                  -- 'monthly' | 'yearly'
  status            TEXT DEFAULT 'active', -- 'active' | 'cancelled' | 'expired' | 'paused'
  cashfree_order_id TEXT UNIQUE,
  cashfree_sub_id   TEXT,                  -- recurring subscription ID
  amount_paid       INTEGER,               -- paise (₹199 = 19900)
  currency          TEXT DEFAULT 'INR',
  started_at        TIMESTAMPTZ,
  expires_at        TIMESTAMPTZ,
  cancelled_at      TIMESTAMPTZ,
  cancel_reason     TEXT,                  -- 'user_request' | 'payment_failed' | 'admin'
  cancelled_by      TEXT                   -- 'user' | 'admin' | 'system'
);
CREATE INDEX idx_sub_user ON subscriptions(user_id, status);
CREATE INDEX idx_sub_expiry ON subscriptions(expires_at) WHERE status = 'active';

-- ── PAYMENT LOG ───────────────────────────────────────────────────────────
CREATE TABLE payment_log (
  id                BIGSERIAL PRIMARY KEY,
  user_id           TEXT REFERENCES users(id),
  subscription_id   TEXT REFERENCES subscriptions(id),
  cashfree_order_id TEXT,
  cashfree_payment_id TEXT,
  amount            INTEGER,               -- paise
  currency          TEXT DEFAULT 'INR',
  status            TEXT,                  -- 'SUCCESS' | 'FAILED' | 'PENDING' | 'REFUNDED'
  payment_method    TEXT,                  -- 'UPI' | 'CARD' | 'NETBANKING' | 'WALLET'
  fail_reason       TEXT,
  refund_amount     INTEGER,
  refund_at         TIMESTAMPTZ,
  webhook_raw       JSONB,                 -- full Cashfree webhook payload (audit)
  paid_at           TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_payment_user ON payment_log(user_id, paid_at DESC);
CREATE INDEX idx_payment_status ON payment_log(status, paid_at DESC);

-- ── CHAT MESSAGES ─────────────────────────────────────────────────────────
CREATE TABLE chat_messages (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      TEXT REFERENCES users(id) ON DELETE CASCADE,
  session_id   TEXT NOT NULL,              -- "{user_id}_{page_context}_{date}"
  page_context TEXT DEFAULT 'general',     -- 'kundali'|'panchang'|'sky'|'general'|...
  role         TEXT NOT NULL,              -- 'user' | 'assistant'
  content      TEXT NOT NULL,
  confidence   TEXT,                       -- 'HIGH' | 'MEDIUM' | 'LOW'
  sources      JSONB,                      -- [{book, chunk_id, score}]
  tokens_used  INTEGER,
  response_ms  INTEGER,                    -- AI response time in ms
  flagged      BOOLEAN DEFAULT FALSE,      -- admin flag for review
  flag_reason  TEXT,
  created_at   TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_chat_user ON chat_messages(user_id, created_at DESC);
CREATE INDEX idx_chat_session ON chat_messages(session_id);
CREATE INDEX idx_chat_flagged ON chat_messages(flagged) WHERE flagged = TRUE;

-- ── KUNDALI LOG ───────────────────────────────────────────────────────────
CREATE TABLE kundali_log (
  id           BIGSERIAL PRIMARY KEY,
  user_id      TEXT REFERENCES users(id),
  birth_date   TEXT, birth_time TEXT,
  birth_city   TEXT,
  birth_lat    REAL, birth_lon REAL, birth_tz REAL,
  result_json  TEXT,                       -- full KundaliResponse
  calc_ms      INTEGER,                    -- calculation time ms
  source       TEXT DEFAULT 'web',         -- 'web' | 'android' | 'ios'
  is_saved     BOOLEAN DEFAULT FALSE,      -- user saved it to saved_kundalis
  created_at   TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_kundali_user ON kundali_log(user_id, created_at DESC);

-- ── SAVED KUNDALIS ────────────────────────────────────────────────────────
CREATE TABLE saved_kundalis (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      TEXT REFERENCES users(id) ON DELETE CASCADE,
  label        TEXT,                       -- "My chart" | "Mom" | "Client - Ramesh"
  birth_date   TEXT, birth_time TEXT,
  birth_city   TEXT,
  birth_lat    REAL, birth_lon REAL, birth_tz REAL,
  kundali_json TEXT,
  is_shared    BOOLEAN DEFAULT FALSE,
  share_token  TEXT UNIQUE,                -- for /kundli?share=xxx public URL
  created_at   TIMESTAMPTZ DEFAULT now()
);

-- ── PALMISTRY LOG ─────────────────────────────────────────────────────────
CREATE TABLE palmistry_log (
  id           BIGSERIAL PRIMARY KEY,
  user_id      TEXT REFERENCES users(id),
  image_hash   TEXT,                       -- SHA256 of uploaded image (no image stored)
  image_size   INTEGER,                    -- bytes
  result_json  TEXT,                       -- full Gemini Vision response
  lines_found  JSONB,                      -- {life_line, heart_line, head_line, ...}
  confidence   TEXT,
  tokens_used  INTEGER,
  response_ms  INTEGER,
  created_at   TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_palm_user ON palmistry_log(user_id, created_at DESC);

-- ── USAGE LOG ─────────────────────────────────────────────────────────────
CREATE TABLE usage_log (
  id          BIGSERIAL PRIMARY KEY,
  user_id     TEXT REFERENCES users(id) ON DELETE CASCADE,
  feature     TEXT,  -- 'ai_chat'|'kundali'|'search'|'palmistry'|'compatibility'|'gochar'
  source      TEXT DEFAULT 'web',          -- 'web' | 'android' | 'ios'
  metadata    JSONB,                       -- {page_context, tokens, response_ms, plan}
  used_at     TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_usage_user_date ON usage_log(user_id, used_at DESC);
CREATE INDEX idx_usage_feature ON usage_log(feature, used_at DESC);

-- ── DELETED ACCOUNTS (soft-delete audit) ─────────────────────────────────
CREATE TABLE deleted_accounts (
  id             BIGSERIAL PRIMARY KEY,
  user_id        TEXT,
  phone          TEXT,
  name           TEXT,
  plan_at_delete TEXT,
  total_chats    INTEGER,
  total_payments INTEGER,
  delete_reason  TEXT,                     -- 'user_request' | 'admin_ban' | 'gdpr'
  deleted_by     TEXT,                     -- 'user' | 'admin'
  deleted_at     TIMESTAMPTZ DEFAULT now()
);

-- ── ADMIN ACTIONS LOG ────────────────────────────────────────────────────
-- Every admin action is logged (who did what to whom)
CREATE TABLE admin_log (
  id          BIGSERIAL PRIMARY KEY,
  admin_id    TEXT REFERENCES users(id),
  action      TEXT,    -- 'ban_user'|'refund'|'change_plan'|'delete_user'|'flag_message'
  target_id   TEXT,    -- user_id or payment_id or message_id
  target_type TEXT,    -- 'user' | 'payment' | 'chat_message' | 'subscription'
  details     JSONB,   -- {old_value, new_value, reason}
  performed_at TIMESTAMPTZ DEFAULT now()
);
```

---

### 27.3 Admin API Endpoints (Complete)

```python
# api/routers/admin.py
# All routes: Depends(get_admin_user) — role='admin' required
# Every admin action is logged to admin_log table

# ── DASHBOARD ────────────────────────────────────────────────────
GET  /api/admin/stats
     → { total_users, new_today, new_week, mau, dau,
         paid_users, revenue_today, revenue_month, revenue_total,
         chats_today, kundalis_today, palm_readings_today,
         active_subscriptions: {jyotishi_monthly, jyotishi_yearly, acharya_monthly, acharya_yearly} }

# ── USER MANAGEMENT ──────────────────────────────────────────────
GET  /api/admin/users?search=&plan=&status=&page=&limit=
     → paginated list: [{ id, name, phone, plan, status, created_at, last_login,
                          total_chats, total_payments, last_active }]

GET  /api/admin/users/{user_id}
     → full profile: { ...user_row, subscription, usage_today,
                        login_history[-5], total_kundalis, total_chats,
                        total_palmistry, lifetime_paid_inr }

PATCH /api/admin/users/{user_id}
     body: { status?, role?, plan?, note }
     → can set: status='suspended'|'active', role='admin'|'user', plan override
     → logs to admin_log

DELETE /api/admin/users/{user_id}
     body: { reason, hard_delete: bool }
     → soft delete: sets users.status='deleted', users.deleted_at=now()
     → saves to deleted_accounts audit table
     → hard delete: removes all rows + cancels active subscription
     → logs to admin_log

POST /api/admin/users/{user_id}/ban
     body: { reason }
     → sets role='banned', cancels subscription, logs admin_log

POST /api/admin/users/{user_id}/unban
     → sets role='user', logs admin_log

# ── CHAT HISTORY ─────────────────────────────────────────────────
GET  /api/admin/users/{user_id}/chats?page_context=&page=&limit=
     → full chat history with all fields (confidence, tokens, sources, flagged)

POST /api/admin/chats/{message_id}/flag
     body: { reason }
     → sets flagged=TRUE, flag_reason, logs admin_log

DELETE /api/admin/users/{user_id}/chats
     body: { page_context? }   # optional: clear specific context only
     → deletes all or context-specific chat messages, logs admin_log

# ── KUNDALI HISTORY ──────────────────────────────────────────────
GET  /api/admin/users/{user_id}/kundalis
     → all kundali calculations: [{ id, birth_details, calc_ms, created_at, is_saved }]

GET  /api/admin/users/{user_id}/kundalis/{id}
     → full result_json of that calculation

# ── PALMISTRY HISTORY ────────────────────────────────────────────
GET  /api/admin/users/{user_id}/palmistry
     → all palm readings: [{ id, image_hash, lines_found, confidence, created_at }]

GET  /api/admin/users/{user_id}/palmistry/{id}
     → full result_json

# ── PAYMENT & SUBSCRIPTION ───────────────────────────────────────
GET  /api/admin/users/{user_id}/payments
     → full payment history: [{ id, plan, amount, status, method, paid_at, cashfree_order_id }]

GET  /api/admin/users/{user_id}/subscription
     → current + past subscriptions with all fields

POST /api/admin/users/{user_id}/subscription/cancel
     body: { reason }
     → cancels active subscription via Cashfree API + updates DB, logs admin_log

POST /api/admin/users/{user_id}/subscription/extend
     body: { days, reason }
     → extends expires_at manually (goodwill gesture), logs admin_log

POST /api/admin/payments/{payment_id}/refund
     body: { amount?, reason }
     → calls Cashfree refund API, updates payment_log, logs admin_log

POST /api/admin/users/{user_id}/grant-plan
     body: { plan, days, reason }
     → manually grant free access (influencer, support resolution), logs admin_log

# ── SUBSCRIPTION OVERVIEW ─────────────────────────────────────────
GET  /api/admin/subscriptions?status=&plan=&expiring_days=
     → all subscriptions with user name/phone

GET  /api/admin/subscriptions/expiring
     → users whose subscription expires in next 7 days (for renewal reminders)

GET  /api/admin/revenue
     → { today, this_week, this_month, all_time,
         by_plan: {jyotishi_monthly: x, ...},
         by_method: {upi: x, card: y, ...},
         refunds_this_month }

# ── ANALYTICS ────────────────────────────────────────────────────
GET  /api/admin/analytics/chat
     → { top_questions[-20], page_context_dist, avg_tokens, avg_response_ms,
         daily_chats[-30], flagged_messages[-10] }

GET  /api/admin/analytics/features
     → feature usage counts: { kundali, ai_chat, palmistry, compatibility, ... }
     by day/week/month

GET  /api/admin/analytics/users
     → { signups_by_day[-30], retention_day1/day7/day30,
         plan_conversion_rate, churn_rate }

# ── ADMIN AUDIT LOG ──────────────────────────────────────────────
GET  /api/admin/logs
     → all admin actions with who did what when

# ── BULK ACTIONS ─────────────────────────────────────────────────
POST /api/admin/notify
     body: { user_ids[]|all, title, body, type: 'push'|'email' }
     → send push notification to specific users or all

POST /api/admin/export/users
     → CSV export: name, phone, plan, created_at, total_paid
```

---

### 27.4 Admin Panel UI (`api.brahmasmi.bimoraai.com/admin`) ✅ BUILT

**Location:** `admin-app/` — separate React+Vite build, deployed to `/var/www/brahm-admin/`
**Auth:** `btoa("username:secret_key")` → `X-Admin-Key` header (sessionStorage). Legacy plain-key still works as fallback.
**Style:** Light theme — white/warm-off-white, gold (#B07A00) + saffron (#D4540A) accents. Collapsible sidebar (w-56/w-14).

```
api.brahmasmi.bimoraai.com/admin
│
├── /login              Username + Secret Key form → btoa encode → preloadAll() on success
│
├── /dashboard          Stats grid (users/revenue/activity) + revenue cards
│
├── /users              Paginated table (search + plan/status filter) → /users/:id
│   └── /users/:id      7-tab user detail page (replaces old modal)
│       ├── Profile     — all fields grid: ID, phone, birth, plan, Cashfree order, lifetime paid
│       ├── Chats       — page_context filter + flag per message + pagination
│       ├── Kundalis    — calc history + expandable raw JSON
│       ├── Palmistry   — lines_found chips + confidence + tokens
│       ├── Payments    — transaction history + Refund button
│       ├── Usage       — today's feature usage bar chart
│       └── Logins      — IP + device + success/fail + fail reason
│
├── /payments           Revenue summary (Today/Month/Total) + all transactions + Refund action
│
├── /subscriptions      ← NEW (v7.0)
│   ├── Summary cards: Active | New This Month | Cancelled (30d) | Expiring 7d | Total Revenue
│   ├── Plan distribution chart (premium/standard × monthly/yearly/manual)
│   ├── Filter: search name/phone | status | plan | period
│   ├── SubRow (expandable): full detail grid + days_left badge (red/amber/green)
│   └── ActionModal: Extend (days + reason) | Cancel (reason) | Grant Plan (plan + days + reason)
│
├── /chats              ← UPGRADED (v7.0)
│   ├── Conversations grouped by session_id (user+AI turn pairs)
│   ├── ConversationCard: user name/phone, page context icon, turn count, flag indicator
│   ├── TurnRow: numbered | user msg (blue) + AI response (amber) | expand/collapse
│   │   ├── Confidence badge: GREEN / AMBER / RED
│   │   ├── response_ms + tokens_used
│   │   └── Copy button
│   ├── Flagged tab — only sessions containing flagged messages
│   └── Analytics tab — context distribution with % progress bars
│
├── /api-monitor        ← NEW (v7.0) — moved from Dashboard
│   ├── Summary: Total Requests | Avg Latency | Total Errors | Success Rate
│   ├── Request volume chart (inline SVG — hourly for today, daily for 7d/30d)
│   ├── Top endpoints table: hits + avg_ms (color-coded) + error %
│   ├── Slowest endpoints ranked by avg latency
│   ├── Status distribution (2xx green / 4xx amber / 5xx red) with progress bars
│   ├── Method breakdown (GET/POST/DELETE counts)
│   ├── Errors by endpoint table
│   └── Period filter: Today | Last 7 Days | Last 30 Days
│
├── /logs               Admin action log — Time | Admin | Action (color-coded) | Target | Details JSON
└── /deleted-accounts   30-day grace window accounts for review
```

**Performance:** `preloadAll()` warms cache for all tabs on login. 10-min GET cache. No spinners on nav.
**N+1 fix:** `/admin/users` and `/admin/chats` use batch `IN` queries + retry-once wrapper (eliminates HTTP/2 `RemoteProtocolError`).

---

### 27.5 Admin Access Control

**Auth:** `X-Admin-Key` header = `btoa("username:secret_key")`

```python
# api/routers/admin.py

ADMIN_KEY      = os.getenv("ADMIN_SECRET_KEY", "")
ADMIN_USERNAME = os.getenv("ADMIN_USERNAME", "admin")

def _check(request: Request):
    token = request.headers.get("X-Admin-Key", "")
    try:
        decoded = base64.b64decode(token).decode()
        username, secret = decoded.split(":", 1)
        if username == ADMIN_USERNAME and secret == ADMIN_KEY:
            return  # ✅ new format
    except Exception:
        pass
    # Legacy fallback: plain secret key (backward compat)
    if token == ADMIN_KEY:
        return
    raise HTTPException(401, "Unauthorized")
```

```
# /etc/systemd/system/brahm-api.service
Environment=ADMIN_SECRET_KEY=your_secret_key_here
Environment=ADMIN_USERNAME=your_admin_username
```

**React guard:** `AdminLayout.tsx` checks `sessionStorage.getItem("admin-key")` → redirects to `/login` if missing.

---

### 27.6 Pre-built Supabase Studio SQL Queries

Save these in Supabase SQL Editor → "Saved Queries":

```sql
-- 1. DAILY PULSE
SELECT
  (SELECT COUNT(*) FROM users WHERE created_at > now() - interval '24h') AS new_users_24h,
  (SELECT COUNT(*) FROM chat_messages WHERE created_at > now() - interval '24h') AS chats_24h,
  (SELECT COUNT(*) FROM kundali_log WHERE created_at > now() - interval '24h') AS kundalis_24h,
  (SELECT COUNT(*) FROM palmistry_log WHERE created_at > now() - interval '24h') AS palm_24h,
  (SELECT COALESCE(SUM(amount_paid)/100.0,0) FROM payment_log WHERE status='SUCCESS' AND paid_at > now() - interval '24h') AS revenue_24h_inr,
  (SELECT COUNT(*) FROM subscriptions WHERE status='active' AND plan != 'free') AS paid_users_total;

-- 2. FULL USER PROFILE (replace phone)
SELECT u.id, u.name, u.phone, u.role, u.status, u.created_at, u.last_login,
       u.birth_date, u.birth_city, u.lang_pref,
       s.plan, s.period, s.status AS sub_status, s.expires_at, s.amount_paid/100.0 AS paid_inr,
       (SELECT COUNT(*) FROM chat_messages WHERE user_id = u.id) AS total_chats,
       (SELECT COUNT(*) FROM kundali_log WHERE user_id = u.id) AS total_kundalis,
       (SELECT COUNT(*) FROM palmistry_log WHERE user_id = u.id) AS total_palm_readings,
       (SELECT COALESCE(SUM(amount_paid)/100.0,0) FROM payment_log WHERE user_id = u.id AND status='SUCCESS') AS lifetime_paid_inr
FROM users u
LEFT JOIN subscriptions s ON s.user_id = u.id AND s.status = 'active'
WHERE u.phone = '+919876543210';

-- 3. USER'S FULL CHAT HISTORY (replace user_id)
SELECT session_id, page_context, role, content, confidence,
       tokens_used, response_ms, flagged, created_at
FROM chat_messages
WHERE user_id = 'user_xxx'
ORDER BY created_at ASC;

-- 4. USER'S ALL KUNDALI CALCULATIONS
SELECT id, birth_date, birth_time, birth_city, calc_ms, is_saved, source, created_at
FROM kundali_log
WHERE user_id = 'user_xxx'
ORDER BY created_at DESC;

-- 5. USER'S PAYMENT HISTORY
SELECT cashfree_order_id, amount/100.0 AS inr, status, payment_method, fail_reason, paid_at
FROM payment_log
WHERE user_id = 'user_xxx'
ORDER BY paid_at DESC;

-- 6. USER'S PALMISTRY HISTORY
SELECT id, lines_found, confidence, tokens_used, created_at
FROM palmistry_log
WHERE user_id = 'user_xxx'
ORDER BY created_at DESC;

-- 7. REVENUE BREAKDOWN
SELECT
  period_label,
  COUNT(*) AS payments,
  SUM(amount)/100.0 AS total_inr
FROM (
  SELECT TO_CHAR(paid_at, 'YYYY-MM') AS period_label, amount
  FROM payment_log WHERE status = 'SUCCESS'
) t
GROUP BY period_label ORDER BY period_label DESC LIMIT 12;

-- 8. TOP QUESTIONS ASKED (last 30 days)
SELECT content, COUNT(*) AS times_asked
FROM chat_messages
WHERE role = 'user' AND created_at > now() - interval '30 days'
GROUP BY content ORDER BY times_asked DESC LIMIT 25;

-- 9. FEATURE USAGE BREAKDOWN
SELECT feature, source, COUNT(*) AS uses
FROM usage_log
WHERE used_at > now() - interval '30 days'
GROUP BY feature, source ORDER BY uses DESC;

-- 10. SUBSCRIPTIONS EXPIRING IN 7 DAYS
SELECT u.name, u.phone, u.lang_pref, s.plan, s.period, s.expires_at,
       s.amount_paid/100.0 AS paid_inr
FROM users u JOIN subscriptions s ON s.user_id = u.id
WHERE s.status = 'active' AND s.expires_at BETWEEN now() AND now() + interval '7 days'
ORDER BY s.expires_at;

-- 11. CHURN: RECENTLY CANCELLED
SELECT u.name, u.phone, s.plan, s.cancelled_at, s.cancel_reason, s.cancelled_by
FROM users u JOIN subscriptions s ON s.user_id = u.id
WHERE s.status = 'cancelled' AND s.cancelled_at > now() - interval '30 days'
ORDER BY s.cancelled_at DESC;

-- 12. MAU + DAU
SELECT
  COUNT(DISTINCT CASE WHEN used_at > now() - interval '24h' THEN user_id END) AS dau,
  COUNT(DISTINCT CASE WHEN used_at > now() - interval '30d' THEN user_id END) AS mau
FROM usage_log;

-- 13. BANNED / SUSPENDED USERS
SELECT id, name, phone, role, status, created_at
FROM users WHERE status != 'active' OR role = 'banned'
ORDER BY created_at DESC;

-- 14. ADMIN ACTION LOG
SELECT a.performed_at, u.name AS admin_name, a.action, a.target_type, a.target_id, a.details
FROM admin_log a JOIN users u ON u.id = a.admin_id
ORDER BY a.performed_at DESC LIMIT 50;
```

---

### 27.7 Admin Build Status

```
✅ DONE — Frontend (src/pages/AdminPage.tsx):
  ✅ Dashboard — 15 stat cards + top endpoints
  ✅ Users tab — search, filter by plan/status, full table, User Detail Modal
  ✅ User Detail Modal — 7 sub-tabs:
       Profile / Chats / Kundalis / Palmistry / Payments / Usage / Logins
  ✅ All user actions:
       Change Plan | Grant Free Days | Extend Sub | Cancel Sub
       Suspend/Unsuspend | Ban/Unban | Clear Chats | Delete Account
  ✅ Payments tab — revenue summary, filter, Refund button per row
  ✅ Chat Monitor — recent, flagged, analytics (top questions + context dist)
  ✅ Admin Log — all admin actions, color-coded by action type
  ✅ Mobile-responsive — bottom tab bar on small screens
  ✅ Auth — X-Admin-Key header (sessionStorage)

⬜ TODO — Backend (api/routers/admin.py) — needed for frontend to work:
  GET  /api/admin/stats
  GET  /api/admin/users?search=&plan=&status=&page=&limit=
  GET  /api/admin/users/{id}
  PATCH /api/admin/users/{id}           → change plan/status/role
  DELETE /api/admin/users/{id}          → soft/hard delete
  POST /api/admin/users/{id}/ban
  POST /api/admin/users/{id}/unban
  POST /api/admin/users/{id}/grant-plan
  POST /api/admin/users/{id}/subscription/cancel
  POST /api/admin/users/{id}/subscription/extend
  GET  /api/admin/users/{id}/chats?page_context=&page=
  DELETE /api/admin/users/{id}/chats
  POST /api/admin/chats/{msg_id}/flag
  GET  /api/admin/users/{id}/kundalis
  GET  /api/admin/users/{id}/palmistry
  GET  /api/admin/users/{id}/payments
  GET  /api/admin/users/{id}/logins
  GET  /api/admin/payments?status=&page=
  POST /api/admin/payments/{id}/refund  → calls Cashfree refund API
  GET  /api/admin/revenue
  GET  /api/admin/chats?flagged=&page=
  GET  /api/admin/analytics/chat        → top questions + context dist
  GET  /api/admin/logs?page=

  Rules for all backend admin routes:
  - Depends(get_admin_user) — role='admin' required on every route
  - Every mutating action → INSERT into admin_log table
  - Cancellation/ban → also call Cashfree cancel API if active subscription

⬜ TODO — Future additions (after 500+ users):
  → Push notification broadcaster (send to all / paid / specific user)
  → CSV export: users, revenue
  → Grafana iframe embed for API health dashboard
  → Realtime new user / payment alerts (Supabase Realtime)
```

---

## 22. AI INTELLIGENCE ENGINE — BRAHM AI v4.0
# Last Updated: 2026-03-20 (Two-Pass Reasoning + Tool Calling architecture)

> **Vision:** Aryabhata (precision) + Varahmihira (wisdom) ka living synthesis —
> sirf books quote nahi karta, khud sochta hai, calculations run karta hai,
> output analyze karta hai, aur real Pandit ki tarah jawab deta hai.

---

## 22.0 FINAL ARCHITECTURE DECISION (v4.0)

### Core Principle
```
Better reasoning flow > more features > more APIs
90% cases mein RAG nahi chahiye — pure reasoning + calculation better hai
```

### The Two-Pass + Tool Calling Flow
```
User Query + page_context + page_data + kundali_summary
          ↓
┌─────────────────────────────────────────┐
│  PASS 1: THINK (hidden, ~0.5s)          │
│  Gemini returns structured JSON:         │
│  {                                       │
│    "intent": "career prediction",        │
│    "needs_calculation": true,            │
│    "calc_services": ["dasha","gochar"],  │
│    "needs_rag": false,                   │
│    "kundali_focus": ["10th","Saturn"],   │
│    "response_depth": "master",           │
│    "response_lang": "hi"                 │
│  }                                       │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│  TOOL EXECUTION (agar needed)           │
│  needs_calculation=true:                │
│    → kundali_service.calc_kundali()     │
│    → panchang_service methods           │
│    → muhurta_service                    │
│    → compatibility_service              │
│  needs_rag=true (rare):                 │
│    → hybrid_search(query, top_k=3)      │
└──────────────┬──────────────────────────┘
               ↓
┌─────────────────────────────────────────┐
│  PASS 2: ANSWER (streamed)              │
│  Gemini gets:                           │
│    - User question                      │
│    - Pass 1 intent analysis             │
│    - Calculation output (if ran)        │
│    - Kundali compressed summary         │
│    - Page context + data               │
│    - RAG docs (only if needs_rag=true)  │
│    - Response structure template        │
│  → Streams expert Pandit-level answer   │
└─────────────────────────────────────────┘
```

### When RAG is used vs not
```
needs_rag = TRUE only when:
  - User explicitly asks "shastra mein kya likha hai"
  - User asks "Brihat Parashara ke anusar"
  - Deep shlok meaning / scripture quote needed
  - query_type = DEEP_VEDIC

needs_rag = FALSE (90% cases):
  - Personal predictions ("meri shadi kab")
  - Calculation-based ("dasha kab badlega")
  - Explanations of generated reports
  - General astrology facts
  - Conversational
```

---

---

### 22.1 Backend Files (Implemented ✅)

```
api/services/query_router.py      ← Pass 1: Gemini JSON decision + birth_data extraction
api/services/prompt_builder.py    ← Pass 2: builds final answer prompt (MASTER_PERSONA)
api/services/tool_executor.py     ← runs calc services based on Pass 1 decision
api/services/geo_service.py       ← city → lat/lon/tz (cities.json → Nominatim/OSM → fallback)
api/services/rag_service.py       ← updated: accepts decision + tool_results + kundali_summary
api/routers/chat.py               ← updated: two-pass flow + geo_service + birth_data extraction
api/models/chat.py                ← updated: page_context, page_data, include_user_chart
```

### 22.2 Frontend Files (Implemented ✅)

```
src/components/PageBot.tsx         ← floating bot on every page (bottom-right)
src/hooks/useChat.ts               ← rewritten: pageContext + pageData + store auto-read
src/lib/api.ts                     ← updated: streamChat accepts page_context + page_data
src/lib/cities.ts                  ← updated: searchCities() worldwide via /api/geocode
src/store/kundliStore.ts           ← updated: lat/lon/tz in BirthDetails, setCity action
src/pages/KundliPage.tsx           ← PageBot added (pageContext="kundali")
src/pages/PanchangPage.tsx         ← PageBot added (pageContext="panchang")
src/pages/CompatibilityPage.tsx    ← PageBot added (pageContext="compatibility")
src/pages/SkyPage.tsx              ← PageBot added (pageContext="sky")
src/pages/PalmistryPage.tsx        ← PageBot added (pageContext="palmistry")
src/pages/HoroscopePage.tsx        ← PageBot added (pageContext="horoscope")
src/pages/OnboardingPage.tsx       ← saves lat/lon/tz to store on success
```

### 22.3 Pass 1 — Decision Engine (query_router.py) ✅ LIVE

Gemini returns structured JSON — no hardcoded keyword rules.
Also extracts birth_data from current message + conversation history:

```python
PASS1_PROMPT = """
You are the decision engine for Brahm AI — a Vedic astrology AI.

User query: "{query}"
Conversation history (last 6 messages): {history_summary}
Page context: {page_context}
Kundali available: {has_kundali}
Page data keys: {page_data_keys}

Return ONLY valid JSON (no explanation):
{{
  "intent": "one line: what user actually wants",
  "query_type": "CONVERSATIONAL|SIMPLE_FACT|DEEP_VEDIC|CHART_ANALYSIS|REPORT_ANALYSIS|RECOMMENDATION",
  "needs_calculation": true or false,
  "calc_services": [],
  "needs_rag": true or false,
  "kundali_focus": [],
  "response_depth": "basic|deep|master",
  "response_lang": "hi|en|sa",
  "birth_data": {{
    "date": "YYYY-MM-DD or null",   ← extracted from message OR history
    "time": "HH:MM or null",        ← 24h format
    "place": "city name or null",
    "name": "person name or null"
  }}
}}
"""

# birth_data rules:
# - Scan BOTH current query AND history (carries forward if seen before)
# - date: any format → YYYY-MM-DD
# - time: any format (8:25 PM, raat 8 baje) → HH:MM (24h)
# - needs_calculation=true only if birth_data has at least date+time+place
```

### 22.4 Tool Executor (tool_executor.py)

Runs actual Python calculation services based on Pass 1 JSON:

```python
async def execute_tools(decision: dict, user_birth_data: dict, page_data: dict) -> dict:
    results = {}

    if "dasha" in decision.get("calc_services", []):
        results["dasha"] = kundali_service.get_dasha_timeline(user_birth_data)

    if "panchang" in decision.get("calc_services", []):
        results["panchang"] = panchang_service.get_panchang(
            lat=user_birth_data.get("lat", 28.6),
            lon=user_birth_data.get("lon", 77.2),
            tz=5.5
        )

    if "muhurta" in decision.get("calc_services", []):
        event = decision["calc_params"].get("event", "general")
        results["muhurta"] = muhurta_service.get_muhurta(event=event, **user_birth_data)

    if "compatibility" in decision.get("calc_services", []):
        # page_data already has compatibility result — no re-calculation needed
        results["compatibility"] = page_data

    if "kundali" in decision.get("calc_services", []):
        results["kundali"] = kundali_service.calc_kundali(user_birth_data)

    return results
```

### 22.5 Kundali Compression

Never send raw kundali to Gemini. Send compressed summary:

```python
def compress_kundali(kundali: dict) -> dict:
    """Extract only what matters for reasoning."""
    planets = kundali.get("planets", {})
    dashas = kundali.get("dashas", [])
    current_dasha = next((d for d in dashas if d.get("is_current")), {})
    yogas = [y.get("name") for y in kundali.get("yogas", [])[:5]]

    return {
        "lagna": kundali.get("lagna", {}).get("rashi", "?"),
        "moon_rashi": planets.get("Chandra", {}).get("rashi", "?"),
        "sun_rashi": planets.get("Surya", {}).get("rashi", "?"),
        "current_dasha": f"{current_dasha.get('planet','?')} until {current_dasha.get('end','?')}",
        "key_planets": {
            name: f"{p.get('rashi','?')} {p.get('house','?')}H {'(vakri)' if p.get('retro') else ''}"
            for name, p in planets.items()
        },
        "yogas": yogas,
        "asc_degree": kundali.get("lagna", {}).get("degree", "?"),
    }
```

### 22.6 Pass 2 — Master Answer Prompt (prompt_builder.py) ✅ LIVE

```python
MASTER_PERSONA = """
Tum Brahm AI ho — ek sampurna Vedic jyotishi aur aadhunik calculator ka sangam.

Tumhare paas yeh sab available hai:
- User ka kundali data (agar diya gaya)
- Fresh calculation results (agar run kiye)
- Page ka current data (jo user dekh raha hai)
- Vedic books ka context (sirf jab relevant)

Jawab dene ka structure (hamesha follow karo):
1. Seedha point — user kya jaanna chahta hai, woh pehle bolo
2. Astrological karan — specific graha, house, dasha ka naam lo
3. Timing — exact period ya year agar possible ho
4. Reality check — honest bolo, darawna nahi, hopeful bhi nahi agar sach nahi
5. Upay — sirf agar user ne manga ho ya critical situation ho

Style rules:
- Generic mat bolo — "Shani aapke 7th house mein hai" jaise specific bolo
- Sanskrit terms use karo lekin TURANT Hindi mein explain karo
- Compassionate lekin honest
- Max 350 words — jab tak user ne detail na manga ho
- Kabhi false prediction nahi — agar data nahi toh honestly kaho
"""

def build_pass2_prompt(
    query: str,
    intent: str,
    tool_results: dict,
    kundali_summary: dict,
    page_context: str,
    page_data: dict,
    rag_docs: list,
    language: str,
) -> str:
    # Assemble all context
    calc_section = format_tool_results(tool_results) if tool_results else ""
    kundali_section = format_kundali_summary(kundali_summary) if kundali_summary else ""
    page_section = format_page_data(page_context, page_data) if page_data else ""
    rag_section = format_rag_docs(rag_docs) if rag_docs else ""
    lang_prefix = {"hi": "हिंदी में जवाब दो।", "en": "Answer in English.", "sa": "संस्कृत में।"}.get(language, "हिंदी में जवाब दो।")

    return f"""{MASTER_PERSONA}

{lang_prefix}

{kundali_section}
{page_section}
{calc_section}
{rag_section}

User ne poocha: {query}
Intent: {intent}

Ab is sab ke basis pe expert jawab do."""
```

### 22.7 Response Quality — Before vs After

```
❌ BEFORE (old system):
User: "hi"
AI: "According to Brihat Parashara Hora Shastra, greetings are..."

✅ AFTER (new system):
User: "hi"
AI: "Pranaam! 🙏 Aaj main aapki kya seva kar sakta hoon?"

---

❌ BEFORE:
User: "meri shadi kab hogi"
AI: "According to classical texts, marriage timing depends on..."

✅ AFTER (with kundali + dasha calculation):
AI: "Aapki kundali dekhi — 7th house mein Shukra hai jo vivah ke liye bahut shubh hai.
     Abhi Shani mahadasha chal rahi hai (2031 tak).

     Sabse shubh vivah period: 2027-2028
     Karan: Jupiter aapke 7th house ko aspect karega,
     aur Shani antardasha mein stability aayegi.

     2026 November-December bhi dekh sakte hain —
     Shukra strong position mein hai."

---

❌ BEFORE:
User: "yeh 28/36 score achha hai?"
AI: "28/36 score means good compatibility..."

✅ AFTER (with page_data — compatibility result):
AI: "28/36 — Uttam Maitri hai, lekin Nadi Dosha hai jo serious hai.

     Strong hai: Graha Maitri 5/5 — mental compatibility excellent
     Weak hai: Nadi 0/8 — swasthya + santaan mein dhyan rakhna

     Nadi Dosha ka upay:
     1. Mahamrityunjaya mantra — 21 din, 108 baar daily
     2. Vivah se pehle Nadi Nivarana puja — Kashi mein best

     Overall: Vivah ho sakta hai, upay zaruri hain."
```

### 22.8 Frontend — PageBot Component

**Har page pe floating bot — bottom-right corner:**

```typescript
// Usage on each page:
<PageBot
  pageContext="compatibility"
  pageData={compatibilityResult}    // current page data auto-sent
  includeUserChart={false}          // true for kundali/timeline/sky pages
/>

// Suggested questions shown in bot based on pageContext:
PAGE_SUGGESTIONS = {
  kundali:       ["Sabse strong graha kaun hai?", "Agle saal kaisa rahega?", ...],
  panchang:      ["Aaj ki tithi ka mahatva?", "Rahukaal mein kya nahi karna?", ...],
  compatibility: ["Yeh score achha hai?", "Nadi dosha ke upay?", ...],
  sky:           ["Aaj ke graha mujhpe kaisa asar karenge?", ...],
  palmistry:     ["Meri jeewan rekha kaisi hai?", ...],
}
```

### 22.9 Auto-Analysis on Report Generation

```typescript
// CompatibilityPage.tsx — jab result aata hai, auto-analyze:
useEffect(() => {
  if (result && !autoAnalyzed) {
    pageBot.sendMessage("__auto_analyze__", {
      pageContext: "compatibility",
      pageData: result
    })
    setAutoAnalyzed(true)
  }
}, [result])
```

Backend pe `__auto_analyze__` trigger detect hota hai aur full report analysis prompt build karta hai.

### 22.9b Geo Service (geo_service.py) ✅ LIVE

City name → (lat, lon, tz_offset) with worldwide coverage:

```
Priority chain:
  1. cities.json       → 730 Indian cities, instant lookup (lowercase match + partial match)
  2. Nominatim (OSM)   → worldwide, free, no API key, geopy>=2.4.0
  3. timezonefinder    → gets correct UTC offset from lat/lon (offline, timezonefinder>=6.5.0)
  4. Delhi fallback    → (28.6139, 77.2090, 5.5) if everything fails

Installed on VM: pip install geopy timezonefinder
Results cached in memory for session.
```

### 22.10 Calculation Services Available (Tool Executor)

| Service | Method | Used For |
|---------|--------|----------|
| `kundali_service` | `calc_kundali(birth_data)` | Full chart D-1, houses, planets ✅ |
| `kundali_service` | `get_dasha_timeline(birth_data)` | Vimshottari dasha periods ✅ |
| `kundali_service` | `calc_navamsha(birth_data)` | D-9 chart for marriage analysis ⬜ |
| `panchang_service` | `get_panchang(lat,lon,tz,date)` | Today's tithi, nakshatra, yoga ✅ |
| `panchang_service` | `get_current_transits(birth_data)` | Gochar — current planets over natal ⬜ |
| `muhurta_service` | `get_muhurta(event,date_range,...)` | Best dates for events ✅ |
| `compatibility_service` | already in `page_data` | No re-calc needed ✅ |
| `grahan_service` | `get_upcoming_grahan(year)` | Eclipse dates ✅ |

### 22.11 Implementation Order — Step by Step

```
Phase 1 — Backend ✅ COMPLETE (2026-03-20):
  [1] ✅ tool_executor.py — compress_kundali + execute_tools
  [2] ✅ query_router.py — Pass 1 JSON + birth_data extraction from history
  [3] ✅ prompt_builder.py — MASTER_PERSONA + build_pass2_prompt
  [4] ✅ rag_service.py — generate_stream with decision/tool_results/kundali_summary
  [5] ✅ chat.py — full two-pass + geo_service + birth_data priority chain
  [6] ✅ geo_service.py — worldwide geocoding (cities.json → Nominatim/OSM → timezonefinder)
  [7] ✅ cities.py — /api/geocode endpoint added
  [8] ✅ requirements.txt — geopy + timezonefinder installed on VM

Phase 2 — Frontend ✅ COMPLETE (2026-03-20):
  [9]  ✅ api.ts — streamChat accepts page_context + page_data
  [10] ✅ useChat.ts — rewritten: store auto-read, localStorage persist, page context
  [11] ✅ kundliStore.ts — lat/lon/tz in BirthDetails, setCity action
  [12] ✅ PageBot.tsx — floating bot with page suggestions + streaming
  [13] ✅ OnboardingPage — saves city coords to store
  [14] ✅ cities.ts — searchCities() worldwide

Phase 3 — Page Integration ✅ COMPLETE (2026-03-20):
  [15] ✅ KundliPage, PanchangPage, SkyPage — PageBot added
  [16] ✅ CompatibilityPage, PalmistryPage, HoroscopePage — PageBot added

Phase 4 — AI Intelligence Improvements (Next):
  [17] ⬜ Gochar service — current planetary transits over natal chart
         → Add to tool_executor.py + panchang_service.py
         → Always trigger for CHART_ANALYSIS / PREDICTION queries
         → Critical for timing predictions (Jupiter/Saturn/Rahu positions)

  [18] ⬜ Navamsha D-9 chart
         → Add calc_navamsha() to kundali_service.py
         → Include D-9 lagna + D-9 7th house in compress_kundali()
         → Required for marriage, spouse, dharma predictions

  [19] ⬜ Language style matching
         → Pass 1 already detects user_language_style (hi/en/hinglish)
         → Pass 2 prompt_builder must enforce it — user Hinglish → AI Hinglish
         → Add style_instruction per detected language to build_pass2_prompt()

  [20] ⬜ Fact-sheet memory
         → Replace raw localStorage 30-msg log with compact JSON fact sheet
         → Keys: name, dob, place, key_questions[], key_predictions[]
         → AI can say "aapne pehle bataya tha..." across sessions

  [21] ⬜ Confidence level in responses
         → Add to MASTER_PERSONA: "Agar dasha + gochar agree karte hain → high confidence"
         → "Agar sirf natal chart se → medium confidence"

  [22] ⬜ Auto-analysis trigger on compatibility report generation
  [23] ⬜ AIChatPage.tsx — update to new useChat with pageContext="general"

VM Deploy: git push → VM: cd ~/books && git pull origin main && sudo systemctl restart brahm-api.service
Frontend:  pnpm run build && sudo cp -r dist/* /var/www/brahm-ai/
```

---

## 23. AI Improvement Analysis (2026-03-20)

### What's Missing — Priority Order

#### 🔴 Critical (implement next)

**Gochar (Transits)**
- Current state: Predictions based only on natal chart (static)
- Missing: Where is Jupiter/Saturn/Rahu TODAY relative to natal positions?
- Impact: "Shaadi kb hogi" → needs to know when Jupiter aspects 7th house in transit
- Fix: `panchang_service.get_current_transits(natal_chart)` → compare current planets to natal

**Navamsha D-9**
- Current state: Only D-1 Rashi chart used
- Missing: D-9 is compulsory for marriage, D-10 for career in Vedic
- Fix: Add `calc_navamsha()` to kundali_service → include in compress_kundali

**Language Style Matching**
- Current state: AI always replies in formal Hindi regardless of user style
- Missing: User writes Hinglish → AI should write Hinglish
- Fix: Pass 1 already detects `response_lang` — enforce in Pass 2 prompt

#### 🟡 Important (phase 2)

**Fact-sheet Memory**
- Replace 30 raw messages in localStorage with structured fact extraction
- `{name, dob, city, questions_asked:[], predictions_given:[]}`
- Cross-session continuity: "aapne 5 chats pehle bataya tha..."

**Confidence Score**
- AI overclaims — "aapki shaadi 2027 mein hogi" without caveats
- Should say: "high confidence (dasha + gochar agree)" vs "medium (only natal)"

#### ❌ Skip for now
- Shadbala / Ashtakavarga (marginal improvement, very complex)
- Birth-time rectification (separate large feature)
- DB-based cross-device memory (no user auth DB yet)
- Verifier/checker layer (overkill)

*Section 22-23 Updated: 2026-03-20 | Brahm AI v4.0*
