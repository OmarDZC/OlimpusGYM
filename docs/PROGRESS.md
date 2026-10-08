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

### Trozo 5b - Entrenador con IA: backend y frontend (2026-10-08)
- Hecho:
  - `POST /me/workout/generate` (`GenerateWorkoutRequest`: goal, level, daysPerWeek, equipment, limitations):
    - Comprueba cuota `ACTIVE` y que la tarifa tenga `ai_trainer` **antes** de llamar al servicio de IA (403 si no, y no gasta ninguna llamada al LLM: verificado con un test que comprueba que `TrainerClient` no se invoca en ese caso).
    - `TrainerClient` (nuevo paquete `workout.ai`) llama a `POST /v1/trainer/plan` del servicio de IA con `RestClient` (timeout de conexión 5 s, de lectura 90 s: una rutina real puede tardar). Si no logra ni conectar, o el servicio de IA responde 503, lo relaya como 503; si responde cualquier otro error, 502. Mensajes en español llano, iguales en espíritu a los del servicio de IA.
    - La respuesta del servicio de IA (`AiWeeklyPlan`, 1 = lunes) se traduce al `WeeklyPlan` del backend (`PlanDay`/`PlanExercise`, 0 = lunes): mismo mapeo 1 a 1 día por día y ejercicio por ejercicio que ya usaba la rutina manual del Trozo 3, sin aplastar ni reordenar nada que decidiera el modelo.
    - Si ya existía una rutina generada esta semana, **se sustituye**: se borran sus series marcadas (ya no pertenecerían a ningún ejercicio real de la rutina nueva) y se guarda una fila nueva con `source=AI`.
  - `/entrenador-ia` (nueva página, navegación pública): configurador real (objetivo, nivel, frecuencia semanal, material disponible, limitaciones) que llama a `POST /me/workout/generate` y muestra la rutina generada completa (todos los días, no solo "hoy") en el panel de la derecha. Si no has iniciado sesión, el envío te manda a `/acceso` (mismo patrón que "Reservar" en `/horario`); si tu tarifa no incluye IA o el servicio no responde, se ve el error tal cual lo manda el backend.
  - `/cuenta/entrenamiento` (antes un aviso de "llega pronto", ya no era verdad) ahora explica dónde generarla (`/entrenador-ia`) y dónde verla y marcar series (`Inicio`, sin tocar esa pantalla: ya funcionaba desde el Trozo 3 y lee cualquier rutina sin importar si es `AI` o `MANUAL`). El aviso de "Todavía no tienes una rutina asignada" de `TodayWorkoutCard` ahora enlaza a `/entrenador-ia` en vez de decir que la función no existe todavía.
  - Tests nuevos en `WorkoutAndMembershipHttpTest` (con `TrainerClient` sustituido por `@MockitoBean`, sin tocar red real): genera y guarda bien con tarifa con IA, 403 sin IA en la tarifa (y cero interacciones con el cliente de IA), 503 si el servicio de IA no responde, y que generar dos veces en la misma semana sustituye la rutina y borra las series de la anterior (y es una fila nueva, no la misma reutilizada).
  - Verificado también en vivo contra Docker real (reconstruidas las imágenes de backend y frontend): registrada una socia con tarifa Completa, configurador de `/entrenador-ia` funcionando con los valores por defecto del diseño de referencia, envío real → **503** con el mensaje amable (no hay LLM configurado en esta máquina, ver "Pendiente"), confirmado en la pestaña de red que la petición pasó de verdad por backend → servicio de IA. Con una socia con tarifa Básica, el mismo formulario da **403** "Tu tarifa no incluye el entrenador con IA." al instante (sin esperar al servicio de IA, se nota en lo rápido que responde).
