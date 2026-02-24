BEGIN;

CREATE OR REPLACE FUNCTION api_search_inventory_items(
  p_user_id UUID,
  p_text TEXT DEFAULT NULL,
  p_house_id UUID DEFAULT NULL,
  p_house_location_leaf_id UUID DEFAULT NULL,
  p_limit INT DEFAULT 50
)
RETURNS TABLE (
  inventory_item_id UUID,
  object_id UUID,
  object_kiwi_id UUID,
  object_name TEXT,
  object_description TEXT,
  nickname TEXT,
  house_id UUID,
  house_name TEXT,
  house_location_leaf_id UUID,
  house_location_path TEXT,
  rank REAL
) AS $$
BEGIN
  IF p_limit IS NULL OR p_limit < 1 THEN
    RAISE EXCEPTION 'p_limit must be >= 1';
  END IF;

  RETURN QUERY
  SELECT
    ii.inventory_item_id,
    o.object_id,
    o.object_kiwi_id,
    o.name,
    o.description,
    ii.nickname,
    h.house_id,
    h.name,
    hl.house_location_id,
    hl.path,
    CASE
      WHEN p_text IS NULL OR btrim(p_text) = '' THEN 1.0::REAL
      ELSE GREATEST(
        similarity(o.name, p_text),
        similarity(COALESCE(ii.nickname, ''), p_text),
        similarity(COALESCE(hl.path, ''), p_text)
      )
    END::REAL AS rank
  FROM inventory_items ii
  INNER JOIN objects o
    ON o.object_id = ii.object_id
  INNER JOIN item_current_location icl
    ON icl.inventory_item_id = ii.inventory_item_id
   AND icl.is_current = TRUE
   AND icl.enabled = TRUE
  INNER JOIN house_locations hl
    ON hl.house_location_id = icl.house_location_leaf_id
   AND hl.enabled = TRUE
  INNER JOIN houses h
    ON h.house_id = hl.house_id
   AND h.enabled = TRUE
  WHERE ii.user_id = p_user_id
    AND ii.enabled = TRUE
    AND o.enabled = TRUE
    AND (
      p_text IS NULL
      OR btrim(p_text) = ''
      OR o.name ILIKE '%' || p_text || '%'
      OR COALESCE(o.description, '') ILIKE '%' || p_text || '%'
      OR COALESCE(ii.nickname, '') ILIKE '%' || p_text || '%'
      OR COALESCE(hl.path, '') ILIKE '%' || p_text || '%'
      OR o.name % p_text
    )
    AND (p_house_id IS NULL OR h.house_id = p_house_id)
    AND (p_house_location_leaf_id IS NULL OR hl.house_location_id = p_house_location_leaf_id)
  ORDER BY rank DESC, ii.updated_at DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_move_inventory_item(
  p_inventory_item_id UUID,
  p_to_house_location_leaf_id UUID,
  p_moved_by TEXT DEFAULT NULL,
  p_movement_reason TEXT DEFAULT 'manual_transfer',
  p_notes TEXT DEFAULT NULL,
  p_moved_at TIMESTAMPTZ DEFAULT now()
)
RETURNS TABLE (
  item_movement_id UUID,
  inventory_item_id UUID,
  from_house_location_leaf_id UUID,
  to_house_location_leaf_id UUID,
  moved_at TIMESTAMPTZ
) AS $$
DECLARE
  v_current_location UUID;
  v_movement_id UUID;
  v_effective_moved_at TIMESTAMPTZ;
