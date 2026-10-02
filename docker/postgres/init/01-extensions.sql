-- Runs as POSTGRES_USER against POSTGRES_DB.

-- gen_random_uuid() and crypt(); handy for primary keys and password hashes.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Trigram indexes for ILIKE / fuzzy search.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
