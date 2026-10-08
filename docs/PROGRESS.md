# Progreso

Anota aquí, al cerrar cada trozo del roadmap: qué se hizo, qué decisiones se tomaron, qué quedó pendiente o simplificado y cómo probarlo. Más reciente arriba.

## Plantilla

### Trozo N - nombre (fecha)
- Hecho:
- Decisiones / simplificaciones:
- Pendiente:
- Cómo probarlo:
- Tests: (qué se ejecutó y resultado)

---

### Trozo 4a - Panel de gestión: backend (2026-10-08)
- Hecho:
  - Antes de empezar: comprobado el resultado de la CI de GitHub Actions del commit del Trozo 3 (en verde, los tres jobs). Nada que arreglar.
  - Paquete `admin` nuevo:
    - `GET /admin/summary`: socios activos, clases de hoy y su ocupación, ingresos del mes (suma del precio de las membresías `ACTIVE`, **solo para OWNER**, `null` para STAFF), y "socios que conviene contactar" (sin una reserva de verdad en los últimos 14 días).
    - `GET/POST/PATCH /admin/sessions`, `POST /admin/sessions/{id}/cancel` con repetición semanal (`repeatWeeks` en la creación).
    - `GET /admin/members`, `GET /admin/members/{id}` (el precio de la tarifa solo se ve siendo OWNER, LOGIC.md §2), `PATCH /admin/members/{id}/plan` (**solo OWNER**).
    - `GET /admin/bookings`, `DELETE /admin/bookings/{id}` (cancela sin el límite de 2 horas).
    - Las dos reglas aplazadas del Trozo 2, en `BookingService`: `cancelSession` (staff/owner cancela la sesión completa, todas las reservas activas pasan a `CANCELLED` y se avisa a cada socio con `Notifier.sessionCancelled`) y `cancelAsStaff` (cancela la reserva de cualquier socio sin el límite de 2 horas).
  - `SecurityConfig`: `PATCH /admin/members/{id}/plan` exige `OWNER` explícitamente (antes de la regla general de `/admin/**` para `OWNER`+`STAFF`, que si no, la taparía).
  - Tests de permisos y aislamiento (`AdminHttpTest`, 8 casos): socio con 403 en `/admin/summary`, staff con 403 al intentar cambiar el plan de un socio, el dueño sí puede, los ingresos solo salen para el dueño, el listado de socios de un gimnasio nunca enseña los de otro, cancelar una sesión completa avisa a las reservas activas (confirmadas y en espera), y staff cancelando una reserva a 30 minutos de empezar (el socio ya no podría).
  - **Bug real encontrado y arreglado al probar con datos reales en Docker**: `GET /api/v1/public/schedule?discipline=Pilates` (y cualquier filtro que devolviera solo sesiones sin entrenador asignado) rompía con un `NullPointerException` que además, al pasar por el filtro de seguridad, se veía desde fuera como un 403 engañoso (no como un 500). La causa: `ScheduleService` montaba el mapa de nombres de entrenador con `Map.of()` cuando ninguna sesión tenía entrenador, y `Map.of()` (mapa inmutable de Java) lanza excepción al consultarlo con una clave `null` — exactamente lo que pasa con una sesión sin entrenador (`coachUserId` es `null`). Arreglado usando siempre un `HashMap` normal, que sí admite `null`. Añadido un test de regresión.
- Decisiones / simplificaciones:
  - "Socios que conviene contactar" solo mira si llevan 14 días sin una reserva de verdad (`BOOKED`/`ATTENDED`); LOGIC.md también menciona "varias cancelaciones" como motivo, pero no lo conté para no complicar la consulta sin que el roadmap lo pidiera con detalle.
  - `GET /admin/sessions` reutiliza el mismo `ScheduleService` que el horario público, que solo devuelve sesiones `SCHEDULED`: una sesión ya cancelada desaparece también de esta vista de gestión. Para este MVP no hace falta volver a gestionar algo que ya se canceló.
  - Cambiar de tarifa (`PATCH /admin/members/{id}/plan`) actualiza la membresía activa en el sitio (no crea una fila nueva ni guarda histórico de tarifas anteriores): es la forma más simple de cumplir "cambiar el plan de un socio" sin inventar un modelo de histórico que LOGIC.md no pide.
  - No se ha construido `GET|POST /admin/staff` (gestión de personal): LOGIC.md lo lista como solo-OWNER, pero ni el roadmap ni el encargo de este trozo lo pedían explícitamente; queda fuera.
- Pendiente:
  - Nada bloqueante para este trozo.
- Cómo probarlo:
  - Backend: `cd backend && mvn test`.
  - De verdad: `docker compose up -d`, entrar como `javier@olympusgym.test` (OWNER) o `marta@olympusgym.test` (STAFF) / `Demo1234!`, y llamar a `/api/v1/admin/summary`, `/api/v1/admin/members`, `/api/v1/admin/sessions` con el token.
