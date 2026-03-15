# AGENTS.md

## Objetivo del proyecto

HouseDB es una consola para recordar que objetos existen dentro de una casa, en que ubicacion estan y que informacion ayuda a reencontrarlos despues.

El modelo mental correcto es:

- El centro del producto son los objetos.
- Las casas y locaciones son contexto fisico.
- La metadata HouseDB ayuda a encontrar objetos dentro de casa.
- La metadata Kiwi ayuda a interoperar con Kiwi y otros sistemas.

Cuando se diseñe o modifique el frontend, evitar una UI "por endpoint". Los flujos principales deben ser:

- autenticacion
- buscar objetos
- registrar objeto
- ver detalle del objeto
- mover objeto
- administrar casas y locaciones como soporte
- administrar metadata reusable como soporte

## Estructura del repo

- `frontend`: SPA en Vue 3 + Vite + Pug + Sass + Vue Router + PWA
- `backend/java`: backend Java multi-modulo
- `db/sql`: migraciones SQL y funciones PostgreSQL
- `openapi/housedb-openapi.yaml`: contrato fuente de la API
- `helm/housedb-backend` y `helm/housedb-frontend`: charts de despliegue
- `.github/workflows`: build, publish y deploy

## Regla operativa importante

Antes de implementar algo nuevo:

1. Revisar `openapi/housedb-openapi.yaml`.
2. Revisar handlers Java del backend si el endpoint es nuevo o ambiguo.
3. Verificar si el frontend ya consume algo parecido en `frontend/src/lib/api.js`.
4. Implementar la UX sin romper flujos existentes.

No asumir que el OpenAPI y el frontend estan alineados. Confirmarlo cada vez.

## Backend: donde mirar

Documentacion tecnica:

- `backend/java/docs/README.md`
- `backend/java/docs/arquitectura.md`
- `backend/java/docs/operacion.md`

Handlers HTTP:

- `backend/java/housedb-parent/housedb-transport-jetty/src/main/java/com/rafex/housedb/handlers`

Recursos importantes:

- `handlers/AuthRouterHandler.java`
- `handlers/RefreshTokenHandler.java`
- `handlers/items/*`
- `handlers/houses/*`
- `handlers/metadata/*`

## Frontend: donde mirar

Archivos base:

- `frontend/src/lib/api.js`: cliente HTTP central
- `frontend/src/router/index.js`: rutas, auth guard y SEO basico
- `frontend/src/stores/session.js`: sesion JWT y renovacion
- `frontend/src/layouts/AdminLayout.vue`: shell principal

Vistas principales:

- `frontend/src/views/DashboardView.vue`
- `frontend/src/views/ObjectListView.vue`
- `frontend/src/views/AddObjectView.vue`
- `frontend/src/views/HousesListView.vue`
- `frontend/src/views/LocationsListView.vue`
- `frontend/src/views/UsersView.vue`

Componentes reutilizables:

- `frontend/src/components/PaginationControls.vue`
- `frontend/src/components/MetadataFieldEditor.vue`
- `frontend/src/components/SessionRenewalModal.vue`

## Estado funcional que se debe preservar

### Autenticacion

- La app pide login antes de mostrar informacion privada.
- Se maneja JWT con renovacion previa a expiracion.
- Si la sesion expira, aparece modal de reautenticacion.

### Objetos

- Buscar objetos y abrir detalle.
- Ver timeline / historial.
- Mover objeto a otra locacion.
- Registrar objeto con metadata amigable.

### Casas y locaciones

- Listado de casas.
- Listado de locaciones por casa usando `GET /houses/{houseId}/locations`.
- Alta de casas y alta de locaciones.

### Metadata reusable

Ya existen endpoints para catalogos y plantillas:

- `GET /metadata-catalogs`
- `POST /metadata-catalogs`
- `GET /metadata-templates`
- `POST /metadata-templates`

Uso esperado:

- Los catalogos guardan claves reutilizables y sugerencias.
- Las plantillas guardan formularios reutilizables en JSON.
- El alta de objeto debe seguir usando ambos para construir una UX amigable.

## Convenciones de UX ya acordadas

- No mostrar JSON crudo a usuarios normales si se puede evitar.
- HouseDB metadata se presenta como "datos para encontrarlo".
- Kiwi metadata se presenta como "datos tecnicos de integracion" y debe ir subordinada o colapsada.
- La UI debe ser object-first, no house-first ni endpoint-first.

## Paginacion

Las tablas del frontend usan `PaginationControls.vue`.

Regla:

- Toda lista paginada debe permitir cambiar `limit`.
- Al cambiar `limit`, reiniciar `offset` a `0`.
- No asumir que `count` sea total global; varios endpoints reportan conteo de pagina.
- Usar `paginationFromResponse(...)` desde `frontend/src/lib/api.js`.

## SEO, indexacion y PWA

Estado actual:

- Hay PWA via `vite-plugin-pwa`.
- `frontend/vite.config.js` genera `robots.txt` y `sitemap.xml`.
- `frontend/src/router/index.js` actualiza `title`, `description`, `robots`, OG y canonical.

Regla:

- Tratar como indexables solo rutas publicas utiles.
- Mantener rutas autenticadas en `noindex,nofollow`.

## Deploy y entorno

Frontend:

- En local, Vite proxya `/api` a `http://localhost:8080`.
- En build productivo, usar `VITE_API_BASE_URL`.
- Para indexacion/canonical usar `VITE_SITE_URL`.

Backend local:

- normalmente en `http://localhost:8080`

URL cloud conocida:

- `https://housedb.v1.rafex.cloud`

## Docker, Helm y seguridad

Frontend:

- usar `nginxinc/nginx-unprivileged`
- escuchar en `8080`
- mantener despliegue non-root

Si se toca Helm del frontend:

- conservar `runAsNonRoot`
- conservar `readOnlyRootFilesystem`
- montar `emptyDir` en rutas que Nginx necesita escribir
- probes deben apuntar al puerto correcto del contenedor

## Base de datos y migraciones

Migraciones recientes relevantes:

- `V15__auth_refresh_tokens.sql`
- `V16__metadata_templates.sql`
- `V17__metadata_create_functions.sql`

Si falla autenticacion con `database error`, revisar que la base tenga aplicadas las migraciones recientes.

## Comandos utiles

Raiz:

- `make frontend-dev`
- `make frontend-dev-cloud`
- `make build`
- `make test`
- `make db-migrate`

Frontend:

- `cd frontend && npm install`
- `cd frontend && npm run dev`
- `cd frontend && npm run build`

Backend:

- `cd backend/java/housedb-parent && ./mvnw -q -pl housedb-transport-jetty -am -DskipTests package`

## Checklist para futuros agentes

Antes de cerrar cambios:

1. Revisar `git status`.
2. Compilar el frontend con `npm run build` si hubo cambios web.
3. Compilar backend si hubo cambios Java.
4. Si se tocaron endpoints, verificar OpenAPI.
5. Si se tocaron tablas o funciones, verificar migraciones SQL.
6. Si se tocó deploy, verificar workflows y charts.

## Principio de diseño

Cada cambio debe ayudar a responder mejor esta pregunta del usuario final:

"Si tengo ese objeto, donde esta y como lo vuelvo a encontrar rapido?"
