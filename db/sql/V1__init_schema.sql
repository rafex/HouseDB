BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TYPE item_status AS ENUM (
  'active',
  'inactive',
  'lost',
  'archived'
);

CREATE TABLE categories (
  category_id UUID PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE locations (
  location_id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  parent_location_id UUID REFERENCES locations(location_id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE items (
  item_id UUID PRIMARY KEY,
  category_id UUID NOT NULL REFERENCES categories(category_id),
  location_id UUID REFERENCES locations(location_id),
  name TEXT NOT NULL,
  description TEXT,
  quantity INTEGER NOT NULL DEFAULT 1,
  status item_status NOT NULL DEFAULT 'active',
  tags TEXT[] NOT NULL DEFAULT '{}',
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_items_name ON items(name);
CREATE INDEX idx_items_name_trgm ON items USING gin (name gin_trgm_ops);
CREATE INDEX idx_items_category_id ON items(category_id);
CREATE INDEX idx_items_location_id ON items(location_id);

CREATE TABLE favorites (
  favorite_id UUID PRIMARY KEY,
  item_id UUID NOT NULL REFERENCES items(item_id) ON DELETE CASCADE,
  note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE item_events (
  event_id UUID PRIMARY KEY,
  item_id UUID NOT NULL REFERENCES items(item_id) ON DELETE CASCADE,
  event_type TEXT NOT NULL,
  event_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_item_events_item_id ON item_events(item_id);
CREATE INDEX idx_item_events_created_at ON item_events(created_at);

COMMIT;
