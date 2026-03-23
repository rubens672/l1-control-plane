-- =========================================
-- SCHEMA: control_plane (Security / Users)
-- =========================================
-- This is a reference schema for potential future migration from
-- Spring Security in-memory to DB-based authentication.

CREATE TABLE IF NOT EXISTS control_plane.users (
  id          BIGSERIAL PRIMARY KEY,
  username    TEXT NOT NULL UNIQUE,
  password    TEXT NOT NULL, -- BCrypt hash
  enabled     BOOLEAN NOT NULL DEFAULT true,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS control_plane.authorities (
  username    TEXT NOT NULL REFERENCES control_plane.users(username) ON DELETE CASCADE,
  authority   TEXT NOT NULL,
  UNIQUE(username, authority)
);

-- Default Admin User (Password is 'password123' using BCrypt)
-- Insert only if the user doesn't already exist
INSERT INTO control_plane.users (username, password, enabled)
VALUES ('admin', '$2a$10$X/bX5mG99R0U.uJ0gqP.tOJOTv7iM.c.c.c.c.c.c.c.c.c.c.c.c.c', true)
ON CONFLICT (username) DO NOTHING;

INSERT INTO control_plane.authorities (username, authority)
VALUES ('admin', 'ROLE_ADMIN')
ON CONFLICT (username, authority) DO NOTHING;
