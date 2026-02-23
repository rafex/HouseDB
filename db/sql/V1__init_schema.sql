BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TYPE inventory_item_status AS ENUM (
  'active',
  'inactive',
  'lost',
  'archived'
);

CREATE TYPE house_member_role AS ENUM (
  'owner',
  'family',
  'guest'
);

CREATE TYPE location_kind AS ENUM (
  'house',
  'area',
  'room',
  'furniture',
  'shelf',
  'slot'
);

CREATE TYPE position_vertical AS ENUM (
  'top',
  'middle',
  'bottom'
);

CREATE TYPE position_horizontal AS ENUM (
  'left',
  'center',
  'right'
);

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID NOT NULL DEFAULT gen_random_uuid(),
  username TEXT NOT NULL UNIQUE,
  password_hash BYTEA,
  user_kiwi_id UUID UNIQUE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_users_user_id
ON users(user_id);

CREATE TABLE houses (
  id BIGSERIAL PRIMARY KEY,
  house_id UUID NOT NULL DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  street TEXT,
  number_ext TEXT,
  number_int TEXT,
  neighborhood TEXT,
  city TEXT,
  state TEXT,
  zip_code TEXT,
  country TEXT,
  latitude NUMERIC(10, 7),
  longitude NUMERIC(10, 7),
  geo_location GEOGRAPHY(Point, 4326),
  url_map TEXT,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_houses_latitude CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
  CONSTRAINT chk_houses_longitude CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180)),
  CONSTRAINT chk_houses_lat_lon_pair CHECK (
    (latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)
  )
);

CREATE UNIQUE INDEX idx_houses_house_id
ON houses(house_id);

CREATE INDEX idx_houses_geo_location
ON houses
USING gist (geo_location);

CREATE TABLE house_members (
  id BIGSERIAL PRIMARY KEY,
  house_member_id UUID NOT NULL DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  house_id UUID NOT NULL REFERENCES houses(house_id) ON DELETE CASCADE,
  role house_member_role NOT NULL DEFAULT 'owner',
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_house_members_user_house UNIQUE (user_id, house_id)
);

CREATE UNIQUE INDEX idx_house_members_house_member_id
ON house_members(house_member_id);

CREATE INDEX idx_house_members_house_id
ON house_members(house_id);

CREATE TABLE house_locations (
  id BIGSERIAL PRIMARY KEY,
  house_location_id UUID NOT NULL DEFAULT gen_random_uuid(),
  kiwi_location_id UUID UNIQUE,
  kiwi_parent_location_id UUID,
  house_id UUID NOT NULL REFERENCES houses(house_id) ON DELETE CASCADE,
  parent_house_location_id UUID REFERENCES house_locations(house_location_id) ON DELETE CASCADE,
  location_kind location_kind NOT NULL,
  name TEXT NOT NULL,
  path TEXT,
  level_depth INT NOT NULL DEFAULT 0,
  latitude NUMERIC(10, 7),
  longitude NUMERIC(10, 7),
  geo_location GEOGRAPHY(Point, 4326),
  position_vertical position_vertical,
  position_horizontal position_horizontal,
  reference_code TEXT,
  is_leaf BOOLEAN NOT NULL DEFAULT FALSE,
  notes TEXT,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_house_locations_latitude CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
  CONSTRAINT chk_house_locations_longitude CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180)),
  CONSTRAINT chk_house_locations_lat_lon_pair CHECK (
    (latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)
  ),
  CONSTRAINT chk_house_locations_reference_code_not_blank CHECK (reference_code IS NULL OR btrim(reference_code) <> ''),
  CONSTRAINT chk_house_locations_path_not_blank CHECK (path IS NULL OR btrim(path) <> '')
);

CREATE UNIQUE INDEX idx_house_locations_house_location_id
ON house_locations(house_location_id);

CREATE INDEX idx_house_locations_house_parent
ON house_locations(house_id, parent_house_location_id);

CREATE INDEX idx_house_locations_path_trgm
ON house_locations
USING gin (path gin_trgm_ops);

CREATE INDEX idx_house_locations_geo_location
ON house_locations
USING gist (geo_location);

CREATE INDEX idx_house_locations_kiwi_parent_location_id
ON house_locations(kiwi_parent_location_id);

