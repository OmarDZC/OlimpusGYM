# Sistema de diseño: OlympusGYM

Esta es la fuente de verdad del estilo. Las pantallas de referencia están en `docs/design/` (HTML y capturas exportados de Stitch). **Se usan como guía visual, no se copian tal cual**: están hechas con el CDN de Tailwind, imágenes alojadas fuera y algunos fallos que se listan abajo. Hay que reconstruirlas como componentes de Next.js con los tokens de este documento.

`docs/design/stitch-design-system.md` es el sistema que generó Stitch ("Kinetic Obsidian"). Úsalo de apoyo, pero si choca con este documento, manda este.

## 1. Idea de marca
Gimnasio de los de siempre (hierro, tiza, esfuerzo real) con una web moderna y viva. Hilo conductor: una **línea de pulso** (latido). En el logo, la línea forma una cima de montaña (guiño al Olimpo). Nada de columnas griegas, estatuas ni laureles.

**Logo**: texto `OLYMPUS` + trazo SVG de pulso + `GYM`, en tipografía condensada ExtraBold en mayúsculas. Hazlo en texto/SVG (nunca una imagen que pueda romperse) y muéstralo **una sola vez por pantalla**.

## 2. Tokens (Tailwind v4, en `frontend/app/globals.css`)

```css
@import "tailwindcss";

@theme {
  /* Oscuro (por defecto) */
  --color-bg: #0B0D10;
  --color-surface: #14181D;
  --color-surface-2: #1B2128;
  --color-border: #232A31;
  --color-text: #F2F4F5;
  --color-text-muted: #8B949E;
  --color-text-dim: #525A65;
  --color-accent: #C6F432;        /* lima eléctrico */
  --color-accent-hover: #D5F758;
  --color-on-accent: #0B0D10;     /* texto sobre botón lima */
  --color-danger: #FF5C5C;
  --color-warning: #FFB020;

  --font-display: "Barlow Condensed", "Arial Narrow", sans-serif;
  --font-sans: "Inter", system-ui, sans-serif;
  --font-mono: ui-monospace, "SFMono-Regular", Menlo, monospace;

  --radius-card: 12px;
  --radius-control: 8px;
}

/* Tema claro opcional: se activa con data-theme="light" en <html> */
:root[data-theme="light"] {
  --color-bg: #FAFAF7;
  --color-surface: #FFFFFF;
  --color-surface-2: #F1F1EC;
  --color-border: #E2E2DB;
  --color-text: #15171A;
  --color-text-muted: #5B6470;
  --color-text-dim: #8A929C;
  --color-accent-text: #4F6B00;   /* lima oscurecido para TEXTO sobre fondo claro */
  --color-danger: #C62828;
  --color-warning: #A86A00;
}
```

Notas:
- **Oscuro por defecto.** Interruptor sol/luna en la barra superior; guarda la preferencia (localStorage con `try/catch`) y respeta `prefers-color-scheme` solo la primera vez. Evita el parpadeo con un script mínimo en `<head>`.
- El lima `#C6F432` sobre fondo claro **no tiene contraste** como texto: usa `--color-accent-text` para texto y deja el lima solo para rellenos con texto oscuro encima.
- Carga las fuentes con `next/font/google`: `Barlow_Condensed` (peso 800) e `Inter` (400, 500, 600, 700). Nunca debe caer a una fuente con serifas.

## 3. Tipografía
- Titulares: Barlow Condensed ExtraBold, MAYÚSCULAS, muy grandes (hero hasta ~9rem en escritorio). **Una palabra del titular en lima** (p. ej. FORJA TU PROPIO **OLIMPO**).
- Cuerpo: Inter 14-16 px. Cifras con `tabular-nums`. Horas del horario en mono / estilo marcador.
- Etiquetas pequeñas en mayúsculas con espaciado: úsalas con moderación (solo encabezados de sección), no en todas partes.

## 4. Componentes y reglas
- **Navegación superior** (sin barra lateral en escritorio) con menús desplegables **accesibles con teclado y con clic**, no solo con hover. En móvil, menú hamburguesa.
- Botón primario: relleno lima, texto oscuro, radio 8 px, `active:scale-[0.98]`. Secundario: superficie con borde. Destructivo: transparente con borde rojo suave.
- Tarjetas: superficie + borde 1 px, radio 12 px, **sin sombras difusas** (la profundidad sale del tono y del borde).
- Alterna fondos de sección entre `bg` y `surface` para dar ritmo.
- Barras de progreso/aforo en gris claro neutro; el lima solo para lo más importante. Rojo para "completa".
- Foco visible: `outline: 2px solid var(--color-accent); outline-offset: 2px`.
- Fotos: blanco y negro, alto contraste, grano sutil, degradado oscuro solo donde va texto. **La foto del hero tiene que verse** (en la referencia está demasiado oscurecida: sube su visibilidad).

## 5. Texto: reglas de copy
Español llano, directo y motivador, como habla un gimnasio real. Frases cortas.