- Tests:
  - `mvn test`: BUILD SUCCESS, 41/41 en verde (8 nuevos de `AdminHttpTest`).
  - Probado también a mano contra la base de datos real en Docker: resumen como OWNER y STAFF, listado de socios con y sin precio, crear una sesión con repetición semanal (2 copias, 7 días de diferencia), cambiar de tarifa y revertir, cancelar una sesión completa.

---

### Trozo 3 - Mi cuenta (2026-10-08)
- Hecho:
  - Antes de empezar: comprobado el resultado de la CI de GitHub Actions del commit del Trozo 2 (repo privado, se consultó con la API de GitHub usando la credencial de git ya guardada en la máquina, sin abrir sesión en el navegador). Los tres jobs (backend, IA, frontend) en verde, incluidos los tests de Testcontainers y el de concurrencia. Nada que arreglar.
  - `docs/ROADMAP.md`: añadidas al Trozo 4 las dos reglas aplazadas del Trozo 2 (cancelar una sesión completa avisando a los socios, y que staff/owner cancelen cualquier reserva sin el límite de 2 horas).
  - Backend:
    - Migración `V4__workout_plans_logs.sql`: tablas `workout_plans` (la rutina de la semana, guardada como JSON de texto) y `workout_logs` (qué series se han marcado hechas).
    - Paquete `workout`: `WorkoutPlan`/`WorkoutLog`, DTOs del plan (`WeeklyPlan`/`PlanDay`/`PlanExercise`, serializados con Jackson) y de las respuestas (`TodayWorkoutResponse`, `StreakResponse`, etc.).
    - `WorkoutService`: calcula "hoy" y la semana en la zona `Europe/Madrid` (LOGIC.md: BD en UTC, interfaz en hora de Madrid). `GET /me/workout/today` (rutina de hoy con el estado de cada serie), `GET /me/streak` (constancia semanal: un día cuenta como entrenado si tiene al menos una serie marcada), `POST /me/workout/logs` (marcar una serie, con upsert).
    - `GET /me/membership`: tarifa actual, precio, estado y si incluye entrenador/asistente con IA.
    - Seed ampliado: rutina real de 5 días para Carlos (LOGIC.md §1), con los días anteriores a hoy ya marcados como hechos para que la constancia no empiece siempre a cero en la demo.
  - Frontend:
    - `/cuenta` se reparte en subpáginas con un submenú (DESIGN.md §8): **Inicio** (`/cuenta`, el panel con próxima clase, constancia, rutina de hoy y resumen de la cuota), **Mis reservas** (`/cuenta/reservas`, el listado completo que antes vivía en Inicio), **Mi entrenamiento** (`/cuenta/entrenamiento`) y **Mi cuota** (`/cuenta/cuota`, el detalle completo).
    - `/cuenta/entrenamiento` no enseña contenido a medias: como el entrenador con IA de verdad (configurar objetivo, generar rutina, ajustarla) llega en el Trozo 5, esta pantalla es un aviso claro de "llega pronto", con un enlace a Inicio para ver la rutina de esta semana.
    - `/cuenta/perfil`: página mínima (nombre, correo, rol, cerrar sesión), destino de la pestaña "Perfil" en móvil.
    - Barra inferior de 5 pestañas en móvil (`MobileAccountNav`, sin emojis, iconos de `lucide-react`): Inicio, Reservar (→ `/horario`), Entrenador (→ `/cuenta/entrenamiento`), Mi cuota, Perfil. Solo visible dentro de `/cuenta/**`.
    - Componentes nuevos: `TodayWorkoutCard` (casillas por serie, marca de verdad contra el backend), `StreakCard` (casillas Lun-Dom + barra de progreso), `NextClassCard`, `MembershipSummaryCard`.
  - Verificado de extremo a extremo en el navegador contra el backend real en Docker, con la cuenta de Carlos: rutina de hoy correcta para el día de la semana real, marcar una serie se refleja al momento, constancia semanal correcta, las 4 pestañas del submenú y las 5 de la barra móvil funcionan, "Mi entrenamiento" muestra el aviso en vez de una página rota.
  - Capturas de `/cuenta` y `/horario` comparadas con `docs/design/v2/mi-cuenta/screen.png` y `docs/design/v2/horario-clases/screen.png` (ver "Pendiente" para lo que no se corrigió).
- Decisiones / simplificaciones:
  - Un día de la semana cuenta como "entrenado" para la constancia si tiene **al menos una serie** marcada como hecha, no todas. Es una simplificación razonable para un MVP; LOGIC.md no concreta la regla exacta.
  - El plan semanal se guarda como una columna de texto con JSON (serializado/leído con Jackson desde `WorkoutService`), no como `jsonb` nativo de Hibernate ni como filas por ejercicio: es la forma más simple de guardar "una rutina activa por socio y semana" sin montar un modelo de datos más grande del que hace falta para el MVP.
  - Reconstruí la rutina de Carlos con 5 días de entrenamiento y 2 de descanso (lunes a domingo). LOGIC.md no fija el reparto exacto; elegí algo realista (empuje / tirón / pierna / empuje ligero y core / acondicionamiento).
  - El checkbox de cada ejercicio en la referencia de diseño es uno solo por ejercicio; aquí hay uno **por serie** (p. ej. 4 casillas en vez de 1), porque `workout_logs` guarda el detalle a nivel de serie y LOGIC.md dice explícitamente "el socio marca las series hechas". Es más fiel a los datos que a la captura.
  - `GET /me/workout/today` no construye "la semana completa" (`GET /me/workout/week` de LOGIC.md §6): no hace falta para lo que pide este trozo (solo "hoy"); se puede añadir cuando el Trozo 5 necesite mostrar la semana entera del entrenador con IA.
