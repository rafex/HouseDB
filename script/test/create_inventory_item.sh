#!/bin/sh

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

OBJECT_ID=""
HOUSE_LOCATION_ID=""
ITEM_NICKNAME="${ITEM_NICKNAME:-linterna negra}"
OBJECT_NAME="${OBJECT_NAME:-Linterna de campamento}"
OBJECT_CATEGORY="${OBJECT_CATEGORY:-camping}"
OBJECT_DESCRIPTION="${OBJECT_DESCRIPTION:-Objeto creado desde HouseDB}"
OBJECT_TYPE="${OBJECT_TYPE:-EQUIPMENT}"
OBJECT_TAGS="${OBJECT_TAGS:-camping,linterna}"
KIWI_METADATA_JSON="${KIWI_METADATA_JSON:-{\"source\":\"housedb\",\"priority\":\"medium\"}}"
HOUSEDB_METADATA_JSON="${HOUSEDB_METADATA_JSON:-{\"purchaseDate\":\"2026-02-26\",\"warrantyMonths\":12}}"
TOKEN="${TOKEN:-}"

usage() {
  cat <<EOF >&2
Uso: $0 -L HOUSE_LOCATION_ID [-N ITEM_NICKNAME] [-M OBJECT_NAME] [-C OBJECT_CATEGORY] [-D OBJECT_DESCRIPTION] [-T OBJECT_TYPE] [-G OBJECT_TAGS] [-K KIWI_METADATA_JSON] [-H HOUSEDB_METADATA_JSON] [-u BASE_URL] -t TOKEN

  -L HOUSE_LOCATION_ID UUID locación hoja (requerido)
  -N ITEM_NICKNAME     Nickname del inventario
  -M OBJECT_NAME       Nombre del objeto a crear en Kiwi
  -C OBJECT_CATEGORY   Categoría/tags para Kiwi
  -D OBJECT_DESCRIPTION Descripción del objeto
  -T OBJECT_TYPE       Tipo para Kiwi (default: EQUIPMENT)
  -G OBJECT_TAGS       Tags CSV para Kiwi (default: camping,linterna)
  -K KIWI_METADATA_JSON JSON metadata para Kiwi
  -H HOUSEDB_METADATA_JSON JSON metadata para HouseDB (inventory_items.metadata)
  -u BASE_URL          URL base HouseDB (default: $BASE_URL)
  -t TOKEN             Bearer token de usuario (requerido)
  -h                   Ayuda
EOF
  exit 2
}

while getopts "L:N:M:C:D:T:G:K:H:u:t:h" opt; do
  case "$opt" in
    L) HOUSE_LOCATION_ID="$OPTARG" ;;
    N) ITEM_NICKNAME="$OPTARG" ;;
    M) OBJECT_NAME="$OPTARG" ;;
    C) OBJECT_CATEGORY="$OPTARG" ;;
    D) OBJECT_DESCRIPTION="$OPTARG" ;;
    T) OBJECT_TYPE="$OPTARG" ;;
    G) OBJECT_TAGS="$OPTARG" ;;
    K) KIWI_METADATA_JSON="$OPTARG" ;;
    H) HOUSEDB_METADATA_JSON="$OPTARG" ;;
    u) BASE_URL="$OPTARG" ;;
    t) TOKEN="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

[ -n "$HOUSE_LOCATION_ID" ] || usage
[ -n "$TOKEN" ] || usage

object_tags_json=$(printf '%s' "$OBJECT_TAGS" | awk -F',' '
BEGIN { printf "[" }
{
  for (i=1; i<=NF; i++) {
    gsub(/^[ \t]+|[ \t]+$/, "", $i);
    if (length($i) > 0) {
      if (c++ > 0) printf ",";
      gsub(/"/, "\\\"", $i);
      printf "\"%s\"", $i;
    }
  }
}
END { printf "]" }
')

payload=$(jq -n \
  --arg objectName "$OBJECT_NAME" \
  --arg objectDescription "$OBJECT_DESCRIPTION" \
  --arg objectCategory "$OBJECT_CATEGORY" \
  --arg objectType "$OBJECT_TYPE" \
  --argjson objectTags "$object_tags_json" \
  --argjson kiwiMetadata "$KIWI_METADATA_JSON" \
  --argjson housedbMetadata "$HOUSEDB_METADATA_JSON" \
  --arg nickname "$ITEM_NICKNAME" \
  --arg houseLocationLeafId "$HOUSE_LOCATION_ID" \
  '{objectName:$objectName,objectDescription:$objectDescription,objectCategory:$objectCategory,objectType:$objectType,objectTags:$objectTags,kiwiMetadata:$kiwiMetadata,housedbMetadata:$housedbMetadata,nickname:$nickname,houseLocationLeafId:$houseLocationLeafId,conditionStatus:"active",movedBy:"script/test/create_inventory_item.sh",notes:"alta e2e"}')

response=$(printf '%s' "$payload" | api_post_json_bearer "$TOKEN" /items)
http_code=$(split_response "$response" | sed -n '1p')
body=$(split_response "$response" | sed -n '2,$p')

if [ "$http_code" -ge 300 ]; then
  echo "Error HTTP $http_code al crear inventario" >&2
  print_json "$body" >&2
  exit 1
fi

printf '%s\n' "$body"
