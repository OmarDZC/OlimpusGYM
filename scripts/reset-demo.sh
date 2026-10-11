#!/usr/bin/env bash
# Reinicio nocturno de la demo pública (Trozo 8a-2): borra todo lo que haya creado o
# modificado la gente que prueba la demo (altas, reservas, rutinas...) y deja que el backend
# vuelva a sembrar los datos de ejemplo de siempre al reiniciarse (BusinessDataSeeder +
# DevDataSeeder, en modo "demo").
#
# Pensado para cron, en el propio servidor donde corre docker-compose.prod.yml:
#   0 4 * * * cd /ruta/al/proyecto && ./scripts/reset-demo.sh >> /var/log/gymflow-reset-demo.log 2>&1
#
# Uso manual: ./scripts/reset-demo.sh [ruta-al-.env.prod]
set -euo pipefail

cd "$(dirname "$0")/.."

ENV_FILE="${1:-.env.prod}"
COMPOSE_FILE="docker-compose.prod.yml"

if [ ! -f "$ENV_FILE" ]; then
    echo "[reset-demo] No se encuentra $ENV_FILE. Uso: $0 [ruta-al-.env.prod]" >&2
    exit 1
fi

ACTIVE_PROFILE=$(grep -E '^SPRING_PROFILES_ACTIVE=' "$ENV_FILE" | tail -n1 | cut -d= -f2- || true)
if [ "$ACTIVE_PROFILE" != "demo" ]; then
    echo "[reset-demo] SPRING_PROFILES_ACTIVE en $ENV_FILE es '${ACTIVE_PROFILE:-<vacío>}', no 'demo'." >&2
    echo "[reset-demo] Este script borra datos de socios y reservas: solo se ejecuta en modo demo. Abortando." >&2
    exit 1
fi

POSTGRES_DB=$(grep -E '^POSTGRES_DB=' "$ENV_FILE" | tail -n1 | cut -d= -f2- || true)
POSTGRES_USER=$(grep -E '^POSTGRES_USER=' "$ENV_FILE" | tail -n1 | cut -d= -f2- || true)
POSTGRES_DB="${POSTGRES_DB:-gymflow}"
POSTGRES_USER="${POSTGRES_USER:-gymflow}"

echo "[reset-demo] $(date -u +%FT%TZ) Borrando datos de ejemplo (socios, reservas, rutinas, horario, mensajes)..."

# Se deja a propósito "tenants" y "plans" sin tocar: son los datos de negocio reales (el
# tenant "olympusgym" y las 3 tarifas), no datos de ejemplo, y BusinessDataSeeder los
# recrearía igual de todas formas. CASCADE arrastra las tablas que dependen por clave ajena.
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T postgres \
    psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 <<SQL
TRUNCATE TABLE
    contact_messages,
    membership_plan_changes,
    workout_logs,
    workout_plans,
    bookings,
    class_sessions,
    memberships,
    users
CASCADE;
SQL

echo "[reset-demo] Datos borrados. Reiniciando el backend para que vuelva a sembrar la demo..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" restart backend

echo "[reset-demo] $(date -u +%FT%TZ) Demo reiniciada."
