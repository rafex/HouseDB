BEGIN;

DROP FUNCTION IF EXISTS api_search_inventory_items(UUID, TEXT, UUID, UUID, INT);
CREATE OR REPLACE FUNCTION api_search_inventory_items(
  p_user_id UUID,
  p_text TEXT DEFAULT NULL,
  p_house_id UUID DEFAULT NULL,
  p_house_location_leaf_id UUID DEFAULT NULL,
  p_limit INT DEFAULT 50,
  p_offset INT DEFAULT 0
)
RETURNS TABLE (
  inventory_item_id UUID,
  object_id UUID,
  object_kiwi_id UUID,
  object_name TEXT,
  object_description TEXT,
  object_category TEXT,
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
  IF p_offset IS NULL OR p_offset < 0 THEN
    RAISE EXCEPTION 'p_offset must be >= 0';
  END IF;

  RETURN QUERY
  SELECT
    ii.inventory_item_id,
    o.object_id,
    o.object_kiwi_id,
    o.name,
    o.description,
    o.category,
    ii.nickname,
    h.house_id,
    h.name,
    hl.house_location_id,
    hl.path,
    CASE
      WHEN p_text IS NULL OR btrim(p_text) = '' THEN 1.0::REAL
      ELSE GREATEST(
        similarity(o.name, p_text),
        similarity(COALESCE(o.category, ''), p_text),
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
      OR COALESCE(o.category, '') ILIKE '%' || p_text || '%'
      OR COALESCE(o.description, '') ILIKE '%' || p_text || '%'
      OR COALESCE(ii.nickname, '') ILIKE '%' || p_text || '%'
      OR COALESCE(hl.path, '') ILIKE '%' || p_text || '%'
      OR o.name % p_text
      OR COALESCE(o.category, '') % p_text
    )
    AND (p_house_id IS NULL OR h.house_id = p_house_id)
    AND (p_house_location_leaf_id IS NULL OR hl.house_location_id = p_house_location_leaf_id)
  ORDER BY rank DESC, ii.updated_at DESC
  LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS api_list_inventory_by_location(UUID, UUID, UUID, BOOLEAN, INT);
CREATE OR REPLACE FUNCTION api_list_inventory_by_location(
  p_user_id UUID,
  p_house_id UUID DEFAULT NULL,
  p_house_location_id UUID DEFAULT NULL,
  p_include_descendants BOOLEAN DEFAULT TRUE,
  p_limit INT DEFAULT 200,
  p_offset INT DEFAULT 0
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
  IF p_offset IS NULL OR p_offset < 0 THEN
    RAISE EXCEPTION 'p_offset must be >= 0';
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
  LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS api_inventory_item_timeline(UUID, INT);
CREATE OR REPLACE FUNCTION api_inventory_item_timeline(
  p_inventory_item_id UUID,
  p_limit INT DEFAULT 100,
  p_offset INT DEFAULT 0
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
  IF p_offset IS NULL OR p_offset < 0 THEN
    RAISE EXCEPTION 'p_offset must be >= 0';
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
  LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS api_search_inventory_items_near_point(UUID, NUMERIC, NUMERIC, DOUBLE PRECISION, INT);
CREATE OR REPLACE FUNCTION api_search_inventory_items_near_point(
  p_user_id UUID,
  p_latitude NUMERIC,
  p_longitude NUMERIC,
  p_radius_meters DOUBLE PRECISION DEFAULT 1000,
  p_limit INT DEFAULT 50,
  p_offset INT DEFAULT 0
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
  IF p_offset IS NULL OR p_offset < 0 THEN
    RAISE EXCEPTION 'p_offset must be >= 0';
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
  LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS api_list_user_houses(UUID, BOOLEAN, INT);
CREATE OR REPLACE FUNCTION api_list_user_houses(
  p_user_id UUID,
  p_include_disabled BOOLEAN DEFAULT FALSE,
  p_limit INT DEFAULT 200,
  p_offset INT DEFAULT 0
)
RETURNS TABLE (
  house_id UUID,
  name TEXT,
  description TEXT,
  city TEXT,
  state TEXT,
  country TEXT,
  role house_member_role,
  member_enabled BOOLEAN,
  house_enabled BOOLEAN
) AS $$
BEGIN
  IF p_user_id IS NULL THEN
    RAISE EXCEPTION 'p_user_id is required';
  END IF;
  IF p_limit IS NULL OR p_limit < 1 THEN
    RAISE EXCEPTION 'p_limit must be >= 1';
  END IF;
  IF p_offset IS NULL OR p_offset < 0 THEN
    RAISE EXCEPTION 'p_offset must be >= 0';
  END IF;

  RETURN QUERY
  SELECT
    h.house_id,
    h.name,
    h.description,
    h.city,
    h.state,
    h.country,
    hm.role,
    hm.enabled,
    h.enabled
  FROM house_members hm
  INNER JOIN houses h
    ON h.house_id = hm.house_id
  WHERE hm.user_id = p_user_id
    AND (COALESCE(p_include_disabled, FALSE) = TRUE OR (hm.enabled = TRUE AND h.enabled = TRUE))
  ORDER BY lower(h.name), h.created_at DESC
  LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS api_list_house_members(UUID, BOOLEAN, INT);
CREATE OR REPLACE FUNCTION api_list_house_members(
  p_house_id UUID,
  p_include_disabled BOOLEAN DEFAULT FALSE,
  p_limit INT DEFAULT 200,
  p_offset INT DEFAULT 0
)
RETURNS TABLE (
  house_member_id UUID,
  house_id UUID,
  user_id UUID,
  role house_member_role,
  enabled BOOLEAN
) AS $$
BEGIN
  IF p_house_id IS NULL THEN
    RAISE EXCEPTION 'p_house_id is required';
  END IF;
  IF p_limit IS NULL OR p_limit < 1 THEN
    RAISE EXCEPTION 'p_limit must be >= 1';
  END IF;
  IF p_offset IS NULL OR p_offset < 0 THEN
    RAISE EXCEPTION 'p_offset must be >= 0';
  END IF;

  RETURN QUERY
  SELECT
    hm.house_member_id,
    hm.house_id,
    hm.user_id,
    hm.role,
    hm.enabled
  FROM house_members hm
  WHERE hm.house_id = p_house_id
    AND (COALESCE(p_include_disabled, FALSE) = TRUE OR hm.enabled = TRUE)
  ORDER BY hm.created_at DESC
  LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

COMMIT;
