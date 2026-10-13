# Demo en vivo desde tu PC con un tunel gratuito de Cloudflare (Trozo 9g, ver docs/DEPLOY.md).
#
# Autosuficiente: resuelve la raiz del repo por la ubicacion de este script (no por la
# carpeta desde la que lo lances), genera los secretos que falten en .env.prod sin tocar los
# que ya haya, copia el proveedor de IA desde tu .env local (gitignorado, sin imprimir
# valores) y arranca el stack sin necesitar saber de antemano la URL del tunel: el frontend
# llama a la API con una ruta relativa (mismo origen que la pagina), asi que funciona igual
# con la URL que de verdad te de cloudflared, tanto en tu PC como desde el movil.
#
# Lo unico que tienes que escribir a mano es DEMO_OWNER_EMAIL y DEMO_OWNER_PASSWORD en
# .env.prod (en la raiz del proyecto); si el archivo no existe, este script lo crea con esas
# dos lineas en blanco y para ahi para que las rellenes.
#
# Uso: ./scripts/demo-tunnel.ps1

$ErrorActionPreference = "Stop"

# Raiz del repo = la carpeta que contiene esta carpeta scripts/, sea cual sea el directorio
# actual desde el que se lance el script.
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $RepoRoot
Write-Host "[demo-tunnel] Raiz del proyecto: $RepoRoot"

$EnvProdPath = Join-Path $RepoRoot ".env.prod"
$EnvLocalPath = Join-Path $RepoRoot ".env"

function Read-DotEnv([string]$Path) {
    $values = [ordered]@{}
    if (Test-Path $Path) {
        foreach ($line in Get-Content -Path $Path -Encoding UTF8) {
            # Quita la marca BOM si quedo pegada a la primera clave (ocurre si el archivo se
            # creo o edito con un editor o comando que escribe BOM; sin esto, esa clave no
            # coincide nunca con su nombre real y el script la trata como si no existiera).
            $clean = $line.TrimStart([char]0xFEFF)
            if ($clean -notmatch "=" -or $clean.TrimStart().StartsWith("#")) { continue }
            $parts = $clean -split "=", 2
            $key = $parts[0].Trim()
            if ($key) { $values[$key] = $parts[1] }
        }
    }
    return $values
}

function Write-DotEnv([string]$Path, $Values) {
    $lines = foreach ($key in $Values.Keys) { "$key=$($Values[$key])" }
    # Sin BOM: Set-Content -Encoding utf8 en PowerShell 5.1 si lo escribe, y una marca BOM
    # pegada a la primera clave es justo lo que causaba que esa clave se leyera mal despues.
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($Path, [string[]]$lines, $utf8NoBom)
}

function New-RandomSecret {
    $bytes = New-Object byte[] 36
    $rng = [System.Security.Cryptography.RNGCryptoServiceProvider]::new()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}

if (-not (Test-Path $EnvProdPath)) {
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($EnvProdPath, [string[]]@(
        "# Creado por scripts/demo-tunnel.ps1. Rellena estas dos lineas y vuelve a ejecutar el script:"
        "DEMO_OWNER_EMAIL="
        "DEMO_OWNER_PASSWORD="
    ), $utf8NoBom)
    Write-Host "[demo-tunnel] Creado $EnvProdPath."
    Write-Host "[demo-tunnel] Abrelo, escribe DEMO_OWNER_EMAIL y DEMO_OWNER_PASSWORD (12 caracteres o mas), y vuelve a ejecutar este script."
    exit 1
}

$prod = Read-DotEnv $EnvProdPath
$local = Read-DotEnv $EnvLocalPath

# 1. Secretos generados con un generador criptografico, solo si faltan: nunca se tocan los
#    que ya hubiera (reusar el mismo Postgres entre ejecuciones necesita la misma contraseña).
$changed = $false
foreach ($key in @("JWT_SECRET", "POSTGRES_PASSWORD", "REDIS_PASSWORD")) {
    if (-not $prod.Contains($key) -or [string]::IsNullOrWhiteSpace($prod[$key])) {
        $prod[$key] = New-RandomSecret
        $changed = $true
        Write-Host "[demo-tunnel] Generado $key (no se muestra)."
    }
}

# 2. Proveedor de IA copiado de tu .env local, sin imprimir ningun valor.
foreach ($key in @("LLM_BASE_URL", "LLM_API_KEY", "LLM_MODEL")) {
    if (-not $local.Contains($key) -or [string]::IsNullOrWhiteSpace($local[$key])) {
        Write-Error "Falta $key en .env (raiz del proyecto). Rellenalo ahi (es tu configuracion normal de desarrollo) y vuelve a ejecutar este script."
        exit 1
    }
    if ($prod[$key] -ne $local[$key]) { $prod[$key] = $local[$key]; $changed = $true }
}
foreach ($key in @("LLM_FALLBACK_BASE_URL", "LLM_FALLBACK_API_KEY", "LLM_FALLBACK_MODEL")) {
    if ($local.Contains($key) -and -not [string]::IsNullOrWhiteSpace($local[$key]) -and $prod[$key] -ne $local[$key]) {
        $prod[$key] = $local[$key]
        $changed = $true
    }
}
Write-Host "[demo-tunnel] Proveedor de IA (y respaldo, si lo tienes) copiado desde .env."

# 3. Perfil demo por defecto (docker-compose.prod.yml arranca en "prod" si no se dice nada).
if (-not $prod.Contains("SPRING_PROFILES_ACTIVE") -or [string]::IsNullOrWhiteSpace($prod["SPRING_PROFILES_ACTIVE"])) {
    $prod["SPRING_PROFILES_ACTIVE"] = "demo"
    $changed = $true
}

# 4. El unico OWNER real de la demo: tienes que haberlo escrito tu a mano.
if (-not $prod.Contains("DEMO_OWNER_EMAIL") -or [string]::IsNullOrWhiteSpace($prod["DEMO_OWNER_EMAIL"])) {
    Write-Error "Falta DEMO_OWNER_EMAIL en .env.prod. Abrelo y escribe: DEMO_OWNER_EMAIL=tu-correo@ejemplo.com"
    exit 1
}
if (-not $prod.Contains("DEMO_OWNER_PASSWORD") -or [string]::IsNullOrWhiteSpace($prod["DEMO_OWNER_PASSWORD"])) {
    Write-Error "Falta DEMO_OWNER_PASSWORD en .env.prod. Abrelo y escribe: DEMO_OWNER_PASSWORD=... (12 caracteres o mas)"
    exit 1
}
if ($prod["DEMO_OWNER_PASSWORD"].Trim().Length -lt 12) {
    Write-Error "DEMO_OWNER_PASSWORD tiene menos de 12 caracteres. Alargala en .env.prod y vuelve a ejecutar este script."
    exit 1
}

if ($changed) {
    Write-DotEnv $EnvProdPath $prod
    Write-Host "[demo-tunnel] .env.prod actualizado."
}

if (-not (Get-Command cloudflared -ErrorAction SilentlyContinue)) {
    Write-Error "cloudflared no esta instalado. Instalalo con: winget install Cloudflare.cloudflared"
    exit 1
}

Write-Host "[demo-tunnel] Arrancando el stack de produccion en modo demo..."
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "[demo-tunnel] Reiniciando la demo a sus datos de ejemplo..."
& bash ./scripts/reset-demo.sh .env.prod

Write-Host "[demo-tunnel] Abriendo el tunel hacia Caddy (puerto 80). Deja esta ventana abierta."
Write-Host "[demo-tunnel] La URL publica aparece debajo (cambia cada vez); compartela cuando la veas."
cloudflared tunnel --url http://localhost:80
