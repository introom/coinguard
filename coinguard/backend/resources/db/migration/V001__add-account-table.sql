-- for uuid generation
-- not need. we are using postgres 13.
-- create extension if not exists "uuid-ossp";

-- 
-- user table

-- the table cannot be named "user" because it is reserved.
create table account (
  id uuid not null primary key default gen_random_uuid(),
  -- see https://www.postgresqltutorial.com/postgresql-timestamp/
  created_at timestamptz not null default clock_timestamp(),
  email text not null unique,
  "password" text not null
);
