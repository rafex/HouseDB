BEGIN;

CREATE OR REPLACE FUNCTION api_list_house_locations(
  p_house_id UUID,
  p_include_disabled BOOLEAN DEFAULT FALSE,
  p_limit INT DEFAULT 200,
  p_offset INT DEFAULT 0
)
RETURNS TABLE (
  house_location_id UUID,
  house_id UUID,
  kiwi_location_id UUID,
  kiwi_parent_location_id UUID,
  parent_house_location_id UUID,
  location_kind location_kind,
  name TEXT,
  path TEXT,
  level_depth INT,
  latitude NUMERIC,
  longitude NUMERIC,
  reference_code TEXT,
  is_leaf BOOLEAN,
  notes TEXT,
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
    hl.house_location_id,
    hl.house_id,
    hl.kiwi_location_id,
    hl.kiwi_parent_location_id,
    hl.parent_house_location_id,
    hl.location_kind,
    hl.name,
    hl.path,
    hl.level_depth,
    hl.latitude,
    hl.longitude,
    hl.reference_code,
    hl.is_leaf,
    hl.notes,
    hl.enabled
  FROM house_locations hl
  WHERE hl.house_id = p_house_id
    AND (COALESCE(p_include_disabled, FALSE) = TRUE OR hl.enabled = TRUE)
  ORDER BY hl.level_depth ASC, COALESCE(hl.path, hl.name) ASC, hl.name ASC
  LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

COMMIT;
