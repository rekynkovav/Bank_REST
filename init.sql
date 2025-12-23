CREATE USER bank_user WITH ENCRYPTED PASSWORD 'bank_pass';

CREATE DATABASE bank_db OWNER bank_user;

GRANT ALL PRIVILEGES ON DATABASE bank_db TO bank_user;

\c bank_db;

GRANT ALL ON SCHEMA public TO bank_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO bank_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO bank_user;

ALTER USER bank_user SET search_path = public;