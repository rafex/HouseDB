# Frontend HouseDB

Frontend base para HouseDB con `Vue 3`, `Vite`, `Pug`, `Sass` y `Vue Router`.

## Scripts

- `npm install`
- `npm run dev`
- `npm run build`
- `npm run preview`
- `make frontend-dev` desde la raiz del repo para arrancar el frontend contra `http://localhost:8080`
- `make frontend-dev-cloud` desde la raiz del repo para arrancar el frontend contra `https://housedb.v1.rafex.cloud`

## Variables

- `VITE_API_BASE_URL`: URL base del backend a consumir, por defecto `http://localhost:8080`

## Estructura

- `src/layouts`: layout principal tipo admin
- `src/views`: dashboard, casas, inventario y usuarios/API
- `src/components`: componentes reutilizables
- `src/lib`: cliente HTTP para HouseDB
- `src/stores`: sesion JWT persistida
- `src/router`: configuracion de `vue-router`

## Nota visual

La interfaz toma como referencia un dashboard administrativo con sidebar vertical, topbar y tarjetas de indicadores, adaptado a la identidad de HouseDB.
