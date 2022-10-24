create table wallet (
	id uuid not null primary key default gen_random_uuid(),
  account_id uuid not null references account (id) on delete cascade,
	-- there shall not be any trailing comma, e.g., 
	-- data jsonb not null,
	data jsonb not null
); 