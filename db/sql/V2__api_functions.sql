BEGIN;

CREATE OR REPLACE FUNCTION api_search_items(
  p_text TEXT,
  p_category_id UUID DEFAULT NULL,
  p_location_id UUID DEFAULT NULL,
  p_limit INT DEFAULT 50
)
RETURNS TABLE (
  item_id UUID,
  name TEXT,
  quantity INTEGER,
  status item_status,
  location_id UUID,
  rank REAL
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    i.item_id,
    i.name,
    i.quantity,
    i.status,
    i.location_id,
    similarity(i.name, p_text) AS rank
  FROM items i
  WHERE (p_text IS NULL OR i.name ILIKE '%' || p_text || '%')
    AND (p_category_id IS NULL OR i.category_id = p_category_id)
    AND (p_location_id IS NULL OR i.location_id = p_location_id)
  ORDER BY rank DESC, i.updated_at DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_fuzzy_items(
  p_text TEXT,
  p_limit INT DEFAULT 10
)
RETURNS TABLE (
  item_id UUID,
  name TEXT,
  score REAL
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    i.item_id,
    i.name,
    similarity(i.name, p_text) AS score
  FROM items i
  WHERE i.name % p_text
  ORDER BY score DESC
  LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

COMMIT;
