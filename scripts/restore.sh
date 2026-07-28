#!/usr/bin/env bash
# Restauração a partir de dump custom (-Fc) gerado por backup.sh.
# ATENÇÃO: destrói/reescreve o banco de destino. Use apenas com aprovação.
#
# Uso:
#   export POSTGRES_USER=... POSTGRES_PASSWORD=... POSTGRES_DB=systemcommerce
#   ./scripts/restore.sh ./backups/systemcommerce_YYYYMMDD.dump

set -euo pipefail

DUMP="${1:?Informe o caminho do arquivo .dump}"
CONTAINER="${CONTAINER:-systemcommerce-api-db}"
POSTGRES_DB="${POSTGRES_DB:-systemcommerce}"
POSTGRES_USER="${POSTGRES_USER:-systemcommerce}"

if [[ ! -f "${DUMP}" ]]; then
  echo "Arquivo não encontrado: ${DUMP}" >&2
  exit 1
fi

echo "ATENÇÃO: restauração no banco '${POSTGRES_DB}' a partir de ${DUMP}"
read -r -p "Digite RESTORE para confirmar: " CONFIRM
[[ "${CONFIRM}" == "RESTORE" ]] || { echo "Abortado."; exit 1; }

if [[ -n "${POSTGRES_PASSWORD:-}" ]]; then
  export PGPASSWORD="${POSTGRES_PASSWORD}"
fi

if docker ps --format '{{.Names}}' | grep -qx "${CONTAINER}"; then
  docker cp "${DUMP}" "${CONTAINER}:/tmp/sc_restore.dump"
  docker exec -e PGPASSWORD="${POSTGRES_PASSWORD:-}" "${CONTAINER}" \
    pg_restore -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --clean --if-exists --no-owner --no-acl \
    /tmp/sc_restore.dump || true
  docker exec "${CONTAINER}" rm -f /tmp/sc_restore.dump
else
  pg_restore -h "${HOST:-localhost}" -p "${PORT:-5432}" -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" \
    --clean --if-exists --no-owner --no-acl "${DUMP}" || true
fi

echo "Restore finalizado. Valide com: SELECT COUNT(*) FROM flyway_schema_history;"
echo "Reinicie a API e confira /actuator/health."