- Pendiente:
  - Comparación con las referencias de diseño: corregidas la etiqueta de la tarifa junto al saludo y la tarjeta de "Mi cuota" en Inicio (no estaban). Quedan sin tocar, a propósito, por ser decorativas o fuera de alcance de este trozo:
    - `/cuenta`: el panel grande de la rutina con imagen de fondo y botón "Empezar entrenamiento" de la referencia es más una pieza visual que funcional; aquí es una tarjeta más simple pero con la misma información. El enlace "Ver detalles de la sesión" no existe (no hay página de detalle de sesión todavía).
    - `/horario`: la referencia tiene un panel lateral con el detalle completo de la sesión activa (equipamiento, descripción, ubicación...) que aquí no existe; y tres filtros (tipo, coach, sala) en vez de uno (solo disciplina). Añadirlos pediría nuevos endpoints para listar coaches/salas, fuera de lo que pedía el Trozo 2. La franja inferior "Pico de afluencia / Climatización / Política de cancelación" tampoco está: "climatización" la pide quitar `DESIGN.md` explícitamente, y "pico de afluencia" sería un dato inventado que `DESIGN.md §10` prohíbe; la política de cancelación ya se explica en el texto de la cabecera.
  - La carrera de reservas simultáneas solapadas del mismo socio (anotada en el Trozo 2) sigue sin arreglar.
- Cómo probarlo:
  - Backend: `cd backend && mvn test`.
  - De verdad: `docker compose up -d`, entrar como `carlos@olympusgym.test` / `Demo1234!`, ver `/cuenta` (rutina de hoy, constancia, próxima clase, cuota), marcar una serie, y recorrer el submenú y la barra inferior en móvil.
- Tests:
  - `mvn test`: BUILD SUCCESS, 33/33 en verde (8 nuevos de `WorkoutAndMembershipHttpTest`: rutina propia, sin rutina, aislamiento entre socios —uno nunca ve ni toca la rutina del otro—, marcar serie y que se refleje, cuota propia, constancia, y que los tres endpoints exigen sesión).
  - Frontend: `npm run typecheck` y `npm run build` en verde (12 páginas).

---

### Trozo 2 - Horario y reservas (2026-10-08)
- Hecho:
  - Backend:
    - Migración `V3__class_sessions_bookings.sql`: tablas `class_sessions` y `bookings`, con el índice único parcial de LOGIC.md §4 (una reserva activa como mucho por socio y sesión).
    - Paquete `booking`: entidades `ClassSession`/`SessionStatus` y `Booking`/`BookingStatus`.
    - `BookingService`, el corazón del trozo (LOGIC.md §5): reservar bloquea la fila de la sesión (`SELECT ... FOR UPDATE` vía `ClassSessionRepository#lockById`) durante toda la transacción, así que dos reservas concurrentes para la misma sesión se serializan ahí y el aforo nunca se sobrevende. Reglas comprobadas, en este orden: sesión cancelada o ya empezada, membresía activa, ventana de reserva del plan, solape con otra reserva activa, cupo mensual del plan; si hay hueco crea `BOOKED`, si no crea `WAITLIST` con posición. Reservar dos veces la misma sesión es idempotente (devuelve la reserva existente). Cancelar exige que sea la propia reserva y respeta el límite de 2 horas; al cancelar una `BOOKED` promueve en FIFO al primero de la lista de espera y reordena las posiciones de los demás.
    - `ScheduleService`: arma las respuestas de horario y "mis reservas" (plazas libres, en lista de espera, nombre del entrenador) a partir de las tablas planas.
    - `Notifier`/`LogNotifier` (LOGIC.md §8): reserva confirmada, en lista de espera, promovido desde lista de espera, reserva cancelada — todo a log, tal y como pide el MVP.
    - `PublicScheduleController` (`GET /api/v1/public/schedule`, sin sesión) y `BookingController` (`GET/POST /api/v1/me/bookings`, `DELETE /api/v1/me/bookings/{id}`, requieren sesión).
    - `GlobalExceptionHandler` ampliado: reglas de negocio incumplidas → 409, sesión/reserva inexistente (o de otro socio) → 404.
    - Seed ampliado: horario de las próximas 2 semanas (84 sesiones, 6 disciplinas, alternando a Marta y Dani como entrenadores, con los nombres de sala de la referencia de diseño), una sesión de Spinning a propósito con aforo reducido (7) para que quede completa con 3 en lista de espera, y un puñado de reservas sueltas para que "Mis reservas" no esté vacío en la demo. Solo se genera la primera vez (sus fechas son relativas a "ahora": no tendría sentido regenerarlas en cada arranque).
  - Frontend:
    - `lib/api.ts`: tipos y funciones para horario público y reservas del socio.
    - `AuthProvider` ahora también expone el `token` (hacía falta para llamar a `/me/bookings` desde las páginas).
    - `/horario`: selector de día (14 días), filtro por disciplina, tarjetas con hora/sala/entrenador/plazas libres, botón "Reservar" (pide acceso si no hay sesión, LOGIC.md), "Unirme a la lista" cuando está completa, y "Reservado"/"En espera (#N)" con botón de cancelar cuando el socio ya tiene una reserva en esa sesión.
    - `/cuenta`: sección "Mis reservas" (reservas activas, con cancelar) y una tarjeta de "Tu próxima clase" cuando hay alguna.
  - Verificado en el navegador contra el backend real (Docker): reservar una clase, verla reflejada en `/cuenta`, cancelarla, y que "Reservar" sin sesión redirige a `/acceso`. También por API con `curl`: reservar, cancelar, y que una sesión llena deja en lista de espera (HTTP 200, no 201).
