package com.medicare.medicine;

import com.medicare.identity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "dose_logs", indexes = {
    @Index(name = "idx_dose_logs_user_time", columnList = "user_id, action_time"),
    @Index(name = "idx_dose_logs_occurrence", columnList = "occurrence_id")
})
public class DoseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "occurrence_id", nullable = false)
    private ReminderOccurrence occurrence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "action_time", nullable = false)
    private Instant actionTime;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DoseLog() {
    }

    public DoseLog(ReminderOccurrence occurrence, User user, String action, Instant actionTime, String notes) {
        this.occurrence = occurrence;
        this.user = user;
        this.action = action;
        this.actionTime = actionTime;
        this.notes = notes;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (actionTime == null) {
            actionTime = Instant.now();
        }
    }

    public Long getId() { return id; }
    public ReminderOccurrence getOccurrence() { return occurrence; }
    public User getUser() { return user; }
    public String getAction() { return action; }
    public Instant getActionTime() { return actionTime; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}
