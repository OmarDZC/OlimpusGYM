# OlympusGYM

Plataforma SaaS para gimnasios, de principio a fin: socios y tarifas, reservas de clases con control de aforo y lista de espera, asistente de IA que reserva por chat y entrenador personal con IA. Este repositorio es **OlympusGYM**, un gimnasio ficticio con su web pública, área de socio y panel de gestión.

Documentación del proyecto en `docs/`: `PROJECT_BRIEF.md` (alcance y decisiones), `LOGIC.md` (reglas y API), `DESIGN.md` (estilo), `ROADMAP.md` (orden de construcción) y `design/` (pantallas de referencia).

> Estado: ver `docs/PROGRESS.md` para el detalle de cada trozo completado y `docs/ROADMAP.md` para el orden de construcción.

## Cómo está montado

```
OlympusGYM/
├── backend/        Spring Boot 3 (Java 21): API principal, auth, reservas, pagos
├── ai-service/     FastAPI (Python): entrenador, asistente de reservas, RAG
├── frontend/       Next.js + TypeScript + Tailwind: panel del gimnasio y app del socio
├── docs/           Roadmap y decisiones de arquitectura
├── docker-compose.yml       Desarrollo: Postgres (pgvector), Redis, los 3 servicios
├── docker-compose.prod.yml Producción: Postgres, Caddy (HTTPS automático), los 3 servicios
├── .env.example    Variables de entorno de desarrollo (copiar a .env)
├── .env.prod.example  Variables de entorno de producción (copiar a .env.prod)
└── Makefile        Atajos (make infra, make up, make backend-test...)
```

Cada carpeta es un servicio independiente con su propio `Dockerfile`. El `docker-compose.yml` solo los conecta entre sí.

```
Navegador ──> frontend (3000) ──> backend Spring (8080) ──> PostgreSQL / Redis
                                        │
                                        └──REST──> ai-service FastAPI (8000) ──> LLM (Ollama/Groq/Gemini...)
```

- **Multi-tenancy:** una sola base de datos con `tenant_id` en cada tabla de negocio.
- **IA:** el proveedor de LLM es intercambiable (API compatible con OpenAI). Por defecto Ollama local, gratis y sin tokens. Ver `.env.example`.

## Requisitos

- Java 21 y Maven 3.9+
- Python 3.12+
- Node 22+
- Docker y Docker Compose (para levantar todo junto)

## Arranque rápido

```bash
cp .env.example .env
make infra          # Postgres y Redis
```

Después, cada servicio por separado en su terminal:

```bash
# Backend (http://localhost:8080/api/v1/public/ping)
cd backend && mvn spring-boot:run

# Servicio de IA (http://localhost:8000/health)
cd ai-service && python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt && uvicorn app.main:app --reload

# Frontend (http://localhost:3000)
cd frontend && npm install && npm run dev
```

O todo con Docker: `make up`.

### Cambiar de proveedor de IA (entrenador y asistente)

El servicio de IA habla con el LLM a través de un cliente compatible con OpenAI
(`ai-service/app/llm.py`): cambiar de proveedor es solo cambiar tres variables en `.env`
(ver `.env.example` para la lista completa), sin tocar código.

| Proveedor | `LLM_BASE_URL` | `LLM_API_KEY` | `LLM_MODEL` |
|---|---|---|---|
| Ollama (por defecto, local y gratis) | `http://ollama:11434/v1` | `ollama` (no se usa, pero no puede ir vacío) | `llama3.1:8b` |
| Groq (capa gratuita con límites) | `https://api.groq.com/openai/v1` | tu clave de Groq | un modelo disponible en tu cuenta |
| Google Gemini (AI Studio) | `https://generativelanguage.googleapis.com/v1beta/openai/` | tu clave de Gemini | un modelo disponible en tu cuenta |
| OpenRouter (modelos `:free`) | `https://openrouter.ai/api/v1` | tu clave de OpenRouter | un modelo con sufijo `:free` |

Ollama local:

```bash
docker compose --profile local-llm up -d ollama
docker compose exec ollama ollama pull llama3.1:8b
```

Los modelos gratuitos y sus límites cambian con el tiempo: consulta la web de cada proveedor
antes de fijar `LLM_MODEL`.

### Proveedor de IA de respaldo (opcional)

Las capas gratuitas tienen cupos pequeños: con Groq, por ejemplo, una sola rutina de varios
días ya puede agotar el límite de tokens de salida por minuto. `LLM_FALLBACK_BASE_URL`,
`LLM_FALLBACK_API_KEY` y `LLM_FALLBACK_MODEL` (vacías por defecto) activan un segundo
proveedor: si el principal agota sus reintentos con un 429 (límite de peticiones) o un 5xx,
se prueba una vez ahí antes de darse por vencido, tanto en el entrenador como en el
asistente. Sin estas variables, el comportamiento es el de siempre (solo el principal).

```bash
# En .env, con Google Gemini como respaldo de Groq:
LLM_FALLBACK_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/
LLM_FALLBACK_API_KEY=tu_clave_de_gemini
LLM_FALLBACK_MODEL=gemini-2.0-flash   # o el modelo disponible en tu cuenta
```

