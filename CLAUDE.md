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
- Infra local: `docker-compose.yml` (Postgres con pgvector, Redis, Ollama opcional). Redis solo si aporta algo; no es obligatorio en el MVP. RabbitMQ se quitó en el Trozo 8a-1: nada lo usaba (ni `@RabbitListener` ni `RabbitTemplate` en el backend).
- Producción: `docker-compose.prod.yml` (Postgres normal, sin pgvector — no se usa —, y Caddy como proxy inverso con HTTPS automático). Ver "Despliegue en producción" en `README.md`.

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

- Trozos verticales (backend + frontend de una funcionalidad) según `docs/ROADMAP.md`. Tests en verde y una entrada en `docs/PROGRESS.md` antes de pasar al siguiente.
- Si algo no está especificado, elige la opción más simple, anótala en `docs/PROGRESS.md` y sigue. Pregunta solo si estás bloqueado o si una decisión no se puede deshacer.
- No añadas funcionalidades fuera de alcance, ni siquiera "porque estaban en el diseño de referencia".
- Mantén el estilo: reconstruye las pantallas como componentes reutilizables con los tokens de `docs/DESIGN.md`; no copies el HTML de Stitch, ni uses su CDN ni sus imágenes externas.
- Di siempre con claridad qué verificaste y qué no pudiste verificar.

## Git

- **Sin firma de IA** en commits, ramas ni pull requests (ver `.claude/settings.json`,
  `attribution.commit`/`attribution.pr` vacíos).
- **Nunca commits directos a `main`.** Una rama por trozo (o por sub-parte del trozo, si el
  encargo lo pide en varios commits), pull request, y merge con **squash** cuando la CI esté en
  verde.
- **Un commit por funcionalidad**, mensaje `tipo: descripción corta en español` (p. ej. `feat:
  límite de uso de la IA por socio y por día`, `fix: cabecera CORS en producción`). Los tests de
  esa funcionalidad van **dentro** del mismo commit, nunca en uno aparte.
- Si la CI falla en una rama abierta, el arreglo va en esa misma rama (commit nuevo), no en una
  rama ni PR aparte.
- El historial ya existente (commits directos a `main` de antes de esta regla) no se reescribe
  todavía: esto aplica desde ahora en adelante.
- **El flujo completo (rama, pull request, CI y squash) lo haces tú solo, sin preguntar**,
  usando `gh` si está disponible o la API de GitHub con el token del credential helper de git
  si no (`git credential fill`; nunca imprimas el token). Si un check de CI falla, averigua la
  causa real con la salida mínima necesaria (el check que falla y las últimas ~60 líneas del
  job, no el log entero), arréglalo en la misma rama (commit nuevo de tipo `fix:`) y reintenta.
  Solo te paras a preguntar si la misma pull request falla dos veces por causas distintas.

## Reglas de ahorro (uso limitado del plan)

- Sin navegador ni capturas de pantalla salvo que un fallo no se pueda comprobar de otra
  forma (tests y `curl` primero).
- Lee solo los archivos que necesites para la tarea concreta; no releas documentación entera
  "por si acaso".
- Durante el desarrollo, ejecuta solo los tests del área que estás tocando; la suite completa
  (backend + frontend) una sola vez, al final.
- Si la tarea usa el LLM real (no el simulado de los tests), manténlo a un puñado de llamadas
  en total, espaciadas para no chocar con límites de cuota.
- Informe final corto (unas 15 líneas): qué se arregló, la causa real de cada fallo (no la
  sospechada si resultó ser otra), qué se verificó y qué no. Sin documentos nuevos aparte de
  una entrada breve en `docs/PROGRESS.md`.

## Comandos

```bash
make infra          # Postgres, Redis
make backend-test   # mvn test
make ai-test        # pytest
cd frontend && npm run dev | npm run typecheck | npm run build
```

## Estado actual

Trozos 0, 1, 2, 3, 4 y 5 (backend y frontend) completados, verificados con Docker real. Autenticación con JWT y roles (Trozo 1). Horario y reservas (Trozo 2): aforo sin sobreventa con bloqueo de fila (`BookingService`), lista de espera FIFO, cancelación con límite de 2 horas, notificaciones por log. Mi cuenta (Trozo 3): rutina de la semana, rutina de hoy con casillas por serie, constancia semanal, cuota, y `/cuenta` repartida en subpáginas con submenú y barra inferior móvil. Panel de gestión (Trozo 4): backend con `GET /admin/summary` (ingresos solo para OWNER), gestión de sesiones con repetición semanal, listado de socios (precio solo para OWNER), cambio de tarifa (solo OWNER), cancelar sesión completa y cancelar cualquier reserva sin el límite de 2h (staff/owner); frontend `/admin` con navegación superior sin barra lateral, rol visible bajo el avatar, y el mismo contenido gateado por rol (ingresos, precios, cambio de tarifa solo para OWNER). Entrenador con IA (Trozo 5): servicio de IA con `POST /v1/trainer/plan` (LLM compatible con OpenAI, validación Pydantic, hasta 2 reintentos, 503 si no hay proveedor disponible); backend con `POST /me/workout/generate` (exige `ai_trainer` en la tarifa, sustituye la rutina de la semana si ya había una) y `/entrenador-ia` con el configurador real. Tests end-to-end con Playwright (`e2e/`, también en CI): login por rol, reservar/cancelar una clase, y que el staff cancele una sesión y la reserva del socio quede cancelada. Asistente de reservas con IA (Trozo 6, backend y frontend): servicio de IA con `POST /v1/assistant/chat` (tool calling: listar_clases, mis_reservas, reservar_clase, cancelar_reserva; reservar/cancelar solo proponen, nunca ejecutan, hasta que el socio confirma en una petición aparte que valida el backend real); backend con `POST /me/assistant/chat` (exige `ai_assistant` en la tarifa, reenvía con el token del propio socio); frontend con el botón "Reservar con IA" en `/horario` que abre un chat accesible (foco gestionado, `aria-live`, Escape, sin emojis/puntos suspensivos) con tarjeta de confirmación antes de ejecutar nada. Tests end-to-end con Playwright (`e2e/`, también en CI): login por rol, reservar/cancelar una clase, que el staff cancele una sesión, y que el socio reserve hablando con el asistente (con un LLM simulado, `e2e/fake-llm`, activado solo por `docker-compose.e2e.yml`). `docker compose up` levanta los seis servicios; los 52 tests del backend y los 19 del servicio de IA pasan. CORS configurado en `SecurityConfig` (`gymflow.cors.allowed-origins`). Detalle completo en `docs/PROGRESS.md`. Pendiente: la posible carrera entre reservas solapadas simultáneas del mismo socio, y no hay un LLM real configurado en esta máquina (ver PROGRESS.md). Siguiente: Trozo 7 (web pública viva) según `docs/ROADMAP.md`.

## Convenciones

- Documentación en español. Código y commits en inglés o español, pero consistente dentro de un archivo.
- Cada funcionalidad nueva lleva tests. En el backend, Testcontainers para lo que toque la BD (se omiten si no hay Docker; dilo en el informe).
- Endpoints bajo `/api/v1/`. Errores con formato uniforme (ProblemDetail, RFC 9457).
- Antes de dar un trozo por terminado: tests en verde, build correcto y `docs/PROGRESS.md` actualizado.
