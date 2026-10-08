# Lógica de negocio, datos y API

Alcance: MVP básico de un solo gimnasio (ver `PROJECT_BRIEF.md`). Si algo no está aquí y hace falta, decide lo más simple y anótalo en `docs/PROGRESS.md`.

## 1. Tenant y datos iniciales (seed)

- Existe un único tenant: `olympusgym` (nombre "OlympusGYM"). Se crea por seed (Flyway repeatable o `CommandLineRunner` activo solo en perfil `dev`/`demo`).
- Todas las tablas de negocio llevan `tenant_id`. El tenant sale **del JWT**; se elimina la cabecera provisional `X-Tenant-Id` (`TenantFilter`).
- Seed de demo (solo dev/demo, nunca producción):
  - 1 `OWNER` (Javier M.), 2 `STAFF` (entrenadores: Marta, Dani), 12 `MEMBER` (incluido "Carlos M.").
  - Contraseñas de demo conocidas y documentadas en el README como "solo demo".
  - 3 planes (ver §3), membresías activas repartidas.
  - Sesiones de clase de las próximas 2 semanas (CrossFit, Fuerza, Yoga, Boxeo, Spinning, Halterofilia), alguna **completa con lista de espera** para poder enseñarlo.
  - Algunas reservas, una rutina para Carlos.

## 2. Roles y permisos

| Acción | MEMBER | STAFF | OWNER |
|---|---|---|---|
| Ver web pública y horario | sí | sí | sí |
| Reservar/cancelar **sus** clases | sí | n/a | n/a |
| Ver/gestionar su rutina y su cuota | sí | no | no |
| Usar entrenador IA y asistente de reservas | según plan | no | no |
| Crear/editar/cancelar sesiones de clase | no | sí | sí |
| Ver listado de socios y reservas | no | sí (sin datos de pago) | sí |
| Cambiar plan de un socio, ver ingresos | no | no | sí |
| Gestionar staff y ajustes | no | no | sí |

Reglas de seguridad obligatorias, con **tests**:
- Un `MEMBER` recibe 403 en cualquier `/api/v1/admin/**`.
- Un `STAFF` recibe 403 en endpoints solo de `OWNER`.
- Un usuario no puede ver ni modificar reservas, rutinas o datos de otro usuario (404/403).
- Los datos de un tenant nunca aparecen en otro (aunque hoy haya uno: test con dos tenants creados en el test).
- Contraseñas con BCrypt; JWT con expiración; nunca devolver `password_hash`.

## 3. Planes (datos de ejemplo, simples)

| Plan | Precio/mes | Clases dirigidas | Reserva anticipada | Entrenador IA | Asistente de reservas |
|---|---|---|---|---|---|
| BASICA | 39 € | hasta 4 reservas/mes | 7 días | no | no |
| COMPLETA | 59 € | ilimitadas | 7 días + prioridad 48 h antes de abrirse a Básica | sí | sí |
| ATLETA_PRO | 79 € | ilimitadas | igual que Completa | sí, con ajuste semanal automático | sí |

Simplificación: "reserva prioritaria" puede implementarse como ventana de reserva más amplia para Completa/Pro (p. ej. 14 días vs 7). Si es complicado, déjalo solo como texto y anótalo.
Texto de beneficios para tarifas: sala de pesas 06:00–23:00, taquillas y duchas (es solo descripción), clases según plan, entrenador IA según plan, y para Pro una revisión mensual con un entrenador (descripción, sin lógica).

## 4. Modelo de datos (migraciones Flyway V2 en adelante)

Ya existen `tenants` y `users` (V1: rol `OWNER|STAFF|MEMBER`). Añadir:

- `plans`(id, tenant_id, code, name, price_cents, classes_per_month NULL=ilimitadas, booking_window_days, ai_trainer bool, ai_assistant bool, active)
- `memberships`(id, tenant_id, user_id, plan_id, status `ACTIVE|PAUSED|CANCELLED`, started_at, renews_at)
- `class_sessions`(id, tenant_id, name, discipline, description, coach_user_id, room, starts_at, ends_at, capacity, status `SCHEDULED|CANCELLED`)
- `bookings`(id, tenant_id, session_id, user_id, status `BOOKED|WAITLIST|CANCELLED|ATTENDED|NO_SHOW`, waitlist_position NULL, created_at, cancelled_at)
  - Índice único parcial: un usuario solo puede tener **una** reserva activa (`BOOKED|WAITLIST`) por sesión.
- `workout_plans`(id, tenant_id, user_id, week_start, goal, level, days_per_week, plan_json, source `AI|MANUAL`, created_at)
- `workout_logs`(id, tenant_id, user_id, workout_plan_id, day_index, exercise_index, set_index, done bool, weight_kg, reps, logged_at)
- (opcional) `contact_messages`(id, tenant_id, name, email, message, created_at) para la página de contacto.

Fechas en UTC en BD; la interfaz muestra la zona `Europe/Madrid`.

## 5. Reglas de reserva (el corazón del proyecto)