### Probar el asistente de reservas con un proveedor real (ejemplo con Groq)

El asistente (`/horario` → botón "Reservar con IA") usa el mismo cliente y las mismas
variables que el entrenador, así que no hace falta nada específico para él:

```bash
# En .env:
LLM_BASE_URL=https://api.groq.com/openai/v1
LLM_API_KEY=tu_clave_de_groq
LLM_MODEL=llama-3.1-8b-instant   # o el modelo que tengas disponible en tu cuenta

docker compose up -d --build ai-service   # recoge las variables nuevas de .env
```

Después, entra con una cuenta de tarifa Completa o Atleta Pro (p. ej.
`laura@olympusgym.test` / `Demo1234!`), abre `/horario`, pulsa "Reservar con IA" y pídele que
consulte el horario, reserve o cancele una clase. Solo ejecuta algo cuando confirmas la
propuesta: hasta entonces puedes pedirle lo que quieras sin miedo a que reserve de verdad.

## Credenciales de demo

Con `SPRING_PROFILES_ACTIVE=dev` (el valor por defecto de `make up`, desarrollo local): 12
socios de ejemplo y un OWNER fijo, todos con la contraseña `Demo1234!`. Solo existen en
local, nunca se despliegan.

| Rol | Correo | Contraseña | Notas |
|---|---|---|---|
| OWNER (dueño) | `javier@olympusgym.test` | `Demo1234!` | Cuenta de ejemplo del dueño |
| STAFF | `marta@olympusgym.test` / `dani@olympusgym.test` | `Demo1234!` | Entrenadores de ejemplo |
| MEMBER | `carlos@olympusgym.test` y 11 socios más | `Demo1234!` | Ver `DevDataSeeder` para la lista completa y sus tarifas |

Con `SPRING_PROFILES_ACTIVE=demo` (despliegue público), el conjunto es distinto y más
pequeño — ver la tabla de cuentas públicas en "Despliegue en producción" más abajo;
`javier@olympusgym.test` no existe ahí. En producción (`prod`) el seed
de dev/demo no se ejecuta nunca (`DevDataSeeder` está limitado a esos dos perfiles): el
administrador real se crea con `ProductionAdminSeeder` a partir de `ADMIN_EMAIL` y
`ADMIN_PASSWORD`.

## Tests

```bash
make backend-test   # JUnit (los de Testcontainers requieren Docker y se omiten sin él)
make ai-test        # pytest
cd frontend && npm run typecheck
```

### End-to-end (Playwright)

Tests de navegador reales contra el stack levantado en Docker: login por rol, reservar y
cancelar una clase como socio, que el staff cancele una sesión y la reserva del socio quede
cancelada, y que el socio reserve hablando con el asistente y confirmando la propuesta. Viven
en `e2e/`, aparte de `frontend/` (no son parte del build de Next.js).

```bash
docker compose up -d postgres redis backend frontend   # escenarios sin IA
cd e2e && npm install && npx playwright install --with-deps chromium
npx playwright test
```

El escenario del asistente necesita además el servicio de IA, pero con un LLM simulado
(`e2e/fake-llm`, un doble mínimo compatible con OpenAI, sin red externa ni coste) en vez de un
proveedor real — ver `docker-compose.e2e.yml`:

```bash
docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build \
  postgres redis backend ai-service frontend fake-llm
cd e2e && npx playwright test
```

También corren en GitHub Actions (job `e2e` de la CI) contra el mismo `docker compose`.

## Despliegue en producción

`docker-compose.prod.yml` (Trozo 8a-1 y 8a-2): backend, servicio de IA, frontend, PostgreSQL
normal (sin pgvector — nada lo usa todavía, ver `docs/PROGRESS.md`), Redis (con contraseña,
solo para el límite de uso de la IA) y Caddy como único punto de entrada, con HTTPS
automático (Let's Encrypt) cuando `DOMAIN` es un dominio real. Solo Caddy publica puertos (80
y 443); el resto de servicios solo son alcanzables entre sí, por la red interna de Docker
Compose. Guía completa paso a paso para un VPS Ubuntu en `docs/DEPLOY.md`.

¿Solo quieres enseñar la demo sin un VPS? `docs/DEPLOY.md` también tiene una sección para
levantarla desde tu propio PC con un túnel gratuito de Cloudflare (`scripts/demo-tunnel.ps1`,
Windows): genera los secretos que falten, copia el proveedor de IA de tu `.env` y abre el
túnel sin tener que configurar `PUBLIC_URL` a mano.