- Decisiones / simplificaciones:
  - No se construyó `GET /me/workout/week` (lo menciona `LOGIC.md §6`): `/entrenador-ia` ya recibe la rutina completa en la propia respuesta de `generate`, y `/cuenta` solo necesita "hoy" (ya existía). Añadir un endpoint que nada de lo pedido en este trozo usa habría sido construir por adelantado.
  - "Límite por plan" (`ROADMAP.md`, Trozo 5) se interpretó como la comprobación de `ai_trainer` de la tarifa (403 si no la incluye), no como un límite de cuántas veces se puede regenerar la rutina: `LOGIC.md` no define ese límite en ningún sitio y no quise inventarlo.
  - `POST /v1/trainer/adjust` (ajuste semanal automático, solo Atleta Pro) sigue sin construirse, igual que se anotó en el Trozo 5a: no lo pedía este encargo.
  - El "material disponible" del configurador es de selección única (igual que en la referencia visual, donde solo se ve una opción marcada a la vez), aunque el backend admite una lista: para esta tarifa de complejidad no hacía falta multi-selección.
  - El peso sugerido por ejercicio que se ve en la referencia visual (p. ej. "80 kg") no se implementó (ya anotado en el Trozo 5a: haría falta saber las marcas personales del socio, que no existen en el modelo de datos). La interfaz lo esconde automáticamente cuando `weightKg` es `null`, igual que ya hacía `TodayWorkoutCard` desde el Trozo 3.
- Pendiente:
  - Sigue sin haber un LLM real configurado en esta máquina: lo generado con éxito (panel derecho con una rutina real) está verificado por el test de backend que comprueba la forma exacta de la respuesta mapeada (la misma que consume el frontend) y por revisión del código de `/entrenador-ia`, que reutiliza el mismo patrón ya probado de `TodayWorkoutCard`; pero no se ha visto una rutina real generada por un modelo de verdad en el navegador. En cuanto haya un proveedor configurado (ver README, sección "Cambiar de proveedor de IA"), probarlo de verdad y revisar a mano que el español y los ejercicios tengan sentido.
  - La llamada al servicio de IA sin proveedor configurado tarda unos 30 segundos en devolver el 503 (el SDK de OpenAI reintenta un par de veces por su cuenta antes de rendirse, y resolver el nombre `ollama` sin ese contenedor levantado también tarda). No es un error, pero con un proveedor real configurado debería responder mucho antes; si algún día molesta, se puede bajar `max_retries` del cliente de `ai-service/app/llm.py`.
- Cómo probarlo:
  - Backend: `cd backend && mvn test`.
  - De verdad: `docker compose up -d` (con o sin `ai-service`/Ollama; sin LLM real verás el 503), entra en `/acceso` con una cuenta de tarifa Completa o Atleta Pro (p. ej. `laura@olympusgym.test` / `Demo1234!`) y ve a `/entrenador-ia`.
  - Frontend: `npm run typecheck && npm run build`.
- Tests:
  - `mvn test`: BUILD SUCCESS, 45/45 en verde (4 nuevos en `WorkoutAndMembershipHttpTest`).
  - `npm run typecheck` y `npm run build`: sin errores, 16 páginas (nueva: `/entrenador-ia`).
  - Tests end-to-end de Playwright existentes (login, reservar/cancelar, cancelar sesión del Mini-trozo): repetidos tras reconstruir las imágenes, 5/5 en verde, sin cambios necesarios.

---

### Trozo 5a - Entrenador con IA: servicio de IA (2026-10-08)
- Hecho:
  - `POST /v1/trainer/plan` implementado de verdad (antes devolvía 501). Nuevo módulo `app/trainer.py`:
    - Construye el prompt en español llano a partir de `PlanRequest` (objetivo, nivel, días/semana, material, limitaciones), pidiendo al modelo un JSON con la forma exacta de `WeeklyPlan`/`DayPlan`/`ExercisePlan` (ya existían en `app/schemas.py`), sin bloques de código ni texto alrededor.
    - Valida la respuesta con Pydantic y, además, que el número de días del plan coincida con `days_per_week` pedido y que no haya días repetidos (un fallo real y frecuente de los LLM que Pydantic por sí solo no detecta, porque la lista de días es válida aunque le sobren o falten).
    - **Hasta 2 reintentos** (3 intentos en total): si la respuesta no es un JSON válido o no cumple lo anterior, se le devuelve al modelo el motivo exacto del fallo y se le pide que lo corrija, en vez de repetir el mismo prompt a ciegas.
    - Si falla **hablar** con el proveedor (red, 401, timeout...), se trata como "no disponible": `HTTPException(503)` con un mensaje amable. Si tras los 3 intentos la respuesta sigue sin ser válida, `HTTPException(502)`.
  - El cliente de LLM se inyecta con `Depends(get_client)` (antes se construía dentro de la función): así los tests pueden sustituirlo por uno falso con `app.dependency_overrides`, sin tocar red ni variables de entorno.
  - Tests nuevos (`tests/test_trainer.py`, con un cliente falso en `tests/fakes.py` que imita solo la forma del SDK de OpenAI que se usa): respuesta válida a la primera, respuesta inválida que se corrige al reintentar (y se comprueba que de verdad hizo dos llamadas, no una), fallo definitivo tras 3 intentos → 502, número de días equivocado tratado igual que un JSON inválido, y proveedor inalcanzable → 503.
  - Verificado también contra el servicio real en Docker (reconstruida la imagen): sin Ollama levantado, `POST /v1/trainer/plan` responde `503` con el mensaje amable, tal cual se ve en producción si el LLM configurado no responde.
