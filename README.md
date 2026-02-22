# HouseDB

## Estructura
- `backend/java`: backend Java multi-módulo
- `db`: migraciones y utilidades de base de datos
- `helm/housedb-backend`: chart base para despliegue
- `script`: scripts operativos

## Comandos
- `make build`: compila backend
- `make test`: ejecuta tests backend
- `make db-up`: levanta PostgreSQL con Podman
- `make db-migrate`: ejecuta migraciones Flyway
