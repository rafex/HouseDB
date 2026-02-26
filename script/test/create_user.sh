#!/bin/sh

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

SUFFIX="${SUFFIX:-$(date +%Y%m%d%H%M%S)}"
USERNAME="${USERNAME:-housedb_user_${SUFFIX}}"
PASSWORD="${PASSWORD:-secret123}"
TOKEN="${TOKEN:-}"

usage() {
  cat <<EOF >&2
Uso: $0 [-u BASE_URL] [-U USERNAME] [-P PASSWORD] [-t TOKEN] [-s JWT_SECRET]

  -u BASE_URL   URL base HouseDB (default: $BASE_URL)
  -U USERNAME   Username a crear (default: $USERNAME)
  -P PASSWORD   Password del usuario (default: $PASSWORD)
  -t TOKEN      Bearer token (si se omite, se firma token app con JWT_SECRET)
  -s JWT_SECRET Secret para firmar token app cuando no se pasa -t
  -h            Ayuda
EOF
  exit 2
}

while getopts "u:U:P:t:s:h" opt; do
  case "$opt" in
    u) BASE_URL="$OPTARG" ;;
    U) USERNAME="$OPTARG" ;;
    P) PASSWORD="$OPTARG" ;;
    t) TOKEN="$OPTARG" ;;
    s) JWT_SECRET="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

if [ -z "$TOKEN" ]; then
  TOKEN=$(mint_app_token)
fi

payload=$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
  '{username:$username,password:$password}')

response=$(printf '%s' "$payload" | api_post_json_bearer "$TOKEN" /users)
http_code=$(split_response "$response" | sed -n '1p')
body=$(split_response "$response" | sed -n '2,$p')

if [ "$http_code" -ge 300 ]; then
  echo "Error HTTP $http_code al crear usuario" >&2
  print_json "$body" >&2
  exit 1
fi

printf '%s\n' "$body"
