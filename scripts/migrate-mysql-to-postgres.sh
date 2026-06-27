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
#     with db/user/password stomo_db / stomo_user / stomo_password. Bringing up the
#     current stack once (`docker compose up -d db`) provides exactly this.
#
# What it does: runs pgloader (in a throwaway container on the same network) using
# scripts/mysql-to-postgres.load, which copies only the entity-backed tables and
# resets the Postgres identity sequences.
#
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NETWORK="${STOMO_NETWORK:-stomo_default}"

echo "Running pgloader on network '${NETWORK}' (stomo-mysql -> stomo-postgres)…"
docker run --rm --network "${NETWORK}" \
  -v "${SCRIPT_DIR}/mysql-to-postgres.load:/migrate.load:ro" \
  ghcr.io/dimitri/pgloader:latest pgloader /migrate.load

echo "Done. Verify with:"
echo "  docker exec stomo-postgres psql -U stomo_user -d stomo_db -c '\\dt'"
