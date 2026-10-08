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
