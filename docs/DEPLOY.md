# Desplegar OlympusGYM en un VPS

Guía paso a paso para alguien que **nunca ha desplegado nada antes**. Pensada para un VPS
Ubuntu (22.04 o 24.04) con acceso `root` o `sudo`, por ejemplo de Hetzner, DigitalOcean,
OVH o similar. El más pequeño (1-2 GB de RAM) es suficiente para esta demo.

Todos los comandos son para ejecutarse **dentro del servidor**, por SSH, salvo que se diga lo
contrario.

## 1. Crear el servidor

1. Crea una máquina virtual Ubuntu 22.04 o 24.04 en el proveedor que prefieras.
2. Apunta la IP pública que te dé: la necesitarás más abajo.
3. Conéctate la primera vez como `root`:
   ```bash
   ssh root@TU_IP_PUBLICA
   ```

## 2. Crear un usuario sin privilegios de root

Trabajar siempre como `root` es innecesariamente peligroso. Crea un usuario normal con
permiso para usar `sudo`:

```bash
adduser gymflow
usermod -aG sudo gymflow
```

Copia tu clave SSH al nuevo usuario (si usas contraseña en vez de clave, `adduser` ya te la
habrá pedido) y, desde tu máquina, comprueba que puedes entrar como `gymflow`:

```bash
rsync --archive --chown=gymflow:gymflow ~/.ssh /home/gymflow
exit
ssh gymflow@TU_IP_PUBLICA
```

A partir de aquí, todos los comandos son como `gymflow` (con `sudo` cuando haga falta), no
como `root`.

## 3. Firewall: solo 22, 80 y 443

```bash
sudo apt update
sudo apt install -y ufw
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

Confirma con `sudo ufw status` que solo esos tres puertos están abiertos. Nada más del
stack (Postgres, Redis, el backend, el servicio de IA) debe ser alcanzable desde fuera: eso
ya lo garantiza `docker-compose.prod.yml` (solo Caddy publica puertos), pero el firewall del
propio servidor es una segunda capa de seguridad, por si algún día se añade un servicio sin
pensarlo bien.

## 4. Instalar Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker gymflow
```

Cierra la sesión SSH y vuelve a entrar para que el nuevo grupo (`docker`) surta efecto:

```bash
exit
ssh gymflow@TU_IP_PUBLICA
docker --version
docker compose version
```

Ambos comandos deben responder con un número de versión, no con un error.

## 5. Clonar el repositorio

```bash
sudo apt install -y git
git clone https://github.com/OmarDZC/OlimpusGYM.git
cd OlimpusGYM
```

(Cambia la URL si el repositorio se ha renombrado o es privado y clonas por SSH.)

## 6. Apuntar el dominio (opcional, pero recomendado)

Si tienes un dominio, crea un registro DNS tipo **A** que apunte a la IP pública del
servidor (p. ej. `gymflow.tudominio.com → TU_IP_PUBLICA`). La propagación puede tardar de
minutos a un par de horas.

Si todavía no tienes dominio, puedes probar todo el stack directamente con la IP, sin HTTPS
(ver el siguiente paso): es exactamente el mismo mecanismo que se usó para probar esto en
local durante el desarrollo (Trozo 8a-1/8a-2, `docs/PROGRESS.md`).

## 7. Crear el archivo de variables (`.env.prod`)

```bash
cp .env.prod.example .env.prod
nano .env.prod   # o el editor que prefieras
```

Rellena **todas** las variables, nunca con los valores de ejemplo del archivo. Resumen (el
detalle completo de cada una está en `README.md`, sección "Despliegue en producción"):

