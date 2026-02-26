#!/bin/sh

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

HOUSE_NAME="${HOUSE_NAME:-Casa Demo $(date +%Y%m%d%H%M%S)}"
TOKEN="${TOKEN:-}"

usage() {
  cat <<EOF >&2
Uso: $0 [-n HOUSE_NAME] [-u BASE_URL] -t TOKEN

  -n HOUSE_NAME     Nombre de casa
  -u BASE_URL       URL base HouseDB (default: $BASE_URL)
  -t TOKEN          Bearer token de usuario (requerido)
  -h                Ayuda
EOF
  exit 2
}

while getopts "n:u:t:h" opt; do
  case "$opt" in
    n) HOUSE_NAME="$OPTARG" ;;
    u) BASE_URL="$OPTARG" ;;
    t) TOKEN="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

[ -n "$TOKEN" ] || usage

payload=$(jq -n --arg name "$HOUSE_NAME" '{name:$name}')

response=$(printf '%s' "$payload" | api_post_json_bearer "$TOKEN" /houses)
http_code=$(split_response "$response" | sed -n '1p')
body=$(split_response "$response" | sed -n '2,$p')

if [ "$http_code" -ge 300 ]; then
  echo "Error HTTP $http_code al crear casa" >&2
  print_json "$body" >&2
  exit 1
fi

printf '%s\n' "$body"
