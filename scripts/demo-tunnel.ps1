# Demo en vivo desde tu PC con un tunel gratuito de Cloudflare (Trozo 9f, ver
# docs/DEPLOY.md). No crea cuenta ni dominio: cloudflared imprime una URL temporal de
# *.trycloudflare.com que cambia cada vez.
#
# Requiere: cloudflared instalado (winget install Cloudflare.cloudflared) y un .env.prod
# en la raiz del proyecto con SPRING_PROFILES_ACTIVE=demo, DOMAIN=:80, PUBLIC_URL=http://localhost.
#
# Uso: ./scripts/demo-tunnel.ps1 [ruta-al-.env.prod]

param(
    [string]$EnvFile = ".env.prod"
)

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

if (-not (Test-Path $EnvFile)) {
    Write-Error "No se encuentra $EnvFile. Copia .env.prod.example y rellenalo primero."
    exit 1
}

if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
    Write-Error "cloudflared no esta instalado. Instalalo con: winget install Cloudflare.cloudflared"
    exit 1
}

Write-Host "[demo-tunnel] Arrancando el stack de produccion en modo demo..."
docker compose -f docker-compose.prod.yml --env-file $EnvFile up -d --build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "[demo-tunnel] Reiniciando la demo a sus datos de ejemplo..."
& bash ./scripts/reset-demo.sh $EnvFile

Write-Host "[demo-tunnel] Abriendo el tunel hacia Caddy (puerto 80). Deja esta ventana abierta."
Write-Host "[demo-tunnel] La URL publica aparece debajo; cambia cada vez que se abre el tunel."
cloudflared tunnel --url http://localhost:80
