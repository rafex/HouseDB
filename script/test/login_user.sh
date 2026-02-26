#!/bin/sh

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

SUFFIX="${SUFFIX:-$(date +%Y%m%d%H%M%S)}"
USERNAME="${USERNAME:-housedb_user_${SUFFIX}}"
PASSWORD="${PASSWORD:-secret123}"

usage() {
  cat <<EOF >&2
Uso: $0 [-u BASE_URL] [-U USERNAME] [-P PASSWORD]

  -u BASE_URL   URL base HouseDB (default: $BASE_URL)
  -U USERNAME   Username
  -P PASSWORD   Password
  -h            Ayuda
EOF
  exit 2
}

while getopts "u:U:P:h" opt; do
  case "$opt" in
    u) BASE_URL="$OPTARG" ;;
    U) USERNAME="$OPTARG" ;;
    P) PASSWORD="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

payload=$(jq -n --arg username "$USERNAME" --arg password "$PASSWORD" \
  '{username:$username,password:$password}')

response=$(printf '%s' "$payload" | api_post_json /auth/login)
http_code=$(split_response "$response" | sed -n '1p')
body=$(split_response "$response" | sed -n '2,$p')

if [ "$http_code" -ge 300 ]; then
  echo "Error HTTP $http_code al autenticar usuario" >&2
  print_json "$body" >&2
  exit 1
fi

printf '%s\n' "$body"
