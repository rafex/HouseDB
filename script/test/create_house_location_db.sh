#!/bin/sh

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

HOUSE_ID=""
LOCATION_NAME="${LOCATION_NAME:-Closet principal}"

usage() {
  cat <<EOF >&2
Uso: $0 -H HOUSE_ID [-L LOCATION_NAME] [-d DB_URL]

  -H HOUSE_ID       UUID de la casa (requerido)
  -L LOCATION_NAME  Nombre de locación
  -d DB_URL         URL PostgreSQL para psql (default: $DB_URL)
  -h                Ayuda
EOF
  exit 2
}

while getopts "H:L:d:h" opt; do
  case "$opt" in
    H) HOUSE_ID="$OPTARG" ;;
    L) LOCATION_NAME="$OPTARG" ;;
    d) DB_URL="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

[ -n "$HOUSE_ID" ] || usage
LOCATION_NAME_SQL=$(sql_escape "$LOCATION_NAME")

HOUSE_LOCATION_ID=$(psql_scalar "
INSERT INTO house_locations (
  house_location_id, house_id, location_kind, name, is_leaf, enabled, created_at, updated_at
) VALUES (
  gen_random_uuid(),
  '$HOUSE_ID'::uuid,
  'slot',
  '$LOCATION_NAME_SQL',
  TRUE,
  TRUE,
  NOW(),
  NOW()
)
RETURNING house_location_id::text;
")

jq -n --arg houseId "$HOUSE_ID" --arg houseLocationId "$HOUSE_LOCATION_ID" --arg name "$LOCATION_NAME" \
  '{houseId:$houseId,houseLocationId:$houseLocationId,name:$name}'