- Decisiones / simplificaciones:
  - No se usó el `response_format: json_object` de la API de OpenAI (que fuerza JSON): no todos los proveedores compatibles lo soportan igual (Groq, Gemini vía capa de compatibilidad...), y el encargo pide que el servicio sea agnóstico de verdad. En su lugar, el prompt pide el JSON de forma explícita y estricta, y el parseo quita un posible bloque ```json ... ``` si el modelo lo añade de todas formas.
  - El plan generado no incluye peso sugerido por ejercicio (`weightKg` existe en el esquema del backend pero queda `null` para los planes de IA): calcularlo de verdad necesitaría saber las marcas personales del socio, que no existen en el modelo de datos de `LOGIC.md`. Queda fuera de este trozo; anotado también de cara al Trozo 5b.
  - `POST /v1/trainer/adjust` (ajuste semanal para Atleta Pro, LOGIC.md §7) no se construyó: el encargo de este trozo solo pedía `POST /v1/trainer/plan`. Queda para un trozo posterior si se retoma el roadmap.
- Pendiente:
  - No hay un LLM real configurado en esta máquina (ni Ollama levantado, ni `.env` con otro proveedor): todo lo anterior está verificado con el cliente falso de los tests y, contra el servicio real, solo se ha podido comprobar el camino de "no disponible" (503). Falta probar un plan generado por un modelo de verdad; en cuanto haya un proveedor configurado (`docker compose --profile local-llm up -d ollama` + `ollama pull llama3.1:8b`, o unas claves de Groq/Gemini en `.env`), probarlo con `curl -X POST http://localhost:8000/v1/trainer/plan` y revisar a mano que el español y los ejercicios tengan sentido.
- Cómo probarlo:
  - `cd ai-service && pytest -v` (o con el venv: `.venv/Scripts/python -m pytest -v`).
  - De verdad (sin LLM configurado, para ver el 503): `docker compose up -d ai-service` y luego el `curl` de arriba.
- Tests:
  - `pytest -v`: 7/7 en verde (2 de `test_api.py`, 5 nuevos de `test_trainer.py`).

---

### Mini-trozo - Tests end-to-end con Playwright (2026-10-08)
- Hecho:
  - Antes de empezar: comprobado el resultado de la CI de GitHub Actions del commit del Trozo 4b (en verde, los tres jobs). Nada que arreglar.
  - Proyecto nuevo `e2e/` (aparte de `frontend/`, no forma parte del build de Next.js), con Playwright + TypeScript. Tres specs, contra el stack real en Docker:
    - `login-redirect.spec.ts`: un socio entra en `/cuenta` y, si visita `/admin` a mano, la guarda del cliente lo manda de vuelta; el dueño y el staff entran directamente en `/admin`. Usa las cuentas del seed de demo (solo lectura).
    - `member-booking.spec.ts`: un socio nuevo (registrado dentro del propio test, sin reservas previas del seed) reserva una clase desde `/horario` y la cancela.
    - `staff-cancel-session.spec.ts`: un socio nuevo reserva una clase; el staff la cancela desde `/admin/clases`; se vuelve a entrar como ese socio y su reserva ya no aparece en "Mis reservas" (que solo lista `BOOKED`/`WAITLIST`). Cubre la regla aplazada del Trozo 2/4a (cancelar una sesión cancela todas sus reservas activas), de punta a punta.
  - `e2e/support/helpers.ts`: login y registro **a través del formulario real** (es lo que se quiere probar en el primer test, y mantiene los otros dos realistas); solo la creación de la clase de prueba usa la API directamente como OWNER (`POST /admin/sessions`), porque eso es preparación de datos, no el comportamiento bajo prueba, y así los tests no dependen de rellenar ese formulario ni del horario que ya trae el seed.
  - Clases de prueba con nombre único (marca de tiempo) y en un día futuro fijo (3 días), dentro de la ventana de reserva de cualquier tarifa (7-14 días) y de los 14 días que muestra `/horario`; cada test crea la suya, así que no interfieren entre sí ni con los datos del seed.
  - Se añadieron `data-testid` a las tarjetas de sesión en `/horario` y `/admin/clases`, y al selector de día de `/horario`: sin esto, localizar la tarjeta exacta de la clase de prueba (entre las demás del seed) habría dependido de coincidencias de texto frágiles (nombre repetido, fecha con formato de idioma). No cambia nada visible.
  - Sin esperas arbitrarias: todas las aserciones son de Playwright (`expect(...).toBeVisible()`, `toHaveURL()`, `toHaveCount()`), que reintentan solas hasta que la interfaz reacciona; no hay ningún `waitForTimeout`.
  - Job nuevo `e2e` en `.github/workflows/ci.yml`: levanta Postgres, backend y frontend con `docker compose up -d --wait` (sin `ai-service`, porque ninguno de los tres escenarios usa IA), espera con un bucle acotado (máx. 2 min) a que backend y frontend respondan, instala Chromium de Playwright y corre los tests. Si falla, sube el reporte HTML de Playwright como artefacto y imprime los logs de `docker compose`.
  - `Makefile` (`make e2e`) y `README.md` (sección "End-to-end (Playwright)") documentan cómo correrlos en local.
