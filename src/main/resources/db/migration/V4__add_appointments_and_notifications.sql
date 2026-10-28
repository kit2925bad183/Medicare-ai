CREATE TABLE doctors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    specialty VARCHAR(120) NOT NULL,
    clinic_name VARCHAR(150),
    clinic_address VARCHAR(255),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_doctors PRIMARY KEY (id)
);

CREATE INDEX idx_doctors_specialty ON doctors (specialty);

CREATE TABLE appointments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_at TIMESTAMP(6) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    version BIGINT,
    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES users (id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT ck_appointments_status CHECK (status IN ('BOOKED', 'CANCELLED', 'RESCHEDULED', 'COMPLETED'))
);

CREATE INDEX idx_appointments_patient ON appointments (patient_id, appointment_at);
CREATE INDEX idx_appointments_doctor ON appointments (doctor_id, appointment_at);

CREATE TABLE appointment_reminders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    due_at TIMESTAMP(6) NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(160) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_appointment_reminders PRIMARY KEY (id),
    CONSTRAINT uk_appointment_reminders_key UNIQUE (idempotency_key),
    CONSTRAINT fk_appointment_reminders_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT ck_appointment_reminders_state CHECK (state IN ('PENDING', 'AVAILABLE', 'CANCELLED'))
);

CREATE INDEX idx_appointment_reminders_due_state ON appointment_reminders (state, due_at);