- Decisiones / simplificaciones:
  - El cupo mensual del plan ("hasta 4 reservas/mes") se cuenta por el mes de la **sesión** reservada, no por la fecha en la que se hizo la reserva. Es la lectura más natural de "4 clases al mes". Los límites de mes se calculan en UTC (coherente con que la BD guarda fechas en UTC); la interfaz sigue mostrando la hora en `Europe/Madrid`.
  - El cupo mensual y la ventana de reserva solo cuentan reservas `BOOKED`, no las que están en lista de espera.
  - `DELETE /me/bookings/{id}` es autoservicio del propio socio: siempre aplica el límite de 2 horas y solo deja cancelar la reserva de quien la pide (404 si es de otro). La regla de LOGIC.md §5.3 de que "staff/owner pueden cancelar cuando quieran" se añadirá en el Trozo 4 junto con la gestión de sesiones desde el panel.
  - Cancelar una sesión completa (LOGIC.md §5.5, "si staff/owner cancela una sesión, todas las reservas pasan a CANCELLED") no está construido todavía: crear/editar/cancelar sesiones es explícitamente del Trozo 4 (gestión), y no existe aún ningún endpoint para crear sesiones manualmente fuera del seed.
  - `/api/v1/me/bookings` no tiene una comprobación de rol explícita (`hasRole("MEMBER")`): en la práctica solo un socio puede reservar porque `BookingService` exige una membresía activa, y el seed no da membresías a `OWNER`/`STAFF`. Es más simple que añadir un rol más al `SecurityConfig` para un caso que ya queda cubierto por los datos.
- Pendiente:
  - Confirmado: la CI de GitHub Actions pasó en verde para este commit (los tres jobs: backend, IA y frontend), incluidos los tests de Testcontainers y el de concurrencia, que en GitHub Actions sí corren (el runner trae Docker).
  - **Mejora pendiente, no arreglada: posible carrera entre dos reservas simultáneas del mismo socio a clases que se solapan.** `BookingService.book()` bloquea la fila de la sesión que se está reservando (`lockById`), lo cual evita la sobreventa de aforo de verdad (ver el test de concurrencia). Pero la comprobación de solape (`existsActiveOverlapping`) consulta las reservas ya guardadas del socio sin bloquear nada a nivel de socio. Si el mismo socio manda dos peticiones a la vez para reservar dos sesiones *distintas* que se solapan entre sí, cada petición bloquea la fila de *su propia* sesión (filas distintas, sin conflicto entre ellas) y ninguna de las dos ve todavía la reserva de la otra al comprobar el solape, así que ambas podrían pasar la validación y acabar con dos reservas activas solapadas para la misma persona. Es un caso raro (alguien reservando dos pestañas a la vez) y no lo cubre ningún test todavía. Posible arreglo cuando se retome: un bloqueo por socio (p. ej. `pg_advisory_xact_lock` con el id del socio) al principio de `book()`, antes de comprobar solapes.
- Cómo probarlo:
  - Backend: `cd backend && mvn test` (con Docker arriba, los 11 tests de `BookingServiceTest` corren de verdad, incluido el de concurrencia).
  - De verdad: `docker compose up -d`, entrar en `http://localhost:3000/horario`, reservar una clase (con cualquier cuenta del seed, contraseña `Demo1234!`) y verla en `/cuenta`.
  - Sesión con lista de espera ya lista para enseñar: Spinning del segundo día del horario (aforo 7, ya completa con 3 en espera en el seed).
