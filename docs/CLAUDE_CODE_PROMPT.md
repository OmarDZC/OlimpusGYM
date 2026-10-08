# Prompt maestro para Claude Code

Pégalo en Claude Code desde la raíz del repositorio (`gymflow/`). Todo el contexto largo vive en `docs/`; el prompt solo dice cómo trabajar.

````text
Eres el desarrollador principal de GymFlow, un proyecto de portfolio. Vas a construir el MVP completo de OlympusGYM (un gimnasio ficticio de demostración) sobre el esqueleto que ya existe en este repositorio. Trabaja de forma autónoma hasta terminar todos los trozos, sin esperar mi aprobación entre ellos; yo iré revisando los commits.

PRIMERO, LEE (en este orden) y no empieces a programar hasta haberlo hecho:
1. CLAUDE.md
2. docs/PROJECT_BRIEF.md  (visión, alcance dentro/fuera, roles, decisiones)
3. docs/LOGIC.md          (datos, reglas de reserva, permisos, API, servicio de IA)
4. docs/DESIGN.md         (sistema de diseño, tokens, tono, movimiento, mapa pantalla -> ruta)
5. docs/ROADMAP.md        (trozos 0 a 9)
6. Abre las capturas docs/design/v2/*/screen.png para ver el estilo exacto, y los code.html solo para textos y estructura. Mira también docs/design/reference-v1/ (móvil y panel).

ALCANCE: es un MVP BÁSICO; no tiene que quedar perfecto. NO construyas tienda, varios centros, NFC/pulseras/QR, taquillas, control de acceso, newsletter ni app nativa, aunque aparezcan en el diseño de referencia. Un solo gimnasio (tenant "olympusgym" por seed), con la arquitectura multi-tenant ya definida.

CÓMO TRABAJAR
- Sigue docs/ROADMAP.md por trozos verticales (backend + frontend de cada funcionalidad), empezando por el Trozo 0: el backend NUNCA se ha compilado, así que ejecuta `mvn test` y arregla lo que falle antes de nada.
- Un commit por trozo con mensaje claro. Al cerrar cada trozo: tests en verde, build correcto y una entrada en docs/PROGRESS.md (qué hiciste, qué simplificaste, qué queda, cómo probarlo).
- Si algo no está especificado, elige la opción más simple, anótala en PROGRESS.md y sigue. Solo te detienes y me preguntas si estás bloqueado o si una decisión no se puede deshacer.
- Cumple las reglas de LOGIC.md con tests: aforo sin sobreventa (con test de concurrencia), lista de espera FIFO, cancelación hasta 2 h antes, permisos por rol (un socio recibe 403 en /admin), aislamiento por tenant, JWT con tenant y rol (elimina X-Tenant-Id).
- La IA: cliente compatible con OpenAI configurado por LLM_BASE_URL / LLM_API_KEY / LLM_MODEL. No uses la API de Anthropic ni otro proveedor concreto. Salida validada con Pydantic y reintentos. Los tests usan un LLM simulado, sin red.
- Tres experiencias separadas: web pública, /cuenta (socio) y /admin (dueño y staff). Un único login con redirección por rol. La autorización se comprueba en el servidor.

ESTILO (muy importante, mantenlo en todo el frontend)
- Reconstruye las pantallas como componentes reutilizables de Next.js + Tailwind v4 con los tokens de docs/DESIGN.md. No copies el HTML de Stitch, ni uses su CDN de Tailwind, ni sus imágenes externas.
- Oscuro por defecto con interruptor claro/oscuro. Titulares en Barlow Condensed ExtraBold en mayúsculas con una palabra en lima; cuerpo en Inter. Navegación superior con desplegables accesibles por teclado, sin barra lateral en escritorio.
- Español llano, sin jerga. Respeta la lista de palabras prohibidas de DESIGN.md (telemetría, algoritmo, Neural, SNC, RPE, etc.). Nada de emojis; iconos de línea.
- Nunca cortes texto con puntos suspensivos ni partas importes en dos líneas.
- Datos ficticios etiquetados como "datos de ejemplo".
- Después de construir cada pantalla, hazle una captura (Playwright o el navegador disponible) y compárala con su screen.png de referencia; corrige diferencias de estilo evidentes. Si no puedes capturar, dilo.
- Implementa el movimiento de DESIGN.md sección 6, respetando prefers-reduced-motion. Sin GIFs pesados.
- Imágenes: fotos con licencia o propias en frontend/public (anota créditos en docs/CREDITS.md). Si no puedes obtenerlas, usa marcadores de posición neutros y dilo en PROGRESS.md.

HONESTIDAD
- Di siempre qué verificaste y qué no pudiste verificar (por ejemplo, si no hay Docker para Testcontainers, o si no hay acceso a la red).
- No inventes funcionalidades ni datos "en directo" que no se calculen de verdad.

ENTREGA FINAL
Cuando termines todos los trozos (el 9, Stripe, es opcional), actualiza README.md con la arquitectura, cómo arrancar todo, cómo cambiar de proveedor de IA y las credenciales de demo. Responde en español con: tabla de estado por trozo, lo que no pudiste verificar, simplificaciones tomadas y un máximo de 5 preguntas abiertas para mí.

Empieza ya por el Trozo 0.
````

## Versión corta (si prefieres ir paso a paso)

````text
Lee CLAUDE.md y los documentos de docs/ que enlaza (PROJECT_BRIEF, LOGIC, DESIGN, ROADMAP) y abre docs/design/v2/*/screen.png. Después ejecuta el Trozo 0 del ROADMAP: compila y arregla el backend (`mvn test`), comprueba IA y frontend, y prepara los tokens de diseño y el layout base. Cuando termines, actualiza docs/PROGRESS.md, haz commit y dime qué verificaste y qué no. No sigas con el Trozo 1 hasta que te lo diga.
````
