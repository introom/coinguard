-- see https://docs.microsoft.com/en-us/azure/postgresql/flexible-server/concepts-extensions
-- we gotta manually enable it on azure.
create extension if not exists citext;

--;;
-- the table cannot be named "user" because it is reserved.
create table account (
  id uuid not null primary key default gen_random_uuid(),
  -- see https://www.postgresqltutorial.com/postgresql-timestamp/
  created_at timestamptz not null default clock_timestamp(),
  email citext not null unique,
  -- account/name is fair enough.  account/username is kinda repetitive.
  "name" text not null,
  "password" text not null
);