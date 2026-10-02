# Postgres with the project's init scripts baked in.
#
# Everything in /docker-entrypoint-initdb.d runs ONCE, in lexical order, the
# first time the container starts on an empty data directory. Changing a script
# afterwards has no effect until the volume is wiped:
#   docker compose down -v && docker compose up -d --build
FROM postgres:18-alpine

COPY init/ /docker-entrypoint-initdb.d/
