#!/bin/sh
set -eu

JAVA_OPTS="${JAVA_OPTS:-}"
DB_URL="${DB_URL:-}"

if [ -z "$DB_URL" ]; then
  echo "[start-housedb] WARNING: DB_URL is not set. The application may fail to connect to the database." >&2
fi

echo "[start-housedb] Starting housedb-backend (DB_URL=${DB_URL:+<provided>})"

exec java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=70.0 \
  -XX:+ExitOnOutOfMemoryError \
  $JAVA_OPTS \
  -javaagent:/app/glowroot/glowroot.jar \
  -jar /app/app.jar \
  "$@"
