package com.medicare.web;

import com.medicare.appointment.AppointmentService;
import com.medicare.common.CurrentUserService;
import com.medicare.identity.User;
import com.medicare.medicine.DoseLogRepository;
import com.medicare.medicine.OccurrenceState;
import com.medicare.medicine.ReminderService;
import com.medicare.notification.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardApiController {

    private final ReminderService reminderService;
    private final AppointmentService appointmentService;
    private final NotificationService notificationService;
    private final DoseLogRepository doseLogRepository;
    private final CurrentUserService currentUserService;

    public DashboardApiController(ReminderService reminderService,
                                  AppointmentService appointmentService,
                                  NotificationService notificationService,
                                  DoseLogRepository doseLogRepository,
                                  CurrentUserService currentUserService) {
        this.reminderService = reminderService;
        this.appointmentService = appointmentService;
        this.notificationService = notificationService;
        this.doseLogRepository = doseLogRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse summary() {
        User user = currentUserService.getCurrentUser();
        var upcoming = reminderService.upcoming();
        var appointments = appointmentService.mine();
        var notifications = reminderService.notifications();
        var todayDoses = reminderService.todayOccurrences();

        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long takenCount = doseLogRepository.countByUserIdAndActionAndActionTimeBetween(user.getId(), "TAKEN", thirtyDaysAgo, Instant.now());
        long totalDoses = doseLogRepository.countByUserIdAndActionTimeBetween(user.getId(), thirtyDaysAgo, Instant.now());
        double adherenceRate = totalDoses > 0 ? ((double) takenCount / totalDoses) * 100.0 : 100.0;

        return new DashboardSummaryResponse(
            upcoming.size(),
            (int) appointments.stream().filter(a -> "BOOKED".equals(a.getStatus())).count(),
            notifications.size(),
            todayDoses.size(),
            Math.round(adherenceRate * 10.0) / 10.0,
            "Adherence is calculated solely from recorded dose actions. A delivered reminder does not prove a dose was taken."
        );
    }

    @GetMapping("/doses/today")
    public List<ReminderService.OccurrenceView> todayDoses() {
        return reminderService.todayOccurrences();
    }

    @PostMapping("/doses/{id}/actions")
    public ResponseEntity<DoseActionResponse> recordAction(@PathVariable("id") Long id,
                                                           @Valid @RequestBody DoseActionRequest request) {
        reminderService.recordDoseAction(id, request.action(), request.notes());
        return ResponseEntity.ok(new DoseActionResponse(id, request.action().toUpperCase(), Instant.now()));
    }

    @PostMapping("/push/subscribe")
    public ResponseEntity<Void> subscribePush(@Valid @RequestBody PushSubscriptionRequest request) {
        notificationService.subscribe(request.endpoint(), request.p256dh(), request.auth());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/push/unsubscribe")
    public ResponseEntity<Void> unsubscribePush(@Valid @RequestBody PushUnsubscribeRequest request) {
        notificationService.unsubscribe(request.endpoint());
        return ResponseEntity.ok().build();
    }

    public record DashboardSummaryResponse(int upcomingRemindersCount, int bookedAppointmentsCount,
                                          int notificationsCount, int todayDosesCount,
                                          double thirtyDayAdherenceRate, String adherenceDisclaimer) {
    }

    public record DoseActionRequest(@NotBlank String action, String notes) {
    }

    public record DoseActionResponse(Long occurrenceId, String state, Instant recordedAt) {
    }

    public record PushSubscriptionRequest(@NotBlank String endpoint, @NotBlank String p256dh, @NotBlank String auth) {
    }

    public record PushUnsubscribeRequest(@NotBlank String endpoint) {
    }
}
