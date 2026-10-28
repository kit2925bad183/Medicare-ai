package com.medicare.notification;

import com.medicare.identity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = @UniqueConstraint(name = "uk_notification_preferences_user", columnNames = "user_id"))
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = false;

    @Column(name = "browser_enabled", nullable = false)
    private boolean browserEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = false;

    @Column(name = "quiet_start")
    private LocalTime quietStart;

    @Column(name = "quiet_end")
    private LocalTime quietEnd;

    @Column(name = "time_zone", length = 64)
    private String timeZone;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationPreference() {
    }

    public NotificationPreference(User user) {
        this.user = user;
    }

    @PrePersist
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public boolean isBrowserEnabled() { return browserEnabled; }
    public void setBrowserEnabled(boolean browserEnabled) { this.browserEnabled = browserEnabled; }
    public boolean isPushEnabled() { return pushEnabled; }
    public void setPushEnabled(boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    public LocalTime getQuietStart() { return quietStart; }
    public void setQuietStart(LocalTime quietStart) { this.quietStart = quietStart; }
    public LocalTime getQuietEnd() { return quietEnd; }
    public void setQuietEnd(LocalTime quietEnd) { this.quietEnd = quietEnd; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public Instant getUpdatedAt() { return updatedAt; }
}
