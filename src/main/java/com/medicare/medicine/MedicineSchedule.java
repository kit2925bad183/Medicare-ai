package com.medicare.medicine;

import com.medicare.identity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "medicine_schedules", indexes = {
    @Index(name = "idx_medicine_schedules_owner", columnList = "owner_id")
})
public class MedicineSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "medicine_name", nullable = false, length = 120)
    private String medicineName;

    @Column(length = 500)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ScheduleFrequency frequency;

    @Column(name = "local_time", nullable = false)
    private LocalTime localTime;

    @Column(name = "weekday")
    private Integer weekday;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public MedicineSchedule() {
    }

    public MedicineSchedule(User owner, String medicineName, String instructions,
                            ScheduleFrequency frequency, LocalTime localTime,
                            Integer weekday, LocalDate startDate, String timeZone) {
        this.owner = owner;
        this.medicineName = medicineName;
        this.instructions = instructions;
        this.frequency = frequency;
        this.localTime = localTime;
        this.weekday = weekday;
        this.startDate = startDate;
        this.timeZone = timeZone;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public ScheduleFrequency getFrequency() { return frequency; }
    public void setFrequency(ScheduleFrequency frequency) { this.frequency = frequency; }
    public LocalTime getLocalTime() { return localTime; }
    public void setLocalTime(LocalTime localTime) { this.localTime = localTime; }
    public Integer getWeekday() { return weekday; }
    public void setWeekday(Integer weekday) { this.weekday = weekday; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
