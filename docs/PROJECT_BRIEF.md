# Resumen del proyecto (léelo primero)

Este documento recoge lo decidido con la persona dueña del proyecto en la fase de ideas y diseño. Si algo de aquí choca con el código, pregunta antes de cambiarlo.

## Qué estamos construyendo

- **GymFlow** es la plataforma (el software y este repositorio). Es un proyecto de portfolio de un programador: tiene que demostrar backend serio (Spring), IA (FastAPI), frontend moderno, tests y despliegue.
- **OlympusGYM** es el gimnasio ficticio de demostración que usa GymFlow. Tiene su propia marca y su web. GymFlow solo aparece en letra pequeña ("Powered by GymFlow") y en la sección del entrenador con IA ("Con tecnología GymFlow").
- Todo lo que sea nombres, testimonios, precios, fechas ("desde 1984") o cifras es **dato de ejemplo**. No debe presentarse como real: añade una etiqueta discreta "Datos de ejemplo / demo" en la web.

## Alcance del MVP (importante: es BÁSICO, no tiene que ser perfecto)

### Dentro
- **Web pública** de OlympusGYM: inicio, entrenador IA, horario, tarifas, contacto.
- **Registro y login** (un único login para todos; la redirección depende del rol).
- **Área del socio** (`/cuenta`): próxima clase, reservas, rutina de hoy, constancia semanal, mi cuota.
- **Reservas de clases** con aforo, lista de espera y cancelación hasta 2 h antes.
- **Entrenador con IA**: genera una rutina semanal y la guarda; el socio marca las series hechas.
- **Asistente de reservas con IA** (chat): reserva y cancela con las herramientas del backend, con los permisos del socio.
- **Panel de gestión** (`/admin`) para dueño y staff: resumen, clases (crear/editar/cancelar), socios, reservas.
- **Tres tarifas** (Básica, Completa, Atleta Pro). El pago real con Stripe en modo test es una fase final opcional.
- Tema **oscuro por defecto** con interruptor a claro.

### Fuera (decidido: NO hacer)
- Tienda, carrito, pedidos, descuentos de tienda, "Olympus Store".
- Varios centros o sedes ("Nuestros centros", acceso multisede). El gimnasio tiene **una sola sede**.
- Pulseras, tarjetas NFC, códigos QR, "escanear credencial", taquillas, control de acceso, SPA/sauna.
- Asignación de puesto/bici ("Box A-08"). La reserva es solo una plaza.
- Newsletter, suscripción a novedades, app nativa, descarga de app (la app del socio es la propia web adaptada a móvil, PWA si hay tiempo).
- Multi-tenancy en la interfaz: la arquitectura mantiene `tenant_id` en las tablas, pero solo existe un tenant (`olympusgym`) creado por seed. No hay onboarding de gimnasios.
- Climatización, "telemetría", sensores o cualquier dato que no podamos generar de verdad.

## Las tres experiencias (separadas a propósito)

| Experiencia | Quién | Ruta | Contenido |
|---|---|---|---|
| Web pública | Cualquiera | `/`, `/entrenador-ia`, `/horario`, `/tarifas`, `/contacto` | Marketing y horario con reservas |
| Mi cuenta | `MEMBER` | `/cuenta/*` | Reservas, rutina, cuota, asistente |
| Panel de gestión | `OWNER`, `STAFF` | `/admin/*` | Resumen, clases, socios, reservas |

Un solo formulario de login. Tras entrar: `MEMBER` va a `/cuenta`, `OWNER` y `STAFF` van a `/admin`. Un socio no ve ningún enlace al panel, y **el backend rechaza** cualquier endpoint de gestión si el rol no corresponde (no basta con esconder botones).

## Decisiones tomadas y por qué

- **Spring Boot (negocio) + FastAPI (IA) + Next.js.** Para enseñar microservicio de IA aparte con un monolito modular de negocio. No dividir el backend en más servicios.
- **La IA no depende de un proveedor.** Cliente compatible con OpenAI y variables `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL`. Ollama en local (gratis, sin tokens) para desarrollar; Groq o Gemini (capas gratuitas con límites) para la demo desplegada. Los límites y modelos gratuitos cambian a menudo: no los fijes en el código. Ojo: la capa gratuita de Gemini no es válida para apps con usuarios reales en el EEE; vale para demo propia. Los modelos pequeños fallan más al generar JSON, por eso: salida validada con Pydantic y reintentos. No usar la API de Anthropic ni de ningún proveedor concreto como dependencia.
- **Diseño: oscuro por defecto**, con interruptor claro/oscuro. Al usuario le molesta el fondo blanco. Estilo "gimnasio de los de siempre con web viva": hierro, tiza, tipografía condensada, fotos en blanco y negro con grano, acento lima eléctrico y una **línea de pulso** (latido) como hilo conductor y como logo (el pulso forma una cima de montaña = Olimpo).
- **Debe ser original**: no copiar la web de ningún gimnasio real (la usada como referencia de estructura fue la de un gimnasio de cadena; no copiar su marca, textos ni imágenes).
- **Tono**: español llano y directo, como habla un gimnasio de verdad. Nada de jerga tipo "telemetría", "algoritmo", "neural", "SNC", "RPE", "biomecánica avanzada". Está explícitamente prohibido (ver lista en `DESIGN.md`).
- **Nada inventado como producto real**: no existe "GymFlow Neural 3.1". Solo "GymFlow".
- **El nombre OlympusGYM** es de demostración. Se recomienda comprobar que no coincide con un negocio real antes de publicarlo.

## Segundo proyecto (fuera de alcance aquí)
Un blog que se escribe solo a partir de noticias tech, en un repositorio aparte. No tocar ahora.

## Cómo trabajar
1. Lee `CLAUDE.md`, este documento, `docs/LOGIC.md` (reglas de negocio y API), `docs/DESIGN.md` (estilo) y `docs/ROADMAP.md` (orden).
2. Mira `docs/design/` para las pantallas de referencia (HTML + capturas de Stitch).
3. Trabaja en trozos verticales (backend + frontend de una funcionalidad), con tests, un commit por trozo y una nota en `docs/PROGRESS.md`.