- Decisiones / simplificaciones:
  - Si meter esto en CI hubiera sido costoso (reconstruir imágenes, arrancar Ollama, etc.) se iba a dejar solo en local y avisarlo; no hizo falta: ninguno de los tres escenarios pedidos usa el servicio de IA, así que el job de CI no necesita `ai-service` ni Ollama, solo Postgres + backend + frontend, que ya se instalan rápido.
  - Los tests no limpian las clases de prueba que crean (quedan en el horario, ya sin reservas activas tras cada test). Para este MVP de demo no hace falta: no estorban (nombre claramente de prueba, con marca de tiempo) y evita acoplar los tests a un endpoint de borrado que no existe (solo hay cancelar). Si algún día molesta, se podría añadir un `afterAll` que las cancele vía API.
  - `workers` fijo a 1 en CI (en local corren en paralelo): más lento pero más predecible en el runner compartido de GitHub Actions; con solo 3 specs no penaliza casi nada.
- Pendiente:
  - Nada bloqueante. Quedan fuera del alcance de este mini-trozo (no los pidió el encargo): tests de la reserva en lista de espera/promoción FIFO, y del panel de socios/ingresos del Trozo 4a (esos ya tienen cobertura de integración en el backend).
- Cómo probarlo:
  - `docker compose up -d postgres redis rabbitmq backend frontend` (el backend ya debe tener el seed de `dev`/`demo`).
  - `cd e2e && npm install && npx playwright install --with-deps chromium && npx playwright test`.
- Tests:
  - `npx playwright test`: 5/5 en verde (3 specs, el de login por rol tiene 3 casos), repetido tres veces seguidas sin ningún fallo intermitente.
  - `cd frontend && npm run typecheck`: sin errores, confirma que los `data-testid` añadidos no rompen nada.

---

