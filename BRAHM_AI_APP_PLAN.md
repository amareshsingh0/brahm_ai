# Brahm AI — Mobile App Development Plan
> Decided: 2026-03-21 | Status: Planning Complete, Development Not Started

---

## Overview

Brahm AI ka ek fully native mobile app banana hai — Android (Java) aur iOS (Swift) dono ke liye. Website aur app dono same FastAPI backend use karenge. Koi backend change nahi hoga.

```
Backend (FastAPI — Google Cloud VM)
         brahmasmi.bimoraai.com/api
              │            │            │
        Android App    iOS App      Website
          (Java)        (Swift)    (React Vite)
        Play Store     App Store   brahmasmi.bimoraai.com
```

---

## Decision — Why Native?

| Option | Performance | Stability | Effort |
|--------|------------|-----------|--------|
| Capacitor (WebView) | ❌ Janky | OK | Low |
| React Native | ✅ Good | OK | Medium |
| **Java + Swift (Native)** | ✅✅ Best | ✅✅ Best | High |

**Native chose kiya kyunki:**
- Smoothest 60/120fps experience
- Full Android/iOS API access
- Camera (Palmistry), Haptics, Push Notifications — full support
- Play Store + App Store — no rejection risk
- Long-term stable — no framework dependency issues

---

## Tech Stack

### Android — Java
| Layer | Technology |
|-------|-----------|
| Language | **Java 17 LTS** (records, sealed classes, text blocks, pattern matching instanceof) |
| IDE | Android Studio Hedgehog+ |
| UI | XML Layouts + Material Design 3 |
| HTTP | Retrofit2 + OkHttp3 |
| SSE (AI Chat) | OkHttp EventSource |
| Charts | MPAndroidChart + Custom Canvas (Kundali wheel) |
| Images | Glide |
| Local Storage | Room DB + SharedPreferences |
| Navigation | Jetpack Navigation Component |
| Async | ExecutorService + LiveData |
| Push | Firebase Cloud Messaging (FCM) |
| Auth | OTP via backend (same as web) |

### iOS — Swift
| Layer | Technology |
|-------|-----------|
| Language | **Swift 5.9** / Xcode 15 (async/await, @Observable, mature SwiftUI) |
| IDE | Xcode 15+ (Mac required) |
| UI | SwiftUI |
| HTTP | URLSession + async/await |
| SSE (AI Chat) | URLSession bytes stream |
| Charts | Swift Charts + Custom Canvas |
| Local Storage | UserDefaults + CoreData |
| Navigation | NavigationStack (SwiftUI) |
| Push | APNs + Firebase |
| Auth | OTP via backend (same as web) |

### Shared (Dono Apps)
```
API Base URL  : https://brahmasmi.bimoraai.com/api
Auth System   : Same OTP backend
All Data      : Same FastAPI endpoints
AI Chat       : Same /api/chat SSE endpoint
```

---

## App Structure — All Screens

```
📱 Brahm AI
│
├── 🔐 Auth Flow
│   ├── SplashScreen          (logo + loading)
│   ├── LoginScreen           (phone + OTP)
│   └── OnboardingScreen      (name, DOB, time, city)
│
├── 📲 Main App (Bottom Navigation — 5 tabs)
│   ├── 🏠 Home               (Dashboard — quick stats, chart preview, guidance)
│   ├── ⭐ Kundali            (7 tabs: Chart, Planets, Dashas, Yogas, etc.)
│   ├── 🤖 Chat               (AI Chat — streaming SSE)
│   ├── 📅 Today              (Panchang — tithi, nakshatra, rahukaal)
│   └── 👤 Profile            (account, birth details, plan)
│
└── 📑 Secondary Screens (via navigation)
    ├── GocharScreen          (planetary transits)
    ├── CompatibilityScreen   (kundali milan)
    ├── HoroscopeScreen       (daily rashi)
    ├── MuhurtaScreen         (shubh muhurta finder)
    ├── SadeSatiScreen        (sade sati calculator)
    ├── DoshaScreen           (manglik/dosha analysis)
    ├── GemstoneScreen        (gemstone recommendations)
    ├── KPScreen              (KP system sub-lords)
    ├── PrashnaScreen         (prashna kundali)
    ├── VarshpalScreen        (varshphal solar return)
    ├── RectificationScreen   (birth time rectification)
    ├── PalmistryScreen       (camera + AI palm reading)
    ├── NakshatraScreen       (nakshatra explorer)
    ├── RashiScreen           (rashi explorer)
    ├── YogasScreen           (yoga library)
    ├── RemediesScreen        (remedies)
    ├── TimelineScreen        (dasha timeline)
    ├── SkyScreen             (live sky / planet positions)
    ├── LibraryScreen         (vedic library search)
    ├── MantraScreen          (mantra dictionary)
    └── SubscriptionScreen    (plans + payment)
```

