# OlympusGYM

Plataforma de gestión para gimnasios: socios y tarifas, reservas de clases con control de aforo y lista de espera, asistente de IA que reserva por chat y entrenador personal con IA. **OlympusGYM** es el gimnasio ficticio de demostración, con su web pública, área de socio y panel de gestión.

Documentación del proyecto en `docs/`: `PROJECT_BRIEF.md` (alcance y decisiones), `LOGIC.md` (reglas y API), `DESIGN.md` (estilo), `ROADMAP.md` (orden de construcción) y `design/` (pantallas de referencia).

> Estado: **Trozo 1 en curso (autenticación y roles)**. Mira `docs/PROGRESS.md` para el detalle y `docs/ROADMAP.md` para el orden de construcción.

## Cómo está montado

```
OlympusGYM/
├── backend/        Spring Boot 3 (Java 21): API principal, auth, reservas, pagos
├── ai-service/     FastAPI (Python): entrenador, asistente de reservas, RAG
├── frontend/       Next.js + TypeScript + Tailwind: panel del gimnasio y app del socio
├── docs/           Roadmap y decisiones de arquitectura
├── docker-compose.yml   Une todo: Postgres (pgvector), Redis, RabbitMQ, los 3 servicios
├── .env.example    Variables de entorno (copiar a .env)
└── Makefile        Atajos (make infra, make up, make backend-test...)
```

Cada carpeta es un servicio independiente con su propio `Dockerfile`. El `docker-compose.yml` solo los conecta entre sí.

```
Navegador ──> frontend (3000) ──> backend Spring (8080) ──> PostgreSQL / Redis
                                        │
                                        ├──REST──> ai-service FastAPI (8000) ──> LLM (Ollama/Groq/Gemini...)
                                        └──RabbitMQ (tareas pesadas, Fase 3+)
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
make infra          # Postgres, Redis y RabbitMQ
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

### LLM local gratuito (opcional)

```bash
docker compose --profile local-llm up -d ollama
docker compose exec ollama ollama pull llama3.1:8b
```

Para usar Groq, Gemini u OpenRouter, cambia `LLM_BASE_URL`, `LLM_API_KEY` y `LLM_MODEL` en `.env`.

## Tests

```bash
make backend-test   # JUnit (los de Testcontainers requieren Docker y se omiten sin él)
make ai-test        # pytest
cd frontend && npm run typecheck
```

## Puertos

| Servicio   | Puerto |
|------------|--------|
| frontend   | 3000   |
| backend    | 8080   |
| ai-service | 8000   |
| PostgreSQL | 5432   |
| Redis      | 6379   |
| RabbitMQ   | 5672 (UI en 15672) |
| Ollama     | 11434  |
