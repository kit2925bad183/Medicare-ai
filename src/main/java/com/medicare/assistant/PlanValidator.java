package com.medicare.assistant;

import com.medicare.identity.User;
import com.medicare.medicine.MedicineScheduleRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class PlanValidator {

    private final MedicineScheduleRepository scheduleRepository;

    public PlanValidator(MedicineScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public void validate(RuleBasedAssistant.ProposedSchedule proposal, User user) {
        if (proposal == null) {
            throw new IllegalArgumentException("No proposal to validate.");
        }
        if (proposal.medicineName() == null || proposal.medicineName().isBlank()) {
            throw new IllegalArgumentException("Medicine name cannot be empty.");
        }
        if (proposal.medicineName().length() > 120) {
            throw new IllegalArgumentException("Medicine name cannot exceed 120 characters.");
        }
        if (proposal.localTime() == null) {
            throw new IllegalArgumentException("Reminder time is required.");
        }
        if (proposal.startDate() == null) {
            throw new IllegalArgumentException("Start date is required.");
        }

        ZoneId userZone = ZoneId.of(user.getTimeZone());
        LocalDate today = LocalDate.now(userZone);
        if (proposal.startDate().isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }

        // Check if user already has an active schedule for the exact same medicine and time
        boolean duplicateExists = scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(user.getId()).stream()
            .anyMatch(s -> s.isActive()
                && s.getMedicineName().equalsIgnoreCase(proposal.medicineName())
                && s.getLocalTime().equals(proposal.localTime()));

        if (duplicateExists) {
            throw new IllegalArgumentException("An active reminder for '" + proposal.medicineName()
                + "' at " + proposal.localTime() + " already exists in your schedule.");
        }
    }
}
