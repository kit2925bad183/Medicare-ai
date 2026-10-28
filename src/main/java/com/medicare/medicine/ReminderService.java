package com.medicare.medicine;

import com.medicare.appointment.Appointment;
import com.medicare.appointment.AppointmentReminder;
import com.medicare.appointment.AppointmentReminderRepository;
import com.medicare.common.CurrentUserService;
import com.medicare.identity.User;
import com.medicare.notification.NotificationDispatcherService;
import com.medicare.notification.NotificationPreference;
import com.medicare.notification.NotificationPreferenceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReminderService {

    private final MedicineScheduleRepository scheduleRepository;
    private final ReminderOccurrenceRepository occurrenceRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final DoseLogRepository doseLogRepository;
    private final AppointmentReminderRepository appointmentReminderRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationDispatcherService dispatcherService;
    private final AuditEventRepository auditRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public ReminderService(MedicineScheduleRepository scheduleRepository,
                           ReminderOccurrenceRepository occurrenceRepository,
                           NotificationDeliveryRepository deliveryRepository,
                           DoseLogRepository doseLogRepository,
                           AppointmentReminderRepository appointmentReminderRepository,
                           NotificationPreferenceRepository preferenceRepository,
                           NotificationDispatcherService dispatcherService,
                           AuditEventRepository auditRepository,
                           CurrentUserService currentUserService,
                           Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.deliveryRepository = deliveryRepository;
        this.doseLogRepository = doseLogRepository;
        this.appointmentReminderRepository = appointmentReminderRepository;
        this.preferenceRepository = preferenceRepository;
        this.dispatcherService = dispatcherService;
        this.auditRepository = auditRepository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional
    public ScheduleView create(MedicineScheduleRequest request) {
        User user = currentUserService.getCurrentUser();
        ZoneId zone = zoneFor(user.getTimeZone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (request.getStartDate().isBefore(today)) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }
        if (request.getFrequency() == ScheduleFrequency.WEEKLY
            && (request.getWeekday() == null || request.getWeekday() < 1 || request.getWeekday() > 7)) {
            throw new IllegalArgumentException("Choose a weekday for a weekly reminder.");
        }
        if (request.getFrequency() == ScheduleFrequency.DAILY && request.getWeekday() != null) {
            throw new IllegalArgumentException("Daily reminders do not use a weekday.");
        }

        MedicineSchedule schedule = scheduleRepository.save(new MedicineSchedule(
            user,
            request.getMedicineName().trim(),
            blankToNull(request.getInstructions()),
            request.getFrequency(),
            request.getLocalTime(),
            request.getWeekday(),
            request.getStartDate(),
            zone.getId()
        ));
        auditRepository.save(new AuditEvent(user, "MEDICINE_SCHEDULE_CREATED", schedule.getId()));
        generateFor(schedule, clock.instant(), zone);
        return toView(schedule);
    }

    @Transactional
    public ScheduleView createFromAssistant(MedicineScheduleRequest request, String proposedTimeZone) {
        User user = currentUserService.getCurrentUser();
        if (!user.getTimeZone().equals(proposedTimeZone)) {
            throw new IllegalArgumentException("Your time zone changed. Preview the reminder again before confirming.");
        }
        ScheduleView saved = create(request);
        auditRepository.save(new AuditEvent(user, "ASSISTANT_SCHEDULE_CONFIRMED", saved.id()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ScheduleView> schedules() {
        Long userId = currentUserService.getCurrentUser().getId();
        return scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(userId).stream()
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public LocalDate todayInUserTimeZone() {
        return LocalDate.now(clock.withZone(zoneFor(currentUserService.getCurrentUser().getTimeZone())));
    }

    @Transactional(readOnly = true)
    public List<OccurrenceView> upcoming() {
        User user = currentUserService.getCurrentUser();
        Instant now = clock.instant();
        Instant horizon = now.plus(8, ChronoUnit.DAYS);
        return occurrenceRepository.findAllByScheduleOwnerIdAndDueAtBetweenOrderByDueAtAsc(
                user.getId(), now, horizon).stream()
            .filter(occurrence -> occurrence.getState() != OccurrenceState.CANCELLED)
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OccurrenceView> todayOccurrences() {
        User user = currentUserService.getCurrentUser();
        ZoneId zone = zoneFor(user.getTimeZone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        return occurrenceRepository.findAllByScheduleOwnerIdAndDueAtBetweenOrderByDueAtAsc(user.getId(), start, end)
            .stream()
            .filter(o -> o.getState() != OccurrenceState.CANCELLED)
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationView> notifications() {
        Long userId = currentUserService.getCurrentUser().getId();
        return deliveryRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(userId).stream()
            .map(delivery -> new NotificationView(
                delivery.getId(),
                delivery.getOccurrence() != null
                    ? delivery.getOccurrence().getSchedule().getMedicineName()
                    : (delivery.getAppointment() != null ? "Appointment" : "Reminder"),
                delivery.getOccurrence() != null ? delivery.getOccurrence().getDueAt() : delivery.getCreatedAt(),
                delivery.getOccurrence() != null ? delivery.getOccurrence().getTimeZone() : "UTC",
                delivery.getState()
            ))
            .toList();
    }

    @Transactional
    public void recordDoseAction(Long occurrenceId, String action, String notes) {
        User user = currentUserService.getCurrentUser();
        ReminderOccurrence occurrence = occurrenceRepository.findByIdAndScheduleOwnerId(occurrenceId, user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Reminder occurrence not found."));

        if (occurrence.getState() == OccurrenceState.CANCELLED) {
            throw new IllegalArgumentException("Cannot record dose action for a cancelled reminder.");
        }

        OccurrenceState newState = OccurrenceState.valueOf(action.toUpperCase());
        occurrence.setState(newState);
        doseLogRepository.save(new DoseLog(occurrence, user, newState.name(), clock.instant(), notes));
        auditRepository.save(new AuditEvent(user, "DOSE_" + newState.name(), occurrenceId));
    }

    @Transactional
    public void deactivate(long scheduleId) {
        User user = currentUserService.getCurrentUser();
        MedicineSchedule schedule = scheduleRepository.findByIdAndOwnerId(scheduleId, user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Medicine schedule not found."));
        schedule.setActive(false);
        occurrenceRepository.findAllByScheduleIdAndStateOrderByDueAtAsc(scheduleId, OccurrenceState.PENDING)
            .forEach(occurrence -> occurrence.setState(OccurrenceState.CANCELLED));
        auditRepository.save(new AuditEvent(user, "MEDICINE_SCHEDULE_CANCELLED", scheduleId));
    }

    @Transactional
    public void dismissNotification(long notificationId) {
        User user = currentUserService.getCurrentUser();
        NotificationDelivery delivery = deliveryRepository.findByIdAndRecipientId(notificationId, user.getId())
            .orElseThrow(() -> new EntityNotFoundException("Notification not found."));
        if (!"DISMISSED".equals(delivery.getState())) {
            delivery.setState("DISMISSED");
            auditRepository.save(new AuditEvent(user, "IN_APP_NOTIFICATION_DISMISSED", notificationId));
        }
    }

    @Transactional
    public void generateUpcomingOccurrences() {
        Instant now = clock.instant();
        for (MedicineSchedule schedule : scheduleRepository.findActiveForUpdate()) {
            ZoneId zone = zoneFor(schedule.getTimeZone());
            generateFor(schedule, now, zone);
        }
    }

    @Transactional
    public void publishDueInAppNotifications() {
        Instant now = clock.instant();
        List<ReminderOccurrence> due = occurrenceRepository.findDueForUpdate(
            OccurrenceState.PENDING, now, PageRequest.of(0, 100));

        List<NotificationDelivery> pendingDispatches = new ArrayList<>();

        for (ReminderOccurrence occurrence : due) {
            MedicineSchedule schedule = occurrence.getSchedule();
            if (!schedule.isActive()) {
                occurrence.setState(OccurrenceState.CANCELLED);
                continue;
            }

            User owner = schedule.getOwner();
            if (!deliveryRepository.existsByOccurrenceIdAndChannel(occurrence.getId(), "IN_APP")) {
                deliveryRepository.save(new NotificationDelivery(owner, occurrence));
            }

            NotificationPreference pref = preferenceRepository.findByUserId(owner.getId()).orElse(null);
            if (pref != null && pref.isEmailEnabled()
                && !deliveryRepository.existsByOccurrenceIdAndChannel(occurrence.getId(), "EMAIL")) {
                NotificationDelivery emailDelivery = new NotificationDelivery(owner, occurrence);
                emailDelivery.setChannel("EMAIL");
                emailDelivery.setState("PENDING");
                deliveryRepository.save(emailDelivery);
                pendingDispatches.add(emailDelivery);
            }

            if (pref != null && pref.isPushEnabled()
                && !deliveryRepository.existsByOccurrenceIdAndChannel(occurrence.getId(), "PUSH")) {
                NotificationDelivery pushDelivery = new NotificationDelivery(owner, occurrence);
                pushDelivery.setChannel("PUSH");
                pushDelivery.setState("PENDING");
                deliveryRepository.save(pushDelivery);
                pendingDispatches.add(pushDelivery);
            }

            occurrence.setState(OccurrenceState.AVAILABLE);
        }

        // Dispatch outside long lock where possible
        for (NotificationDelivery d : pendingDispatches) {
            dispatcherService.dispatch(d);
        }
    }

    @Transactional
    public void processDueAppointmentReminders() {
        Instant now = clock.instant();
        List<AppointmentReminder> due = appointmentReminderRepository.findDueForUpdate("PENDING", now);

        List<NotificationDelivery> pendingDispatches = new ArrayList<>();

        for (AppointmentReminder reminder : due) {
            Appointment appt = reminder.getAppointment();
            if ("CANCELLED".equals(appt.getStatus())) {
                reminder.setState("CANCELLED");
                continue;
            }

            User patient = appt.getPatient();
            if (!deliveryRepository.existsByAppointmentIdAndChannel(appt.getId(), "IN_APP")) {
                deliveryRepository.save(new NotificationDelivery(patient, appt));
            }

            NotificationPreference pref = preferenceRepository.findByUserId(patient.getId()).orElse(null);
            if (pref != null && pref.isEmailEnabled()
                && !deliveryRepository.existsByAppointmentIdAndChannel(appt.getId(), "EMAIL")) {
                NotificationDelivery emailDelivery = new NotificationDelivery(patient, appt);
                emailDelivery.setChannel("EMAIL");
                emailDelivery.setState("PENDING");
                deliveryRepository.save(emailDelivery);
                pendingDispatches.add(emailDelivery);
            }

            reminder.setState("AVAILABLE");
        }

        for (NotificationDelivery d : pendingDispatches) {
            dispatcherService.dispatch(d);
        }
    }

    @Transactional
    public void processPendingRetries() {
        Instant now = clock.instant();
        List<NotificationDelivery> retries = deliveryRepository.findPendingRetries(now, PageRequest.of(0, 50));
        for (NotificationDelivery d : retries) {
            dispatcherService.dispatch(d);
        }
    }

    private void generateFor(MedicineSchedule schedule, Instant now, ZoneId zone) {
        LocalDate localToday = now.atZone(zone).toLocalDate();
        LocalDate firstDate = schedule.getStartDate().isAfter(localToday)
            ? schedule.getStartDate() : localToday;
        for (int day = 0; day < 8; day++) {
            LocalDate date = firstDate.plusDays(day);
            if (schedule.getFrequency() == ScheduleFrequency.WEEKLY
                && date.getDayOfWeek().getValue() != schedule.getWeekday()) {
                continue;
            }

            ZonedDateTime due = date.atTime(schedule.getLocalTime()).atZone(zone);
            Instant dueAt = due.toInstant();
            if (dueAt.isBefore(now)) {
                continue;
            }
            String key = "medicine:" + schedule.getId() + ":" + date;
            if (!occurrenceRepository.existsByIdempotencyKey(key)) {
                occurrenceRepository.save(new ReminderOccurrence(schedule, dueAt, key));
            }
        }
    }

    private ZoneId zoneFor(String timeZone) {
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException ex) {
            throw new IllegalStateException("The account has an invalid time zone; update it in settings.", ex);
        }
    }

    private ScheduleView toView(MedicineSchedule schedule) {
        return new ScheduleView(schedule.getId(), schedule.getMedicineName(), schedule.getInstructions(),
            schedule.getFrequency().name(), schedule.getLocalTime(), schedule.getWeekday(),
            schedule.getStartDate(), schedule.getTimeZone(), schedule.isActive());
    }

    private OccurrenceView toView(ReminderOccurrence occurrence) {
        return new OccurrenceView(occurrence.getId(), occurrence.getSchedule().getMedicineName(),
            occurrence.getSchedule().getInstructions(), occurrence.getDueAt(), occurrence.getTimeZone(),
            occurrence.getState().name());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record ScheduleView(Long id, String medicineName, String instructions, String frequency,
                               java.time.LocalTime localTime, Integer weekday, LocalDate startDate,
                               String timeZone, boolean active) {
    }

    public record OccurrenceView(Long id, String medicineName, String instructions, Instant dueAt,
                                 String timeZone, String state) {
    }

    public record NotificationView(Long id, String medicineName, Instant dueAt,
                                   String timeZone, String state) {
    }
}
