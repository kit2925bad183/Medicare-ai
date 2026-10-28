CREATE TABLE dose_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    occurrence_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    action_time TIMESTAMP(6) NOT NULL,
    notes VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_dose_logs PRIMARY KEY (id),
    CONSTRAINT fk_dose_logs_occurrence FOREIGN KEY (occurrence_id) REFERENCES reminder_occurrences (id),
    CONSTRAINT fk_dose_logs_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_dose_logs_action CHECK (action IN ('TAKEN', 'SKIPPED', 'MISSED'))
);

CREATE INDEX idx_dose_logs_user_time ON dose_logs (user_id, action_time);
CREATE INDEX idx_dose_logs_occurrence ON dose_logs (occurrence_id);

ALTER TABLE reminder_occurrences DROP CONSTRAINT ck_reminder_occurrences_state;
ALTER TABLE reminder_occurrences ADD CONSTRAINT ck_reminder_occurrences_state CHECK (state IN ('PENDING', 'AVAILABLE', 'CANCELLED', 'TAKEN', 'SKIPPED', 'MISSED'));

ALTER TABLE notification_deliveries DROP CONSTRAINT ck_notification_deliveries_channel;
ALTER TABLE notification_deliveries DROP CONSTRAINT ck_notification_deliveries_state;
ALTER TABLE notification_deliveries ADD CONSTRAINT ck_notification_deliveries_channel CHECK (channel IN ('IN_APP', 'EMAIL', 'BROWSER', 'PUSH'));
ALTER TABLE notification_deliveries ADD CONSTRAINT ck_notification_deliveries_state CHECK (state IN ('AVAILABLE', 'DISMISSED', 'PENDING', 'SENT', 'FAILED', 'RETRYING'));

ALTER TABLE notification_deliveries ADD COLUMN appointment_id BIGINT;
ALTER TABLE notification_deliveries ADD CONSTRAINT fk_notification_deliveries_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id);
