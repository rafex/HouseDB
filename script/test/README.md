# Script de prueba E2E (HouseDB)

Scripts de prueba manual para validar el flujo principal de HouseDB.

## Scripts modulares

- `create_user.sh`: crea usuario por API securizada (`POST /users`).
- `create_house.sh`: crea casa vía API (`/houses`).
- `create_house_location_db.sh`: crea locación hoja en PostgreSQL.
- `create_object_db.sh`: crea objeto en PostgreSQL.
- `create_inventory_item.sh`: crea item de inventario vía API (`/items`).
- `house_flow.sh`: orquestador E2E que llama a los scripts anteriores.

## Requisitos

- `jq`
- `curl`
- `psql`
- `openssl`
- `uuidgen`
- backend HouseDB corriendo
- DB con migraciones aplicadas

## Uso rápido

```bash
cd /Users/rafex/repository/github/rafex/HouseDB
chmod +x script/test/*.sh

# Flujo completo
script/test/house_flow.sh \
  -u http://localhost:8080 \
  -d postgresql://housedb:housedb@localhost:5432/housedb \
  -s 'CHANGE_ME_NOW_32+chars_secret'
```

Ejemplo modular:

```bash
USER_JSON=$(script/test/create_user.sh -u http://localhost:8080 -s 'CHANGE_ME_NOW_32+chars_secret' -U demo_user_01 -P secret123)
USER_ID=$(printf '%s' "$USER_JSON" | jq -r '.userId')

HOUSE_JSON=$(script/test/create_house.sh -u http://localhost:8080 -s 'CHANGE_ME_NOW_32+chars_secret' -o "$USER_ID" -n "Casa Demo")
HOUSE_ID=$(printf '%s' "$HOUSE_JSON" | jq -r '.houseId')

LOC_JSON=$(script/test/create_house_location_db.sh -d postgresql://housedb:housedb@localhost:5432/housedb -H "$HOUSE_ID" -L "Closet")
LOC_ID=$(printf '%s' "$LOC_JSON" | jq -r '.houseLocationId')

OBJ_JSON=$(script/test/create_object_db.sh -d postgresql://housedb:housedb@localhost:5432/housedb -O "Linterna" -C "camping")
OBJ_ID=$(printf '%s' "$OBJ_JSON" | jq -r '.objectId')

script/test/create_inventory_item.sh -u http://localhost:8080 -s 'CHANGE_ME_NOW_32+chars_secret' -U "$USER_ID" -O "$OBJ_ID" -L "$LOC_ID" -N "linterna negra"
```

Si no pasas `-s`, el script usa por defecto `CHANGE_ME_NOW_32+chars_secret`.
