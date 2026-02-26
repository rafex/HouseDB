#!/bin/sh

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"

: "${BASE_URL:=http://localhost:8080}"
: "${DB_URL:=postgresql://housedb:housedb@localhost:5432/housedb}"
: "${JWT_SECRET:=CHANGE_ME_NOW_32+chars_secret}"

SUFFIX="${SUFFIX:-$(date +%Y%m%d%H%M%S)}"
USERNAME="${USERNAME:-housedb_user_${SUFFIX}}"
PASSWORD="${PASSWORD:-secret123}"
HOUSE_NAME="${HOUSE_NAME:-Casa Demo ${SUFFIX}}"
LOCATION_NAME="${LOCATION_NAME:-Closet principal}"
OBJECT_NAME="${OBJECT_NAME:-Linterna de campamento}"
OBJECT_CATEGORY="${OBJECT_CATEGORY:-camping}"
OBJECT_DESCRIPTION="${OBJECT_DESCRIPTION:-Objeto de prueba e2e HouseDB}"
ITEM_NICKNAME="${ITEM_NICKNAME:-linterna negra}"

usage() {
  cat <<EOF >&2
Uso: $0 [opciones]

Crea flujo de prueba E2E:
1) crea usuario por API securizada
2) crea casa por API (el owner queda miembro)
3) crea locación en DB para la casa
4) crea objeto en DB
5) guarda objeto en inventario por API

Opciones:
  -u BASE_URL          URL base HouseDB (default: $BASE_URL)
  -d DB_URL            URL PostgreSQL para psql (default: $DB_URL)
  -s JWT_SECRET        Secret JWT de HouseDB para firmar token app
  -U USERNAME          Username a crear por API (default: $USERNAME)
  -P PASSWORD          Password del usuario por API (default: $PASSWORD)
  -H HOUSE_NAME        Nombre de la casa (default: "$HOUSE_NAME")
  -L LOCATION_NAME     Nombre de la locación (default: "$LOCATION_NAME")
  -O OBJECT_NAME       Nombre del objeto (default: "$OBJECT_NAME")
  -C OBJECT_CATEGORY   Categoría del objeto (default: "$OBJECT_CATEGORY")
  -N ITEM_NICKNAME     Nickname del item inventario (default: "$ITEM_NICKNAME")
  -h                   Ayuda

Ejemplo:
  $0 -u http://localhost:8080 -d postgresql://housedb:housedb@localhost:5432/housedb
EOF
  exit 2
}

while getopts "u:d:s:U:P:H:L:O:C:N:h" opt; do
  case "$opt" in
    u) BASE_URL="$OPTARG" ;;
    d) DB_URL="$OPTARG" ;;
    s) JWT_SECRET="$OPTARG" ;;
    U) USERNAME="$OPTARG" ;;
    P) PASSWORD="$OPTARG" ;;
    H) HOUSE_NAME="$OPTARG" ;;
    L) LOCATION_NAME="$OPTARG" ;;
    O) OBJECT_NAME="$OPTARG" ;;
    C) OBJECT_CATEGORY="$OPTARG" ;;
    N) ITEM_NICKNAME="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

printf '\n== HouseDB E2E Flow ==\n'
printf 'BASE_URL: %s\n' "$BASE_URL"
printf 'DB_URL: %s\n' "$DB_URL"
printf 'USERNAME: %s\n' "$USERNAME"

printf '\n[1/5] Creando usuario vía API securizada...\n'
user_json=$("$SOURCE_DIR/create_user.sh" -u "$BASE_URL" -s "$JWT_SECRET" -U "$USERNAME" -P "$PASSWORD")
USER_ID=$(printf '%s' "$user_json" | jq -r '.userId')
printf 'user_id: %s\n' "$USER_ID"

printf '\n[2/5] Creando casa vía API...\n'
house_json=$("$SOURCE_DIR/create_house.sh" -u "$BASE_URL" -s "$JWT_SECRET" -o "$USER_ID" -n "$HOUSE_NAME")
HOUSE_ID=$(printf '%s' "$house_json" | jq -r '.houseId')
HOUSE_MEMBER_ID=$(printf '%s' "$house_json" | jq -r '.houseMemberId')
printf '%s\n' "$house_json" | jq .

printf '\n[3/5] Creando locación en DB para la casa...\n'
location_json=$("$SOURCE_DIR/create_house_location_db.sh" -d "$DB_URL" -H "$HOUSE_ID" -L "$LOCATION_NAME")
HOUSE_LOCATION_ID=$(printf '%s' "$location_json" | jq -r '.houseLocationId')
printf 'house_location_id: %s\n' "$HOUSE_LOCATION_ID"

printf '\n[4/5] Creando objeto en DB...\n'
object_json=$("$SOURCE_DIR/create_object_db.sh" -d "$DB_URL" -O "$OBJECT_NAME" -C "$OBJECT_CATEGORY" -D "$OBJECT_DESCRIPTION")
OBJECT_ID=$(printf '%s' "$object_json" | jq -r '.objectId')
printf 'object_id: %s\n' "$OBJECT_ID"

printf '\n[5/5] Guardando objeto en inventario vía API...\n'
item_json=$("$SOURCE_DIR/create_inventory_item.sh" -u "$BASE_URL" -s "$JWT_SECRET" -U "$USER_ID" -O "$OBJECT_ID" -L "$HOUSE_LOCATION_ID" -N "$ITEM_NICKNAME")
INVENTORY_ITEM_ID=$(printf '%s' "$item_json" | jq -r '.inventoryItemId // empty')
ITEM_MOVEMENT_ID=$(printf '%s' "$item_json" | jq -r '.itemMovementId // empty')
printf '%s\n' "$item_json" | jq .

printf '\n== Resumen ==\n'
printf 'user_id: %s\n' "$USER_ID"
printf 'house_id: %s\n' "$HOUSE_ID"
printf 'house_member_id: %s\n' "$HOUSE_MEMBER_ID"
printf 'house_location_id: %s\n' "$HOUSE_LOCATION_ID"
printf 'object_id: %s\n' "$OBJECT_ID"
printf 'inventory_item_id: %s\n' "$INVENTORY_ITEM_ID"
printf 'item_movement_id: %s\n' "$ITEM_MOVEMENT_ID"

printf '\nFlujo completado.\n'
