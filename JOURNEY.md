# StoMo — Development Journey

A narrative log of how the StoMo (Stock Monitor) web app was built out from skeleton to a working,
themed, data-driven dashboard. Written 2026-05-29.

StoMo is a **Spring Boot 4 / Java 21** backend + **React 19 / Vite** frontend that shows a stock
detail dashboard ("Architectural Ledger") with live quotes, an intraday/weekly/monthly chart, a
company panel, a dark↔"Yoda" (light) theme toggle, and an optional Yodish text-translation mode.

---

## 1. Starting point

- **Designs**: two Stitch drafts under `frontend/src/stomo_dark/` and `frontend/src/stomo_yoda_mode/`
  turned out to be the **same screen in two color palettes** → so the deliverable was **one themeable
  component**, not two.
- **"Two REST interfaces for stock prices"** = **Alpha Vantage** + **Finnhub** (keys in `.env`).
- **Backend** was mostly **empty stubs** (controllers/services with no bodies); DTOs existed.
- **Frontend was broken**: `App.jsx` imported a non-existent `Dashboard`, neither **Tailwind** nor
  **lucide-react** was installed, and the ~47 custom color tokens lived only in the Stitch CDN config.

Decision (user): build the **UI + real data fetching**, implementing the empty Spring endpoints.

---

## 2. First working version

**Backend** — gave each REST interface a clear role:
- **Finnhub** (`ReportService`) → real-time quote (`GlobalQuoteDto`, extended with open/high/low/prevClose).
- **Alpha Vantage** (`StockService`) → history, company `OVERVIEW` (`CompanyOverviewDto`), `SYMBOL_SEARCH`.
- `MarketDataService` orchestrates; `MarketDataController` exposes `/api/market/quote|history|overview|search`.
- `GlobalExceptionHandler` maps errors to clean JSON (404 / 502).

**Frontend**:
- Installed Tailwind v3 + PostCSS + autoprefixer + lucide-react.
- **CSS-variable theming**: dark tokens on `:root`, Yoda tokens on `.yoda`, each mapped in
  `tailwind.config.js` as `rgb(var(--token) / <alpha-value>)` so the design's opacity utilities
  (`bg-primary/30`) work. RGB channel values were generated with a throwaway Node script.
- `ThemeProvider` (dark default, toggles `.yoda` on `<html>`, persists to `localStorage`) + `ThemeToggle` (Moon↔Leaf).
- Data layer (`api/marketData.js` + `useMarketData` hook), `Dashboard.jsx`, `PriceChart`, `SymbolSearch`.
- Vite dev proxy `/api` → `http://localhost:8080`.

---

## 3. Gotchas from a bleeding-edge stack (the most useful part to remember)

The project is on very new versions, which caused **five pre-existing breakages** that had to be fixed
before anything ran. None were related to the feature work — they were latent because the scaffold had
never been compiled via Maven or run against a database.

| # | Symptom | Root cause | Fix |
|---|---------|-----------|-----|
| 1 | `package com.fasterxml.jackson.databind does not exist` | **Spring Boot 4 ships Jackson 3** → package is `tools.jackson.*`, and `@JsonProperty` stays `com.fasterxml.jackson.annotation.*` | Parse JSON via `Map<String,Object>` (version-agnostic); inject `tools.jackson.databind.ObjectMapper` where needed |
| 2 | `cannot find symbol: variable log` / missing getters everywhere | **Lombok annotation processor not run** — newer `maven-compiler-plugin` (3.14) no longer auto-detects processors on the classpath | Add Lombok to `<annotationProcessorPaths>` in `pom.xml` |
| 3 | `ClassNotFoundException: org.hibernate.dialect.MySQL8Dialect` | **Hibernate 7 removed `MySQL8Dialect`** | Delete the explicit `spring.jpa.properties.hibernate.dialect` line → Hibernate auto-detects |
| 4 | `mappedBy 'user' which does not exist in PortfolioItem` | `User.portfolioItem` had a `mappedBy` with no inverse field | Add `@ManyToOne User user` to `PortfolioItem` (also makes `PortfolioRepo.findByUserId` valid) |
| 5 | `No property 'username' found for type 'User'` | `UserRepo.findByUsername` referenced a non-existent property (User uses `email`) | Remove the unused method |

