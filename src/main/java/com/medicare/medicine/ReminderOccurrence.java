package com.medicare.medicine;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reminder_occurrences",
    uniqueConstraints = @UniqueConstraint(name = "uk_reminder_occurrences_key", columnNames = "idempotency_key"),
    indexes = @Index(name = "idx_reminder_occurrences_due_state", columnList = "state,due_at"))
public class ReminderOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private MedicineSchedule schedule;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OccurrenceState state = OccurrenceState.PENDING;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 160)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReminderOccurrence() {
    }

    public ReminderOccurrence(MedicineSchedule schedule, Instant dueAt, String idempotencyKey) {
        this.schedule = schedule;
        this.dueAt = dueAt;
        this.timeZone = schedule.getTimeZone();
        this.idempotencyKey = idempotencyKey;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public MedicineSchedule getSchedule() { return schedule; }
    public Instant getDueAt() { return dueAt; }
    public String getTimeZone() { return timeZone; }
    public OccurrenceState getState() { return state; }
    public void setState(OccurrenceState state) { this.state = state; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