### Trozo 4b - Panel de gestión: frontend (2026-10-08)
- Hecho:
  - Referencia visual: `docs/design/v2/panel-gestion/` no existe. Solo hay `docs/design/reference-v1/panel-dueno-v1-con-sidebar/code.html` (sin `screen.png`, solo HTML/texto, marcado como "solo contenido de referencia" en `DESIGN.md`). Se usó únicamente para sacar ideas de contenido (qué tarjetas y textos tienen sentido en un resumen de gimnasio), nunca su maquetación: su barra lateral no se copió porque el encargo pedía explícitamente navegación superior sin barra lateral, y se excluyeron secciones/jerga fuera de alcance de esa referencia ("Tienda", "Ajustes", "Telemetría de Rendimiento", "Algoritmo IA"/"Modelo IA").
  - `AdminNav` (navegación superior, sin barra lateral): 4 enlaces (Resumen, Clases, Socios, Reservas), nombre del usuario y, debajo, una etiqueta con el rol ("Dueño" o "Staff"), y un círculo con las iniciales.
  - `/admin` (resumen): tarjetas de socios activos, clases de hoy, ocupación de hoy, e ingresos del mes (esta última solo se pinta si el backend la devuelve, es decir solo para OWNER); lista de "Socios que conviene contactar" con enlace `mailto:` a cada uno.
  - `/admin/clases`: formulario para crear una clase (nombre, disciplina, sala, inicio/fin, aforo, semanas a repetir) y listado de clases con botón "Cancelar clase" (avisa a los socios con reserva, vía el endpoint del Trozo 4a).
  - `/admin/socios`: listado de socios; el precio de la tarifa y el control para cambiarla solo se muestran si el usuario es OWNER (si es STAFF, aparece el texto "El precio de cada tarifa solo lo ve el dueño.").
  - `/admin/reservas`: listado de reservas activas con botón "Cancelar" y el texto "Como personal del gimnasio puedes cancelar la reserva de cualquier socio, sin el límite de 2 horas." (sin mención al límite de 2 horas que sí aplica al socio en `/cuenta`).
  - Menús: dueño y staff ven las mismas 4 pestañas (LOGIC.md §2 da a STAFF acceso a clases/socios/reservas igual que a OWNER); lo que cambia por rol es el contenido dentro de cada pantalla (ingresos, precios, cambio de tarifa), no la lista de pestañas. Documentado en un comentario en `AdminNav.tsx`.
  - `lib/api.ts`: tipos y funciones nuevas para los seis endpoints del Trozo 4a, siguiendo el patrón ya existente (`throwProblem`/`ApiError`).
  - Verificado de verdad en el navegador contra el backend en Docker:
    - Como OWNER (Javier M.): las 4 tarjetas del resumen incluido "Ingresos del mes", etiqueta "Dueño" bajo el nombre; cancelada una clase desde `/admin/clases` (desaparece de la lista); `/admin/socios` con precios y selector de tarifa visibles; `/admin/reservas` con botones de cancelar.
    - Como STAFF (Marta Gómez): la tarjeta "Ingresos del mes" no aparece; en `/admin/socios` no se ve ni el precio ni el selector de tarifa, solo el aviso de que eso es cosa del dueño.
    - Como MEMBER (Carlos): al entrar a mano en `/admin`, la guarda de ruta (`RequireRole`) redirige de vuelta a `/cuenta` (la autorización real ya la exige el servidor desde el Trozo 4a; esto es solo la comprobación de que el cliente no deja ver la pantalla ni un instante).
    - Sin errores de consola reales en ninguno de los tres casos (solo 404 esperados de rutas futuras que la navegación precarga, como `/entrenador-ia` o `/tarifas`, que todavía no existen).
- Decisiones / simplificaciones:
  - No se construyó una página de detalle por socio (`/admin/socios/{id}`): el backend la expone (`GET /admin/members/{id}`) pero el encargo de este trozo no pedía esa pantalla y el listado ya cubre lo esencial (nombre, correo, tarifa, precio si corresponde).
  - La señal "canceló varias reservas seguidas" que aparecía en la referencia de texto no se implementó: el backend del Trozo 4a solo calcula "socios que conviene contactar" por ausencia de reservas en 14 días (decisión ya tomada y anotada en la entrada del Trozo 4a), así que el frontend no tiene ese dato que mostrar.
  - `/admin/reservas` filtra en el cliente a reservas `BOOKED`/`WAITLIST`: el backend devuelve también historial (`CANCELLED`, `ATTENDED`, `NO_SHOW`) pero no tiene sentido ofrecer "Cancelar" sobre una reserva que ya no está activa.
- Pendiente:
  - Nada bloqueante para este trozo.
- Cómo probarlo:
  - `docker compose up -d`, entrar en `/acceso` como `javier@olympusgym.test` (OWNER) o `marta@olympusgym.test` (STAFF), contraseña `Demo1234!`, y recorrer `/admin`, `/admin/clases`, `/admin/socios`, `/admin/reservas`.
  - Entrar como `carlos@olympusgym.test` (MEMBER) y comprobar que `/admin` redirige a `/cuenta`.
- Tests:
  - `npm run typecheck` y `npm run build`: sin errores, 15 páginas generadas.
  - Sin tests automáticos de frontend en este trozo (no los hay en ningún trozo anterior del frontend); la verificación fue manual en el navegador contra el backend real, descrita arriba.

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