> Lesson: on Spring Boot 4 + Hibernate 7 + Jackson 3, verify the backend actually **boots against the DB**
> early — `mvn compile` passing isn't enough (entity-mapping and query-derivation errors only surface at startup).

Verification baseline: `./mvnw compile` → BUILD SUCCESS, `npm run build` → OK, `npm run lint` → clean.

---

## 4. Yoda translation mode (cost-controlled)

The Yoda (yodish) API is meant to translate **all UI text** when "Yoda Mode" is on, but it's metered.

- **Boot unblocked**: `SpeechService` no longer hard-requires `YODA_API_KEY`.
- **Persistent DB cache** (`YodaTranslation`, keyed by the source string) → each distinct phrase hits the
  paid API **at most once, ever**.
- `YodaService.translateBatch` is **cache-first** and **gracefully passes text through untouched** when no
  key is set or a call fails — so the feature is dormant-but-safe until a key lands in `.env`.
- Frontend `<T>` component + `YodaTextProvider` collect visible strings, **debounce them into one batch
  request**, cache in memory, and only translate while Yoda Mode is active (zero calls in dark mode).

---

## 5. "Search returns nothing" — the debugging detour

Symptom: search returned no results. Root cause: **the backend simply wasn't running** (and a bug made
`SymbolSearch` swallow the error silently). Getting it up surfaced gotchas #3–#5 above. After that:

- **Alpha Vantage `1 request/second` burst limit** tripped because the dashboard fired history + overview
  as two simultaneous AV calls → added a **client-side AV throttle** (serialize AV calls ≥1.1s apart;
  Finnhub stays instant).
- Fixed `SymbolSearch` to show a real message ("Backend not reachable…") instead of nothing.
- Brought up MySQL (`docker compose up -d db`) + backend; verified search (Alpha Vantage) and quote
  (Finnhub) **live**.

---

## 6. History caching + the intraday pivot

**First cut (daily):** cached the daily series in the DB (`PriceHistory`), with a **per-symbol lock**
(concurrent requests for the same symbol coalesce into one fetch + one write; symbol is the PK so no
duplicate rows) and **stale-fallback** (serve cached data if Alpha Vantage is rate-limited). One daily
call backed weekly/monthly via server-side resampling.

**Then the pivot to intraday** — and a key discovery:
- **Alpha Vantage `TIME_SERIES_INTRADAY` is a premium endpoint** (verified: returns a "premium endpoint"
  message on the free key). **Finnhub candles are also paid.** So **no free intraday from either**.
- Found that **Yahoo's public chart endpoint is keyless** and returns delayed intraday (verified):
  `https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=30m&range=60d`.
- Switched the history source to **Yahoo**, kept the DB cache + lock + stale-fallback, and moved
  **resampling to the client** (one fetched series → all timeframes, zero requests on timeframe switch).

**Final timeframe model** (per user spec):
- **Intraday (1D)** → **10-minute** candles of the latest day. Yahoo has no 10m interval, so we fetch
  **5m** and aggregate pairs → 10m.
- **Weekly (1W)** → 5 candles/day over 7 days (**35**).
- **Monthly (1M)** → 1 candle/day over 30 days (**30**).
- Backend caches **two series** per symbol: `coarse` (30m, ~60d → weekly/monthly) + `fine` (10m → intraday).
  `/api/market/history/{symbol}` returns both; the client resamples.

Verified live (IBM): coarse 778×30m + fine 184×10m → intraday 28 (10m), weekly 35, monthly 30.

---

## 7. Chart type toggle + persistence

