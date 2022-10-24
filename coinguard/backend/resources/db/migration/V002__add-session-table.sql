-- add session table
-- see https://docs.microsoft.com/en-us/azure/postgresql/flexible-server/concepts-extensions
-- we gotta manually enable it.
create extension if not exists citext;

create table session (
  id uuid not null primary key default gen_random_uuid(),
  email citext not null unique,
	created_at timestamptz not null default clock_timestamp(),
	updated_at timestamptz not null,
  account_id uuid not null,
  constraint session_account_id_fkey foreign key (account_id) references account(id) on delete cascade
);

create index session_updated_at_idx on session (updated_at);
