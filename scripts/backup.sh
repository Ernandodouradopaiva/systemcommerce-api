#!/usr/bin/env bash
# Backup lógico do PostgreSQL (SystemCommerce).
# Uso:
#   export POSTGRES_USER=... POSTGRES_PASSWORD=... POSTGRES_DB=systemcommerce
#   export BACKUP_DIR=./backups
#   ./scripts/backup.sh
# Opcional: CONTAINER=systemcommerce-api-db (docker exec) ou HOST=localhost PORT=5432 (pg_dump local)

set -euo pipefail

CONTAINER="${CONTAINER:-systemcommerce-api-db}"
POSTGRES_DB="${POSTGRES_DB:-systemcommerce}"
POSTGRES_USER="${POSTGRES_USER:-systemcommerce}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT="${BACKUP_DIR}/systemcommerce_${STAMP}.dump"

mkdir -p "${BACKUP_DIR}"

if [[ -n "${POSTGRES_PASSWORD:-}" ]]; then
  export PGPASSWORD="${POSTGRES_PASSWORD}"
fi

echo "Backup -> ${OUT}"

if docker ps --format '{{.Names}}' | grep -qx "${CONTAINER}"; then
  docker exec -e PGPASSWORD="${POSTGRES_PASSWORD:-}" "${CONTAINER}" \
    pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -Fc -f "/tmp/sc_backup.dump"
  docker cp "${CONTAINER}:/tmp/sc_backup.dump" "${OUT}"
  docker exec "${CONTAINER}" rm -f /tmp/sc_backup.dump
else
  pg_dump -h "${HOST:-localhost}" -p "${PORT:-5432}" -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -Fc -f "${OUT}"
fi

echo "OK: ${OUT}"
echo "Retenção sugerida: 7 diários + 4 semanais + 6 mensais (ajuste à política da empresa)."