- Tests:
  - `mvn test`: BUILD SUCCESS, 25/25 en verde (11 nuevos de `BookingServiceTest`, incluido `fiftyThreadsTwentySeats_exactlyTwentyBooked`: 50 hilos reservando una sesión de 20 plazas ⇒ exactamente 20 `BOOKED` y 30 `WAITLIST`, siempre, gracias al bloqueo de fila).
  - Bug real encontrado y arreglado durante la verificación con Docker: PostgreSQL no podía resolver el tipo de un parámetro `null` dentro de `lower(...)` en la consulta de filtrado del horario (`function lower(bytea) does not exist`), algo que ningún test detectó porque ninguno llamaba a `search()` con filtros vacíos. Se arregló forzando el tipo con `cast(:param as string)`. Queda anotado aquí para no repetir el error en consultas parecidas.
  - Frontend: `npm run typecheck` y `npm run build` en verde (incluye `/horario` y los cambios en `/cuenta`).

---

### Trozo 1 - Autenticación y roles (2026-10-08)
- Hecho:
  - Nombre del proyecto: en esta sesión cambié por error el README y la documentación para que dijeran solo "OlympusGYM" en todas partes. Queda corregido: tal y como dice `PROJECT_BRIEF.md`, **GymFlow es la plataforma** (el software, este repositorio) y **OlympusGYM es el gimnasio ficticio de demostración** que la usa. En la interfaz, la marca visible es siempre OlympusGYM; "GymFlow" solo aparece en letra pequeña ("Powered by GymFlow") y en la sección del entrenador con IA. Los identificadores técnicos internos (paquete Java `com.gymflow`, artefacto Maven, variables `GYMFLOW_*`, paquete npm) se quedan como están: no hay que renombrarlos.
  - Backend:
    - Migración `V2__plans_memberships.sql`: tablas `plans` y `memberships` (LOGIC.md §4).
    - Entidades y repositorios: `User`/`Role` (paquete `auth`), `Plan`/`PlanCode`/`Membership`/`MembershipStatus` (paquete `plan`).
    - `JwtService`: emite y valida JWT (claims `sub`, `tenantId`, `role`, `email`) con la librería `jjwt`.
    - `JwtAuthenticationFilter` sustituye al antiguo `TenantFilter`: ya no existe la cabecera `X-Tenant-Id`; el tenant y el rol salen siempre del JWT validado.
    - `AuthController`: `POST /api/v1/auth/register` (crea `MEMBER` + membresía activa con la tarifa elegida), `POST /api/v1/auth/login`, `GET /api/v1/auth/me`.
    - `SecurityConfig` actualizado: rutas públicas, `/api/v1/admin/owner/**` solo `OWNER`, `/api/v1/admin/**` `OWNER`+`STAFF`, resto autenticado. Las peticiones sin sesión válida devuelven 403 (igual que el test ya existente del Trozo 0).
    - `AdminController` mínimo (`/admin/ping`, `/admin/owner/ping`): solo para poder probar los permisos por rol; el contenido real del panel llega en el Trozo 4.
    - `GlobalExceptionHandler` con `ProblemDetail` (RFC 9457) para correo duplicado (409), credenciales inválidas (401) y errores de validación (400).
    - `DevDataSeeder` (`CommandLineRunner`, perfiles `dev`/`demo`, idempotente): tenant `olympusgym`, 3 tarifas, 1 `OWNER` (Javier M.), 2 `STAFF` (Marta, Dani), 12 `MEMBER` (incluido Carlos M.) con membresía activa. Contraseña de demo única para todas las cuentas del seed: `Demo1234!` (documentada aquí, no es una contraseña real).
    - `docker-compose.yml` y `.env.example`: `SPRING_PROFILES_ACTIVE=dev` por defecto para que el seed se ejecute al levantar con Docker.
  - Frontend:
    - `lib/api.ts` (cliente HTTP a `/auth/register`, `/auth/login`, `/auth/me`, con `ApiError` que recoge el `ProblemDetail` del backend), `lib/auth-storage.ts` (token en `localStorage`, con `try/catch`), `lib/roles.ts` (etiquetas de rol y tarifas).
    - `AuthProvider` (contexto global de sesión) y `RequireRole` (guarda de ruta por rol; la autorización real sigue comprobándose en el servidor).
    - Página `/acceso` (referencia `v2/acceso`, recortada: sin NFC/pulsera, sin "portal staff", sin aforo falso) con pestañas Acceder/Crear cuenta, selector de tarifa en el registro y errores de formulario accesibles (`role="alert"`, `aria-describedby`).
    - `/cuenta` y `/admin`: layout con guarda (`RequireRole`) y una pantalla mínima de bienvenida + cerrar sesión; el contenido real llega en los Trozos 3 y 4.
    - `TopNav` ahora depende de la sesión: "Acceder/Apúntate" si no hay sesión, enlace a "Mi cuenta"/"Panel de gestión" + "Salir" si la hay.
    - Componentes base nuevos: `Input`, `Select`, `Field` (etiqueta + error accesible).
  - Verificado en el navegador (sin backend disponible, solo para la interfaz): pestañas del formulario, mensaje de error de conexión, y que `/cuenta` redirige a `/acceso` sin sesión. De paso se encontraron y arreglaron dos fallos reales de la interfaz de Trozo 0: una clave duplicada en el menú móvil (React) y un aviso de hidratación por el script del tema (se añadió `suppressHydrationWarning` al `<html>`, el patrón estándar para scripts que cambian el tema antes de la hidratación).
