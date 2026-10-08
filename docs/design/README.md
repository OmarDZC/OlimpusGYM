# Referencias de diseño

Exportadas de Stitch. Son **referencia visual**, no código de producción. La guía a seguir es `docs/DESIGN.md`.

```
v2/                        pantallas actuales (oscuro por defecto)
  web-publica/             inicio (code.html + screen.png; code-completo.html es el HTML completo adjunto)
  entrenador-ia/           página del entrenador con IA
  horario-clases/          horario con reservas
  acceso/                  login / registro
  mi-cuenta/               área del socio en escritorio
reference-v1/              versión anterior; solo se aprovecha lo indicado
  movil-inicio/            app del socio (al usuario le gusta como está)
  movil-entrenador/
  movil-reservar/
  panel-dueno-v1-con-sidebar/   contenido del panel; rediseñar con navegación superior
stitch-design-system.md    sistema "Kinetic Obsidian" generado por Stitch (apoyo)
```

Cada `screen.png` se puede abrir para ver el resultado visual. Los `code.html` sirven para ver textos, estructura y clases, pero tienen fallos conocidos (lista en `DESIGN.md`, sección 10) y elementos fuera del alcance del MVP (tienda, varios centros, NFC, pedidos).

Faltan por exportar: el panel de gestión nuevo (con navegación superior) y las pantallas móviles actualizadas. Mientras tanto, usa `reference-v1/` y las reglas de `DESIGN.md`.
