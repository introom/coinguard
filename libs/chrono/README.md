## Database schema setup
`chrono-task` table:

```sql
-- for partition, see 
-- https://www.enterprisedb.com/postgres-tutorials/how-use-table-partitioning-scale-postgresql
create table chrono_task (
	id uuid not null default gen_random_uuid(),
	created_at timestamptz not null default clock_timestamp(),
	modified_at timestamptz not null default clock_timestamp(),
	completed_at timestamptz null,
	scheduled_at timestamptz not null,
	priority smallint default 100,
	queue text not null,
	name text not null,
	props jsonb not null,
	error text null,
	retry_cnt int not null default 0,
	max_retries int not null default 3,
	status text not null default 'pending'::text,

  -- see https://www.postgresql.org/docs/current/ddl-partitioning.html#:~:text=and%20hence%20primary%20keys
  -- on why `status` must be included.
	primary key (id, status)
) partition by list (status);

create table chrono_task_finished partition of chrono_task for values in ('completed', 'failed');
create table chrono_task_unfinished partition of chrono_task default;

create index chrono_task_scheduled_at_queue_key on chrono_task (scheduled_at, queue) where ((status = 'pending'::text) or (status = 'retry'::text));

comment on column chrono_task.name is 'name identifies the same set of tasks.';

```