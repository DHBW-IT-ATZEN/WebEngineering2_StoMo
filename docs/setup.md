# Setup, configuration & troubleshooting

How to install the prerequisites, configure your environment (`.env`), and fix the
problems you're most likely to hit. For *running* the app once it's configured, see
the [README "Running locally"](../README.md#running-locally) section.

---

## 1. Prerequisites (installation)

What you need depends on **how** you run StoMo:

| You want to… | Install |
|---|---|
| Run the whole stack (recommended) | **Docker Desktop** (includes Docker Compose v2) — that's all |
| Develop the backend natively | **JDK 21** (Temurin/OpenJDK) + Docker Desktop (for the database) |
| Develop the frontend natively | **Node.js 20+** and npm |
| Use the local-HTTPS option | **mkcert** (plus the above) |

Notes:

- **Docker Desktop** must be *running* before any `docker compose` command. On Windows it
  also provides the Linux engine the containers need.
- You do **not** need to install Maven — the repo ships the **Maven wrapper** (`mvnw` /
  `mvnw.cmd`), which downloads the right Maven version on first use.
- Check your versions:

  ```bash
  docker --version          # Docker 24+
  docker compose version    # Compose v2.x  (note: "docker compose", not "docker-compose")
  java -version             # 21.x   (only for native backend dev)
  node --version            # 20+    (only for native frontend dev)
  ```

---

## 2. Configure your environment — `.env`

### What `.env` and `.env.example` are for

| File | Committed to git? | Purpose |
|---|---|---|
| **`.env`** | **No** (gitignored) | Holds *your real* secrets and config — API keys, the JWT secret, the DB password. Read automatically by Docker Compose; **never commit it.** |
| **`.env.example`** | **Yes** | A safe, secret-free **template** that lists *every* variable the project understands, with placeholder/empty values. It documents "what do I need to set?" so a teammate can clone the repo and get running without reading the source. |

In short: **`.env.example` is the checked-in blueprint; `.env` is your private filled-in copy.**
Keeping them in sync means the secrets stay out of git while everyone still knows which
variables exist. Whenever you add a new env variable to the app, add it to `.env.example`
too (with a blank or placeholder value).

### First-time setup

```bash
cp .env.example .env      # Windows PowerShell:  Copy-Item .env.example .env
```

Then edit `.env` and set, at minimum:

| Variable | Required? | What to put |
|---|---|---|
| `POSTGRES_PASSWORD` | **Yes** | Any password for the local database, e.g. `stomo_password`. The app and `docker compose` refuse to start without it. |
| `JWT_SECRET` | **Yes for Docker** | A random string **≥ 32 characters**. Docker runs the `prod` profile, which refuses to boot with the built-in dev secret. Generate one: `openssl rand -base64 48`. |
| `POSTGRES_DB` / `POSTGRES_USER` | Optional | Default to `stomo_db` / `stomo_user` if omitted. |
| `FINNHUB_API_KEY` | Optional | Enables live quotes. Without it the quote endpoint returns an upstream error; the rest works. |
| `ALPHAVANTAGE_API_KEY` | Optional | Enables company overview + search. |
| `YODA_API_KEY` | Optional | Enables Yodish translation; otherwise text passes through. |

> Why required vs. optional: `POSTGRES_PASSWORD` guards your data and is deliberately kept out
> of the repo (no default), so it must be supplied. The API keys are genuinely optional — the
> matching feature just stays dormant without them.

---

## 3. Troubleshooting

### Startup / configuration

| Symptom | Cause | Fix |
|---|---|---|
| `required variable POSTGRES_PASSWORD is missing a value` (on `docker compose up`) | No `.env`, or `POSTGRES_PASSWORD` not set in it | `cp .env.example .env` and set `POSTGRES_PASSWORD`. |
| Backend exits with `Could not resolve placeholder 'POSTGRES_PASSWORD'` (native `./mvnw spring-boot:run`) | Spring does **not** read `.env` automatically — only Docker Compose does | Export the var in your shell, or set it in your IDE run config. Bash: `export POSTGRES_PASSWORD=stomo_password`. PowerShell: `$env:POSTGRES_PASSWORD='stomo_password'`. |
| Backend refuses to start: *"Refusing to start under the 'prod' profile with the built-in dev JWT secret"* | Running the `prod` profile (Docker does) with no `JWT_SECRET` | Set a strong `JWT_SECRET` (≥ 32 chars) in `.env`. |
| Backend fails: *"app.jwt.secret must be at least 32 bytes"* | `JWT_SECRET` too short | Use ≥ 32 characters. |

### Database

| Symptom | Cause | Fix |
|---|---|---|
| Backend logs `Connection refused` / `the database system is starting up` | Backend started before Postgres was ready | Compose already waits via a healthcheck; if running natively, start the DB first (`docker compose up -d db`) and give it a few seconds. |
| `password authentication failed for user "stomo_user"` | `.env` password doesn't match the password the DB volume was first created with | The password is baked into the volume on first init. Either set `POSTGRES_PASSWORD` back to the original, or wipe the volume: `docker compose down -v` then `up` again (**deletes all DB data**). |
| Changed `POSTGRES_PASSWORD` but it had no effect | The `pgdata` volume persists the old credentials | `docker compose down -v` to recreate the volume (destroys data), then `up`. |
| Can't log into an app account (locked out) | Brute-force lockout / wrong status | See [docs/postgres.md](postgres.md) → "Inspect a user account" / "Unlock an account". |

### Ports & networking

| Symptom | Cause | Fix |
|---|---|---|
| `Bind for 0.0.0.0:5432 failed: port is already allocated` | A local PostgreSQL (or old container) already uses 5432 | Stop the other service, or change the host port mapping in `docker-compose.yml` (e.g. `"5433:5432"`). |
| Port `8080` / `80` already in use | Another app holds the port | Stop it, or remap the port in `docker-compose.yml`. |
| Frontend loads but API calls 404 / fail | Backend not up, or `/api` not reaching it | Check `docker compose ps` (backend `healthy`); in dev mode the Vite proxy forwards `/api` → `:8080`, so the backend must be running. |
| `http://stomo.lab` / `https://stomo.dev` doesn't resolve | No hosts-file entry | Add `127.0.0.1 stomo.lab` (or `stomo.dev`) to your hosts file — see README Options A/C. |

### Build / general

| Symptom | Cause | Fix |
|---|---|---|
| `docker compose` "command not found" but `docker-compose` works | Old standalone Compose v1 | Install Docker Compose v2 (bundled with current Docker Desktop); use `docker compose` (space). |
| First `docker compose up` is very slow / `libretranslate` stuck `starting` | The translator downloads its en↔de model on first boot (can take a minute+) | Wait — it has a long `start_period`. Translation degrades gracefully meanwhile (text passes through), so it never blocks the backend. |
| `./mvnw` "permission denied" (macOS/Linux) | Wrapper not executable | `chmod +x mvnw`. |
| Want to see what Compose will actually run | — | `docker compose config` prints the fully-resolved configuration (with `.env` applied). |

### Handy diagnostic commands

```bash
docker compose ps                    # service status + health
docker compose logs -f backend       # follow backend logs
docker compose logs -f db            # follow database logs
docker compose config                # show resolved config (validates .env substitution)
docker compose down                  # stop the stack (keeps data)
docker compose down -v               # stop AND delete the DB volume (wipes data)
```
