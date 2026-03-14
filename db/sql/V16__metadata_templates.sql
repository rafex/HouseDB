BEGIN;

CREATE TABLE metadata_templates (
  id BIGSERIAL PRIMARY KEY,
  metadata_template_id UUID NOT NULL DEFAULT gen_random_uuid(),
  metadata_target TEXT NOT NULL,
  code TEXT NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  definition JSONB NOT NULL DEFAULT '[]'::jsonb,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_metadata_templates_target CHECK (
    metadata_target IN ('kiwi_object', 'inventory_item')
  ),
  CONSTRAINT chk_metadata_templates_code_not_blank CHECK (btrim(code) <> ''),
  CONSTRAINT chk_metadata_templates_name_not_blank CHECK (btrim(name) <> ''),
  CONSTRAINT chk_metadata_templates_definition_is_array CHECK (jsonb_typeof(definition) = 'array')
);

CREATE UNIQUE INDEX idx_metadata_templates_metadata_template_id
ON metadata_templates(metadata_template_id);

CREATE UNIQUE INDEX uq_metadata_templates_target_code
ON metadata_templates(metadata_target, lower(code));

CREATE INDEX idx_metadata_templates_target_enabled
ON metadata_templates(metadata_target, enabled, created_at DESC);

CREATE TRIGGER trg_metadata_templates_updated_at
BEFORE UPDATE ON metadata_templates
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

INSERT INTO metadata_templates (metadata_target, code, name, description, definition)
VALUES
  (
    'inventory_item',
    'finder-basics',
    'Pistas para encontrarlo',
    'Campos visuales y practicos para reencontrar un objeto dentro de casa.',
    jsonb_build_array(
      jsonb_build_object('key', 'alias', 'label', 'Alias', 'placeholder', 'Ej. audifonos del escritorio', 'hint', 'Nombre cotidiano con el que lo reconoces rapido.', 'defaultValue', ''),
      jsonb_build_object('key', 'color', 'label', 'Color', 'placeholder', 'Ej. negro mate', 'hint', 'Color o acabado distintivo.', 'defaultValue', ''),
      jsonb_build_object('key', 'material', 'label', 'Material', 'placeholder', 'Ej. metal, plastico, madera', 'hint', 'Material predominante del objeto.', 'defaultValue', ''),
      jsonb_build_object('key', 'visualReference', 'label', 'Referencia visual', 'placeholder', 'Ej. caja azul con etiqueta blanca', 'hint', 'Pista visual que ayuda a reencontrarlo.', 'defaultValue', '')
    )
  ),
  (
    'inventory_item',
    'purchase-and-care',
    'Compra y cuidado',
    'Datos de compra, garantia y cuidado util para HouseDB.',
    jsonb_build_array(
      jsonb_build_object('key', 'purchaseDate', 'label', 'Fecha de compra', 'placeholder', 'Ej. 2026-03-13', 'hint', 'Cuando lo compraste o llego a casa.', 'defaultValue', ''),
      jsonb_build_object('key', 'vendor', 'label', 'Proveedor', 'placeholder', 'Ej. Amazon', 'hint', 'Tienda o persona de quien proviene.', 'defaultValue', ''),
      jsonb_build_object('key', 'purchasePrice', 'label', 'Precio', 'placeholder', 'Ej. 2499', 'hint', 'Monto pagado si te sirve como referencia.', 'defaultValue', ''),
      jsonb_build_object('key', 'careNotes', 'label', 'Nota de cuidado', 'placeholder', 'Ej. guardar en funda, no mojar', 'hint', 'Instrucciones practicas para cuidarlo.', 'defaultValue', '')
    )
  ),
  (
    'kiwi_object',
    'asset-identity',
    'Identidad tecnica',
    'Campos tecnicos comunes para integracion con Kiwi.',
    jsonb_build_array(
      jsonb_build_object('key', 'brand', 'label', 'Brand', 'placeholder', 'Ej. Sony', 'hint', 'Marca del activo.', 'defaultValue', ''),
      jsonb_build_object('key', 'model', 'label', 'Model', 'placeholder', 'Ej. WH-1000XM5', 'hint', 'Modelo tecnico del activo.', 'defaultValue', ''),
      jsonb_build_object('key', 'serialNumber', 'label', 'Serial', 'placeholder', 'Ej. SN-12345', 'hint', 'Serie tecnica del activo.', 'defaultValue', '')
    )
  ),
  (
    'kiwi_object',
    'sync-traceability',
    'Trazabilidad de sincronizacion',
    'Campos tecnicos para rastrear la sincronizacion con Kiwi.',
    jsonb_build_array(
      jsonb_build_object('key', 'source', 'label', 'Source', 'placeholder', 'Ej. housedb', 'hint', 'Sistema origen del dato.', 'defaultValue', 'housedb'),
      jsonb_build_object('key', 'trackingLabel', 'label', 'Tracking label', 'placeholder', 'Ej. source-housedb-001', 'hint', 'Etiqueta tecnica de rastreo.', 'defaultValue', ''),
      jsonb_build_object('key', 'externalReference', 'label', 'Referencia externa', 'placeholder', 'Ej. asset-001', 'hint', 'ID o referencia de otro sistema.', 'defaultValue', '')
    )
  );

CREATE OR REPLACE FUNCTION api_list_metadata_templates(
  p_metadata_target TEXT DEFAULT NULL,
  p_include_disabled BOOLEAN DEFAULT FALSE,
  p_limit INT DEFAULT 50,
  p_offset INT DEFAULT 0
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
    mt.metadata_template_id,
    mt.metadata_target,
    mt.code,
    mt.name,
    mt.description,
    mt.definition,
    mt.enabled
  FROM metadata_templates mt
  WHERE (p_metadata_target IS NULL OR mt.metadata_target = p_metadata_target)
    AND (COALESCE(p_include_disabled, FALSE) = TRUE OR mt.enabled = TRUE)
  ORDER BY mt.metadata_target ASC, mt.name ASC, mt.code ASC
  LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

COMMIT;