BEGIN
  IF p_inventory_item_id IS NULL THEN
    RAISE EXCEPTION 'p_inventory_item_id is required';
  END IF;

  IF p_to_house_location_leaf_id IS NULL THEN
    RAISE EXCEPTION 'p_to_house_location_leaf_id is required';
  END IF;

  v_effective_moved_at := COALESCE(p_moved_at, now());

  -- Serialize moves per inventory item to keep from_location deterministic.
  PERFORM pg_advisory_xact_lock(hashtextextended(p_inventory_item_id::TEXT, 0));

  SELECT icl.house_location_leaf_id
    INTO v_current_location
  FROM item_current_location icl
  WHERE icl.inventory_item_id = p_inventory_item_id
    AND icl.is_current = TRUE
    AND icl.enabled = TRUE
  FOR UPDATE;

  INSERT INTO item_current_location (
    inventory_item_id,
    house_location_leaf_id,
    assigned_at,
    is_current,
    enabled
  ) VALUES (
    p_inventory_item_id,
    p_to_house_location_leaf_id,
    v_effective_moved_at,
    TRUE,
    TRUE
  )
  ON CONFLICT (inventory_item_id)
  DO UPDATE SET
    house_location_leaf_id = EXCLUDED.house_location_leaf_id,
    assigned_at = EXCLUDED.assigned_at,
    is_current = TRUE,
    enabled = TRUE;

  INSERT INTO item_movements (
    inventory_item_id,
    from_house_location_leaf_id,
    to_house_location_leaf_id,
    movement_reason,
    moved_by,
    moved_at,
    notes,
    enabled
  ) VALUES (
    p_inventory_item_id,
    v_current_location,
    p_to_house_location_leaf_id,
    COALESCE(NULLIF(btrim(p_movement_reason), ''), 'manual_transfer'),
    p_moved_by,
    v_effective_moved_at,
    p_notes,
    TRUE
  )
  RETURNING item_movements.item_movement_id
    INTO v_movement_id;

  RETURN QUERY
  SELECT
    v_movement_id,
    p_inventory_item_id,
    v_current_location,
    p_to_house_location_leaf_id,
    v_effective_moved_at;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_create_inventory_item(
  p_user_id UUID,
  p_object_id UUID,
  p_nickname TEXT DEFAULT NULL,
  p_serial_number TEXT DEFAULT NULL,
  p_condition_status inventory_item_status DEFAULT 'active',
  p_house_location_leaf_id UUID DEFAULT NULL,
  p_moved_by TEXT DEFAULT NULL,
  p_notes TEXT DEFAULT NULL
)
RETURNS TABLE (
  inventory_item_id UUID,
  item_movement_id UUID
) AS $$
DECLARE
  v_inventory_item_id UUID;
  v_item_movement_id UUID;
BEGIN
  IF p_user_id IS NULL OR p_object_id IS NULL THEN
    RAISE EXCEPTION 'p_user_id and p_object_id are required';
  END IF;

  INSERT INTO inventory_items (
    user_id,
    object_id,
    nickname,
    serial_number,
    condition_status,
    enabled
  ) VALUES (
    p_user_id,
    p_object_id,
    NULLIF(btrim(p_nickname), ''),
    NULLIF(btrim(p_serial_number), ''),
    COALESCE(p_condition_status, 'active'),
    TRUE
  )
  RETURNING inventory_items.inventory_item_id INTO v_inventory_item_id;

  IF p_house_location_leaf_id IS NOT NULL THEN
    SELECT m.item_movement_id
      INTO v_item_movement_id
    FROM api_move_inventory_item(
      v_inventory_item_id,
      p_house_location_leaf_id,
      p_moved_by,
      'initial_assignment',
      p_notes,
      now()
    ) AS m;
  END IF;

  RETURN QUERY
  SELECT v_inventory_item_id, v_item_movement_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_upsert_house_location_from_kiwi(
  p_house_id UUID,
  p_kiwi_location_id UUID,
  p_kiwi_parent_location_id UUID DEFAULT NULL,
  p_parent_house_location_id UUID DEFAULT NULL,
  p_location_kind location_kind DEFAULT 'slot',
  p_name TEXT DEFAULT NULL,
  p_is_leaf BOOLEAN DEFAULT FALSE,
  p_path TEXT DEFAULT NULL,
  p_reference_code TEXT DEFAULT NULL,
  p_notes TEXT DEFAULT NULL,
  p_latitude NUMERIC DEFAULT NULL,
  p_longitude NUMERIC DEFAULT NULL,
  p_enabled BOOLEAN DEFAULT TRUE
)
RETURNS UUID AS $$
DECLARE
  v_house_location_id UUID;
