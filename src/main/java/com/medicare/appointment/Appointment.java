package com.medicare.appointment;

import com.medicare.identity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointments_patient", columnList = "patient_id,appointment_at"),
    @Index(name = "idx_appointments_doctor", columnList = "doctor_id,appointment_at")
})
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "appointment_at", nullable = false)
    private Instant appointmentAt;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status = "BOOKED";

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Appointment() {
    }

    public Appointment(User patient, Doctor doctor, Instant appointmentAt, String timeZone, String reason) {
        this.patient = patient;
        this.doctor = doctor;
        this.appointmentAt = appointmentAt;
        this.timeZone = timeZone;
        this.reason = reason;
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
    public void setId(Long id) { this.id = id; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public Instant getAppointmentAt() { return appointmentAt; }
    public void setAppointmentAt(Instant appointmentAt) { this.appointmentAt = appointmentAt; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
