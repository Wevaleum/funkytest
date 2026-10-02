-- Developer convenience only: makes an interactive psql session resolve
-- unqualified names to "funkytest" first. The schema itself is created and
-- owned by Flyway, not here.
ALTER ROLE CURRENT_USER SET search_path = funkytest, public;
