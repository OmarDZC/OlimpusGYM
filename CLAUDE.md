# CLAUDE.md

Contexto para Claude Code. Lee esto antes de tocar nada.

## Qué es el proyecto

GymFlow: plataforma SaaS para gimnasios, proyecto de portfolio de un programador. Se demuestra con **OlympusGYM**, un gimnasio ficticio con web propia, área de socio, panel de gestión, reservas de clases con aforo, entrenador con IA y asistente de reservas con IA. Importan la calidad, los tests, el despliegue y que todo esté documentado.

**Es un MVP básico: no tiene que quedar perfecto, pero sí funcionar y estar limpio.** Fuera de alcance (no lo construyas): tienda, varios centros, pulseras/NFC/QR, taquillas, control de acceso, newsletter, app nativa.

## Documentación (léela en este orden)

1. `docs/PROJECT_BRIEF.md`: visión, alcance (dentro/fuera), roles, decisiones y por qué.
2. `docs/LOGIC.md`: datos, reglas de reserva, permisos, API y servicio de IA.
3. `docs/DESIGN.md`: sistema de diseño, tokens, tono del texto, movimiento y mapa pantalla → ruta.
4. `docs/design/`: pantallas de referencia (HTML + capturas). Abre los `screen.png` para ver el estilo. Son guía visual, no código para copiar.
5. `docs/ROADMAP.md`: orden de construcción por trozos verticales.
6. `docs/PROGRESS.md`: bitácora que debes actualizar al cerrar cada trozo.

## Arquitectura

- `backend/`: Spring Boot 3.5, Java 21, monolito modular. Paquete raíz `com.gymflow`, un subpaquete por módulo (`tenant`, `auth`, `plan`, `booking`, `workout`, `admin`...). JPA + Flyway + PostgreSQL. Seguridad stateless con JWT.
- `ai-service/`: FastAPI. Toda la lógica de IA vive aquí. Habla con el backend por REST.
- `frontend/`: Next.js (App Router) + TypeScript + Tailwind v4.
- Infra local: `docker-compose.yml` (Postgres con pgvector, Redis, RabbitMQ, Ollama opcional). Redis y RabbitMQ solo si aportan algo; no son obligatorios en el MVP.

## Decisiones ya tomadas (no las cambies sin avisar)

- **Multi-tenancy en la arquitectura, un solo gimnasio en la práctica:** una BD, `tenant_id` en toda tabla de negocio, un tenant `olympusgym` creado por seed. El tenant sale del JWT (`JwtAuthenticationFilter`), nunca de una cabecera del cliente; la cabecera provisional `X-Tenant-Id` se eliminó en el Trozo 1.
- **Tres experiencias separadas:** web pública, `/cuenta` (socio), `/admin` (dueño y staff). Un login único con redirección por rol. La autorización se comprueba **en el servidor**, no solo ocultando botones.
- **Monolito modular**, no microservicios, salvo el servicio de IA.
- **Migraciones solo con Flyway** (`backend/src/main/resources/db/migration`). `ddl-auto` está en `validate`: nunca `update`.
- **LLM agnóstico:** cliente compatible con OpenAI configurado por `LLM_BASE_URL`, `LLM_API_KEY` y `LLM_MODEL`. No acoples el código a ningún proveedor ni uses la API de Anthropic. Por defecto Ollama (gratis).
- **Salida del LLM siempre validada** con Pydantic, con reintentos. Los tests de IA usan un LLM simulado.
- **Diseño oscuro por defecto** con interruptor a claro. Español llano, sin jerga (lista de palabras prohibidas en `docs/DESIGN.md`).
- Secretos solo por variables de entorno. Nunca en git. Las contraseñas de demo solo existen en el seed dev/demo.
- Todo lo ficticio (nombres, testimonios, cifras, "desde 1984") se etiqueta como dato de ejemplo.

## Cómo trabajar

- Trozos verticales (backend + frontend de una funcionalidad) según `docs/ROADMAP.md`. Un commit por trozo, tests en verde y una entrada en `docs/PROGRESS.md` antes de pasar al siguiente.
- Si algo no está especificado, elige la opción más simple, anótala en `docs/PROGRESS.md` y sigue. Pregunta solo si estás bloqueado o si una decisión no se puede deshacer.
- No añadas funcionalidades fuera de alcance, ni siquiera "porque estaban en el diseño de referencia".
- Mantén el estilo: reconstruye las pantallas como componentes reutilizables con los tokens de `docs/DESIGN.md`; no copies el HTML de Stitch, ni uses su CDN ni sus imágenes externas.
- Di siempre con claridad qué verificaste y qué no pudiste verificar.

## Comandos

```bash
make infra          # Postgres, Redis, RabbitMQ
make backend-test   # mvn test
make ai-test        # pytest
cd frontend && npm run dev | npm run typecheck | npm run build
```

## Estado actual

Trozo 0 y Trozo 1 completados y verificados con Docker real. Autenticación con JWT (registro, login, `/auth/me`), roles (`OWNER`/`STAFF`/`MEMBER`) con permisos comprobados en el servidor, migración de planes y membresías, seed de datos de ejemplo (perfil `dev`/`demo`) y frontend con `/acceso`, guardas de ruta por rol y cierre de sesión. `docker compose up` levanta los seis servicios; los 14 tests del backend pasan (incluidos los de Testcontainers, que ya no se omiten) y el flujo de alta/login/redirección por rol se probó de verdad contra Postgres. CORS configurado en `SecurityConfig` (`gymflow.cors.allowed-origins`): sin esto, cualquier llamada del frontend al backend falla en el navegador aunque funcione por `curl`. Detalle completo en `docs/PROGRESS.md`. Siguiente: Trozo 2 (horario y reservas).

## Convenciones

- Documentación en español. Código y commits en inglés o español, pero consistente dentro de un archivo.
- Cada funcionalidad nueva lleva tests. En el backend, Testcontainers para lo que toque la BD (se omiten si no hay Docker; dilo en el informe).
- Endpoints bajo `/api/v1/`. Errores con formato uniforme (ProblemDetail, RFC 9457).
- Antes de dar un trozo por terminado: tests en verde, build correcto y `docs/PROGRESS.md` actualizado.
