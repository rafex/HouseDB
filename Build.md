# HouseDB

Sera un proyecto en cual tiene como objetivo ayudar a las personas/usuarios a encontrar en su hogar los objetos que necesitan, para esto se tendrá una base de datos con los objetos y sus respectivas categorías, el usuario podrá buscar por categoría o por nombre del objeto, también se tendrá una sección de favoritos donde el usuario podrá guardar los objetos que le interesen. El plus de esto es también ayudarles a recordar que tienen los objetos necesarios para cierta actividad, ejemplo: una persona dirá ire de día campo y el sistema le mencionara dónde esta su mochila, casa de campaña, etc. Esto se apoyara de servicios de LLM para generar las respuestas a las preguntas de los usuarios, también se tendrá una sección de favoritos donde el usuario podrá guardar los objetos que le interesen.

## Estructura del repositorio
- backend/java
- db
- db/sql
- db/make
- helm/housedb-backend
- LICENSE
- Makefile
- README.md
- script

## Stack
- Java 21
- PostgreSQL
- Flyway
- Podman
- Maven (Maven Wrapper)

### Backend Java

El backend será en Java sin frameworks, utilizando la biblioteca estándar de java puedes tener un ejemplo de este otro repositorio mio: https://github.com/rafex/kiwi/tree/main/backend/java
Será solo una API REST separando las cosas en capas:
- housedb-bootstrap
- housedb-common
- housedb-core
- housedb-infra-postgres
- housedb-ports
- housedb-tools
- housedb-transport-grpc
- housedb-transport-jetty
- housedb-transport-rabbitmq
- Makefile (para compilar, testear, etc)


### Database

Habrá que crear una base de datos que estará montada en PostgreSQL y administrada por Flyway, utiliza de ejemplo lo que hice en otro proyecto: https://github.com/rafex/kiwi/tree/main/db
Recuerda que usaremos el sistema Kiwi donde se almacenaran los objetos y aquí crear las tablas necesarias para el objetivo de HouseDB

```mermaid
erDiagram
    USERS {
        BIGSERIAL id PK
        UUID user_id UK "INDEX"
        VARCHAR username
        VARCHAR password
        UUID user_kiwi_id
        INT enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    HOUSES {
        BIGSERIAL id PK
        UUID house_id UK "INDEX"
        VARCHAR name
        VARCHAR description
        VARCHAR street
        VARCHAR number_ext
        VARCHAR number_int
        VARCHAR neighborhood
        VARCHAR city
        VARCHAR state
        VARCHAR zip_code
        VARCHAR country
        VARCHAR latitude
        VARCHAR longitude
        GEOGRAPHY location
        INT enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    HOUSE_MEMBERS {
        BIGSERIAL id PK
        UUID house_member_id UK "INDEX"
        UUID user_id FK "-> USERS.user_id"
        UUID house_id FK "-> HOUSES.house_id"
        VARCHAR role
        INT enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    HOUSE_LOCATIONS {
        BIGSERIAL id PK
        UUID house_location_id UK "INDEX"
        UUID kiwi_location_id "UK, FK logical -> kiwi.locations.location_id"
        UUID kiwi_parent_location_id "FK logical -> kiwi.locations.location_id"
        UUID house_id FK "-> HOUSES.house_id"
        UUID parent_house_location_id FK "SELF"
        VARCHAR location_kind
        VARCHAR name
        VARCHAR path
        INT level_depth
        VARCHAR position_vertical
        VARCHAR position_horizontal
        VARCHAR reference_code
        INT is_leaf
        VARCHAR notes
        INT enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    OBJECTS {
        BIGSERIAL id PK
        UUID object_id UK "INDEX"
        TEXT name
        VARCHAR description
        VARCHAR bucket_image
        UUID object_kiwi_id
        INT enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    INVENTORY_ITEMS {
        BIGSERIAL id PK
        UUID inventory_item_id UK "INDEX"
        UUID user_id FK "-> USERS.user_id"
        UUID object_id FK "-> OBJECTS.object_id"
        VARCHAR nickname
        VARCHAR serial_number
        VARCHAR condition_status
        INT enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    ITEM_CURRENT_LOCATION {
        BIGSERIAL id PK
        UUID item_current_location_id UK "INDEX"
        UUID inventory_item_id FK "-> INVENTORY_ITEMS.inventory_item_id"
        UUID house_location_leaf_id FK "-> HOUSE_LOCATIONS.house_location_id"
        TIMESTAMP assigned_at
        INT is_current
        INT enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    ITEM_MOVEMENTS {
        BIGSERIAL id PK
        UUID item_movement_id UK "INDEX"
        UUID inventory_item_id FK "-> INVENTORY_ITEMS.inventory_item_id"
        UUID from_house_location_leaf_id FK "-> HOUSE_LOCATIONS.house_location_id"
        UUID to_house_location_leaf_id FK "-> HOUSE_LOCATIONS.house_location_id"
        VARCHAR movement_reason
        VARCHAR moved_by
        TIMESTAMP moved_at
        TEXT notes
        INT enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    USERS ||--o{ HOUSE_MEMBERS : belongs_to
    HOUSES ||--o{ HOUSE_MEMBERS : has_members
    HOUSES ||--o{ HOUSE_LOCATIONS : has
    HOUSE_LOCATIONS ||--o{ HOUSE_LOCATIONS : parent_of

    OBJECTS ||--o{ INVENTORY_ITEMS : catalog_of
    USERS ||--o{ INVENTORY_ITEMS : owns

    INVENTORY_ITEMS ||--o| ITEM_CURRENT_LOCATION : current_position
    HOUSE_LOCATIONS ||--o{ ITEM_CURRENT_LOCATION : exact_point

    INVENTORY_ITEMS ||--o{ ITEM_MOVEMENTS : moved
    HOUSE_LOCATIONS ||--o{ ITEM_MOVEMENTS : from
    HOUSE_LOCATIONS ||--o{ ITEM_MOVEMENTS : to

```

