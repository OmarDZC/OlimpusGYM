# GymFlow

Plataforma SaaS para gimnasios: socios y tarifas, reservas de clases con control de aforo y lista de espera, asistente de IA que reserva por chat y entrenador personal con IA. Se demuestra con **OlympusGYM**, un gimnasio ficticio con su web, área de socio y panel de gestión.

Documentación del proyecto en `docs/`: `PROJECT_BRIEF.md` (alcance y decisiones), `LOGIC.md` (reglas y API), `DESIGN.md` (estilo), `ROADMAP.md` (orden de construcción) y `design/` (pantallas de referencia).

> Estado: ver `docs/PROGRESS.md` para el detalle de cada trozo completado y `docs/ROADMAP.md` para el orden de construcción.

## Cómo está montado

```
gymflow/
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

Solo existen con `SPRING_PROFILES_ACTIVE=dev` o `demo` (el valor por defecto de `make up`):
el seed (`DevDataSeeder`) nunca se ejecuta en producción. La contraseña de todas las cuentas
de ejemplo es `Demo1234!`, salvo la del administrador de desarrollo, pensada para entrar
rápido sin memorizarla.

| Rol | Correo | Contraseña | Notas |
|---|---|---|---|
| OWNER (dueño) | `javier@olympusgym.test` | `Demo1234!` | Cuenta de ejemplo del dueño |
| OWNER (admin de desarrollo) | `omaradmin@gmail.com` | `omaradmin` | **Solo para desarrollo local**, no se despliega (ver abajo) |
| STAFF | `marta@olympusgym.test` / `dani@olympusgym.test` | `Demo1234!` | Entrenadores de ejemplo |
| MEMBER | `carlos@olympusgym.test` y 11 socios más | `Demo1234!` | Ver `DevDataSeeder` para la lista completa y sus tarifas |

En producción el seed de dev/demo no se ejecuta nunca (`DevDataSeeder` está limitado a esos dos
perfiles): estas cuentas no existen fuera de desarrollo. El administrador real se crea con
`ProductionAdminSeeder` a partir de `ADMIN_EMAIL` y `ADMIN_PASSWORD` (ver
"Despliegue en producción" más abajo).

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
normal (sin pgvector — nada lo usa todavía, ver `docs/PROGRESS.md`) y Caddy como único punto
de entrada, con HTTPS automático (Let's Encrypt) cuando `DOMAIN` es un dominio real. Solo
Caddy publica puertos (80 y 443); el resto de servicios solo son alcanzables entre sí, por la
red interna de Docker Compose. Sin Redis todavía (a propósito: no aporta nada que el MVP
necesite ahora mismo) ni backups ni métricas (quedan para el Trozo 8a-2, commit 2).

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
- **`demo`**: para una demo pública con datos de ejemplo. `DevDataSeeder` siembra el mismo
  horario, socios y rutinas que en desarrollo (pero con las contraseñas fuertes de
  `DEMO_OWNER_PASSWORD`/`DEMO_MEMBER_PASSWORD`, nunca las de desarrollo, y sin la cuenta de
  admin de desarrollo). `/acceso` muestra una franja de aviso y dos botones ("Entrar como
  socio/dueño de demostración") que inician sesión sin contraseña contra las cuentas de
  ejemplo (`POST /api/v1/auth/demo-login`, que en modo `prod` ni siquiera existe: 404). Para
  reiniciar la demo cada noche a sus datos de siempre: `./scripts/reset-demo.sh .env.prod`
  (pensado para cron; borra lo que hayan creado o cambiado los visitantes y reseeda).

Variables obligatorias en `.env.prod` (el backend se niega a arrancar si faltan o son
inválidas, ver `ProductionSecretsValidator`):

| Variable | Para qué |
|---|---|
| `DOMAIN` | Dominio real para HTTPS automático, o una IP / `:80` para una primera prueba en HTTP |
| `PUBLIC_URL` | URL pública completa con esquema (`https://...` o `http://...`), coherente con `DOMAIN`: el frontend la hornea en el bundle del navegador en tiempo de build, y es el único origen que admite CORS |
| `POSTGRES_PASSWORD` | Sin valor de ejemplo: ponlo tú |
| `JWT_SECRET` | Mínimo 32 caracteres, aleatorio y distinto del valor de ejemplo del repo (`openssl rand -base64 48`) |
| `LLM_BASE_URL` / `LLM_API_KEY` / `LLM_MODEL` | El mismo proveedor compatible con OpenAI que en desarrollo (ver más arriba) |
| `SPRING_PROFILES_ACTIVE` | `prod` (por defecto) o `demo` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Solo con `SPRING_PROFILES_ACTIVE=prod`: el OWNER real del gimnasio (mínimo 12 caracteres en la contraseña) |
| `DEMO_OWNER_PASSWORD` / `DEMO_MEMBER_PASSWORD` | Solo con `SPRING_PROFILES_ACTIVE=demo`: contraseñas fuertes para los botones de demo (`openssl rand -base64 18`) |

**Pendiente, fuera de alcance del Trozo 8a-2 (commit 1)** (ver `docs/PROGRESS.md` para el
detalle): copias de seguridad de Postgres, métricas con Actuator/Prometheus, Redis en
producción y límite de uso de la IA, y la guía `docs/DEPLOY.md` para un VPS (llegan en el
commit 2).

## Puertos

| Servicio   | Puerto |
|------------|--------|
| frontend   | 3000   |
| backend    | 8080   |
| ai-service | 8000   |
| PostgreSQL | 5432   |
| Redis      | 6379   |
| Ollama     | 11434  |
