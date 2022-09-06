create schema if not exists public;
create schema if not exists other;

create table public.foo
(
    id   UUID primary key,
    data JSONB not null
);

create table other.bar
(
    id   UUID primary key,
    data JSONB not null
);