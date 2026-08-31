create table if not exists time_clock_adjustment_outbox (
    id uuid primary key,
    tenant_id varchar(120) not null,
    adjustment_id uuid not null,
    event_type varchar(80) not null,
    employee_id varchar(120) not null,
    original_client_event_id uuid not null,
    decided_by varchar(120) not null,
    decided_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    published_at timestamp with time zone null,
    attempt_count integer not null default 0,
    last_error text null,
    constraint uk_time_clock_adjustment_outbox unique (tenant_id, adjustment_id, event_type)
);

create index if not exists idx_time_clock_adjustment_outbox_pending
    on time_clock_adjustment_outbox (published_at, created_at);
