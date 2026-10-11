#!/usr/bin/env bash
# Restaura una copia de seguridad (de scripts/backup-db.sh) en la base de datos de producción.
# Pensado para una recuperación de verdad tras un desastre: usa --clean para borrar primero
# cualquier objeto existente, así que funciona igual si la base de datos está vacía o si ya
# tiene datos (los sustituye por los de la copia).
#
# Uso: ./scripts/restore-db.sh <ruta-al-volcado.dump> [ruta-al-.env.prod]
#
# Pide confirmación escrita porque sustituye todos los datos de la base de destino. Para
# scripts automáticos (p. ej. probarlo contra una base vacía), añade --yes.
set -euo pipefail

cd "$(dirname "$0")/.."

DUMP_FILE=""
ENV_FILE=".env.prod"
ASSUME_YES=false

for arg in "$@"; do
    case "$arg" in
        --yes) ASSUME_YES=true ;;
        *)
            if [ -z "$DUMP_FILE" ]; then DUMP_FILE="$arg"; else ENV_FILE="$arg"; fi
            ;;
    esac
done

if [ -z "$DUMP_FILE" ] || [ ! -f "$DUMP_FILE" ]; then
    echo "[restore] Uso: $0 <ruta-al-volcado.dump> [ruta-al-.env.prod] [--yes]" >&2
    exit 1
fi
if [ ! -f "$ENV_FILE" ]; then
    echo "[restore] No se encuentra $ENV_FILE." >&2
    exit 1
fi

COMPOSE_FILE="docker-compose.prod.yml"
POSTGRES_DB=$(grep -E '^POSTGRES_DB=' "$ENV_FILE" | tail -n1 | cut -d= -f2- || true)
POSTGRES_USER=$(grep -E '^POSTGRES_USER=' "$ENV_FILE" | tail -n1 | cut -d= -f2- || true)
POSTGRES_DB="${POSTGRES_DB:-gymflow}"
POSTGRES_USER="${POSTGRES_USER:-gymflow}"

if [ "$ASSUME_YES" != true ]; then
    echo "Esto BORRA y sustituye todos los datos de la base '$POSTGRES_DB' con el contenido de:"
    echo "  $DUMP_FILE"
    read -r -p "Escribe RESTAURAR para continuar: " CONFIRM
    if [ "$CONFIRM" != "RESTAURAR" ]; then
        echo "[restore] Cancelado." >&2
        exit 1
    fi
fi

echo "[restore] $(date -u +%FT%TZ) Restaurando $DUMP_FILE en $POSTGRES_DB..."
cat "$DUMP_FILE" | docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T postgres \
    pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --no-privileges

echo "[restore] $(date -u +%FT%TZ) Restauración terminada. Reinicia el backend para que recargue:"
echo "  docker compose -f $COMPOSE_FILE --env-file $ENV_FILE restart backend"