---

## Phase Plan — Android First

### Phase 1 — Foundation + MVP (Week 1)
> **Goal:** Working app with core screens, testable on device

- [ ] Android Studio project setup
  - Package: `com.bimoraai.brahm`
  - Language: Java 17 LTS
  - Min SDK: API 26 (Android 8.0) — 95%+ devices
  - Target SDK: API 35 (Android 15)
- [ ] Project structure (MVVM architecture)
- [ ] Retrofit2 + OkHttp setup (API client)
- [ ] SharedPreferences (auth token, birth details)
- [ ] Firebase setup (google-services.json)
- [ ] Bottom Navigation (5 tabs)
- [ ] SplashScreen
- [ ] LoginScreen (OTP flow)
- [ ] OnboardingScreen (birth details + city search)
- [ ] HomeScreen (Dashboard)
- [ ] TodayScreen (Panchang)
- [ ] ProfileScreen

**Deliverable:** APK install karke login → dashboard → panchang dekh sako

---

### Phase 2 — Core Features (Week 2)
> **Goal:** AI Chat + Kundali working

- [ ] ChatScreen — SSE streaming AI responses
  - OkHttp EventSource
  - Message bubbles UI
  - Suggested queries chips
  - Confidence badge
- [ ] KundaliScreen — 7 tabs
  - Chart tab (custom Canvas — Kundali wheel)
  - Planets tab (table)
  - Dashas tab (timeline)
  - Yogas tab (cards)
  - Alerts tab
  - Shadbala tab
  - Navamsha tab
- [ ] GocharScreen (planetary transits)
- [ ] HoroscopeScreen (daily rashi)

**Deliverable:** Full kundali + AI chat working

---

### Phase 3 — All Remaining Screens (Week 3)
> **Goal:** Feature parity with website

- [ ] CompatibilityScreen (kundali milan)
- [ ] MuhurtaScreen (activity finder)
- [ ] SadeSatiScreen
- [ ] DoshaScreen (manglik)
- [ ] GemstoneScreen
- [ ] KPScreen (sub-lords table)
- [ ] PrashnaScreen
- [ ] VarshpalScreen
- [ ] RectificationScreen
- [ ] PalmistryScreen (camera + Gemini Vision)
- [ ] NakshatraScreen
- [ ] RashiScreen
- [ ] TimelineScreen (dasha chart)
- [ ] SkyScreen
- [ ] LibraryScreen
- [ ] MantraScreen
- [ ] SubscriptionScreen

**Deliverable:** All features working on Android

---

### Phase 4 — Polish + Play Store (Week 4)
> **Goal:** Production ready + Store listing

- [ ] FCM Push Notifications
  - Daily Rahu Kaal alert
  - Daily horoscope notification
- [ ] App icon (512×512 + adaptive icon)
- [ ] Splash screen (branded)
- [ ] Dark theme (match website)
- [ ] Offline handling (no internet screen)
- [ ] Error states on all screens
- [ ] Loading skeletons
- [ ] ProGuard / R8 rules
- [ ] Signed APK → AAB (Android App Bundle)
- [ ] Play Store listing
  - App name, description (Hindi + English)
  - Screenshots (phone + tablet)
  - Feature graphic
- [ ] Submit for review (3-7 din)

**Deliverable:** App live on Play Store ✅

---

### Phase 5 — iOS Port in Swift (Week 5-6)
> **Goal:** Same app in SwiftUI for iPhone

- [ ] Xcode project setup (Mac required)
- [ ] Bundle ID: `com.bimoraai.brahm`
- [ ] SwiftUI navigation structure
- [ ] URLSession API client
- [ ] SSE streaming (AI Chat)
- [ ] All screens ported from Android design
- [ ] Swift Charts (Kundali wheel)
- [ ] APNs push notifications
- [ ] TestFlight beta
- [ ] App Store submission

**Deliverable:** App live on App Store ✅

---

## Android Project Structure (MVVM)