CREATE UNIQUE INDEX uq_house_locations_sibling
ON house_locations (
  house_id,
  COALESCE(parent_house_location_id, '00000000-0000-0000-0000-000000000000'::UUID),
  lower(name),
  COALESCE(position_vertical::TEXT, ''),
  COALESCE(position_horizontal::TEXT, '')
);

CREATE TABLE objects (
  id BIGSERIAL PRIMARY KEY,
  object_id UUID NOT NULL DEFAULT gen_random_uuid(),
  object_kiwi_id UUID UNIQUE,
  name TEXT NOT NULL,
  description TEXT,
  bucket_image TEXT,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_objects_object_id
ON objects(object_id);

CREATE INDEX idx_objects_name_trgm
ON objects
USING gin (name gin_trgm_ops);

CREATE TABLE inventory_items (
  id BIGSERIAL PRIMARY KEY,
  inventory_item_id UUID NOT NULL DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  object_id UUID NOT NULL REFERENCES objects(object_id),
  nickname TEXT,
  serial_number TEXT,
  condition_status inventory_item_status NOT NULL DEFAULT 'active',
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_inventory_items_inventory_item_id
ON inventory_items(inventory_item_id);

CREATE INDEX idx_inventory_items_user_id
ON inventory_items(user_id);

CREATE INDEX idx_inventory_items_object_id
ON inventory_items(object_id);

CREATE UNIQUE INDEX uq_inventory_items_user_serial
ON inventory_items(user_id, serial_number)
WHERE serial_number IS NOT NULL;

CREATE TABLE item_current_location (
  id BIGSERIAL PRIMARY KEY,
  item_current_location_id UUID NOT NULL DEFAULT gen_random_uuid(),
  inventory_item_id UUID NOT NULL REFERENCES inventory_items(inventory_item_id) ON DELETE CASCADE,
  house_location_leaf_id UUID NOT NULL REFERENCES house_locations(house_location_id),
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_item_current_location_inventory_item UNIQUE (inventory_item_id)
);

CREATE UNIQUE INDEX idx_item_current_location_item_current_location_id
ON item_current_location(item_current_location_id);

CREATE INDEX idx_item_current_location_house_location_leaf_id
ON item_current_location(house_location_leaf_id);

CREATE TABLE item_movements (
  id BIGSERIAL PRIMARY KEY,
  item_movement_id UUID NOT NULL DEFAULT gen_random_uuid(),
  inventory_item_id UUID NOT NULL REFERENCES inventory_items(inventory_item_id) ON DELETE CASCADE,
  from_house_location_leaf_id UUID REFERENCES house_locations(house_location_id),
  to_house_location_leaf_id UUID NOT NULL REFERENCES house_locations(house_location_id),
  movement_reason TEXT NOT NULL DEFAULT 'manual_transfer',
  moved_by TEXT,
  moved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  notes TEXT,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_item_movements_non_empty_reason CHECK (btrim(movement_reason) <> ''),
  CONSTRAINT chk_item_movements_not_same_location CHECK (
    from_house_location_leaf_id IS NULL OR from_house_location_leaf_id <> to_house_location_leaf_id
  )
);

CREATE UNIQUE INDEX idx_item_movements_item_movement_id
ON item_movements(item_movement_id);

CREATE INDEX idx_item_movements_inventory_item_moved_at
ON item_movements(inventory_item_id, moved_at DESC);

CREATE INDEX idx_item_movements_from_house_location_leaf_id
ON item_movements(from_house_location_leaf_id);

CREATE INDEX idx_item_movements_to_house_location_leaf_id
ON item_movements(to_house_location_leaf_id);

CREATE TABLE favorites (
  id BIGSERIAL PRIMARY KEY,
  favorite_id UUID NOT NULL DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  inventory_item_id UUID NOT NULL REFERENCES inventory_items(inventory_item_id) ON DELETE CASCADE,
  note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_favorites_user_inventory UNIQUE (user_id, inventory_item_id)
);

CREATE UNIQUE INDEX idx_favorites_favorite_id
ON favorites(favorite_id);

CREATE INDEX idx_favorites_user_id
ON favorites(user_id);

CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_sync_geo_from_lat_lon()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
    NEW.geo_location := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::GEOGRAPHY;
  ELSIF NEW.latitude IS NULL AND NEW.longitude IS NULL THEN
    NEW.geo_location := NULL;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_house_locations_validate_and_fill()
RETURNS TRIGGER AS $$
DECLARE
  v_parent_house_id UUID;
  v_parent_depth INT;
  v_parent_path TEXT;
BEGIN
  IF NEW.parent_house_location_id IS NULL THEN
    NEW.level_depth := 0;
    IF NEW.path IS NULL OR btrim(NEW.path) = '' THEN
      NEW.path := NEW.name;
    END IF;
    RETURN NEW;
  END IF;

  SELECT house_id, level_depth, path
    INTO v_parent_house_id, v_parent_depth, v_parent_path
  FROM house_locations
  WHERE house_location_id = NEW.parent_house_location_id;

  IF v_parent_house_id IS NULL THEN
    RAISE EXCEPTION 'parent_house_location_id % does not exist', NEW.parent_house_location_id;
  END IF;

  IF v_parent_house_id <> NEW.house_id THEN
    RAISE EXCEPTION 'parent_house_location_id % belongs to a different house', NEW.parent_house_location_id;
  END IF;

  NEW.level_depth := v_parent_depth + 1;

  IF NEW.path IS NULL OR btrim(NEW.path) = '' THEN
    NEW.path := v_parent_path || ' > ' || NEW.name;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_assert_house_location_leaf(p_house_location_id UUID, p_field_name TEXT)
RETURNS VOID AS $$
DECLARE
  v_is_leaf BOOLEAN;
BEGIN
  SELECT is_leaf
    INTO v_is_leaf
  FROM house_locations
  WHERE house_location_id = p_house_location_id;

  IF v_is_leaf IS DISTINCT FROM TRUE THEN
    RAISE EXCEPTION '% (%) must reference a leaf house location (is_leaf = true)', p_field_name, p_house_location_id;
  END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_item_current_location_validate_leaf()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM fn_assert_house_location_leaf(NEW.house_location_leaf_id, 'house_location_leaf_id');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_item_movements_validate_leaf()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.from_house_location_leaf_id IS NOT NULL THEN
    PERFORM fn_assert_house_location_leaf(NEW.from_house_location_leaf_id, 'from_house_location_leaf_id');
  END IF;

  PERFORM fn_assert_house_location_leaf(NEW.to_house_location_leaf_id, 'to_house_location_leaf_id');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_set_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_houses_set_updated_at
BEFORE UPDATE ON houses
FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_houses_sync_geo_from_lat_lon
BEFORE INSERT OR UPDATE ON houses
FOR EACH ROW
EXECUTE FUNCTION trg_sync_geo_from_lat_lon();

CREATE TRIGGER trg_house_members_set_updated_at
BEFORE UPDATE ON house_members
FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_house_locations_set_updated_at
BEFORE UPDATE ON house_locations
FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_house_locations_sync_geo_from_lat_lon
BEFORE INSERT OR UPDATE ON house_locations
FOR EACH ROW
EXECUTE FUNCTION trg_sync_geo_from_lat_lon();

CREATE TRIGGER trg_house_locations_validate_and_fill
BEFORE INSERT OR UPDATE ON house_locations
FOR EACH ROW
EXECUTE FUNCTION trg_house_locations_validate_and_fill();

CREATE TRIGGER trg_objects_set_updated_at
BEFORE UPDATE ON objects
FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_inventory_items_set_updated_at
BEFORE UPDATE ON inventory_items
FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_item_current_location_set_updated_at
BEFORE UPDATE ON item_current_location
FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_item_current_location_validate_leaf
BEFORE INSERT OR UPDATE ON item_current_location
FOR EACH ROW
EXECUTE FUNCTION trg_item_current_location_validate_leaf();

CREATE TRIGGER trg_item_movements_set_updated_at
BEFORE UPDATE ON item_movements
FOR EACH ROW
EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_item_movements_validate_leaf
BEFORE INSERT OR UPDATE ON item_movements
FOR EACH ROW
EXECUTE FUNCTION trg_item_movements_validate_leaf();

COMMIT;
