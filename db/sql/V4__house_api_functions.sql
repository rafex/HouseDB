BEGIN;

CREATE OR REPLACE FUNCTION api_create_house(
  p_owner_user_id UUID,
  p_name TEXT,
  p_description TEXT DEFAULT NULL,
  p_street TEXT DEFAULT NULL,
  p_number_ext TEXT DEFAULT NULL,
  p_number_int TEXT DEFAULT NULL,
  p_neighborhood TEXT DEFAULT NULL,
  p_city TEXT DEFAULT NULL,
  p_state TEXT DEFAULT NULL,
  p_zip_code TEXT DEFAULT NULL,
  p_country TEXT DEFAULT NULL,
  p_latitude NUMERIC DEFAULT NULL,
  p_longitude NUMERIC DEFAULT NULL,
  p_url_map TEXT DEFAULT NULL
)
RETURNS TABLE (
  house_id UUID,
  house_member_id UUID
) AS $$
DECLARE
  v_house_id UUID;
  v_house_member_id UUID;
BEGIN
  IF p_owner_user_id IS NULL THEN
    RAISE EXCEPTION 'p_owner_user_id is required';
  END IF;

  IF p_name IS NULL OR btrim(p_name) = '' THEN
    RAISE EXCEPTION 'p_name is required';
  END IF;

  INSERT INTO houses (
    name,
    description,
    street,
    number_ext,
    number_int,
    neighborhood,
    city,
    state,
    zip_code,
    country,
    latitude,
    longitude,
    url_map,
    enabled
  ) VALUES (
    btrim(p_name),
    NULLIF(btrim(p_description), ''),
    NULLIF(btrim(p_street), ''),
    NULLIF(btrim(p_number_ext), ''),
    NULLIF(btrim(p_number_int), ''),
    NULLIF(btrim(p_neighborhood), ''),
    NULLIF(btrim(p_city), ''),
    NULLIF(btrim(p_state), ''),
    NULLIF(btrim(p_zip_code), ''),
    NULLIF(btrim(p_country), ''),
    p_latitude,
    p_longitude,
    NULLIF(btrim(p_url_map), ''),
    TRUE
  )
  RETURNING houses.house_id INTO v_house_id;

  INSERT INTO house_members (
    user_id,
    house_id,
    role,
    enabled
  ) VALUES (
    p_owner_user_id,
    v_house_id,
    'owner',
    TRUE
  )
  RETURNING house_members.house_member_id INTO v_house_member_id;

  RETURN QUERY SELECT v_house_id, v_house_member_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_upsert_house_member(
  p_house_id UUID,
  p_user_id UUID,
  p_role house_member_role DEFAULT 'guest',
  p_enabled BOOLEAN DEFAULT TRUE
)
RETURNS TABLE (
  house_member_id UUID,
  house_id UUID,
  user_id UUID,
  role house_member_role,
  enabled BOOLEAN
) AS $$
BEGIN
  IF p_house_id IS NULL OR p_user_id IS NULL THEN
    RAISE EXCEPTION 'p_house_id and p_user_id are required';
  END IF;

  RETURN QUERY
  INSERT INTO house_members (
    user_id,
    house_id,
    role,
    enabled
  ) VALUES (
    p_user_id,
    p_house_id,
    COALESCE(p_role, 'guest'),
    COALESCE(p_enabled, TRUE)
  )
  ON CONFLICT (user_id, house_id)
  DO UPDATE SET
    role = EXCLUDED.role,
    enabled = EXCLUDED.enabled
  RETURNING
    house_members.house_member_id,
    house_members.house_id,
    house_members.user_id,
    house_members.role,
    house_members.enabled;
END;
$$ LANGUAGE plpgsql;

COMMIT;
