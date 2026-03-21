# Brahm AI — Full Architecture Document
# Last Updated: 2026-03-21 (v5.0 — Native Mobile Apps Added)

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
| **Android** | Java 17 LTS + MVVM + Retrofit2 | Google Play Store | 🔜 Phase 1-4 |
| **iOS** | Swift 5.9 + SwiftUI + URLSession | Apple App Store | 🔜 Phase 5-6 |

All three platforms share the **same FastAPI backend** — zero backend changes needed for mobile.

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

#### Core Feature Endpoints (unchanged)
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

#### Admin Endpoints (NEW — admin JWT only)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/admin/users` | Admin JWT | List all users with plan/usage |
| GET | `/api/admin/users/{user_id}` | Admin JWT | Single user full profile |
| PATCH | `/api/admin/users/{user_id}` | Admin JWT | Update user plan/status |
| GET | `/api/admin/subscriptions` | Admin JWT | All active subscriptions |
| GET | `/api/admin/stats` | Admin JWT | MAU, revenue, feature usage |

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

## 17. DEPLOYMENT ARCHITECTURE (UPDATED)

```
VM: 34.135.70.190
│
├── Port 7860 — Gradio (fallback)
│   pm2: brahm-gradio
│
├── Port 8000 — FastAPI (production API)
│   pm2: brahm-api
│   cmd: uvicorn api.main:app --host 0.0.0.0 --port 8000 --workers 1
│
└── Port 3000 — React static (optional)
    serve dist/ via nginx or python3 -m http.server

GCloud Firewall:
  brahm-api:       tcp:8000
  brahm-gradio:    tcp:7860

Environment vars on VM (.env file in ~/books/api/):
  JWT_SECRET=<64-char random>
  CASHFREE_APP_ID=<from Cashfree dashboard>
  CASHFREE_SECRET=<from Cashfree dashboard>
  MSG91_AUTH_KEY=<from MSG91>          (for OTP SMS)
  FIREBASE_SERVER_KEY=<from Firebase>  (for push notifications)
  ADMIN_PHONE=+91XXXXXXXXXX            (your phone — gets role='admin')

Production domain (future):
  brahmai.app → nginx reverse proxy → 8000
  www.brahmai.app → serve React dist/
  api.brahmai.app → FastAPI

App Store:
  Bundle ID: ai.brahm.app
  Deployment: capacitor build → Xcode/Android Studio → App Store Connect / Play Console
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
| Storage | localStorage | JWT, birth details |
| Payments | Cashfree JS SDK | In-app checkout |

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
| Auth | python-jose (JWT) | HS256 |
| OTP | MSG91 / Firebase Auth | SMS OTP |
| Payments | Cashfree Payments API | Webhooks |
| LLM | Qwen2.5-7B-Instruct | 4-bit nf4 |
| Embedding | paraphrase-multilingual-MiniLM-L12-v2 | 384 dim |
| Reranker | cross-encoder/ms-marco-MiniLM-L-6-v2 | |
| Vector DB | FAISS HNSW | 1.1M chunks |
| Keyword | rank_bm25 | |
| Astronomy | pyswisseph | |
| DB | SQLite (dev) / PostgreSQL (prod) | |
| Process | PM2 | |

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
