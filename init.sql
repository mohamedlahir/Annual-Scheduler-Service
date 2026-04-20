-- init.sql for postgres used by docker-compose
-- Creates the databases used by the services

CREATE DATABASE auth_db;
CREATE DATABASE user_profile_db;
CREATE DATABASE scheduling_db;

-- Optionally create extensions or initial tables if needed
-- For example, to enable pgcrypto: CREATE EXTENSION IF NOT EXISTS pgcrypto;

