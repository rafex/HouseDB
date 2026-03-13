BEGIN;

CREATE TABLE metadata_catalogs (
  id BIGSERIAL PRIMARY KEY,
  metadata_catalog_id UUID NOT NULL DEFAULT gen_random_uuid(),
  metadata_target TEXT NOT NULL,
  code TEXT NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  payload JSONB NOT NULL DEFAULT '{}'::jsonb,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_metadata_catalogs_target CHECK (
    metadata_target IN ('kiwi_object', 'inventory_item')
  ),
  CONSTRAINT chk_metadata_catalogs_code_not_blank CHECK (btrim(code) <> ''),
  CONSTRAINT chk_metadata_catalogs_name_not_blank CHECK (btrim(name) <> '')
);

CREATE UNIQUE INDEX idx_metadata_catalogs_metadata_catalog_id
ON metadata_catalogs(metadata_catalog_id);

CREATE UNIQUE INDEX uq_metadata_catalogs_target_code
ON metadata_catalogs(metadata_target, lower(code));

CREATE INDEX idx_metadata_catalogs_target_enabled
ON metadata_catalogs(metadata_target, enabled, created_at DESC);

CREATE TRIGGER trg_metadata_catalogs_updated_at
BEFORE UPDATE ON metadata_catalogs
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

INSERT INTO metadata_catalogs (metadata_target, code, name, description, payload)
VALUES
  (
    'inventory_item',
    'purchase-basic',
    'Compra basica',
    'Datos de compra reutilizables para items de inventario.',
    jsonb_build_object(
      'purchaseDate', '',
      'purchasePrice', '',
      'currency', 'MXN',
      'vendor', ''
    )
  ),
  (
    'inventory_item',
    'warranty',
    'Garantia',
    'Plantilla para vigencia de garantia y comprobantes.',
    jsonb_build_object(
      'warrantyExpiresAt', '',
      'invoiceNumber', '',
      'supportContact', ''
    )
  ),
  (
    'kiwi_object',
    'source-housedb',
    'Trazabilidad HouseDB',
    'Metadatos para objetos sincronizados con Kiwi desde HouseDB.',
    jsonb_build_object(
      'source', 'housedb',
      'trackingLabel', '',
      'brand', ''
    )
  ),
  (
    'kiwi_object',
    'asset-identity',
    'Identidad de activo',
    'Plantilla para marca, modelo y serie del activo remoto.',
    jsonb_build_object(
      'brand', '',
      'model', '',
      'serialNumber', ''
    )
  );

CREATE OR REPLACE FUNCTION api_list_metadata_catalogs(
  p_metadata_target TEXT DEFAULT NULL,
  p_include_disabled BOOLEAN DEFAULT FALSE,
  p_limit INT DEFAULT 50,
  p_offset INT DEFAULT 0
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
  IF p_limit IS NULL OR p_limit < 1 THEN
    RAISE EXCEPTION 'p_limit must be >= 1';
  END IF;

  IF p_offset IS NULL OR p_offset < 0 THEN
    RAISE EXCEPTION 'p_offset must be >= 0';
  END IF;

  IF p_metadata_target IS NOT NULL
     AND p_metadata_target NOT IN ('kiwi_object', 'inventory_item') THEN
    RAISE EXCEPTION 'p_metadata_target must be one of: kiwi_object, inventory_item';
  END IF;

  RETURN QUERY
  SELECT
    mc.metadata_catalog_id,
    mc.metadata_target,
    mc.code,
    mc.name,
    mc.description,
    mc.payload,
    mc.enabled
  FROM metadata_catalogs mc
  WHERE (p_metadata_target IS NULL OR mc.metadata_target = p_metadata_target)
    AND (COALESCE(p_include_disabled, FALSE) = TRUE OR mc.enabled = TRUE)
  ORDER BY mc.metadata_target ASC, mc.name ASC, mc.code ASC
  LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

COMMIT;
