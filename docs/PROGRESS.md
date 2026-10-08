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

### Trozo 1 - Autenticación y roles (2026-10-08)
- Hecho:
  - Nombre del proyecto: a partir de ahora todo lo nuevo (docs, texto de cara al usuario, commits) usa **OlympusGYM**, no "GymFlow". Corregido `README.md`. Los identificadores técnicos internos que ya existían (paquete Java `com.gymflow`, artefacto Maven, variables `GYMFLOW_*`) se dejaron sin tocar por ahora: cambiarlos es un refactor grande aparte (ver "Dudas" más abajo).
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
  - Pregunta para el dueño del proyecto: ¿también quieres renombrar los identificadores técnicos internos (paquete Java `com.gymflow` → `com.olympusgym`, artefacto Maven `gymflow-backend`, variables de entorno `GYMFLOW_*`, nombre del paquete npm `gymflow-frontend`) o se quedan como están? Es un cambio grande (toca casi todos los ficheros del backend) y no lo he hecho sin que lo confirmes.
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
