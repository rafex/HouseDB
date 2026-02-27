BEGIN;

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
  ON CONFLICT ON CONSTRAINT uq_item_current_location_inventory_item
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
    v_movement_id AS item_movement_id,
    p_inventory_item_id AS inventory_item_id,
    v_current_location AS from_house_location_leaf_id,
    p_to_house_location_leaf_id AS to_house_location_leaf_id,
    v_effective_moved_at AS moved_at;
END;
$$ LANGUAGE plpgsql;

COMMIT;
