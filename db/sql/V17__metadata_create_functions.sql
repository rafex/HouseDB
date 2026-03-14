BEGIN;

CREATE OR REPLACE FUNCTION api_create_metadata_catalog(
  p_metadata_target TEXT,
  p_code TEXT,
  p_name TEXT,
  p_description TEXT DEFAULT NULL,
  p_payload JSONB DEFAULT '{}'::jsonb,
  p_enabled BOOLEAN DEFAULT TRUE
)
RETURNS TABLE (
  metadata_catalog_id UUID,
  metadata_target TEXT,
  code TEXT,
  name TEXT,
  description TEXT,
  payload JSONB,
  enabled BOOLEAN
) AS $$
BEGIN
  IF p_metadata_target IS NULL OR p_metadata_target NOT IN ('kiwi_object', 'inventory_item') THEN
    RAISE EXCEPTION 'p_metadata_target must be one of: kiwi_object, inventory_item';
  END IF;

  IF p_code IS NULL OR btrim(p_code) = '' THEN
    RAISE EXCEPTION 'p_code is required';
  END IF;

  IF p_name IS NULL OR btrim(p_name) = '' THEN
    RAISE EXCEPTION 'p_name is required';
  END IF;

  IF p_payload IS NULL OR jsonb_typeof(p_payload) <> 'object' THEN
    RAISE EXCEPTION 'p_payload must be a JSON object';
  END IF;

  RETURN QUERY
  INSERT INTO metadata_catalogs (
    metadata_target,
    code,
    name,
    description,
    payload,
    enabled
  ) VALUES (
    p_metadata_target,
    btrim(p_code),
    btrim(p_name),
    CASE WHEN p_description IS NULL OR btrim(p_description) = '' THEN NULL ELSE btrim(p_description) END,
    p_payload,
    COALESCE(p_enabled, TRUE)
  )
  RETURNING
    metadata_catalogs.metadata_catalog_id,
    metadata_catalogs.metadata_target,
    metadata_catalogs.code,
    metadata_catalogs.name,
    metadata_catalogs.description,
    metadata_catalogs.payload,
    metadata_catalogs.enabled;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_create_metadata_template(
  p_metadata_target TEXT,
  p_code TEXT,
  p_name TEXT,
  p_description TEXT DEFAULT NULL,
  p_definition JSONB DEFAULT '[]'::jsonb,
  p_enabled BOOLEAN DEFAULT TRUE
)
RETURNS TABLE (
  metadata_template_id UUID,
  metadata_target TEXT,
  code TEXT,
  name TEXT,
  description TEXT,
  definition JSONB,
  enabled BOOLEAN
) AS $$
BEGIN
  IF p_metadata_target IS NULL OR p_metadata_target NOT IN ('kiwi_object', 'inventory_item') THEN
    RAISE EXCEPTION 'p_metadata_target must be one of: kiwi_object, inventory_item';
  END IF;

  IF p_code IS NULL OR btrim(p_code) = '' THEN
    RAISE EXCEPTION 'p_code is required';
  END IF;

  IF p_name IS NULL OR btrim(p_name) = '' THEN
    RAISE EXCEPTION 'p_name is required';
  END IF;

  IF p_definition IS NULL OR jsonb_typeof(p_definition) <> 'array' THEN
    RAISE EXCEPTION 'p_definition must be a JSON array';
  END IF;

  RETURN QUERY
  INSERT INTO metadata_templates (
    metadata_target,
    code,
    name,
    description,
    definition,
    enabled
  ) VALUES (
    p_metadata_target,
    btrim(p_code),
    btrim(p_name),
    CASE WHEN p_description IS NULL OR btrim(p_description) = '' THEN NULL ELSE btrim(p_description) END,
    p_definition,
    COALESCE(p_enabled, TRUE)
  )
  RETURNING
    metadata_templates.metadata_template_id,
    metadata_templates.metadata_target,
    metadata_templates.code,
    metadata_templates.name,
    metadata_templates.description,
    metadata_templates.definition,
    metadata_templates.enabled;
END;
$$ LANGUAGE plpgsql;

COMMIT;
