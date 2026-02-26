BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'users_status') THEN
    CREATE TYPE users_status AS ENUM ('active', 'inactive', 'deleted', 'archived');
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'roles_status') THEN
    CREATE TYPE roles_status AS ENUM ('active', 'inactive', 'archived');
  END IF;
END
$$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'app_client_status') THEN
    CREATE TYPE app_client_status AS ENUM ('active', 'inactive', 'archived');
  END IF;
END
$$;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS salt BYTEA,
  ADD COLUMN IF NOT EXISTS iterations INT,
  ADD COLUMN IF NOT EXISTS status users_status NOT NULL DEFAULT 'active';

UPDATE users
SET salt = decode(md5(gen_random_uuid()::text), 'hex')
WHERE salt IS NULL;

UPDATE users
SET iterations = 120000
WHERE iterations IS NULL;

ALTER TABLE users
  ALTER COLUMN salt SET NOT NULL,
  ALTER COLUMN iterations SET NOT NULL;

CREATE TABLE IF NOT EXISTS roles (
  id BIGSERIAL PRIMARY KEY,
  role_id UUID NOT NULL DEFAULT gen_random_uuid(),
  name TEXT NOT NULL UNIQUE,
  description TEXT,
  status roles_status NOT NULL DEFAULT 'active',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_roles_role_id
ON roles(role_id);

CREATE TABLE IF NOT EXISTS user_roles (
  user_fk BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_fk BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_fk, role_fk)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user
ON user_roles(user_fk);

CREATE INDEX IF NOT EXISTS idx_user_roles_role
ON user_roles(role_fk);

CREATE TABLE IF NOT EXISTS app_clients (
  id BIGSERIAL PRIMARY KEY,
  app_client_id UUID NOT NULL DEFAULT gen_random_uuid(),
  client_id TEXT NOT NULL UNIQUE,
  name TEXT,
  secret_hash BYTEA NOT NULL,
  salt BYTEA NOT NULL,
  iterations INT NOT NULL,
  roles TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  status app_client_status NOT NULL DEFAULT 'active',
  last_used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_app_clients_app_client_id
ON app_clients(app_client_id);

CREATE INDEX IF NOT EXISTS idx_app_clients_status
ON app_clients(status);

CREATE INDEX IF NOT EXISTS idx_app_clients_roles
ON app_clients
USING GIN (roles);

INSERT INTO roles (role_id, name, description, status)
VALUES
    (gen_random_uuid(), 'ADMIN', 'Administrador del sistema', 'active'),
    (gen_random_uuid(), 'USER', 'Usuario estándar', 'active'),
    (gen_random_uuid(), 'READONLY', 'Usuario de solo lectura', 'active'),
    (gen_random_uuid(), 'AUDITOR', 'Usuario auditor', 'active')
ON CONFLICT (name) DO NOTHING;

COMMIT;