BEGIN
  IF p_house_id IS NULL OR p_kiwi_location_id IS NULL THEN
    RAISE EXCEPTION 'p_house_id and p_kiwi_location_id are required';
  END IF;

  INSERT INTO house_locations (
    kiwi_location_id,
    kiwi_parent_location_id,
    house_id,
    parent_house_location_id,
    location_kind,
    name,
    path,
    latitude,
    longitude,
    reference_code,
    is_leaf,
    notes,
    enabled
  ) VALUES (
    p_kiwi_location_id,
    p_kiwi_parent_location_id,
    p_house_id,
    p_parent_house_location_id,
    COALESCE(p_location_kind, 'slot'),
    COALESCE(NULLIF(btrim(p_name), ''), 'unnamed-location'),
    NULLIF(btrim(p_path), ''),
    p_latitude,
    p_longitude,
    NULLIF(btrim(p_reference_code), ''),
    COALESCE(p_is_leaf, FALSE),
    NULLIF(btrim(p_notes), ''),
    COALESCE(p_enabled, TRUE)
  )
  ON CONFLICT (kiwi_location_id)
  DO UPDATE SET
    kiwi_parent_location_id = EXCLUDED.kiwi_parent_location_id,
    house_id = EXCLUDED.house_id,
    parent_house_location_id = EXCLUDED.parent_house_location_id,
    location_kind = EXCLUDED.location_kind,
    name = EXCLUDED.name,
    path = EXCLUDED.path,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    reference_code = EXCLUDED.reference_code,
    is_leaf = EXCLUDED.is_leaf,
    notes = EXCLUDED.notes,
    enabled = EXCLUDED.enabled
  RETURNING house_locations.house_location_id INTO v_house_location_id;

  RETURN v_house_location_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_list_inventory_by_location(
  p_user_id UUID,
  p_house_id UUID DEFAULT NULL,
  p_house_location_id UUID DEFAULT NULL,
  p_include_descendants BOOLEAN DEFAULT TRUE,
  p_limit INT DEFAULT 200
)
RETURNS TABLE (
  inventory_item_id UUID,
  object_id UUID,
  object_name TEXT,
  nickname TEXT,
  house_id UUID,
  house_name TEXT,
  house_location_leaf_id UUID,
  house_location_path TEXT,
  assigned_at TIMESTAMPTZ
) AS $$
BEGIN
  IF p_limit IS NULL OR p_limit < 1 THEN
    RAISE EXCEPTION 'p_limit must be >= 1';
  END IF;

  RETURN QUERY
  WITH RECURSIVE location_scope AS (
    SELECT hl.house_location_id
    FROM house_locations hl
    WHERE p_house_location_id IS NOT NULL
      AND hl.house_location_id = p_house_location_id

    UNION ALL

    SELECT child.house_location_id
    FROM house_locations child
    INNER JOIN location_scope ls
      ON child.parent_house_location_id = ls.house_location_id
    WHERE COALESCE(p_include_descendants, TRUE)
  )
  SELECT
    ii.inventory_item_id,
    o.object_id,
    o.name,
    ii.nickname,
    h.house_id,
    h.name,
    hl.house_location_id,
    hl.path,
    icl.assigned_at
  FROM inventory_items ii
  INNER JOIN objects o
    ON o.object_id = ii.object_id
  INNER JOIN item_current_location icl
    ON icl.inventory_item_id = ii.inventory_item_id
   AND icl.enabled = TRUE
   AND icl.is_current = TRUE
  INNER JOIN house_locations hl
    ON hl.house_location_id = icl.house_location_leaf_id
   AND hl.enabled = TRUE
  INNER JOIN houses h
    ON h.house_id = hl.house_id
   AND h.enabled = TRUE
  WHERE ii.user_id = p_user_id
    AND ii.enabled = TRUE
    AND o.enabled = TRUE
    AND (p_house_id IS NULL OR h.house_id = p_house_id)
    AND (
      p_house_location_id IS NULL
      OR hl.house_location_id IN (SELECT house_location_id FROM location_scope)
    )
  ORDER BY hl.path, o.name, ii.inventory_item_id
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_inventory_item_timeline(
  p_inventory_item_id UUID,
  p_limit INT DEFAULT 100
)
RETURNS TABLE (
  item_movement_id UUID,
  inventory_item_id UUID,
  movement_reason TEXT,
  moved_by TEXT,
  moved_at TIMESTAMPTZ,
  from_house_location_leaf_id UUID,
  from_house_location_path TEXT,
  to_house_location_leaf_id UUID,
  to_house_location_path TEXT,
  notes TEXT
) AS $$
BEGIN
  IF p_inventory_item_id IS NULL THEN
    RAISE EXCEPTION 'p_inventory_item_id is required';
  END IF;

  IF p_limit IS NULL OR p_limit < 1 THEN
    RAISE EXCEPTION 'p_limit must be >= 1';
  END IF;

  RETURN QUERY
  SELECT
    im.item_movement_id,
    im.inventory_item_id,
    im.movement_reason,
    im.moved_by,
    im.moved_at,
    im.from_house_location_leaf_id,
    from_hl.path,
    im.to_house_location_leaf_id,
    to_hl.path,
    im.notes
  FROM item_movements im
  LEFT JOIN house_locations from_hl
    ON from_hl.house_location_id = im.from_house_location_leaf_id
  INNER JOIN house_locations to_hl
    ON to_hl.house_location_id = im.to_house_location_leaf_id
  WHERE im.inventory_item_id = p_inventory_item_id
    AND im.enabled = TRUE
  ORDER BY im.moved_at DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_set_favorite_item(
  p_user_id UUID,
  p_inventory_item_id UUID,
  p_is_favorite BOOLEAN DEFAULT TRUE,
  p_note TEXT DEFAULT NULL
)
RETURNS TABLE (
  user_id UUID,
  inventory_item_id UUID,
  is_favorite BOOLEAN
) AS $$
BEGIN
  IF p_user_id IS NULL OR p_inventory_item_id IS NULL THEN
    RAISE EXCEPTION 'p_user_id and p_inventory_item_id are required';
  END IF;

  IF COALESCE(p_is_favorite, TRUE) THEN
    INSERT INTO favorites (user_id, inventory_item_id, note)
    VALUES (p_user_id, p_inventory_item_id, NULLIF(btrim(p_note), ''))
    ON CONFLICT (user_id, inventory_item_id)
    DO UPDATE SET
      note = EXCLUDED.note;

    RETURN QUERY
    SELECT p_user_id, p_inventory_item_id, TRUE;
  ELSE
    DELETE FROM favorites f
    WHERE f.user_id = p_user_id
      AND f.inventory_item_id = p_inventory_item_id;

    RETURN QUERY
    SELECT p_user_id, p_inventory_item_id, FALSE;
  END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_search_inventory_items_near_point(
  p_user_id UUID,
  p_latitude NUMERIC,
  p_longitude NUMERIC,
  p_radius_meters DOUBLE PRECISION DEFAULT 1000,
  p_limit INT DEFAULT 50
)
RETURNS TABLE (
  inventory_item_id UUID,
  object_id UUID,
  object_name TEXT,
  house_id UUID,
  house_name TEXT,
  house_location_leaf_id UUID,
  house_location_path TEXT,
  distance_meters DOUBLE PRECISION
) AS $$
DECLARE
  v_origin GEOGRAPHY(Point, 4326);
