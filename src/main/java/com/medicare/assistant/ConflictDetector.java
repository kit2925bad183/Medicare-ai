package com.medicare.assistant;

import com.medicare.medicine.MedicineSchedule;
import com.medicare.medicine.MedicineScheduleRepository;
import com.medicare.medicine.ScheduleFrequency;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConflictDetector {

    private final MedicineScheduleRepository scheduleRepository;

    public ConflictDetector(MedicineScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public List<String> detectConflicts(Long userId) {
        List<MedicineSchedule> schedules = scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(userId)
            .stream()
            .filter(MedicineSchedule::isActive)
            .toList();

        List<String> findings = new ArrayList<>();

        if (schedules.isEmpty()) {
            return findings;
        }

        // Check for missing instructions
        for (MedicineSchedule s : schedules) {
            if (s.getInstructions() == null || s.getInstructions().isBlank()) {
                findings.add("Schedule for '" + s.getMedicineName() + "' at " + s.getLocalTime()
                    + " has no dosage/meal instructions specified.");
            }
            if (s.getFrequency() == ScheduleFrequency.WEEKLY && s.getWeekday() == null) {
                findings.add("Weekly schedule for '" + s.getMedicineName() + "' is incomplete: missing weekday.");
            }
        }

        // Check for time overlaps
        for (int i = 0; i < schedules.size(); i++) {
            for (int j = i + 1; j < schedules.size(); j++) {
                MedicineSchedule s1 = schedules.get(i);
                MedicineSchedule s2 = schedules.get(j);

                if (s1.getLocalTime().equals(s2.getLocalTime())) {
                    findings.add("Exact time clash: '" + s1.getMedicineName() + "' and '"
                        + s2.getMedicineName() + "' are both scheduled at " + s1.getLocalTime() + ".");
                } else {
                    long diffMinutes = Math.abs(ChronoUnit.MINUTES.between(s1.getLocalTime(), s2.getLocalTime()));
                    if (diffMinutes <= 15) {
                        findings.add("Close interval alert (" + diffMinutes + " min apart): '"
                            + s1.getMedicineName() + "' at " + s1.getLocalTime() + " and '"
                            + s2.getMedicineName() + "' at " + s2.getLocalTime() + ".");
                    }
                }
            }
        }

        return findings;
    }
}
