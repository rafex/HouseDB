#!/bin/sh

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

OBJECT_NAME="${OBJECT_NAME:-Linterna de campamento}"
OBJECT_CATEGORY="${OBJECT_CATEGORY:-camping}"
OBJECT_DESCRIPTION="${OBJECT_DESCRIPTION:-Objeto de prueba e2e HouseDB}"

usage() {
  cat <<EOF >&2
Uso: $0 [-O OBJECT_NAME] [-C OBJECT_CATEGORY] [-D OBJECT_DESCRIPTION] [-d DB_URL]

  -O OBJECT_NAME         Nombre del objeto
  -C OBJECT_CATEGORY     Categoría
  -D OBJECT_DESCRIPTION  Descripción
  -d DB_URL              URL PostgreSQL para psql (default: $DB_URL)
  -h                     Ayuda
EOF
  exit 2
}

while getopts "O:C:D:d:h" opt; do
  case "$opt" in
    O) OBJECT_NAME="$OPTARG" ;;
    C) OBJECT_CATEGORY="$OPTARG" ;;
    D) OBJECT_DESCRIPTION="$OPTARG" ;;
    d) DB_URL="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

OBJECT_NAME_SQL=$(sql_escape "$OBJECT_NAME")
OBJECT_CATEGORY_SQL=$(sql_escape "$OBJECT_CATEGORY")
OBJECT_DESCRIPTION_SQL=$(sql_escape "$OBJECT_DESCRIPTION")

OBJECT_ID=$(psql_scalar "
INSERT INTO objects (
  object_id, name, description, category, enabled, created_at, updated_at
) VALUES (
  gen_random_uuid(),
  '$OBJECT_NAME_SQL',
  '$OBJECT_DESCRIPTION_SQL',
  '$OBJECT_CATEGORY_SQL',
  TRUE,
  NOW(),
  NOW()
)
RETURNING object_id::text;
")

jq -n --arg objectId "$OBJECT_ID" --arg name "$OBJECT_NAME" --arg category "$OBJECT_CATEGORY" \
  '{objectId:$objectId,name:$name,category:$category}'
