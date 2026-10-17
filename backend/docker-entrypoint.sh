#!/bin/sh
# Arranque del contenedor de la API.
#
# Las plataformas (Railway, Docker con volúmenes) montan el disco persistente como propiedad de root.
# El contenedor arranca como root solo para dejar ese directorio escribible por el usuario de la
# aplicación y en seguida baja a ese usuario: la JVM nunca corre con privilegios (AC-15.6, AC-21.4).
set -e

UPLOADS="${UPLOADS_DIR:-/var/data/uploads}"
RUN_JAVA='exec java $JAVA_OPTS -jar /app/app.jar'

if [ "$(id -u)" = "0" ]; then
  mkdir -p "$UPLOADS"
  chown sigepes:sigepes "$(dirname "$UPLOADS")" 2>/dev/null || true
  chown -R sigepes:sigepes "$UPLOADS"
  exec su-exec sigepes:sigepes sh -c "$RUN_JAVA"
fi

exec sh -c "$RUN_JAVA"
