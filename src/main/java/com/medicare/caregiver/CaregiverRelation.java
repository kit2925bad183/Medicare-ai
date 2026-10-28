package com.medicare.caregiver;

import com.medicare.identity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "caregivers", uniqueConstraints = @UniqueConstraint(name = "uk_caregiver_relation", columnNames = {"patient_id", "caregiver_id"}))
public class CaregiverRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_id", nullable = false)
    private User caregiver;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "read_only", nullable = false)
    private boolean readOnly = true;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public CaregiverRelation() {
    }

    public CaregiverRelation(User patient, User caregiver) {
        this.patient = patient;
        this.caregiver = caregiver;
    }

    @PrePersist
    void onCreate() {
        if (invitedAt == null) {
            invitedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getPatient() { return patient; }
    public User getCaregiver() { return caregiver; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isReadOnly() { return readOnly; }
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
    public Instant getInvitedAt() { return invitedAt; }
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
}