| Variable | Qué poner |
|---|---|
| `DOMAIN` | Tu dominio real (p. ej. `gymflow.tudominio.com`), o `:80` si de momento solo tienes la IP |
| `PUBLIC_URL` | `https://gymflow.tudominio.com` con dominio real, o `http://TU_IP_PUBLICA` sin él |
| `POSTGRES_PASSWORD` | `openssl rand -base64 24` |
| `REDIS_PASSWORD` | `openssl rand -base64 24` |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `LLM_BASE_URL` / `LLM_API_KEY` / `LLM_MODEL` | Los de tu proveedor de IA compatible con OpenAI |
| `SPRING_PROFILES_ACTIVE` | `prod` para un despliegue real, o `demo` para una demo pública |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Solo si `SPRING_PROFILES_ACTIVE=prod`: tu cuenta de dueño real (contraseña de 12 caracteres o más) |
| `DEMO_OWNER_PASSWORD` / `DEMO_MEMBER_PASSWORD` | Solo si `SPRING_PROFILES_ACTIVE=demo`: `openssl rand -base64 18` cada una |

El comando `openssl rand -base64 N` ya viene instalado en Ubuntu; genera cada secreto por
separado, no reutilices el mismo valor en dos variables.

## 8. Arrancar el stack

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

La primera vez tarda unos minutos (construye las tres imágenes). Comprueba que los seis
servicios llegan a `healthy`:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
```

Si alguno no llega a `healthy` en un par de minutos, mira sus logs:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod logs backend --tail 100
```

## 9. Comprobar que funciona

- Abre `http://TU_IP_PUBLICA` (o `https://tu-dominio` si ya tienes DNS y HTTPS) en el
  navegador: debe cargar la portada.
- Si usaste `SPRING_PROFILES_ACTIVE=prod`: entra en `/acceso` con `ADMIN_EMAIL`/
  `ADMIN_PASSWORD`. Confirma que **no** aparecen los botones de demo (si aparecieran, algo
  está mal configurado).
- Si usaste `SPRING_PROFILES_ACTIVE=demo`: confirma que en `/acceso` sí aparecen la franja
  de aviso y los botones "Entrar como socio/dueño de demostración", y que funcionan.
- Con un dominio real, comprueba el candado HTTPS del navegador (Caddy pide el certificado
  solo; la primera vez puede tardar hasta un minuto).

## 10. Backups y métricas (Actuator)

`/actuator/health` y `/actuator/metrics` del backend **no son públicos**: Caddy no los
expone (ver `Caddyfile`), así que solo son alcanzables desde dentro del servidor. Para
consultarlos:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod exec backend \
  curl -s http://localhost:8080/actuator/health
```

O, para verlos desde tu propio navegador sin exponer nada al público, abre un túnel SSH
desde tu máquina:

```bash
ssh -L 8080:localhost:8080 gymflow@TU_IP_PUBLICA
```

y, mientras ese túnel esté abierto, visita `http://localhost:8080/actuator/health` en tu
navegador (necesitarías además publicar el puerto del backend en el servidor temporalmente
para probar esto desde fuera del contenedor; normalmente basta con el primer comando,
`docker compose exec`).

### Copias de seguridad

```bash
./scripts/backup-db.sh .env.prod
```

Guarda un volcado comprimido en `backups/`, con la fecha en el nombre, y conserva solo los
últimos 7. Para restaurar (por ejemplo, tras perder el servidor):

```bash
./scripts/restore-db.sh backups/gymflow_AAAA-MM-DD_HHMMSS.dump .env.prod
```

Pide confirmación escrita antes de sustituir los datos (añade `--yes` para scripts
automáticos). Reinicia el backend después:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod restart backend
```

## 11. Programar backups y, si usas demo, el reinicio nocturno

```bash
crontab -e
```

Añade (ajusta la ruta `/home/gymflow/OlimpusGYM` a la tuya real):

```cron
# Copia de seguridad cada noche a las 3:00
0 3 * * * cd /home/gymflow/OlimpusGYM && ./scripts/backup-db.sh >> /var/log/gymflow-backup.log 2>&1

