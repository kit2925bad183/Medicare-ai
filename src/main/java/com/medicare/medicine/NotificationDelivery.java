package com.medicare.medicine;

import com.medicare.appointment.Appointment;
import com.medicare.identity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notification_deliveries",
    indexes = {
        @Index(name = "idx_notification_deliveries_recipient", columnList = "recipient_id,created_at"),
        @Index(name = "idx_notification_deliveries_retry", columnList = "state,next_retry_at")
    })
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occurrence_id")
    private ReminderOccurrence occurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(nullable = false, length = 16)
    private String channel = "IN_APP";

    @Column(nullable = false, length = 32)
    private String state = "AVAILABLE";

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "last_error_category", length = 32)
    private String lastErrorCategory;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationDelivery() {
    }

    public NotificationDelivery(User recipient, ReminderOccurrence occurrence) {
        this.recipient = recipient;
        this.occurrence = occurrence;
    }

    public NotificationDelivery(User recipient, String channel, String state) {
        this.recipient = recipient;
        this.channel = channel != null ? channel : "IN_APP";
        this.state = state != null ? state : "AVAILABLE";
    }

    public NotificationDelivery(User recipient, ReminderOccurrence occurrence, String channel, String state) {
        this.recipient = recipient;
        this.occurrence = occurrence;
        this.channel = channel != null ? channel : "IN_APP";
        this.state = state != null ? state : "AVAILABLE";
    }

    public NotificationDelivery(User recipient, Appointment appointment) {
        this.recipient = recipient;
        this.appointment = appointment;
    }

    public NotificationDelivery(User recipient, Appointment appointment, String channel, String state) {
        this.recipient = recipient;
        this.appointment = appointment;
        this.channel = channel != null ? channel : "IN_APP";
        this.state = state != null ? state : "AVAILABLE";
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getRecipient() { return recipient; }
    public ReminderOccurrence getOccurrence() { return occurrence; }
    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getLastErrorCategory() { return lastErrorCategory; }
    public void setLastErrorCategory(String lastErrorCategory) { this.lastErrorCategory = lastErrorCategory; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getCreatedAt() { return createdAt; }
}
