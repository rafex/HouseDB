BEGIN;

CREATE OR REPLACE FUNCTION api_list_user_houses(
  p_user_id UUID,
  p_include_disabled BOOLEAN DEFAULT FALSE,
  p_limit INT DEFAULT 200
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
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_list_house_members(
  p_house_id UUID,
  p_include_disabled BOOLEAN DEFAULT FALSE,
  p_limit INT DEFAULT 200
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
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

COMMIT;
