create table foo
(
    id   INT primary key,
    data TEXT not null
);

insert into foo(id, data)
values (1, 'some value with placeholder ${placeholder}')