```
app/
├── src/main/
│   ├── java/com/bimoraai/brahm/
│   │   ├── api/
│   │   │   ├── ApiClient.java          ← Retrofit setup
│   │   │   ├── ApiService.java         ← all endpoints
│   │   │   └── SseManager.java         ← OkHttp SSE for AI chat
│   │   ├── model/
│   │   │   ├── KundaliData.java
│   │   │   ├── PanchangData.java
│   │   │   ├── ChatMessage.java
│   │   │   └── UserProfile.java
│   │   ├── repository/
│   │   │   ├── KundaliRepository.java
│   │   │   ├── ChatRepository.java
│   │   │   └── PanchangRepository.java
│   │   ├── viewmodel/
│   │   │   ├── KundaliViewModel.java
│   │   │   ├── ChatViewModel.java
│   │   │   └── PanchangViewModel.java
│   │   ├── ui/
│   │   │   ├── auth/
│   │   │   │   ├── LoginActivity.java
│   │   │   │   └── OnboardingActivity.java
│   │   │   ├── main/
│   │   │   │   └── MainActivity.java   ← bottom nav host
│   │   │   ├── home/
│   │   │   │   └── HomeFragment.java
│   │   │   ├── kundali/
│   │   │   │   ├── KundaliFragment.java
│   │   │   │   └── KundaliChartView.java ← custom Canvas
│   │   │   ├── chat/
│   │   │   │   └── ChatFragment.java
│   │   │   ├── today/
│   │   │   │   └── TodayFragment.java
│   │   │   └── profile/
│   │   │       └── ProfileFragment.java
│   │   └── utils/
│   │       ├── PrefsHelper.java        ← SharedPreferences wrapper
│   │       └── DateUtils.java
│   └── res/
│       ├── layout/                     ← XML layouts
│       ├── navigation/                 ← nav_graph.xml
│       ├── values/                     ← colors, strings, themes
│       └── drawable/                   ← icons, assets
├── build.gradle                        ← dependencies
└── google-services.json                ← Firebase config
```

---

## Key API Endpoints (already working)

```
POST /api/chat              ← AI Chat (SSE stream)
POST /api/kundali           ← Birth chart
GET  /api/panchang          ← Today's panchang
GET  /api/gochar            ← Current transits
POST /api/gochar/analyze    ← Personal transit analysis
GET  /api/horoscope/{rashi} ← Daily horoscope
POST /api/compatibility     ← Kundali milan
POST /api/muhurta/activity  ← Muhurta finder
GET  /api/planets/now       ← Live sky data
POST /api/palmistry         ← Palm analysis (Gemini Vision)
POST /api/kp                ← KP system
POST /api/prashna           ← Prashna kundali
POST /api/varshphal         ← Varshphal
POST /api/rectification     ← Birth time rectification
GET  /api/sade-sati         ← Sade sati calculator
GET  /api/dosha             ← Dosha analysis
GET  /api/gemstones         ← Gemstone recommendations
GET  /api/grahan            ← Eclipse calendar
GET  /api/festivals         ← Festival calendar
GET  /api/search            ← Vedic library / mantra / knowledge search
GET  /api/cities            ← City search (lat/lon/tz lookup)
POST /api/auth/send-otp     ← OTP login
POST /api/auth/verify-otp   ← OTP verify → JWT token
GET  /api/user/me           ← User profile
PATCH /api/user/me          ← Update profile / birth details
```

---

## Pre-requisites Checklist

### Android (Phase 1-4)
- [ ] Android Studio installed (latest stable)
- [ ] Java 17 JDK configured in Android Studio
- [ ] Android emulator OR physical device (Android 8+)
- [ ] Firebase project created → `google-services.json` downloaded
- [ ] Google Play Developer account ($25 one-time)
- [ ] Package name decided: `com.bimoraai.brahm`

### iOS (Phase 5-6)
- [ ] Mac with macOS Sonoma+
- [ ] Xcode 15+ installed
- [ ] Apple Developer account ($99/year)
- [ ] iPhone for testing

---

## Design Guidelines

```
Theme:        Dark (match website — cosmic dark theme)
Primary:      #7C3AED (purple — same as web)
Background:   #09090B (zinc-950)
Surface:      #18181B (zinc-900)
Text:         #FAFAFA (zinc-50)
Accent:       #F59E0B (amber — star-gold)
Font:         Poppins (same as web)
Corner Radius: 12dp (rounded-xl equivalent)
```

---

## Notes

- Android pehle banayenge, iOS baad mein
- Both apps same backend — zero backend changes
- Kundali wheel ka custom Canvas draw karna hoga (Java + Swift dono mein)
- AI Chat SSE streaming — dono platforms pe alag implementation
- Play Store beta testing pehle karenge (internal testing track)
- Firebase ek hi project mein dono apps add kar sakte hain

---

*Last updated: 2026-03-21*
