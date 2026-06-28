# StoMo — Stock Monitor

StoMo is a full-stack stock-monitoring web application. You can research a stock (live quote,
interactive intraday/weekly/monthly price chart, and a company "dossier"), search for symbols,
and curate **multiple personal watchlists** that track performance from the moment you add a
symbol. The backend enriches its own data by calling several external market-data APIs.

Built for the *Web Engineering 2* assignment (DHBW). Theme: **stock monitoring** (not the
lecture's SkyWatch example).

---

## Architecture

```
┌──────────┐      REST/JSON     ┌─────────────┐      HTTPS      ┌──────────────────────┐
│ FRONTEND │ ◄────────────────► │   BACKEND   │ ◄────────────► │  THIRD-PARTY APIs    │
│ (React,  │     /api/**        │ (Spring     │                │  Finnhub · Yahoo ·   │
│  Vite)   │                    │  Boot, JPA) │                │  Alpha Vantage · …   │
└──────────┘                    │      │      │                └──────────────────────┘
                                │      ▼      │
                                │ ┌──────────┐│
                                │ │PostgreSQL││  (H2 in-memory for tests)
                                │ └──────────┘│
                                └─────────────┘
```

- **Frontend** — React 19 + Vite + Tailwind. Communicates **only** with the backend over `/api`.
- **Backend** — Spring Boot 4 (Java 21) with a clean **Controller → Service → Repository**
  layering and Spring Data JPA.
- **Database** — PostgreSQL at runtime; in-memory **H2** for the test suite.
- **Third-party** — the backend aggregates data from Finnhub, Yahoo Finance and Alpha Vantage
  (details below), plus an optional Yodish translation API.

---

## Tech stack

| Component | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4 (Web MVC, Data JPA, Validation, Security / OAuth2 Resource Server) |
| Frontend | React 19, Vite, Tailwind CSS, lucide-react |
| Database | PostgreSQL 16 (runtime), H2 (tests) |
| Build | Maven (`mvnw` wrapper) / npm |
| Auth | JWT (HS256), Spring Security |
| Packaging | Docker + Docker Compose |

---

## Features

- **Stock dashboard** — live quote (price, change, OHLC), a candle/line price chart with
  intraday / weekly / monthly timeframes, and a company dossier (market cap, P/E, dividend
  yield, 52-week range, description).
- **Symbol search** — search by company name or ticker.
- **Authentication** — register / login with hashed passwords (BCrypt) and stateless JWT.
- **Watchlists** — each user can keep **several named watchlists** (full CRUD); add/remove
  symbols, with performance measured from the price captured when the symbol was added.
- **Theming** — dark ↔ "Yoda" theme toggle, plus an optional Yodish UI-text translation mode.

---

## Data model

Three related entities form the core domain, plus two cache tables.

```
User ──1:N──► Watchlist ──1:N──► WatchlistItem
```

| Entity | Purpose | Key relationships |
|---|---|---|
| `User` | Account (email, name, BCrypt password, status) | `@OneToMany` watchlists |
| `Watchlist` | A named list of symbols; unique per (user, name) | `@ManyToOne` user · `@OneToMany` items |
| `WatchlistItem` | A symbol in a list + its start price; unique per (watchlist, symbol) | `@ManyToOne` watchlist |
| `PriceHistory` | Per-symbol cached Yahoo price series (cost control) | — |
| `YodaTranslation` | Per-phrase translation cache (cost control) | — |

Deleting a watchlist cascades to its items; deleting a user cascades to their watchlists.

---

## REST API

Base URL `http://localhost:8080`. All responses are JSON; errors use sensible HTTP status
codes (`400` validation, `401` auth, `404` not found, `409` conflict, `502` upstream) via a
central `@RestControllerAdvice`.

### Auth — `/api/auth`
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/register` | `{firstname,lastname,email,password}` | `201` + JWT; validated (`@Email`, password ≥ 8) |
| POST | `/login` | `{email,password}` | `200` + JWT; `401` on bad credentials |
| GET | `/me` | — | 🔒 current user |

### Market data — `/api/market` (public)
| Method | Path | Notes |
|---|---|---|
| GET | `/quote/{symbol}` | Live quote (Finnhub) |
| GET | `/history/{symbol}` | Cached price series (Yahoo) |
| GET | `/overview/{symbol}` | Company overview (Alpha Vantage) |
| GET | `/search?q={query}` | Symbol search (Alpha Vantage) |

### Watchlists — `/api/watchlists` (🔒 JWT required)
| Method | Path | Notes |
|---|---|---|
| GET | `/` | List the user's watchlists (auto-creates a default if none) |
| GET | `/{id}` | Read one |
| POST | `/` | **Create** `{name}` |
| PUT | `/{id}` | **Update** (rename) `{name}` |
| DELETE | `/{id}` | **Delete** |
| POST | `/{id}/items` | Add a symbol `{symbol}` |
| DELETE | `/{id}/items/{itemId}` | Remove a symbol |

### Yoda translation — `/api/yoda` (public)
| Method | Path | Notes |
|---|---|---|
| GET | `/translate?text=` | Translate one phrase |
| POST | `/translate` | Batch: `{texts:[...]}` → `{source: translated}` |
| GET | `/status` | `{enabled: bool}` (whether a key is configured) |

---

## Running locally

> **First time?** Create your `.env` before starting — see
> [docs/setup.md](docs/setup.md) for installation prerequisites, the full `.env` walkthrough,
> and troubleshooting.
>
> ```bash
> cp .env.example .env     # then set POSTGRES_PASSWORD and JWT_SECRET (≥ 32 chars)
> ```

### Option A — full stack with Docker (recommended)

```bash
cp .env.example .env              # first time only — set POSTGRES_PASSWORD + JWT_SECRET
docker compose up -d --build      # PostgreSQL + backend (:8080) + frontend (:80)
# open http://localhost   (or http://stomo.lab — see the note below)
docker compose down               # stop everything
```

**Nicer URL (optional):** to use `http://stomo.lab` instead of `localhost`, add a hosts entry
`127.0.0.1 stomo.lab` — `C:\Windows\System32\drivers\etc\hosts` on Windows, `/etc/hosts` on
macOS/Linux (needs admin/sudo). No certificate needed; it's plain HTTP.

### Option B — dev mode (PostgreSQL in Docker, app native, hot reload)

```bash
# 1) database only
docker compose up -d db

# 2) backend (:8080)        — Windows: use mvnw.cmd
./mvnw spring-boot:run

# 3) frontend (:5173)
cd frontend && npm install && npm run dev   # proxies /api → :8080
```

### Option C — serve at https://stomo.dev (local HTTPS, optional)

For a production-style URL instead of `localhost`, the stack can run behind nginx TLS at
**https://stomo.dev**, using a locally-trusted [mkcert](https://github.com/FiloSottile/mkcert)
certificate. The default `docker compose up` (Option A) stays on plain HTTP, so this is opt-in.

```bash
# 1) one-time: trust a local CA
mkcert -install

# 2) generate the cert + key into frontend/certs/  (details: frontend/certs/README.md)
cd frontend/certs && mkcert stomo.dev && cd ../..

# 3) map the hostname to localhost (needs admin/sudo):  127.0.0.1 stomo.dev
#    Windows: C:\Windows\System32\drivers\etc\hosts   ·   macOS/Linux: /etc/hosts

# 4) start with the HTTPS override
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d --build
# open https://stomo.dev
```

`.dev` is HSTS-preloaded, so browsers force HTTPS — the mkcert certificate is what lets it load
without warnings.

### Environment variables

Configure these in a `.env` file (copy [`.env.example`](.env.example) — it lists every
variable). Docker Compose loads `.env` automatically. Full walkthrough: [docs/setup.md](docs/setup.md).

| Variable | Used for | Required? |
|---|---|---|
| `POSTGRES_PASSWORD` | database password | **Yes** — app / `docker compose` refuse to start without it |
| `JWT_SECRET` | JWT signing secret (≥ 32 chars) | **Yes for Docker** (the `prod` profile rejects the dev default); optional for native dev |
| `POSTGRES_DB` / `POSTGRES_USER` | database name / user | Optional — default to `stomo_db` / `stomo_user` |
| `FINNHUB_API_KEY` | live quotes | Optional — quote endpoint errors without it |
| `ALPHAVANTAGE_API_KEY` | company overview + search | Optional — those endpoints error without it |
| `YODA_API_KEY` | Yodish translation | Optional — UI text shown untranslated (passthrough) |

The optional API keys only gate their own feature — the rest of the app works without them
(Yahoo history is keyless). PostgreSQL runs on port `5432`.

### Migrating data from an old MySQL instance

A fresh install needs nothing here — Hibernate creates the schema on first boot and the cache
tables refill from the external APIs. Only if you have a pre-switch **MySQL** database whose data
you want to keep:

```bash
docker compose up -d db                       # start the new PostgreSQL (host: stomo-postgres)
# (the old MySQL container must still be running as "stomo-mysql" on the same network)
./scripts/migrate-mysql-to-postgres.sh        # pgloader copies the entity tables + resets sequences
```

It uses [`scripts/mysql-to-postgres.load`](scripts/mysql-to-postgres.load): only entity-backed
tables are copied (legacy `portfolio_items` / `translations` tables are skipped) and the identity
sequences are reset so new inserts continue past the migrated ids.

---

## Testing

```bash
./mvnw test     # Windows: mvnw.cmd test
```

The suite runs against an **in-memory H2** database (via the `test` profile) — no PostgreSQL and no
API keys required, so it is fully self-contained and CI-friendly. **45 tests** cover three
layers:

- **Unit** (Mockito) — watchlist business logic (CRUD, validation, conflicts) and JWT issuing.
- **Integration** (H2) — JPA relationships, ownership scoping, unique constraints, cascade delete.
- **API** (MockMvc) — register → token → full watchlist CRUD, including `401` / `400` / `409` paths.

---

## Third-party APIs

| API | Role | Auth | Notes |
|---|---|---|---|
| **Finnhub** | Real-time quotes | API key | Free tier sufficient |
| **Yahoo Finance** | Intraday/historical price series | keyless | Unofficial; **cached in the DB** with stale-fallback |
| **Alpha Vantage** | Company overview + symbol search | API key | Free tier (≈25 req/day); client-side throttled |
| **Yodish (RapidAPI)** | Optional UI-text translation | API key | Cached per phrase; passthrough without a key |

History data is persisted in `price_history` and translations in `yoda_translations` so each
external call is made at most as often as needed — protecting the free-tier rate limits.

---

## Project structure

```
src/main/java/com/dhbw/webeng2/stomo/
  controller/   REST controllers (auth, market, watchlists, yoda)
  service/      business logic + third-party integrations
  repository/   Spring Data JPA repositories
  model/        entities, DTOs, enums
  exception/    custom exceptions + global handler
  config/       security / JWT configuration
frontend/src/
  components/   dashboard, chart, watchlist, auth modal, …
  api/          fetch wrappers for the backend
  auth/ theme/  React context providers
```

---

## Bonus features implemented

- ✅ **Authentication** with Spring Security + JWT
- ✅ **OpenAPI / Swagger** docs (`/swagger-ui.html`)
- ✅ **Docker** + Docker Compose for the whole stack
- ✅ **CI** (GitHub Actions) — builds & tests backend and frontend on every push
- ✅ Test suite with **> 10 tests** (45) across unit / integration / API
- ✅ **PostgreSQL** as the runtime database (H2 for tests)