```bash
cp .env.prod.example .env.prod   # y rellena los valores reales, nunca los de ejemplo
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

El tenant único `olympusgym` y las 3 tarifas reales (`BusinessDataSeeder`) se crean siempre,
en cualquier perfil. Qué más hay, lo decide `SPRING_PROFILES_ACTIVE` (variable de
`docker-compose.prod.yml`, por defecto `prod`):

- **`prod`**: solo el tenant, las tarifas y el OWNER real (`ProductionAdminSeeder`, a partir
  de `ADMIN_EMAIL`/`ADMIN_PASSWORD`). Nada de ejemplo: ni socios, ni horario, ni cuentas de
  demo.
- **`demo`**: para una demo pública con datos de ejemplo. Un único OWNER real sale de
  `DEMO_OWNER_EMAIL`/`DEMO_OWNER_PASSWORD` (contraseña de 12 caracteres o más; el arranque
  falla con un mensaje claro si faltan o es corta) y **nunca aparece en los botones de
  acceso ni aquí abajo**: solo sirve para entrar a mano desde `/acceso` como cualquier otra
  cuenta. Las cuentas públicas (2 staff, 3 socios — uno por tarifa) usan la misma contraseña
  simple que el seed de desarrollo, `Demo1234!`, y sí están pensadas para enseñarse:

  | Cuenta pública | Correo |
  |---|---|
  | Staff | `marta@olympusgym.test` / `dani@olympusgym.test` |
  | Socio Básica | `marcos@olympusgym.test` |
  | Socio Completa | `laura@olympusgym.test` |
  | Socio Atleta Pro | `carlos@olympusgym.test` |

  `/acceso` muestra una franja de aviso y dos botones ("Entrar como staff/socio de
  demostración") que inician sesión sin contraseña contra una cuenta fija de cada tipo
  (`POST /api/v1/auth/demo-login`, que en modo `prod` ni siquiera existe: 404). `/contacto` y
  el registro en `/acceso` avisan de no escribir datos personales reales. Para reiniciar la
  demo cada noche a sus datos de siempre: `./scripts/reset-demo.sh .env.prod` (pensado para
  cron; deja exactamente el OWNER + staff + socios de arriba, borra todo lo demás).

Variables obligatorias en `.env.prod` (el backend se niega a arrancar si faltan o son
inválidas, ver `ProductionSecretsValidator`):

| Variable | Para qué |
|---|---|
| `DOMAIN` | Dominio real para HTTPS automático, o una IP / `:80` para una primera prueba en HTTP |
| `PUBLIC_URL` | URL pública completa con esquema (`https://...` o `http://...`), coherente con `DOMAIN`: el frontend la hornea en el bundle del navegador en tiempo de build, y es el único origen que admite CORS |
| `POSTGRES_PASSWORD` | Sin valor de ejemplo: ponlo tú |
| `REDIS_PASSWORD` | Sin valor de ejemplo: ponlo tú (`openssl rand -base64 24`) |
| `JWT_SECRET` | Mínimo 32 caracteres, aleatorio y distinto del valor de ejemplo del repo (`openssl rand -base64 48`) |
| `LLM_BASE_URL` / `LLM_API_KEY` / `LLM_MODEL` | El mismo proveedor compatible con OpenAI que en desarrollo (ver más arriba) |
| `LLM_FALLBACK_BASE_URL` / `LLM_FALLBACK_API_KEY` / `LLM_FALLBACK_MODEL` (opcionales) | Proveedor de respaldo si el principal se satura (ver "Proveedor de IA de respaldo" más arriba) |
| `SPRING_PROFILES_ACTIVE` | `prod` (por defecto) o `demo` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Solo con `SPRING_PROFILES_ACTIVE=prod`: el OWNER real del gimnasio (mínimo 12 caracteres en la contraseña) |
| `DEMO_OWNER_EMAIL` / `DEMO_OWNER_PASSWORD` | Solo con `SPRING_PROFILES_ACTIVE=demo`: el único OWNER real, nunca público (mínimo 12 caracteres en la contraseña) |
| `AI_LIMIT_*` (opcionales) | Límite de uso de la IA por socio/día y tope global diario; valores por defecto razonables si se dejan en blanco (ver `.env.prod.example`) |

### Copias de seguridad y Actuator

`scripts/backup-db.sh` (volcado comprimido, con fecha, conserva las últimas 7) y
`scripts/restore-db.sh` (restaura, con confirmación escrita) — ver `docs/DEPLOY.md` para
programarlos con cron. `/actuator/health` y `/actuator/metrics` del backend nunca son
públicos (Caddy no los expone); se consultan desde dentro del servidor, ver `docs/DEPLOY.md`.

### Límite de uso de la IA

Por socio y por día según su tarifa (COMPLETA: 5 rutinas / 40 mensajes al asistente por
defecto; ATLETA_PRO: más holgado), más un tope global diario para toda la aplicación (para no
agotar el cupo gratuito del proveedor del LLM). Al superarlo, 429 con un mensaje claro en
español ("Has alcanzado tu límite diario..." o, si es el tope global, "La demostración ha
alcanzado su límite de IA de hoy, vuelve mañana"). Si Redis falla, la IA sigue funcionando
igual (el límite simplemente no se aplica esa vez; se registra un aviso). Contadores en
Redis (`AiUsageLimiter`), configurables por variable de entorno (ver tabla de arriba).

## Puertos

| Servicio   | Puerto |
|------------|--------|
| frontend   | 3000   |
| backend    | 8080   |
| ai-service | 8000   |
| PostgreSQL | 5432   |
| Redis      | 6379   |
| Ollama     | 11434  |
