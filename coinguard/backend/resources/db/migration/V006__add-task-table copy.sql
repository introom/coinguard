-- for partition, see 
-- https://www.enterprisedb.com/postgres-tutorials/how-use-table-partitioning-scale-postgresql
create table task (
	id uuid not null default gen_random_uuid(),
	created_at timestamptz not null default clock_timestamp(),
	modified_at timestamptz not null default clock_timestamp(),
	completed_at timestamptz null,
	scheduled_at timestamptz not null,
	priority smallint null default 100,
	queue text not null,
	name text not null,
	props jsonb not null,
	error text null,
	retry_num int not null default 0,
	max_retries int not null default 3,
	status text not null default 'new'::text,

	constraint task_pkey primary key (id, status)
) partition by list (status);

create index task_scheduled_at_queue_idx on task (scheduled_at, queue) where ((status = 'new'::text) or (status = 'retry'::text));
