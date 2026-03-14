create table orders (
    id          uuid primary key,
    customer_id varchar(64)    not null,
    amount      numeric(12, 2) not null,
    status      varchar(16)    not null,
    created_at  timestamp with time zone not null,
    updated_at  timestamp with time zone not null,
    version     bigint         not null
);
