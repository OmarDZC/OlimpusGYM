#!/usr/bin/env bash
# Copia de seguridad de la base de datos de producción (Trozo 8a-2, Commit 2): un volcado
# comprimido por noche (formato "custom" de pg_dump, comprimido de por sí, restaurable con
# pg_restore o con scripts/restore-db.sh), con la fecha en el nombre, conservando solo las
# últimas 7 copias.
#
# Cron, todas las noches a las 3:00, desde la raíz del proyecto en el servidor:
#   0 3 * * * cd /ruta/al/proyecto && ./scripts/backup-db.sh >> /var/log/gymflow-backup.log 2>&1
#
# Uso manual: ./scripts/backup-db.sh [ruta-al-.env.prod] [carpeta-de-copias]
set -euo pipefail

cd "$(dirname "$0")/.."

ENV_FILE="${1:-.env.prod}"
BACKUP_DIR="${2:-backups}"
COMPOSE_FILE="docker-compose.prod.yml"
KEEP_LAST=7

if [ ! -f "$ENV_FILE" ]; then
    echo "[backup] No se encuentra $ENV_FILE. Uso: $0 [ruta-al-.env.prod] [carpeta-de-copias]" >&2
    exit 1
fi

POSTGRES_DB=$(grep -E '^POSTGRES_DB=' "$ENV_FILE" | tail -n1 | cut -d= -f2- || true)
POSTGRES_USER=$(grep -E '^POSTGRES_USER=' "$ENV_FILE" | tail -n1 | cut -d= -f2- || true)
POSTGRES_DB="${POSTGRES_DB:-gymflow}"
POSTGRES_USER="${POSTGRES_USER:-gymflow}"

mkdir -p "$BACKUP_DIR"
TIMESTAMP=$(date -u +%Y-%m-%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/gymflow_${TIMESTAMP}.dump"

echo "[backup] $(date -u +%FT%TZ) Volcando $POSTGRES_DB a $BACKUP_FILE..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T postgres \
    pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc > "$BACKUP_FILE"

SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[backup] Copia creada: $BACKUP_FILE ($SIZE)."

# Conserva solo las últimas 7: borra el resto, de más antigua a más reciente.
EXISTING=$(ls -1t "$BACKUP_DIR"/gymflow_*.dump 2>/dev/null | wc -l)
if [ "$EXISTING" -gt "$KEEP_LAST" ]; then
    ls -1t "$BACKUP_DIR"/gymflow_*.dump | tail -n +"$((KEEP_LAST + 1))" | while read -r old; do
        echo "[backup] Borrando copia antigua: $old"
        rm -f "$old"
    done
fi

echo "[backup] $(date -u +%FT%TZ) Listo. Copias guardadas: $(ls -1 "$BACKUP_DIR"/gymflow_*.dump 2>/dev/null | wc -l)."
