create table session (
  id uuid not null primary key default gen_random_uuid(),
  token text not null unique,
	created_at timestamptz not null default clock_timestamp(),
	updated_at timestamptz not null default clock_timestamp(),
  account_id uuid not null,
  constraint session_account_id_fkey foreign key (account_id) references account(id) on delete cascade
);

-- the separation `--;;` is to indicate to migratus a separate statement.
--;;

-- `index` naming format is <table-name>_<key-name>_{key, pkey, fkey}
create index session_updated_at_key on session (updated_at);
