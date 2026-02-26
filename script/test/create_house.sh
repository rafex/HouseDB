#!/bin/sh

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

OWNER_USER_ID=""
HOUSE_NAME="${HOUSE_NAME:-Casa Demo $(date +%Y%m%d%H%M%S)}"
TOKEN="${TOKEN:-}"

usage() {
  cat <<EOF >&2
Uso: $0 -o OWNER_USER_ID [-n HOUSE_NAME] [-u BASE_URL] [-t TOKEN] [-s JWT_SECRET]

  -o OWNER_USER_ID  UUID del owner (requerido)
  -n HOUSE_NAME     Nombre de casa
  -u BASE_URL       URL base HouseDB (default: $BASE_URL)
  -t TOKEN          Bearer token (si se omite, se firma token app con JWT_SECRET)
  -s JWT_SECRET     Secret para firmar token app cuando no se pasa -t
  -h                Ayuda
EOF
  exit 2
}

while getopts "o:n:u:t:s:h" opt; do
  case "$opt" in
    o) OWNER_USER_ID="$OPTARG" ;;
    n) HOUSE_NAME="$OPTARG" ;;
    u) BASE_URL="$OPTARG" ;;
    t) TOKEN="$OPTARG" ;;
    s) JWT_SECRET="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

[ -n "$OWNER_USER_ID" ] || usage

if [ -z "$TOKEN" ]; then
  TOKEN=$(mint_app_token)
fi

payload=$(jq -n --arg ownerUserId "$OWNER_USER_ID" --arg name "$HOUSE_NAME" \
  '{ownerUserId:$ownerUserId,name:$name}')

response=$(printf '%s' "$payload" | api_post_json_bearer "$TOKEN" /houses)
http_code=$(split_response "$response" | sed -n '1p')
body=$(split_response "$response" | sed -n '2,$p')

if [ "$http_code" -ge 300 ]; then
  echo "Error HTTP $http_code al crear casa" >&2
  print_json "$body" >&2
  exit 1
fi

printf '%s\n' "$body"
