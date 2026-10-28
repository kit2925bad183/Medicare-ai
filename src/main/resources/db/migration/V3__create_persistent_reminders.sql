UPDATE users SET time_zone = 'UTC' WHERE time_zone IS NULL;

CREATE TABLE medicine_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    medicine_name VARCHAR(120) NOT NULL,
    instructions VARCHAR(500),
    frequency VARCHAR(10) NOT NULL,
    local_time TIME NOT NULL,
    weekday INTEGER,
    start_date DATE NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_medicine_schedules PRIMARY KEY (id),
    CONSTRAINT fk_medicine_schedules_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_medicine_schedules_frequency CHECK (frequency IN ('DAILY', 'WEEKLY')),
    CONSTRAINT ck_medicine_schedules_weekday CHECK (weekday IS NULL OR weekday BETWEEN 1 AND 7)
);

CREATE INDEX idx_medicine_schedules_owner ON medicine_schedules (owner_id);

CREATE TABLE reminder_occurrences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    due_at TIMESTAMP(6) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    state VARCHAR(12) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(160) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_reminder_occurrences PRIMARY KEY (id),
    CONSTRAINT uk_reminder_occurrences_key UNIQUE (idempotency_key),
    CONSTRAINT fk_reminder_occurrences_schedule FOREIGN KEY (schedule_id) REFERENCES medicine_schedules (id),
    CONSTRAINT ck_reminder_occurrences_state CHECK (state IN ('PENDING', 'AVAILABLE', 'CANCELLED'))
);

CREATE INDEX idx_reminder_occurrences_due_state ON reminder_occurrences (state, due_at);

CREATE TABLE notification_deliveries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    occurrence_id BIGINT NOT NULL,
    channel VARCHAR(16) NOT NULL DEFAULT 'IN_APP',
    state VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_notification_deliveries PRIMARY KEY (id),
    CONSTRAINT uk_notification_delivery_occurrence_channel UNIQUE (occurrence_id, channel),
    CONSTRAINT fk_notification_deliveries_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT fk_notification_deliveries_occurrence FOREIGN KEY (occurrence_id) REFERENCES reminder_occurrences (id),
    CONSTRAINT ck_notification_deliveries_channel CHECK (channel = 'IN_APP'),
    CONSTRAINT ck_notification_deliveries_state CHECK (state IN ('AVAILABLE', 'DISMISSED'))
);

CREATE INDEX idx_notification_deliveries_recipient ON notification_deliveries (recipient_id, created_at);

CREATE TABLE audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    subject_id BIGINT,
    occurred_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_audit_events PRIMARY KEY (id),
    CONSTRAINT fk_audit_events_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_audit_events_user_time ON audit_events (user_id, occurred_at);