- Added a **candles ⇄ line** toggle in the chart controls.
  - Candles use open/high/low/close + up/down color; line uses **close only** as an SVG path with a
    primary-gradient area fill, sharing the same vertical scale. Color follows the theme.
- The chart-type choice now **persists in `localStorage`** (`stomo-chart-type`), like the theme.

---

## Current architecture

```
React (Vite :5173)  --/api proxy-->  Spring Boot (:8080)  -->  MySQL (Docker :3306)
   |                                      |
   |-- quote      --> MarketDataController --> ReportService      --> Finnhub      (live quote, free)
   |-- history    -->                    --> PriceHistoryService  --> Yahoo        (30m + 10m, keyless, cached in DB)
   |-- overview   -->                    --> StockService         --> Alpha Vantage (OVERVIEW, free, 25/day)
   |-- search     -->                    --> StockService         --> Alpha Vantage (SYMBOL_SEARCH)
   |-- yoda       --> YodaController      --> YodaService          --> RapidAPI yodish (cached in DB)
```

- **Theming**: CSS variables; `.yoda` class on `<html>`; `localStorage: stomo-theme`.
- **Caching**: `price_history` (per-symbol 30m + 10m series, TTL 720 min) and `yoda_translations`
  (per-phrase) — both protect against rate limits and survive restarts.
- **Rate-limit posture**: Finnhub generous; Alpha Vantage **25/day** (search + overview) → throttled
  client-side; Yahoo keyless but **unofficial** (stale-fallback covers transient failures).

## How to run

### Easy mode — full stack in Docker

```bash
docker compose up -d --build       # builds & starts MySQL + backend + frontend
# Open http://localhost:5173
docker compose down                # stop everything
```

`docker-compose.yml` brings up three services:
- `db` (MySQL 8) on :3306
- `backend` (Spring Boot, image built from the project root `Dockerfile`) on :8080,
  with API keys loaded via `env_file: .env`
- `frontend` (Vite build served by nginx, image from `frontend/Dockerfile`) on :5173,
  with nginx forwarding `/api/*` to the `backend` service inside the compose network

After code changes, rebuild the relevant image: `docker compose up -d --build backend`
(or `... --build frontend`).

### Dev mode — Docker just for MySQL, run backend + frontend natively (hot reload)

```bash
# 1. Database only
docker compose up -d db

# 2. Backend (needs ALPHAVANTAGE_API_KEY + FINNHUB_API_KEY on the env; YODA_API_KEY optional)
set -a && source .env && set +a && ./mvnw spring-boot:run     # :8080

# 3. Frontend dev server (separate terminal)
cd frontend && npm run dev                                    # :5173, proxies /api to :8080
```

Use this when you're actively editing — Vite's HMR + Spring DevTools restart on save.

## Key files

- Backend services: `src/main/java/com/dhbw/webeng2/stomo/service/` — `ReportService` (Finnhub),
  `StockService` (Alpha Vantage), `YahooService` (intraday), `PriceHistoryService` (cache+lock+resample),
  `YodaService` (translation+cache), `MarketDataService` (orchestration).
- Controllers: `controller/MarketDataController`, `controller/YodaController`.
- Frontend: `frontend/src/components/Dashboard.jsx`, `PriceChart.jsx`, `SymbolSearch.jsx`, `ThemeToggle.jsx`,
  `T.jsx`; `frontend/src/theme/` (theme + Yoda providers); `frontend/src/utils/resample.js` (client resampling);
  `frontend/src/api/marketData.js`.

## Known follow-ups / caveats

- Yahoo is an **unofficial** endpoint — fine for a student/analysis app, but it can change or rate-limit;
  Twelve Data (free, official, needs a key) is the swap-in if it gets flaky.
- Company **overview** is still a per-load Alpha Vantage call — could be DB-cached like history to save quota.
- `UserController.getCurrentUser()` has a latent infinite-recursion bug (doesn't block startup; fails if called).
