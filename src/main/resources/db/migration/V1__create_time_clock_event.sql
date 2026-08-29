CREATE TABLE time_clock_event (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    employee_id VARCHAR(100) NOT NULL,
    client_event_id UUID NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_time_clock_event_identity
        UNIQUE (tenant_id, employee_id, client_event_id)
);

CREATE INDEX idx_time_clock_event_tenant_employee_occurred_at
    ON time_clock_event (tenant_id, employee_id, occurred_at);
