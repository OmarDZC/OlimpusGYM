# Roadmap (MVP básico, en trozos verticales)

Alcance y reglas en `PROJECT_BRIEF.md` y `LOGIC.md`; estilo en `DESIGN.md`. **No hay tienda, ni varios centros, ni NFC/pulseras.**

Cada trozo = backend + frontend de una funcionalidad, con tests, un commit y una entrada en `docs/PROGRESS.md`. No empieces el siguiente sin cerrar el anterior (tests en verde, build correcto).

## Trozo 0 - Verificar el esqueleto
- Ejecutar `mvn test` (el backend nunca se compiló: arreglar lo que falle), `pytest` y `npm ci && npm run typecheck && npm run build`.
- Validar `docker compose config` y levantar `make infra`.
- Preparar el frontend: Tailwind con los tokens de `DESIGN.md`, fuentes con `next/font`, tema oscuro/claro con interruptor, layout con navegación superior y componentes base (botón, tarjeta, etiqueta, desplegable accesible).
- Añadir CI mínima (GitHub Actions): backend, IA y frontend.

## Trozo 1 - Autenticación y roles
- Migración V2: planes, membresías. Seed del tenant `olympusgym`, owner, staff, socios y planes (solo perfil dev/demo).
- Registro de socio, login con JWT (con `tenantId` y `role`), `GET /auth/me`. Eliminar `X-Tenant-Id`.
- Filtro por `tenant_id` y tests de aislamiento. Tests de permisos (403 por rol).
- Frontend: página `/acceso` (referencia `v2/acceso`), redirección por rol (`/cuenta` o `/admin`), guardas de ruta y cierre de sesión.

## Trozo 2 - Horario y reservas
- Migración: `class_sessions`, `bookings`. Reglas de `LOGIC.md` §5 (aforo, lista de espera, cancelación hasta 2 h, promoción FIFO, solapes, cupo del plan).
- **Test de concurrencia de aforo** y tests de cada regla.
- `Notifier` que escribe en log.
- Frontend: `/horario` público (referencia `v2/horario-clases`) con reserva (pide acceso si no hay sesión) y "Mis reservas" en `/cuenta`.

## Trozo 3 - Mi cuenta
- `/cuenta` según `v2/mi-cuenta`: próxima clase, constancia semanal, rutina de hoy (aún manual o de seed), mi cuota. Versión móvil con barra inferior (referencia `reference-v1/movil-*`).

## Trozo 4 - Panel de gestión
- `GET /admin/summary`, gestión de sesiones (crear, editar, cancelar, repetir semanalmente), listado de socios y reservas, "socios que conviene contactar". Cambio de plan solo para dueño.
- Reglas de `LOGIC.md` §5 aplazadas del Trozo 2 (necesitan la gestión de sesiones de este trozo):
  - Si staff/owner cancela una sesión completa, todas sus reservas pasan a `CANCELLED` y se avisa a los socios (`Notifier`).
  - Staff/owner pueden cancelar la reserva de cualquier socio sin el límite de 2 horas (a diferencia de `DELETE /me/bookings/{id}`, que es autoservicio del propio socio).
- Frontend `/admin` con navegación superior, rol visible bajo el avatar, textos llanos. Dueño y staff ven menús distintos.

## Trozo 5 - Entrenador con IA
- Servicio de IA: `POST /v1/trainer/plan` con LLM + validación Pydantic + reintentos (tests con LLM simulado). Ajuste semanal para Pro.
- Backend: `POST /me/workout/generate`, `workout_plans`, `workout_logs`, `GET /me/workout/*`, límite por plan.
- Frontend: `/entrenador-ia` con el configurador real y la rutina generada; en `/cuenta`, marcar series.
- Documentar en el README cómo cambiar de proveedor (Ollama, Groq, Gemini).

## Trozo 6 - Asistente de reservas con IA
- `POST /v1/assistant/chat` con tool calling contra la API del backend con el token del socio. Confirmación previa a reservar/cancelar. Defensas de seguridad de `LOGIC.md` §7 con tests (incluido un intento de prompt injection).
- Frontend: chat en "Reservar" (botón "Reservar con IA").

## Trozo 7 - Web pública viva
- `/` y `/tarifas`, `/contacto` según `v2/web-publica` recortada. Franja "En directo" con `GET /public/live`, horario de hoy, secciones, tarifas, testimonios de ejemplo (etiquetados), "Dónde estamos".
- Movimiento de `DESIGN.md` §6 con `prefers-reduced-motion`. Imágenes con licencia y `docs/CREDITS.md`.

## Trozo 8 - Producción y portfolio
- Dockerfiles finales, despliegue en VPS con Caddy (HTTPS), variables de entorno, backups de Postgres, métricas con Actuator.
- README final con diagrama de arquitectura, decisiones técnicas, capturas y vídeo demo de 2 minutos. Etiqueta de "Datos de ejemplo".

## Trozo 9 (opcional) - Pagos
- Stripe Checkout en modo test para las 3 tarifas y webhook que actualiza `memberships`.