BEGIN
  IF p_latitude IS NULL OR p_longitude IS NULL THEN
    RAISE EXCEPTION 'p_latitude and p_longitude are required';
  END IF;

  IF p_radius_meters IS NULL OR p_radius_meters <= 0 THEN
    RAISE EXCEPTION 'p_radius_meters must be > 0';
  END IF;

  IF p_limit IS NULL OR p_limit < 1 THEN
    RAISE EXCEPTION 'p_limit must be >= 1';
  END IF;

  v_origin := ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::GEOGRAPHY;

  RETURN QUERY
  SELECT
    ii.inventory_item_id,
    o.object_id,
    o.name,
    h.house_id,
    h.name,
    hl.house_location_id,
    hl.path,
    ST_Distance(COALESCE(hl.geo_location, h.geo_location), v_origin) AS distance_meters
  FROM inventory_items ii
  INNER JOIN objects o
    ON o.object_id = ii.object_id
  INNER JOIN item_current_location icl
    ON icl.inventory_item_id = ii.inventory_item_id
   AND icl.is_current = TRUE
   AND icl.enabled = TRUE
  INNER JOIN house_locations hl
    ON hl.house_location_id = icl.house_location_leaf_id
   AND hl.enabled = TRUE
  INNER JOIN houses h
    ON h.house_id = hl.house_id
   AND h.enabled = TRUE
  WHERE ii.user_id = p_user_id
    AND ii.enabled = TRUE
    AND o.enabled = TRUE
    AND COALESCE(hl.geo_location, h.geo_location) IS NOT NULL
    AND ST_DWithin(COALESCE(hl.geo_location, h.geo_location), v_origin, p_radius_meters)
  ORDER BY distance_meters ASC, ii.updated_at DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

COMMIT;
