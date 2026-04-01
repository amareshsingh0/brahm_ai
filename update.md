# Brahm AI — Update Log

## Latest Updates (2026-03-31)

### Android App

#### Auth / Security
- **Friendly network errors**: Connection failures (timeout, no internet) no longer show raw IP addresses, ports, or URLs. Users see clean messages like "Unable to connect. Please check your internet connection."

#### Muhurta — Date Range Picker
- Replaced plain text fields (`YYYY-MM-DD`) with proper **date picker dialogs** (same calendar UI used in birth input fields)
- From / To fields show formatted date (e.g. `23 Apr 2025`) and open a calendar on tap
- Gold accent colors consistent with app theme

#### ChatBot FAB Position
- Fixed `PageBotFab` appearing too low on all pages — added `navigationBarsPadding()` so it respects system gesture bar / nav buttons on all devices

#### Scroll-to-Top Button — Full Logic Rewrite
- **Was broken**: used `lastScrolledBackward` which stays `true` after scroll stops, causing button to stay visible indefinitely
- **Fixed**: `snapshotFlow` watches every scroll frame — shows only when actively scrolling UP, hides instantly when reaching top
- **Auto-hides in 0.5s** after scroll stops (was 1.5s)
- Position baked into component with `navigationBarsPadding()` — all 23 call sites simplified to `Modifier.align(Alignment.BottomEnd)`

---

### Website

#### PageBot — All Pages Registered
Added `useRegisterPageBot` hook to all 22 feature pages so the AI chatbot knows the page context:
- `KundliPage`, `HoroscopePage`, `CompatibilityPage`, `GocharPage`, `KPPage`
- `SadeSatiPage`, `DoshaPage`, `GemstoneRecommendationsPage`, `RectificationPage`, `PrashnaPage`
- `VarshpalPage`, `PalmistryPage`, `RashiExplorer`, `NakshatraExplorer`, `YogasPage`
- `RemediesPage`, `SkyPage`, `MantraDictionaryPage`, `GotraFinderPage`, `MuhurtaPage`, `CalendarPage`, `GrahanPage`

Pages with kundali data pass relevant chart data as context; others pass `{}`.

#### Auth — Friendly Network Errors
- `sendOtp` / `verifyOtp` in `useAuth.ts` now wrap `fetch()` in try/catch and throw clean messages instead of raw browser errors (which included server URL and port)

#### Scroll-to-Top Button
- Position lowered: `bottom-36` → `bottom-24` on mobile, `md:bottom-20` → `md:bottom-16` on desktop
- Same 0.5s auto-hide logic: shows on scroll-up only, hides immediately on scroll-down or at top

---

### Previous Session

#### Android App
- **Global auth guard** in `AppNavHost.kt` — reactive `collectAsState` on access token, redirects to Login when token cleared
- **Token clear on refresh fail** in `ApiClient.kt` — `runBlocking { tokenDataStore.clear() }` triggers auth guard
- **ChatScreen header removed** — duplicate "Brahm AI / ONLINE" header removed from chat screen
- **SmartToy icon fix** — replaced `Icons.Default.SmartToy` (requires extended lib) with `Icons.Default.Android` for PageBotFab
- **Extra bottom margin fix** — removed duplicate Scaffold `floatingActionButton` from GocharScreen, KPScreen, SadeSatiScreen, VarshpalScreen

#### Website
- **Pricing section removed** from landing page
- **Footer simplified** — only logo + tagline, all nav links removed
- **All pages protected** — `/rashi`, `/nakshatra`, `/horoscope`, `/stories`, `/panchang`, `/grahan`, `/calendar` require login
- **Header fixed** — no longer scrolls with content (`h-screen` + `flex-shrink-0` layout)
- **AIChatPage full rewrite** — history drawer, pin/archive/rename/delete, copy + regenerate buttons on AI messages
- **PageBot on all pages** — `pageBotStore` + `useRegisterPageBot` hook, excluded from dashboard/panchang/stories/library/chat
