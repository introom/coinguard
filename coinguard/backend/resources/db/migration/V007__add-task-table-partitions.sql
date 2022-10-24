-- for partition, see 
-- https://www.enterprisedb.com/postgres-tutorials/how-use-table-partitioning-scale-postgresql

create table task_completed partition of task for values in ('completed', 'failed');

create table task_default partition of task default;

