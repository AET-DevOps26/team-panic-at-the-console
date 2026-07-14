#!/bin/bash
set -e

# Creates one database per service on the shared Postgres instance.
# Each service should use its own DB; this mirrors prod isolation without
# running nine separate containers locally.
for db in incidents events users notifications rules webhooks; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE $db;
    GRANT ALL PRIVILEGES ON DATABASE $db TO $POSTGRES_USER;
EOSQL
done
