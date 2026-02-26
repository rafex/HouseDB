-- ==========================================
-- Bootstrap de permisos para Flyway migrator
-- ==========================================

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_roles WHERE rolname = 'flyway_migrator'
  ) THEN
    CREATE ROLE flyway_migrator
      LOGIN
      PASSWORD 'CHANGE_ME_flyway_migrator_password'
      NOSUPERUSER
      NOCREATEDB
      NOCREATEROLE
      INHERIT;
  END IF;
END
$$;

-- Asegura conexión a la base objetivo
GRANT CONNECT ON DATABASE housedb TO flyway_migrator;

-- Esquema de trabajo
GRANT USAGE, CREATE ON SCHEMA public TO flyway_migrator;

-- Objetos existentes
GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA public
TO flyway_migrator;

GRANT USAGE, SELECT, UPDATE
ON ALL SEQUENCES IN SCHEMA public
TO flyway_migrator;

GRANT EXECUTE
ON ALL FUNCTIONS IN SCHEMA public
TO flyway_migrator;

GRANT EXECUTE
ON ALL ROUTINES IN SCHEMA public
TO flyway_migrator;

-- Objetos futuros
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO flyway_migrator;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO flyway_migrator;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT EXECUTE ON FUNCTIONS TO flyway_migrator;