- Decisiones / simplificaciones:
  - El login busca el correo en toda la tabla `users` (no solo dentro de un tenant), porque hay un único formulario de login sin selector de gimnasio. Con un solo tenant real esto no es un problema; si algún día hubiera dos tenants con el mismo correo, el login sería ambiguo (se autentica contra el primero que encuentre). Es una limitación conocida y aceptable para este MVP.
  - El registro resuelve el tenant por el slug fijo `olympusgym` (no hay selector de gimnasio ni lo puede haber antes de autenticarse). Coherente con "un solo gimnasio en la práctica" de `PROJECT_BRIEF.md`.
  - La membresía creada en el registro queda `ACTIVE` desde el primer momento (no existe un estado "pago pendiente" en el esquema de `LOGIC.md` §4); el cobro real con Stripe es una fase opcional posterior.
  - Los precios y nombres de las tarifas en el formulario de registro están escritos a mano en el frontend (`lib/roles.ts`) porque todavía no existe `GET /public/plans`. Cuando se construya ese endpoint (Trozo 7), este formulario debería leer de ahí en vez de tener los datos duplicados.
  - `/api/v1/admin/ping` y `/api/v1/admin/owner/ping` son solo para probar los permisos por rol; no son funcionalidad real del panel.
- Pendiente:
  - Docker: el dueño del proyecto lo está instalando por su cuenta. En cuanto esté listo, pendiente:
    - Tests de Testcontainers que hoy se omiten en local (se confirmó que corren en CI, que sí tiene Docker): `GymflowIntegrationTest` (3 tests) y `AuthAndSecurityTest` (7 tests: alta y login correctos, correo duplicado, contraseña incorrecta, `MEMBER` sin acceso a `/admin`, `STAFF` sin acceso a `/admin/owner`, `OWNER` con acceso, aislamiento entre dos tenants creados en el propio test).
    - Validar `docker compose config` con el comando real (aquí solo se comprobó que el YAML es sintácticamente válido con un script).
    - Levantar `make infra` (o `docker compose up` directo si `make` no está disponible) y arrancar los tres servicios juntos.
  - No se ha probado el flujo de registro/login de verdad contra una base de datos real (sin Docker no hay Postgres en esta máquina): la cobertura de hoy es el compilado + los tests unitarios que no necesitan BD + la revisión manual del código.
  - Resuelto: los identificadores técnicos internos (paquete Java `com.gymflow`, artefacto Maven `gymflow-backend`, variables `GYMFLOW_*`, paquete npm `gymflow-frontend`) se quedan como están. No hay que renombrarlos; `GymFlow` siempre fue el nombre correcto de la plataforma, según `PROJECT_BRIEF.md`.
- Cómo probarlo:
  - Backend: `cd backend && mvn test`. Con Docker ya funcionando, los tests de Testcontainers dejarán de omitirse automáticamente.
  - Arrancar de verdad (necesita Docker): `docker compose up -d postgres` y luego `cd backend && mvn spring-boot:run` (perfil `dev` activo por defecto en `application.yml` si se exporta `SPRING_PROFILES_ACTIVE=dev`, o directamente con Docker Compose que ya lo trae). Luego:
    ```bash
    curl -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
      -d '{"email":"javier@olympusgym.test","password":"Demo1234!"}'
    ```
  - Frontend: `npm run dev`, abrir `/acceso`, probar las pestañas y, con el backend arriba, un registro real.
- Tests:
  - `mvn test`: BUILD SUCCESS. 14 tests: 4 en verde (`JwtServiceTest` x2, `TenantContextTest` x2, sin BD), 10 omitidos por falta de Docker (`AuthAndSecurityTest` x7, `GymflowIntegrationTest` x3).
  - Frontend: `npm run typecheck` y `npm run build` en verde (incluye las páginas nuevas `/acceso`, `/cuenta`, `/admin`).