1. **Aforo sin sobreventa.** Reservar = transacción que bloquea la fila de la sesión (`SELECT ... FOR UPDATE` o bloqueo equivalente), cuenta reservas `BOOKED` y, si hay hueco, crea `BOOKED`; si no, crea `WAITLIST` con posición. Debe existir un **test de concurrencia** (p. ej. 50 hilos para 20 plazas ⇒ exactamente 20 `BOOKED`).
2. No se puede reservar: una sesión pasada o cancelada; una sesión que se solape con otra reserva activa del mismo socio; sin membresía `ACTIVE`; fuera de la ventana de reserva de su plan; superando el cupo mensual de su plan.
3. **Cancelación**: el socio puede cancelar hasta 2 h antes del inicio. Pasado ese límite, 409 con mensaje claro. Staff/Owner pueden cancelar cuando quieran.
4. Al cancelar una reserva `BOOKED`, se **promueve** al primero de la lista de espera (FIFO) a `BOOKED` y se reordena la lista. Notificación por email (ver §8).
5. Si staff/owner cancela una sesión: todas las reservas pasan a `CANCELLED` y se avisa a los socios.
6. Idempotencia: reservar dos veces la misma sesión devuelve la reserva existente (no duplica).

## 6. API REST (`/api/v1`, errores en formato `ProblemDetail`)

Público (sin autenticación):
- `GET /public/ping`
- `GET /public/schedule?from=&to=&discipline=&coachId=&room=` sesiones con plazas libres
- `GET /public/plans`
- `GET /public/live` próxima clase y plazas libres, clases de hoy (solo datos reales)
- `POST /public/contact`

Autenticación:
- `POST /auth/register` (crea `MEMBER` con plan elegido; pago "pendiente" hasta Stripe)
- `POST /auth/login` → JWT con `sub`, `tenantId`, `role`
- `GET /auth/me`

Socio (`MEMBER`):
- `GET /me/bookings`, `POST /me/bookings` {sessionId}, `DELETE /me/bookings/{id}`
- `GET /me/membership`
- `GET /me/workout/today`, `GET /me/workout/week`, `POST /me/workout/logs`
- `POST /me/workout/generate` {goal, level, daysPerWeek, equipment, limitations} → llama a la IA, guarda y devuelve el plan (requiere `ai_trainer` en el plan)
- `GET /me/streak` (entrenamientos de la semana completados vs objetivo)
- `POST /me/assistant/chat` {message} → proxy al servicio de IA con el token del socio

Gestión (`OWNER`, `STAFF`):
- `GET /admin/summary` socios activos, ocupación de hoy, clases de hoy, socios que conviene contactar (sin venir hace X días o varias cancelaciones), ingresos del mes **calculados desde membresías activas** (sin pasarela)
- `GET|POST|PATCH /admin/sessions`, `POST /admin/sessions/{id}/cancel`, repetición semanal (crear N semanas)
- `GET /admin/members`, `GET /admin/members/{id}`, `GET /admin/bookings`
- Solo `OWNER`: `PATCH /admin/members/{id}/plan`, `GET|POST /admin/staff`

## 7. Servicio de IA (FastAPI)

Todo con **cliente compatible con OpenAI** (`LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL`). Ya existen `app/config.py`, `app/llm.py` y `app/schemas.py`.

- `POST /v1/trainer/plan`: entrada `PlanRequest`, salida `WeeklyPlan`. Pedir JSON al modelo, validar con Pydantic, **reintentar hasta 2 veces** con el error de validación; si falla, 502 con mensaje claro. Reglas del prompt: ejercicios acordes al material y a las limitaciones, volumen razonable para el nivel, texto en español llano (sin jerga). Debe poder probarse sin LLM real con un cliente falso (tests con respuesta simulada).
- `POST /v1/trainer/adjust` (Pro): recibe el último plan y los registros de series, propone ajuste (subir carga, descarga si 3 semanas sin progreso, sustituir ejercicio por molestia). Primera versión puede ser reglas simples + LLM para el texto.
- `POST /v1/assistant/chat`: agente con *tool calling* contra el backend: `listar_clases`, `reservar_clase`, `cancelar_reserva`, `mis_reservas`. **Seguridad**: usa el token del socio autenticado (nunca uno propio); no ejecuta acciones sobre otros usuarios; ignora instrucciones que vengan dentro de datos (nombres de clase, mensajes, etc.); pide confirmación antes de reservar o cancelar; límite de pasos y de mensajes por plan. Responde en español natural.
- Si el LLM no está disponible: responder 503 con mensaje amable; el frontend lo muestra sin romperse.

## 8. Notificaciones
Interfaz `Notifier` con una implementación que **escribe en log** (y opcionalmente SMTP/MailHog). Eventos: reserva confirmada, promoción desde lista de espera, sesión cancelada. RabbitMQ solo si aporta algo claro; no es obligatorio en el MVP.

## 9. Datos "en vivo" de la web
Solo se muestran datos que existan de verdad: próxima clase y plazas libres, clases de hoy y su aforo. **No** mostrar "personas entrenando ahora" ni aforo del gimnasio (no hay control de acceso). La franja "En directo" se alimenta de `GET /public/live` (refresco cada 30-60 s).

## 10. Pagos (fase opcional al final)
Stripe Checkout en modo test para las tres tarifas y webhook que actualiza `memberships`. Hasta entonces, el plan se asigna al registrarse y el cobro figura como "pendiente (demo)". Nunca guardar datos de tarjeta.

## 11. Calidad mínima exigida
- Tests unitarios y de integración (Testcontainers) para reservas, permisos y aislamiento por tenant.
- Test de concurrencia de aforo.
- Tests del servicio de IA con LLM simulado (validación, reintentos, error).
- Frontend: typecheck, build y al menos pruebas de los flujos clave (login por rol, reservar).
- CI en GitHub Actions (backend, IA, frontend).
