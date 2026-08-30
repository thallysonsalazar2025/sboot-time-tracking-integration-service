CREATE TABLE time_clock_adjustment (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    employee_id VARCHAR(100) NOT NULL,
    original_client_event_id UUID NOT NULL,
    justification VARCHAR(1000) NOT NULL,
    requested_by VARCHAR(150) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL,
    decided_by VARCHAR(150),
    decided_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_time_clock_adjustment_tenant_employee_requested_at
    ON time_clock_adjustment (tenant_id, employee_id, requested_at);

CREATE INDEX idx_time_clock_adjustment_tenant_status
    ON time_clock_adjustment (tenant_id, status);