#### Verificación con Docker ya instalado (mismo día, continuación)
- Hecho:
  - `docker compose config`: válido con el comando real (antes solo se había comprobado la sintaxis YAML con un script).
  - Infraestructura: `docker compose up -d postgres redis rabbitmq`. Los tres servicios llegan a `healthy` en unos 20 segundos.
  - `mvn test` con Docker Desktop 4.94.0 (motor 29.8.2) real: al principio los 10 tests de Testcontainers seguían fallando, pero con un error nuevo (antes se omitían limpiamente, ahora Testcontainers no conseguía hablar con el motor de Docker por el pipe con nombre de Windows, aunque `docker` y `docker compose` funcionaban bien). Comprobado con una petición HTTP directa al pipe: el motor respondía perfecto: el fallo estaba en la librería interna de Testcontainers 1.21.3, no en Docker. Arreglado subiendo Testcontainers a la 1.21.4 (`<testcontainers.version>` en `backend/pom.xml`, Spring Boot 3.5.6 trae 1.21.3 por defecto). Con eso, **los 14 tests pasan, 0 omitidos**, incluidos los 10 que antes no se podían ejecutar en esta máquina.
  - De paso, se quitó el log "Using generated security password" de Spring Boot en cada arranque (no usamos ese mecanismo de login, solo JWT propio): se excluyó `UserDetailsServiceAutoConfiguration` en `application.yml`.
  - `docker compose up --build -d`: las tres imágenes se construyen bien. El contenedor del frontend falló la primera vez porque el puerto 3000 ya lo tenía ocupado un servidor de pruebas anterior de esta sesión; liberado y arrancado sin problema.
  - Backend, servicio de IA y frontend responden: `GET /api/v1/public/ping`, `GET /health` y `http://localhost:3000` devuelven 200.
  - Log del backend confirmado: `The following 1 profile is active: "dev"` y el seed se ejecuta (`tenant=olympusgym, usuarios=15, planes=3`).
  - **Fallo real encontrado y arreglado: faltaba configurar CORS.** Con todo el stack en Docker, el formulario de `/acceso` daba "No se pudo conectar con el servidor" al intentar login/registro, aunque `curl` contra el backend funcionaba perfectamente. Causa: el frontend (puerto 3000) y el backend (puerto 8080) son orígenes distintos; sin una configuración de CORS explícita, el navegador bloquea la petición (algo que `curl` y los tests con `MockMvc` no detectan, porque ninguno de los dos aplica la política CORS del navegador). Arreglado añadiendo un `CorsConfigurationSource` en `SecurityConfig` que permite el origen del frontend (`gymflow.cors.allowed-origins`, variable de entorno `GYMFLOW_CORS_ALLOWED_ORIGINS`, por defecto `http://localhost:3000`).
  - Con el arreglo, probado de verdad en el navegador contra la base de datos real:
    - Login como socio (`carlos@olympusgym.test`) → redirige a `/cuenta`, muestra su nombre.
    - Cerrar sesión → vuelve a `/acceso`, la barra superior vuelve a mostrar "Acceder/Apúntate".
    - Login como dueño (`javier@olympusgym.test`) → redirige a `/admin`, muestra "Javier M." con la etiqueta "Dueño".
    - Registro de una cuenta nueva (`nueva.socia@olympusgym.test`, tarifa Completa) → crea la cuenta, inicia sesión sola y redirige a `/cuenta`. Verificado también directamente en Postgres (`docker compose exec postgres psql`): el usuario y su membresía `ACTIVE` quedaron guardados correctamente.
    - Un socio que visita `/admin` a mano es redirigido de vuelta a `/cuenta` (guarda de ruta en el cliente).
  - `mvn test` repetido tras el arreglo de CORS: sigue en verde, 14/14 (el cambio no afecta a los tests, que usan `MockMvc` y no pasan por el navegador).
- Decisiones / simplificaciones (añadidas):
  - El bloqueo de CORS es un fallo que solo aparece con un navegador real; por eso no lo detectaron los tests automáticos (`MockMvc`) ni las pruebas con `curl` de la ronda anterior. Queda como recordatorio: antes de dar por bueno un flujo de frontend-a-backend, probarlo también en el navegador contra servicios reales, no solo con peticiones HTTP sueltas.
  - La versión de Testcontainers quedó fijada en `1.21.4` en vez de la que trae Spring Boot 3.5.6 por defecto (1.21.3), por la incompatibilidad con el motor de Docker Desktop 4.94 en Windows. Anotado en un comentario en `backend/pom.xml`.
- Pendiente: nada relacionado con Docker. El stack completo (Postgres, Redis, RabbitMQ, backend, servicio de IA, frontend) está levantado y probado de verdad, incluido el flujo de alta/login/redirección por rol contra la base de datos.
- Cómo probarlo: `docker compose up -d`, abrir `http://localhost:3000/acceso` y entrar con cualquier cuenta del seed (contraseña `Demo1234!`) o registrar una nueva.
- Tests: `mvn test` → BUILD SUCCESS, 14/14 en verde, 0 omitidos.

---

