BEGIN;

ALTER TABLE objects
  ADD COLUMN IF NOT EXISTS category TEXT;

CREATE INDEX IF NOT EXISTS idx_objects_category_trgm
ON objects
USING gin (category gin_trgm_ops);

DROP FUNCTION IF EXISTS api_search_inventory_items(UUID, TEXT, UUID, UUID, INT);

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
DECLARE
  v_owner_user_id UUID;
BEGIN
  IF p_user_id IS NULL OR p_inventory_item_id IS NULL THEN
    RAISE EXCEPTION 'p_user_id and p_inventory_item_id are required';
  END IF;

  SELECT ii.user_id
    INTO v_owner_user_id
  FROM inventory_items ii
  WHERE ii.inventory_item_id = p_inventory_item_id
    AND ii.enabled = TRUE;

  IF v_owner_user_id IS NULL THEN
    RAISE EXCEPTION 'inventory item not found or disabled';
  END IF;

  IF v_owner_user_id <> p_user_id THEN
    RAISE EXCEPTION 'inventory item does not belong to p_user_id';
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

COMMIT;
