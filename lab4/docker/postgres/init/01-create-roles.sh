#!/bin/sh
# Выполняется один раз при первом запуске контейнера от имени суперпользователя.
# Создаёт БД и две роли:
#   DB_OWNER_USER — владелец схемы, выполняет миграции (DDL);
#   DB_APP_USER   — пользователь приложения, только DML над таблицами.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
     -v db_name="$DB_NAME" \
     -v owner_user="$DB_OWNER_USER" -v owner_password="$DB_OWNER_PASSWORD" \
     -v app_user="$DB_APP_USER" -v app_password="$DB_APP_PASSWORD" <<'SQL'
CREATE ROLE :"owner_user" LOGIN PASSWORD :'owner_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION CONNECTION LIMIT 20;

CREATE DATABASE :"db_name" OWNER :"owner_user";
REVOKE ALL ON DATABASE :"db_name" FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE :"db_name" TO :"owner_user";
GRANT CONNECT ON DATABASE :"db_name" TO :"app_user";

\connect :"db_name"

REVOKE ALL ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO :"owner_user";
GRANT USAGE ON SCHEMA public TO :"app_user";

-- Права на таблицы и последовательности, которые владелец создаст миграциями.
ALTER DEFAULT PRIVILEGES FOR ROLE :"owner_user" IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"owner_user" IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO :"app_user";
SQL
