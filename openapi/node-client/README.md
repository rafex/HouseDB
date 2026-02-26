# housedb-node-client

Proyecto Node.js para consumir la API HouseDB en dos modos:

- CLI (`src/index.js`)
- Web UI visual tipo Postman/Swagger (`src/server.js` + `public/`)

La especificación fuente está en `../housedb-openapi.yaml`.

## Requisitos

- Node.js 18+

## Instalación

```bash
cd /Users/rafex/repository/github/rafex/HouseDB/openapi/node-client
npm install
```

## Modo visual (recomendado)

```bash
npm run web
```

Abre: `http://localhost:3030`

Puedes cambiar el puerto con:

```bash
export HOUSEDB_CLIENT_PORT=3030
npm run web
```

## Modo CLI

```bash
npm run health
npm run start -- login --username demo --password secret
npm run start -- token --client-id app1 --client-secret supersecret
npm run start -- get-item --id UUID
npm run start -- create-item --house-location-leaf-id UUID --object-name "Linterna" --object-category camping --kiwi-metadata '{"source":"housedb"}' --housedb-metadata '{"purchaseDate":"2026-02-26"}'
npm run start -- search-items --q guantes
npm run start -- list-houses
npm run start -- list-house-ids
npm run start -- create-house --name "Casa CDMX"
npm run start -- list-house-members --house-id UUID
npm run start -- upsert-house-member --house-id UUID --user-id UUID --role guest --method PUT
npm run start -- create-house-location --house-id UUID --name "Closet principal"
```

Variables útiles:

```bash
export HOUSEDB_BASE_URL="http://localhost:8080"
export HOUSEDB_TOKEN="<bearer-opcional>"
```

Para endpoints protegidos, primero obtén un JWT con `login` o `token`, y luego exporta `HOUSEDB_TOKEN`.