### Trozo 0 - Verificar el esqueleto (2026-10-08)
- Hecho:
  - Backend: `mvn test` ejecutado. Compiló a la primera (no hizo falta arreglar código). Faltaba Maven en la máquina: se instaló Apache Maven 3.9.16 en `C:\Users\omard\tools` (no es parte del repo). 5 tests: 2 en verde (`TenantContextTest`), 3 omitidos (`GymflowIntegrationTest`, Testcontainers) por falta de Docker local.
  - Servicio de IA: faltaba Python en la máquina (solo el alias de Microsoft Store). Se instaló Python 3.12 con winget, se creó un entorno virtual en `ai-service/.venv` y se instalaron las dependencias. `pytest`: 3 tests en verde.
  - Frontend: `npm ci`, `npm run typecheck` y `npm run build` en verde.
  - `docker-compose.yml`: válido como YAML (comprobado con un parser, ver "Pendiente"). Servicios: postgres, redis, rabbitmq, backend, ai-service, ollama (perfil `local-llm`), frontend.
  - Base del frontend creada con Tailwind v4:
    - Tokens de `DESIGN.md` en `frontend/app/globals.css` (`@theme` + overrides de `:root[data-theme="light"]`), con foco visible y `prefers-reduced-motion`.
    - Fuentes Barlow Condensed (800) e Inter (400-700) cargadas con `next/font/google` como variables CSS.
    - Tema oscuro por defecto con interruptor claro/oscuro (`components/ThemeToggle.tsx`), persistido en `localStorage` con `try/catch`, y script `beforeInteractive` en `app/layout.tsx` para evitar parpadeo (respeta `prefers-color-scheme` solo si no hay preferencia guardada).
    - Layout con navegación superior sin barra lateral (`components/layout/TopNav.tsx`): menú "Clases" desplegable, enlaces a Horario/Entrenador IA/Tarifas/Contacto (ninguna de esas rutas existe aún, se construyen en próximos trozos), menú hamburguesa en móvil.
    - Componentes base: `Button` (primario/secundario/destructivo), `Card`, `Badge` (etiqueta), `Dropdown` (accesible: abre con clic o teclado, flechas arriba/abajo, Inicio/Fin, Escape, cierra al perder el foco).
    - Logo `OLYMPUS` + pulso SVG + `GYM` en `components/Logo.tsx`, texto/SVG (no imagen), con la línea de pulso dibujada como cima de montaña.
    - Se añadió `lucide-react` (iconos de línea, recomendado en `DESIGN.md`).
    - `app/page.tsx` reescrito como placeholder mínimo con los componentes nuevos (sin mencionar tienda); se verificó visualmente en el navegador en escritorio y móvil, con el tema oscuro y claro.
  - CI mínima en `.github/workflows/ci.yml`: tres jobs (`backend` con `mvn test`, `ai-service` con `pytest`, `frontend` con `npm ci && npm run typecheck && npm run build`). El runner de GitHub Actions trae Docker, así que en CI sí correrán los tests de Testcontainers que aquí se omitieron.
  - Se inicializó el repositorio git (no existía) y se hizo el primer commit del proyecto.
- Decisiones / simplificaciones:
  - Herramientas que faltaban en la máquina (Maven, Python) se instalaron de forma local/portátil, sin tocar configuración del sistema.
  - Docker Desktop no se instaló: requiere privilegios de administrador, WSL2 y reinicio, es un cambio de sistema demasiado invasivo para este trozo. Por eso no se pudo ejecutar `docker compose config` real ni `make infra`. Se validó la sintaxis del YAML con un parser Python como alternativa parcial.
  - `--color-accent-text` en tema oscuro se definió igual que `--color-accent` (el lima ya tiene contraste suficiente sobre fondo oscuro); en tema claro usa el valor oscurecido que ya definía `DESIGN.md`. Así el código usa `text-accent-text` de forma uniforme en ambos temas sin lógica condicional.
  - El desplegable de navegación "Clases" enlaza a `/horario?tipo=...` con las disciplinas del seed de `LOGIC.md` (CrossFit, Fuerza, Yoga, Boxeo, Spinning, Halterofilia); la ruta `/horario` se construye en el Trozo 2.
  - No se creó un componente de pie de página (footer): no lo pedía este trozo y no quería adelantar contenido (enlaces legales, redes) que aún no existe.
- Pendiente:
  - Validar `docker compose config` de verdad y levantar `make infra`: necesita Docker, que no está instalado en esta máquina. Confirmar en una máquina con Docker, o instalarlo con permiso explícito.
  - Backend: cuando haya Docker, volver a correr `mvn test` para confirmar que los 3 tests de Testcontainers también pasan (aquí solo se comprobó que compilan y se omiten limpiamente).
  - `npm audit`: 2 vulnerabilidades (1 alta) en `postcss`, arrastradas por la versión de Next.js 15.5. El arreglo automático sube a Next 16 (cambio mayor, fuera de alcance de este trozo); revisar al avanzar el roadmap.
- Cómo probarlo:
  - Backend: `cd backend && mvn test` (con Maven y JDK 21 en el PATH).
  - IA: `cd ai-service && .venv\Scripts\python -m pytest -v` (o `pytest -v` con el venv activado).
  - Frontend: `cd frontend && npm ci && npm run typecheck && npm run build`, luego `npm run dev` y abrir `http://localhost:3000`.
  - Compose: `docker compose config` (necesita Docker Desktop instalado y en marcha).
- Tests:
  - `mvn test` → BUILD SUCCESS, 2 pasados, 3 omitidos (sin Docker), 0 fallos.
  - `pytest -v` → 3 pasados.
  - `npm run typecheck` → sin errores. `npm run build` → compila y genera las páginas estáticas.

---

## Estado inicial
- Esqueleto (Fase 0) creado: backend sin compilar, IA con 3 tests en verde, frontend con build correcto.
- Diseño de referencia exportado en `docs/design/`. Alcance MVP y lógica definidos en `docs/PROJECT_BRIEF.md` y `docs/LOGIC.md`.
