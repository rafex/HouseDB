-- ============================
-- Usuario de aplicación HouseDB
-- ============================

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_roles WHERE rolname = 'housedb_app'
  ) THEN
    CREATE ROLE housedb_app
      LOGIN
      PASSWORD 'CHANGE_ME_houseDB_app_password'
      NOSUPERUSER
      NOCREATEDB
      NOCREATEROLE
      NOINHERIT;
  END IF;
END
$$;

-- ============================
-- Permisos básicos
-- ============================

-- Conectar a la BD
GRANT CONNECT ON DATABASE housedb TO housedb_app;

-- Usar esquema public
GRANT USAGE ON SCHEMA public TO housedb_app;

-- ============================
-- Tablas (DML)
-- ============================

GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA public
TO housedb_app;

-- ============================
-- Funciones API (ejecución)
-- ============================

GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO housedb_app;

-- ============================
-- Secuencias (si existen)
-- ============================

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO housedb_app;

-- ============================
-- Default privileges (opcional)
-- Para futuras tablas/funciones
-- ============================

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO housedb_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO housedb_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT EXECUTE ON FUNCTIONS TO housedb_app;
