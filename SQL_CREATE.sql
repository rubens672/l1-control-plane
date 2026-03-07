-- =========================================
-- SCHEMA: control_plane
-- =========================================
CREATE SCHEMA IF NOT EXISTS control_plane;

-- -----------------------------------------
-- TENANTS
-- -----------------------------------------
CREATE TABLE IF NOT EXISTS control_plane.tenants (
  tenant_id   TEXT PRIMARY KEY,
  name        TEXT NOT NULL,
  status      TEXT NOT NULL DEFAULT 'ACTIVE'
              CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED')),
  plan        TEXT NOT NULL DEFAULT 'BASIC',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -----------------------------------------
-- SUBSCRIPTIONS (1 row per tenant)
-- -----------------------------------------
CREATE TABLE IF NOT EXISTS control_plane.subscriptions (
  tenant_id    TEXT PRIMARY KEY REFERENCES control_plane.tenants(tenant_id) ON DELETE CASCADE,
  status       TEXT NOT NULL DEFAULT 'ACTIVE'
               CHECK (status IN ('ACTIVE','PAST_DUE','CANCELED','EXPIRED')),
  valid_from   TIMESTAMPTZ NOT NULL,
  valid_to     TIMESTAMPTZ NOT NULL,
  max_devices  INTEGER NOT NULL DEFAULT 1 CHECK (max_devices >= 0),
  features     JSONB NOT NULL DEFAULT '{}'::jsonb,
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (valid_to > valid_from)
);

-- -----------------------------------------
-- SITES
-- -----------------------------------------
CREATE TABLE IF NOT EXISTS control_plane.sites (
  site_id     TEXT PRIMARY KEY,
  tenant_id   TEXT NOT NULL REFERENCES control_plane.tenants(tenant_id) ON DELETE CASCADE,
  name        TEXT NOT NULL,
  timezone    TEXT NOT NULL DEFAULT 'Europe/Rome',
  status      TEXT NOT NULL DEFAULT 'ACTIVE'
              CHECK (status IN ('ACTIVE','SUSPENDED')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sites_tenant ON control_plane.sites(tenant_id);

-- -----------------------------------------
-- DEVICES
-- -----------------------------------------
CREATE TABLE IF NOT EXISTS control_plane.devices (
  device_id     TEXT PRIMARY KEY,

  tenant_id     TEXT NOT NULL REFERENCES control_plane.tenants(tenant_id) ON DELETE CASCADE,
  site_id       TEXT NOT NULL REFERENCES control_plane.sites(site_id) ON DELETE CASCADE,

  status        TEXT NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','REVOKED')),

  model         TEXT,
  onboarded_at  TIMESTAMPTZ,
  last_seen_at  TIMESTAMPTZ,

  max_msgs_per_min INTEGER NOT NULL DEFAULT 60 CHECK (max_msgs_per_min > 0),

  -- mTLS binding
  expected_fingerprint_sha256 TEXT, -- set when ACTIVE
  cert_serial   TEXT,
  cert_not_after TIMESTAMPTZ,
  issuer_dn     TEXT,
  subject_dn    TEXT,

  -- provisioning bootstrap (one-time, only while PENDING)
  bootstrap_token_hash   TEXT,
  bootstrap_expires_at   TIMESTAMPTZ,

  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

  -- coerenza basilare
  CHECK (
    (status = 'PENDING' AND expected_fingerprint_sha256 IS NULL)
    OR (status <> 'PENDING')
  ),
  CHECK (
    bootstrap_expires_at IS NULL OR bootstrap_expires_at > now() - interval '365 days'
  )
);

CREATE INDEX IF NOT EXISTS idx_devices_tenant_site ON control_plane.devices(tenant_id, site_id);

CREATE INDEX IF NOT EXISTS idx_devices_site ON control_plane.devices(site_id);

-- (Facoltativo ma utile) velocizza query per fingerprint mismatch investigation
CREATE INDEX IF NOT EXISTS idx_devices_expected_fp ON control_plane.devices(expected_fingerprint_sha256);

-- -----------------------------------------
-- CERT HISTORY
-- -----------------------------------------
CREATE TABLE IF NOT EXISTS control_plane.device_cert_history (
  cert_id        BIGSERIAL PRIMARY KEY,
  device_id      TEXT NOT NULL REFERENCES control_plane.devices(device_id) ON DELETE CASCADE,

  fingerprint_sha256 TEXT NOT NULL,
  cert_serial        TEXT,
  not_before         TIMESTAMPTZ,
  not_after          TIMESTAMPTZ,

  issued_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  revoked_at         TIMESTAMPTZ,
  revoke_reason      TEXT,

  CHECK (revoked_at IS NULL OR revoked_at >= issued_at)
);

CREATE INDEX IF NOT EXISTS idx_cert_history_device ON control_plane.device_cert_history(device_id);

-- -----------------------------------------
-- (OPZIONALE) Audit minimo (gestionale, non telemetria)
-- -----------------------------------------
CREATE TABLE IF NOT EXISTS control_plane.audit_events (
  event_id     BIGSERIAL PRIMARY KEY,
  event_type   TEXT NOT NULL,
  tenant_id    TEXT,
  site_id      TEXT,
  device_id    TEXT,
  details      JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_created_at ON control_plane.audit_events(created_at);

-- -----------------------------------------
-- Trigger "updated_at" (semplice e utile)
-- -----------------------------------------
CREATE OR REPLACE FUNCTION control_plane.set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_tenants_updated_at') THEN
    CREATE TRIGGER trg_tenants_updated_at
    BEFORE UPDATE ON control_plane.tenants
    FOR EACH ROW EXECUTE FUNCTION control_plane.set_updated_at();
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_devices_updated_at') THEN
    CREATE TRIGGER trg_devices_updated_at
    BEFORE UPDATE ON control_plane.devices
    FOR EACH ROW EXECUTE FUNCTION control_plane.set_updated_at();
  END IF;
END$$;