# Solo si SPRING_PROFILES_ACTIVE=demo: reinicia la demo a sus datos de ejemplo cada noche a las 4:00
0 4 * * * cd /home/gymflow/OlimpusGYM && ./scripts/reset-demo.sh >> /var/log/gymflow-reset-demo.log 2>&1
```

Los dos scripts escriben sus propios mensajes con fecha; revisa esos archivos de log de vez
en cuando (`tail -f /var/log/gymflow-backup.log`) para confirmar que siguen funcionando.

## 12. Actualizar con una nueva versión

```bash
cd /home/gymflow/OlimpusGYM
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Esto reconstruye solo lo que haya cambiado y reinicia esos contenedores; Postgres y Redis
siguen con sus datos (volúmenes con nombre). Si una migración de Flyway falla al arrancar el
backend, revisa los logs (`docker compose ... logs backend`) antes de nada: Flyway no
reintenta solo, hay que corregir el problema y volver a `up -d --build`.

## Demo en vivo desde mi PC con Cloudflare Tunnel

Para enseñar la demo sin un VPS: levanta el stack en tu propio PC (Windows) y abre un túnel
gratuito de Cloudflare hacia Caddy. No necesita cuenta de Cloudflare ni dominio propio.

1. Instala `cloudflared` (una sola vez):
   ```powershell
   winget install Cloudflare.cloudflared
   ```
2. Crea `.env.prod` (como en el paso 7, pero en tu PC) con `DOMAIN=:80`, `PUBLIC_URL=http://localhost`
   y `SPRING_PROFILES_ACTIVE=demo` (con `DEMO_OWNER_EMAIL`/`DEMO_OWNER_PASSWORD` propios).
3. Arranca el stack:
   ```powershell
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
   ```
4. Antes de enseñarla, déjala limpia:
   ```powershell
   ./scripts/reset-demo.sh .env.prod
   ```
5. Abre el túnel hacia el puerto de Caddy (80), en una ventana que dejes abierta:
   ```powershell
   cloudflared tunnel --url http://localhost:80
   ```
   `cloudflared` imprime una URL `https://algo-al-azar.trycloudflare.com`: esa es la que
   compartes. Solo expone el puerto de Caddy — Postgres, Redis y el servicio de IA nunca
   publican puerto al host en `docker-compose.prod.yml` (solo Caddy tiene `ports:`), así que
   el túnel no da acceso a nada más.
6. Para pararlo: `Ctrl+C` en la ventana del túnel, y `docker compose -f docker-compose.prod.yml --env-file .env.prod down` para el stack.

Un script listo con los pasos 3-5 en `scripts/demo-tunnel.ps1`.

**Avisos importantes**:
- La URL cambia cada vez que abres el túnel: no la guardes como permanente.
- Solo funciona mientras tu PC esté encendido y conectado — no es un despliegue real.
- Ejecuta `reset-demo.sh` antes de cada sesión en la que vayas a enseñarla, para que quien la
  prueba vea siempre los datos de ejemplo limpios, no lo que dejó la última persona.

## Lista de comprobación final

- [ ] `ufw status` muestra solo 22, 80 y 443 abiertos.
- [ ] `docker compose -f docker-compose.prod.yml --env-file .env.prod ps` muestra los 6
      servicios en `healthy`.
- [ ] La web carga en el navegador (con HTTPS si hay dominio).
- [ ] Login correcto: como OWNER real (`prod`) o con los botones de demo (`demo`), según
      corresponda — y nunca ambos a la vez.
- [ ] `./scripts/backup-db.sh .env.prod` ejecutado al menos una vez a mano y comprobado que
      crea un archivo en `backups/`.
- [ ] Copias de seguridad programadas en `crontab -e`.
- [ ] Si es una demo pública: `./scripts/reset-demo.sh .env.prod` programado también, y
      probado a mano al menos una vez.
- [ ] `.env.prod` **no** está en git (`git status` no debe mostrarlo; ya está en
      `.gitignore`) y tiene permisos que solo tu usuario puede leer (`chmod 600 .env.prod`).
