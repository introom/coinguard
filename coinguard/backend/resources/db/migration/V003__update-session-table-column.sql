alter table session drop column if exists email;

alter table session add column token text not null unique;