CREATE TABLE caregivers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    caregiver_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    read_only BOOLEAN NOT NULL DEFAULT TRUE,
    invited_at TIMESTAMP(6) NOT NULL,
    responded_at TIMESTAMP(6),
    revoked_at TIMESTAMP(6),
    CONSTRAINT pk_caregivers PRIMARY KEY (id),
    CONSTRAINT uk_caregiver_relation UNIQUE (patient_id, caregiver_id),
    CONSTRAINT fk_caregivers_patient FOREIGN KEY (patient_id) REFERENCES users (id),
    CONSTRAINT fk_caregivers_caregiver FOREIGN KEY (caregiver_id) REFERENCES users (id),
    CONSTRAINT ck_caregivers_status CHECK (status IN ('PENDING', 'ACTIVE', 'REVOKED'))
);

CREATE TABLE notification_preferences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    browser_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_start TIME,
    quiet_end TIME,
    time_zone VARCHAR(64),
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_notification_preferences PRIMARY KEY (id),
    CONSTRAINT uk_notification_preferences_user UNIQUE (user_id),
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE push_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    endpoint VARCHAR(512) NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    last_seen_at TIMESTAMP(6),
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_push_subscriptions PRIMARY KEY (id),
    CONSTRAINT fk_push_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_push_subscriptions_user ON push_subscriptions (user_id);

ALTER TABLE notification_deliveries ADD COLUMN attempt_count INT NOT NULL DEFAULT 0;
ALTER TABLE notification_deliveries ADD COLUMN next_retry_at TIMESTAMP(6);
ALTER TABLE notification_deliveries ADD COLUMN last_error_category VARCHAR(32);
ALTER TABLE notification_deliveries ADD COLUMN sent_at TIMESTAMP(6);

ALTER TABLE notification_deliveries MODIFY COLUMN state VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE';
ALTER TABLE notification_deliveries MODIFY COLUMN channel VARCHAR(16) NOT NULL DEFAULT 'IN_APP';

CREATE INDEX idx_notification_deliveries_retry ON notification_deliveries (state, next_retry_at);
