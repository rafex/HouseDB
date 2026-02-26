#!/bin/sh

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

USER_ID=""
OBJECT_ID=""
HOUSE_LOCATION_ID=""
ITEM_NICKNAME="${ITEM_NICKNAME:-linterna negra}"
TOKEN="${TOKEN:-}"

usage() {
  cat <<EOF >&2
Uso: $0 -U USER_ID -O OBJECT_ID -L HOUSE_LOCATION_ID [-N ITEM_NICKNAME] [-u BASE_URL] [-t TOKEN] [-s JWT_SECRET]

  -U USER_ID           UUID del usuario owner del item (requerido)
  -O OBJECT_ID         UUID del objeto (requerido)
  -L HOUSE_LOCATION_ID UUID locación hoja (requerido)
  -N ITEM_NICKNAME     Nickname del inventario
  -u BASE_URL          URL base HouseDB (default: $BASE_URL)
  -t TOKEN             Bearer token (si se omite, se firma token app con JWT_SECRET)
  -s JWT_SECRET        Secret para firmar token app cuando no se pasa -t
  -h                   Ayuda
EOF
  exit 2
}

while getopts "U:O:L:N:u:t:s:h" opt; do
  case "$opt" in
    U) USER_ID="$OPTARG" ;;
    O) OBJECT_ID="$OPTARG" ;;
    L) HOUSE_LOCATION_ID="$OPTARG" ;;
    N) ITEM_NICKNAME="$OPTARG" ;;
    u) BASE_URL="$OPTARG" ;;
    t) TOKEN="$OPTARG" ;;
    s) JWT_SECRET="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

[ -n "$USER_ID" ] || usage
[ -n "$OBJECT_ID" ] || usage
[ -n "$HOUSE_LOCATION_ID" ] || usage

if [ -z "$TOKEN" ]; then
  TOKEN=$(mint_app_token)
fi

payload=$(jq -n \
  --arg userId "$USER_ID" \
  --arg objectId "$OBJECT_ID" \
  --arg nickname "$ITEM_NICKNAME" \
  --arg houseLocationLeafId "$HOUSE_LOCATION_ID" \
  '{userId:$userId,objectId:$objectId,nickname:$nickname,houseLocationLeafId:$houseLocationLeafId,conditionStatus:"active",movedBy:"script/test/create_inventory_item.sh",notes:"alta e2e"}')

response=$(printf '%s' "$payload" | api_post_json_bearer "$TOKEN" /items)
http_code=$(split_response "$response" | sed -n '1p')
body=$(split_response "$response" | sed -n '2,$p')

if [ "$http_code" -ge 300 ]; then
  echo "Error HTTP $http_code al crear inventario" >&2
  print_json "$body" >&2
  exit 1
fi

printf '%s\n' "$body"
