BEGIN;

DROP FUNCTION IF EXISTS api_create_inventory_item(
  UUID, UUID, TEXT, TEXT, inventory_item_status, UUID, TEXT, TEXT
);

CREATE OR REPLACE FUNCTION api_create_inventory_item(
  p_user_id UUID,
  p_object_id UUID,
  p_nickname TEXT DEFAULT NULL,
  p_serial_number TEXT DEFAULT NULL,
  p_condition_status inventory_item_status DEFAULT 'active',
  p_metadata JSONB DEFAULT '{}'::jsonb,
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
    metadata,
    enabled
  ) VALUES (
    p_user_id,
    p_object_id,
    NULLIF(btrim(p_nickname), ''),
    NULLIF(btrim(p_serial_number), ''),
    COALESCE(p_condition_status, 'active'),
    COALESCE(p_metadata, '{}'::jsonb),
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

COMMIT;
