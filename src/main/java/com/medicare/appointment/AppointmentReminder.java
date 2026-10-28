package com.medicare.appointment;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "appointment_reminders", uniqueConstraints = @UniqueConstraint(name = "uk_appointment_reminders_key", columnNames = "idempotency_key"),
    indexes = @Index(name = "idx_appointment_reminders_due_state", columnList = "state,due_at"))
public class AppointmentReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(nullable = false, length = 16)
    private String state = "PENDING";

    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppointmentReminder() {
    }

    public AppointmentReminder(Appointment appointment, Instant dueAt, String idempotencyKey) {
        this.appointment = appointment;
        this.dueAt = dueAt;
        this.idempotencyKey = idempotencyKey;
        this.timeZone = appointment.getTimeZone();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Appointment getAppointment() { return appointment; }
    public Instant getDueAt() { return dueAt; }
    public String getTimeZone() { return timeZone; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
