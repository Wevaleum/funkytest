-- Flyway owns the "funkytest" schema from here on. The schema itself is created
-- by Flyway (spring.flyway.schemas), so this migration only sets up what lives
-- inside the database.
--
-- Extensions are pinned to "public" so they are shared rather than duplicated
-- per schema, and are idempotent: the compose image already creates them in
-- docker/postgres/init, while a bare postgres image (Testcontainers) does not.
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

-- Add tables in new V<n>__<description>.sql files. Never edit an applied
-- migration: Flyway validates its checksum on every startup.
