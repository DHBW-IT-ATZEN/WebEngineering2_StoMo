#!/usr/bin/env bash
#
# One-shot data migration MySQL -> PostgreSQL for StoMo.
#
# Use this only if you have an existing MySQL database (from before the switch to
# PostgreSQL) whose data you want to keep. A fresh install needs none of this —
# Hibernate creates the schema on first boot and the cache tables refill from the
# external APIs.
#
# Prerequisites:
#   - The old MySQL container is running and reachable on the Docker network
#     "stomo_default" as host "stomo-mysql" (the pre-switch docker-compose default).
#   - A PostgreSQL container is running on the same network as "stomo-postgres"
#     using the credentials from .env (POSTGRES_USER / POSTGRES_DB / POSTGRES_PASSWORD).
#     Bringing up the current stack once (`docker compose up -d db`) provides exactly this.
#
# Credentials: read from .env (auto-sourced below). POSTGRES_PASSWORD and MYSQL_PASSWORD
# are required and must NOT be hardcoded here. Set the old MySQL password via MYSQL_PASSWORD
# (export it or add it to .env); it is not stored in the repo.
#
# What it does: renders scripts/mysql-to-postgres.load from the environment (envsubst),
# then runs pgloader (in a throwaway container on the same network) to copy only the
# entity-backed tables and reset the Postgres identity sequences.
#
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NETWORK="${STOMO_NETWORK:-stomo_default}"

# Load credentials from .env (gitignored) if present, without overriding the caller's env.
if [[ -f "${SCRIPT_DIR}/../.env" ]]; then
  set -a; . "${SCRIPT_DIR}/../.env"; set +a
fi
# Non-secret identifiers fall back to defaults; passwords are required.
: "${POSTGRES_USER:=stomo_user}"
: "${POSTGRES_DB:=stomo_db}"
: "${MYSQL_USER:=stomo_user}"
: "${MYSQL_DB:=stomo_db}"
: "${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD (e.g. in .env) before migrating}"
: "${MYSQL_PASSWORD:?Set MYSQL_PASSWORD (the old MySQL password) before migrating}"
export POSTGRES_USER POSTGRES_DB POSTGRES_PASSWORD MYSQL_USER MYSQL_DB MYSQL_PASSWORD

# Render the templated load file (substitutes the ${...} placeholders) to a temp file.
RENDERED="$(mktemp)"
trap 'rm -f "${RENDERED}"' EXIT
envsubst < "${SCRIPT_DIR}/mysql-to-postgres.load" > "${RENDERED}"

echo "Running pgloader on network '${NETWORK}' (stomo-mysql -> stomo-postgres)…"
docker run --rm --network "${NETWORK}" \
  -v "${RENDERED}:/migrate.load:ro" \
  ghcr.io/dimitri/pgloader:latest pgloader /migrate.load

echo "Done. Verify with:"
echo "  docker exec stomo-postgres psql -U stomo_user -d stomo_db -c '\\dt'"