**Palabras y cosas prohibidas** (aparecen en la referencia de Stitch y hay que limpiarlas): "telemetría", "algoritmo", "Neural", "GymFlow Neural 3.1", "biomecánica avanzada", "Índice de Fatiga del SNC", "RPE", "velocidad de barra", "supercompensación", "sistema central de atletas", "ubicación completa", "head coach" (usa "entrenador/a"), "agotada" (usa "completa"), emojis (usa iconos de línea).
Sustituciones útiles: "Esfuerzo: 8 de 10", "Cansancio: bajo", "Semana de descarga", "Entrenador inteligente", "En directo".

Nunca cortar texto con puntos suspensivos: los nombres largos pasan a dos líneas. Importes y cifras nunca se parten en dos líneas (`whitespace-nowrap`).

## 6. Movimiento ("la web viva")
Stitch solo da diseños estáticos; el movimiento se implementa en código (CSS, Framer Motion o GSAP). Todo debe respetar `prefers-reduced-motion` (versión sin animación) y no perjudicar el rendimiento (objetivo Lighthouse móvil > 85).

1. **Línea de pulso**: se dibuja sola al cargar (stroke-dashoffset) y late suavemente en la franja "En directo".
2. **Titular del hero** que cambia de palabra resaltada (ENTRENA / RESERVA / PROGRESA), con transición corta.
3. **Franja "En directo"**: datos reales de `GET /public/live` con actualización cada 30-60 s y un punto indicador discreto.
4. **Horario estilo panel de estación**: las horas y las plazas se muestran en casillas individuales y hacen un giro corto cuando cambian (versión sencilla con CSS 3D o animación de números). Si da problemas, déjalo estático pero con el aspecto de las casillas.
5. **Hero**: vídeo corto en bucle, sin sonido, `webm`/`mp4` ligero (nada de GIFs pesados), con la imagen fija como respaldo (`poster`). Opcional: partículas de tiza muy sutiles.
6. **Scroll**: aparición suave de secciones; en la sección del entrenador, la rutina se "construye" al entrar en pantalla.
7. **Micro-interacciones**: botones que reaccionan, tarjetas de "Nuestro hierro" con zoom lento al pasar el ratón, contadores que suben.

## 7. Imágenes y assets
- Las imágenes de las pantallas de Stitch están en servidores de Google y son generadas: **no enlazarlas**. Usa fotos con licencia (Unsplash/Pexels), propias o generadas con licencia clara, guardadas en `frontend/public/` y optimizadas con `next/image`. Anota autor y licencia en `docs/CREDITS.md`.
- Iconos: una librería de línea fina (por ejemplo `lucide-react`) en lugar de Material Symbols por CDN.

## 8. Mapa de pantallas de referencia → rutas

| Referencia (docs/design) | Ruta Next.js | Notas |
|---|---|---|
| `v2/web-publica` | `/` | Quitar: tienda, "nuestros centros", app/NFC, newsletter. Una sola sede en una sección "Dónde estamos" |
| `v2/entrenador-ia` | `/entrenador-ia` | La vista previa de la derecha se rellena con la rutina **real** generada por la IA |
| `v2/horario-clases` | `/horario` | Quitar "climatización". Mantener política de cancelación (hasta 2 h antes) |
| `v2/acceso` | `/acceso` | Quitar NFC/pulsera/"portal staff"/estado de aforo. Un solo formulario |
| `v2/mi-cuenta` | `/cuenta` | Menú: Inicio, Mis reservas, Mi entrenamiento, Mi cuota (quitar "Tienda"). Quitar taquilla, puesto de bici, pedidos y facturas. Rutina de hoy desde `workout_plans` |
| `reference-v1/panel-dueno-v1-con-sidebar` | `/admin` | **Solo contenido de referencia**: el rediseño debe ir con navegación superior (sin barra lateral), textos llanos y rol visible bajo el avatar ("Dueño"/"Staff"). Llámalo "Panel de gestión" |
| `reference-v1/movil-*` | `/cuenta` en móvil | Al usuario **le gusta el móvil como está**: barra inferior de 5 pestañas: Inicio, Reservar, Entrenador, Mi cuota, Perfil (la pestaña "Tienda" de la referencia se sustituye por "Mi cuota"). Cambiar emojis por iconos, marca OlympusGYM en cabecera |

La app del socio en móvil es la misma web adaptada (responsive/PWA), con barra de pestañas inferior en pantallas pequeñas.

## 9. Accesibilidad mínima
Contraste AA en ambos temas, foco visible, navegación completa con teclado (incluidos los desplegables), `alt` en imágenes, etiquetas en formularios, estados de error legibles, animaciones desactivables.

## 10. Fallos de las referencias de Stitch que NO hay que reproducir
Usos de `truncate` (texto cortado), jerga técnica, un punto parpadeante en exceso, una cita con serifas, hero casi negro, imágenes externas, CDN de Tailwind, dropdowns solo con hover, pastillas verdes en cada número, barras de progreso todas en lima, datos de ocupación que no podemos calcular.
