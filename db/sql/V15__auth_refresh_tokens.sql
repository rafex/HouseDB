BEGIN;

CREATE TABLE auth_refresh_tokens (
  id BIGSERIAL PRIMARY KEY,
  refresh_token_id UUID NOT NULL DEFAULT gen_random_uuid(),
  token_family_id UUID NOT NULL,
  user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  jwt_id TEXT NOT NULL,
  parent_jwt_id TEXT,
  replaced_by_jwt_id TEXT,
  status TEXT NOT NULL DEFAULT 'active',
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_auth_refresh_tokens_refresh_token_id UNIQUE (refresh_token_id),
  CONSTRAINT uq_auth_refresh_tokens_jwt_id UNIQUE (jwt_id),
  CONSTRAINT chk_auth_refresh_tokens_status CHECK (status IN ('active', 'used', 'revoked')),
  CONSTRAINT chk_auth_refresh_tokens_parent_diff CHECK (parent_jwt_id IS NULL OR parent_jwt_id <> jwt_id),
  CONSTRAINT chk_auth_refresh_tokens_replacement_diff CHECK (replaced_by_jwt_id IS NULL OR replaced_by_jwt_id <> jwt_id)
);

CREATE INDEX idx_auth_refresh_tokens_user_status
ON auth_refresh_tokens(user_id, status, expires_at DESC);

CREATE INDEX idx_auth_refresh_tokens_family
ON auth_refresh_tokens(token_family_id, created_at DESC);

CREATE TRIGGER trg_auth_refresh_tokens_updated_at
BEFORE UPDATE ON auth_refresh_tokens
FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE OR REPLACE FUNCTION api_create_refresh_token(
  p_user_id UUID,
  p_token_family_id UUID,
  p_jwt_id TEXT,
  p_expires_at TIMESTAMPTZ,
  p_parent_jwt_id TEXT DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
  v_refresh_token_id UUID;
BEGIN
  IF p_user_id IS NULL OR p_token_family_id IS NULL OR p_jwt_id IS NULL OR btrim(p_jwt_id) = '' OR p_expires_at IS NULL THEN
    RAISE EXCEPTION 'p_user_id, p_token_family_id, p_jwt_id and p_expires_at are required';
  END IF;

  INSERT INTO auth_refresh_tokens (
    token_family_id,
    user_id,
    jwt_id,
    parent_jwt_id,
    status,
    expires_at
  ) VALUES (
    p_token_family_id,
    p_user_id,
    btrim(p_jwt_id),
    NULLIF(btrim(p_parent_jwt_id), ''),
    'active',
    p_expires_at
  )
  RETURNING refresh_token_id INTO v_refresh_token_id;

  RETURN v_refresh_token_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_rotate_refresh_token(
  p_current_jwt_id TEXT,
  p_new_jwt_id TEXT,
  p_new_expires_at TIMESTAMPTZ,
  p_now TIMESTAMPTZ DEFAULT now()
)
RETURNS TABLE (
  status TEXT,
  user_id UUID,
  token_family_id UUID
) AS $$
DECLARE
  v_current auth_refresh_tokens%ROWTYPE;
BEGIN
  IF p_current_jwt_id IS NULL OR btrim(p_current_jwt_id) = '' OR p_new_jwt_id IS NULL OR btrim(p_new_jwt_id) = ''
     OR p_new_expires_at IS NULL THEN
    RAISE EXCEPTION 'p_current_jwt_id, p_new_jwt_id and p_new_expires_at are required';
  END IF;

  SELECT *
    INTO v_current
    FROM auth_refresh_tokens
   WHERE jwt_id = btrim(p_current_jwt_id)
   FOR UPDATE;

  IF NOT FOUND THEN
    RETURN QUERY SELECT 'NOT_FOUND'::TEXT, NULL::UUID, NULL::UUID;
    RETURN;
  END IF;

  IF v_current.status = 'revoked' THEN
    UPDATE auth_refresh_tokens
       SET status = 'revoked',
           revoked_at = COALESCE(revoked_at, p_now)
     WHERE token_family_id = v_current.token_family_id
       AND status <> 'revoked';
    RETURN QUERY SELECT 'REVOKED'::TEXT, v_current.user_id, v_current.token_family_id;
    RETURN;
  END IF;

  IF v_current.status = 'used' THEN
    UPDATE auth_refresh_tokens
       SET status = 'revoked',
           revoked_at = COALESCE(revoked_at, p_now)
     WHERE token_family_id = v_current.token_family_id
       AND status <> 'revoked';
    RETURN QUERY SELECT 'USED'::TEXT, v_current.user_id, v_current.token_family_id;
    RETURN;
  END IF;

  IF v_current.expires_at IS NULL OR v_current.expires_at <= p_now THEN
    RETURN QUERY SELECT 'EXPIRED'::TEXT, v_current.user_id, v_current.token_family_id;
    RETURN;
  END IF;

  UPDATE auth_refresh_tokens
     SET status = 'used',
         used_at = p_now,
         replaced_by_jwt_id = btrim(p_new_jwt_id)
   WHERE jwt_id = btrim(p_current_jwt_id);

  INSERT INTO auth_refresh_tokens (
    token_family_id,
    user_id,
    jwt_id,
    parent_jwt_id,
    status,
    expires_at
  ) VALUES (
    v_current.token_family_id,
    v_current.user_id,
    btrim(p_new_jwt_id),
    v_current.jwt_id,
    'active',
    p_new_expires_at
  );

  RETURN QUERY SELECT 'SUCCESS'::TEXT, v_current.user_id, v_current.token_family_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION api_revoke_refresh_token_family(
  p_token_family_id UUID,
  p_now TIMESTAMPTZ DEFAULT now()
)
RETURNS INTEGER AS $$
DECLARE
  v_count INTEGER;
BEGIN
  IF p_token_family_id IS NULL THEN
    RAISE EXCEPTION 'p_token_family_id is required';
  END IF;

  UPDATE auth_refresh_tokens
     SET status = 'revoked',
         revoked_at = COALESCE(revoked_at, p_now)
   WHERE token_family_id = p_token_family_id
     AND status <> 'revoked';

  GET DIAGNOSTICS v_count = ROW_COUNT;
  RETURN v_count;
END;
$$ LANGUAGE plpgsql;

COMMIT;
