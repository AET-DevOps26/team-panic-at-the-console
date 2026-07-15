#!/bin/bash
set -e

# Creates one database per service on the shared Postgres instance.
# Each service should use its own DB; this mirrors prod isolation without
# running nine separate containers locally.
#
# Runs in two contexts, so creation is guarded to be idempotent:
#  - /docker-entrypoint-initdb.d on the first boot of a fresh data directory
#  - on every deploy (compose `postgres-init` one-shot / Helm pre-upgrade
#    job), which is what creates databases added to this list after an
#    environment's volume was first initialized
for db in incidents events users notifications rules webhooks; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE $db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
    GRANT ALL PRIVILEGES ON DATABASE $db TO $POSTGRES_USER;
EOSQL
done
