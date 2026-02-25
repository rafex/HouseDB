BEGIN;

CREATE OR REPLACE FUNCTION api_get_inventory_item_detail(
  p_inventory_item_id UUID
)
RETURNS TABLE (
  inventory_item_id UUID,
  user_id UUID,
  object_id UUID,
  object_kiwi_id UUID,
  nickname TEXT,
  serial_number TEXT,
  condition_status inventory_item_status,
  inventory_item_enabled BOOLEAN,
  house_id UUID,
  house_name TEXT,
  house_location_leaf_id UUID,
  house_location_path TEXT,
  assigned_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ
) AS $$
BEGIN
  IF p_inventory_item_id IS NULL THEN
    RAISE EXCEPTION 'p_inventory_item_id is required';
  END IF;

  RETURN QUERY
  SELECT
    ii.inventory_item_id,
    ii.user_id,
    ii.object_id,
    o.object_kiwi_id,
    ii.nickname,
    ii.serial_number,
    ii.condition_status,
    ii.enabled,
    h.house_id,
    h.name,
    hl.house_location_id,
    hl.path,
    icl.assigned_at,
    ii.created_at,
    ii.updated_at
  FROM inventory_items ii
  INNER JOIN objects o
    ON o.object_id = ii.object_id
  LEFT JOIN item_current_location icl
    ON icl.inventory_item_id = ii.inventory_item_id
   AND icl.enabled = TRUE
   AND icl.is_current = TRUE
  LEFT JOIN house_locations hl
    ON hl.house_location_id = icl.house_location_leaf_id
   AND hl.enabled = TRUE
  LEFT JOIN houses h
    ON h.house_id = hl.house_id
   AND h.enabled = TRUE
  WHERE ii.inventory_item_id = p_inventory_item_id
    AND ii.enabled = TRUE
    AND o.enabled = TRUE
  LIMIT 1;
END;
$$ LANGUAGE plpgsql;

COMMIT;