Notas operativas para migraciones SQL:
- Mantener `UUID` de negocio (`user_id`, `house_id`, `house_location_id`, `object_id`, `inventory_item_id`) y `id BIGSERIAL` como PK técnica.
- En `HOUSE_MEMBERS`, usar `UNIQUE (user_id, house_id)` para evitar membresías duplicadas.
- `HOUSE_LOCATIONS` mapea 1:1 con `kiwi.locations`:
  - `HOUSE_LOCATIONS.kiwi_location_id` <-> `kiwi.locations.location_id` (UUID, único).
  - `HOUSE_LOCATIONS.kiwi_parent_location_id` <-> padre en Kiwi por UUID para sincronizar árbol sin depender del `id BIGSERIAL` interno de Kiwi.
- Kiwi es la fuente de verdad de locaciones; HouseDB recubre metadata operativa (`path`, posiciones, notas) para resolver búsquedas del caso de uso.
- `HOUSE_LOCATIONS` debe manejar jerarquía real: `casa -> zona -> cuarto -> mueble -> estante/slot`.
- Usar `location_kind` controlado por catálogo/enum (ejemplo: `HOUSE`, `AREA`, `ROOM`, `FURNITURE`, `SHELF`, `SLOT`).
- Guardar ruta legible en `path` para búsquedas rápidas y UX (ejemplo: `Casa CDMX > Cuarto principal > Estante > Superior derecho`).
- Para granularidad física, usar `position_vertical` (`SUPERIOR|MEDIO|INFERIOR`) y `position_horizontal` (`IZQUIERDA|CENTRO|DERECHA`) en nodos hoja.
- En `HOUSE_LOCATIONS`, definir `UNIQUE (house_id, parent_house_location_id, name, position_vertical, position_horizontal)` para evitar duplicados ambiguos.
- Si una locación solo existe en Kiwi, HouseDB puede crear un nodo espejo con `house_id`/`path` derivados para no perder trazabilidad de movimientos.
- En `INVENTORY_ITEMS`, este modelo guarda unidades reales de inventario (ejemplo: "guantes rojos" y "guantes verdes" son dos filas si se quieren rastrear por separado).
- En `ITEM_CURRENT_LOCATION`, usar `UNIQUE (inventory_item_id)` para garantizar una sola ubicación actual por objeto, y validar que la FK apunte a nodo hoja (`is_leaf = 1`).
- En `ITEM_MOVEMENTS`, registrar cada traslado con `from_house_location_leaf_id`, `to_house_location_leaf_id` y `moved_at` para histórico completo.
- Indexar `ITEM_MOVEMENTS (inventory_item_id, moved_at DESC)` y `ITEM_CURRENT_LOCATION (house_location_leaf_id)` para búsquedas rápidas de "dónde está X" y "qué hay en Y".
- Mapeo recomendado de IDs Kiwi en HouseDB:
  - `USERS.user_kiwi_id` <-> `kiwi.users.user_id`
  - `OBJECTS.object_kiwi_id` <-> `kiwi.objects.object_id`
  - `HOUSE_LOCATIONS.kiwi_location_id` <-> `kiwi.locations.location_id`


### Deploy

Usaremos Dockerfile para crear la imagen necesaria y se desplegara en mi k3s con helm revisa los ejemplos de este otro repositorio mio: https://github.com/rafex/kiwi/tree/main/helm/kiwi-backend y https://github.com/rafex/kiwi/tree/main/backend/java
