package com.medicare.medicine;

import com.medicare.identity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_events", indexes = @Index(name = "idx_audit_events_user_time", columnList = "user_id,occurred_at"))
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(User user, String eventType, Long subjectId) {
        this.user = user;
        this.eventType = eventType;
        this.subjectId = subjectId;
    }

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getEventType() { return eventType; }
    public Long getSubjectId() { return subjectId; }
    public Instant getOccurredAt() { return occurredAt; }
}
