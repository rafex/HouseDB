#!/bin/sh

set -eu

command -v jq >/dev/null 2>&1 || {
  echo "Error: jq no está instalado." >&2
  exit 1
}

command -v curl >/dev/null 2>&1 || {
  echo "Error: curl no está instalado." >&2
  exit 1
}

command -v psql >/dev/null 2>&1 || {
  echo "Error: psql no está instalado." >&2
  exit 1
}

command -v openssl >/dev/null 2>&1 || {
  echo "Error: openssl no está instalado." >&2
  exit 1
}

command -v uuidgen >/dev/null 2>&1 || {
  echo "Error: uuidgen no está instalado." >&2
  exit 1
}

: "${BASE_URL:=http://localhost:8080}"
: "${DB_URL:=postgresql://housedb:housedb@localhost:5432/housedb}"
: "${JWT_SECRET:=CHANGE_ME_NOW_32+chars_secret}"
: "${TOKEN_TTL_SECONDS:=3600}"

CURL_COMMON="curl --silent --show-error --header 'Accept: application/json'"

mask_sensitive_headers() {
  printf '%s' "$1" \
    | sed -E "s/(Authorization: Bearer )[^']+/\1***REDACTED***/g" \
    | sed -E "s/(Authorization: Basic )[^']+/\1***REDACTED***/g"
}

run_curl() {
  cmd="$1"
  safe_cmd=$(mask_sensitive_headers "$cmd")
  printf '>> curl: %s\n' "$safe_cmd" >&2
  eval "$cmd"
}

api_post_json_bearer() {
  token="$1"
  path="$2"
  run_curl "$CURL_COMMON -X POST -H 'Authorization: Bearer $token' -H 'Content-Type: application/json' --data-binary @- \"${BASE_URL%/}${path}\" --write-out '\\n%{http_code}'"
}

api_post_json() {
  path="$1"
  run_curl "$CURL_COMMON -X POST -H 'Content-Type: application/json' --data-binary @- \"${BASE_URL%/}${path}\" --write-out '\\n%{http_code}'"
}

split_response() {
  response="$1"
  http_code=$(printf '%s' "$response" | tail -n1)
  body=$(printf '%s' "$response" | sed '$d')
  printf '%s\n%s\n' "$http_code" "$body"
}

print_json() {
  printf '%s\n' "$1" | jq . || printf '%s\n' "$1"
}

psql_scalar() {
  sql="$1"
  psql "$DB_URL" -v ON_ERROR_STOP=1 -tA -c "$sql" | sed '/^[[:space:]]*$/d' | tail -n1
}

sql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

b64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

mint_app_token() {
  now_epoch=$(date +%s)
  exp_epoch=$((now_epoch + TOKEN_TTL_SECONDS))
  sub_id=$(uuidgen | tr '[:upper:]' '[:lower:]')
  client_id="${APP_CLIENT_ID:-script-test-client}"

  header='{"alg":"HS256","typ":"JWT"}'
  payload=$(printf '{"sub":"%s","iss":"%s","aud":"%s","iat":%s,"exp":%s,"roles":["ADMIN"],"token_type":"app","client_id":"%s"}' \
    "$sub_id" \
    "${JWT_ISS:-com.rafex.housedb}" \
    "${JWT_AUD:-housedb-backend}" \
    "$now_epoch" \
    "$exp_epoch" \
    "$client_id")

  header_b64=$(printf '%s' "$header" | b64url)
  payload_b64=$(printf '%s' "$payload" | b64url)
  signing_input="${header_b64}.${payload_b64}"
  signature_b64=$(printf '%s' "$signing_input" | openssl dgst -binary -sha256 -hmac "$JWT_SECRET" | b64url)

  printf '%s.%s\n' "$signing_input" "$signature_b64"
}
