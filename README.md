# HouseDB

## Estructura
- `frontend`: frontend Vue/Vite para la consola HouseDB
- `backend/java`: backend Java multi-módulo
- `db`: migraciones y utilidades de base de datos
- `helm/housedb-backend`: chart base para despliegue
- `script`: scripts operativos

## Comandos
- `cd frontend && npm install && npm run dev`: levanta frontend en desarrollo
- `cd frontend && npm run build`: compila frontend
- `make frontend-install`: instala dependencias del frontend
- `make frontend-dev`: levanta el frontend local
- `make frontend-dev-cloud`: levanta el frontend local usando el backend `https://housedb.v1.rafex.cloud`
- `make build`: compila backend
- `make test`: ejecuta tests backend
- `make db-up`: levanta PostgreSQL con Podman
- `make db-migrate`: ejecuta migraciones Flyway

## Variables de entorno Kiwi (HouseDB backend)
- `KIWI_API_BASE_URL`: URL base de Kiwi (ej. `http://localhost:8080`)
- `KIWI_APP_CLIENT_ID`: client id para pedir token por `client_credentials` (requerido)
- `KIWI_APP_CLIENT_SECRET`: client secret para pedir token por `client_credentials` (requerido)
- `KIWI_BOOTSTRAP_APP_CLIENT`: `true|false` para crear app client automáticamente al iniciar HouseDB
- `KIWI_ADMIN_USERNAME`: usuario admin de Kiwi (requerido si bootstrap está en `true`)
- `KIWI_ADMIN_PASSWORD`: password admin de Kiwi (requerido si bootstrap está en `true`)
- `KIWI_APP_CLIENT_NAME`: nombre del app client a crear (default: `HouseDB`)
- `KIWI_APP_CLIENT_ROLES`: roles CSV del app client (default: `ADMIN`)
