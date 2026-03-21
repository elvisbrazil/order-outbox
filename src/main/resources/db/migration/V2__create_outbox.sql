create table outbox_event (
    id             uuid primary key,
    aggregate_type varchar(64)  not null,
    aggregate_id   varchar(64)  not null,
    event_type     varchar(64)  not null,
    payload        text         not null,
    created_at     timestamp with time zone not null,
    published_at   timestamp with time zone
);

create index idx_outbox_event_unpublished on outbox_event (published_at, created_at);
