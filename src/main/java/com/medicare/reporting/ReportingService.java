package com.medicare.reporting;

import com.medicare.appointment.Appointment;
import com.medicare.appointment.AppointmentRepository;
import com.medicare.identity.User;
import com.medicare.identity.UserRepository;
import com.medicare.medicine.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReportingService {

    public static final String ADHERENCE_DISCLAIMER =
        "Adherence metrics are calculated exclusively from verified dose action events recorded by the user. "
            + "A generated occurrence or delivered reminder is not proof that medication was administered.";

    private final ReminderOccurrenceRepository occurrenceRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final DoseLogRepository doseLogRepository;
    private final MedicineScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditEventRepository auditRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ReportingService(ReminderOccurrenceRepository occurrenceRepository,
                            NotificationDeliveryRepository deliveryRepository,
                            DoseLogRepository doseLogRepository,
                            MedicineScheduleRepository scheduleRepository,
                            AppointmentRepository appointmentRepository,
                            AuditEventRepository auditRepository,
                            UserRepository userRepository,
                            Clock clock) {
        this.occurrenceRepository = occurrenceRepository;
        this.deliveryRepository = deliveryRepository;
        this.doseLogRepository = doseLogRepository;
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdherenceSummary getAdherenceSummary(Long userId) {
        Instant now = clock.instant();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        long sevenDaysTaken = doseLogRepository.countByUserIdAndActionAndActionTimeBetween(userId, "TAKEN", sevenDaysAgo, now);
        long sevenDaysTotal = doseLogRepository.countByUserIdAndActionTimeBetween(userId, sevenDaysAgo, now);
        double sevenDaysRate = sevenDaysTotal > 0 ? ((double) sevenDaysTaken / sevenDaysTotal) * 100.0 : 100.0;

        long thirtyDaysTaken = doseLogRepository.countByUserIdAndActionAndActionTimeBetween(userId, "TAKEN", thirtyDaysAgo, now);
        long thirtyDaysTotal = doseLogRepository.countByUserIdAndActionTimeBetween(userId, thirtyDaysAgo, now);
        double thirtyDaysRate = thirtyDaysTotal > 0 ? ((double) thirtyDaysTaken / thirtyDaysTotal) * 100.0 : 100.0;

        return new AdherenceSummary(
            Math.round(sevenDaysRate * 10.0) / 10.0,
            sevenDaysTaken,
            sevenDaysTotal,
            Math.round(thirtyDaysRate * 10.0) / 10.0,
            thirtyDaysTaken,
            thirtyDaysTotal,
            ADHERENCE_DISCLAIMER
        );
    }

    @Transactional(readOnly = true)
    public List<DoseLog> getRecentDoseLogs(Long userId) {
        Instant thirtyDaysAgo = clock.instant().minus(30, ChronoUnit.DAYS);
        return doseLogRepository.findAllByUserIdAndActionTimeBetweenOrderByActionTimeDesc(userId, thirtyDaysAgo, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<NotificationDelivery> getRecentDeliveries(Long userId) {
        return deliveryRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        Instant now = clock.instant();

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportTimestamp", now.toString());
        export.put("userProfile", Map.of(
            "email", user.getEmail(),
            "firstName", user.getFirstName(),
            "lastName", user.getLastName(),
            "role", user.getRole().name(),
            "timeZone", user.getTimeZone()
        ));

        List<MedicineSchedule> schedules = scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(userId);
        export.put("medicineSchedules", schedules.stream().map(s -> Map.of(
            "medicineName", s.getMedicineName(),
            "frequency", s.getFrequency().name(),
            "localTime", s.getLocalTime().toString(),
            "weekday", s.getWeekday() != null ? s.getWeekday() : "",
            "active", s.isActive()
        )).toList());

        List<Appointment> appointments = appointmentRepository.findAllByPatientIdOrderByAppointmentAtDesc(userId);
        export.put("appointments", appointments.stream().map(a -> Map.of(
            "doctor", a.getDoctor() != null ? a.getDoctor().getFullName() : "Doctor",
            "appointmentAt", a.getAppointmentAt().toString(),
            "status", a.getStatus(),
            "reason", a.getReason() != null ? a.getReason() : ""
        )).toList());

        List<DoseLog> doseLogs = doseLogRepository.findAllByUserIdAndActionTimeBetweenOrderByActionTimeDesc(
            userId, now.minus(90, ChronoUnit.DAYS), now);
        export.put("doseHistory", doseLogs.stream().map(d -> Map.of(
            "medicine", d.getOccurrence().getSchedule().getMedicineName(),
            "action", d.getAction(),
            "actionTime", d.getActionTime().toString(),
            "notes", d.getNotes() != null ? d.getNotes() : ""
        )).toList());

        export.put("adherenceMetrics", getAdherenceSummary(userId));
        return export;
    }

    public record AdherenceSummary(double sevenDayRate, long sevenDayTaken, long sevenDayTotal,
                                  double thirtyDayRate, long thirtyDayTaken, long thirtyDayTotal,
                                  String disclaimer) {
    }
}
