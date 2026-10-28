package com.medicare.medicine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class MedicineScheduleRequest {

    @NotBlank
    @Size(max = 120)
    private String medicineName;

    @Size(max = 500)
    private String instructions;

    @NotNull
    private ScheduleFrequency frequency = ScheduleFrequency.DAILY;

    @NotNull
    private LocalTime localTime;

    private Integer weekday;

    @NotNull
    private LocalDate startDate;

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
}
