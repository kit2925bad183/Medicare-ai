package com.medicare.assistant;

import com.medicare.appointment.Appointment;
import com.medicare.appointment.AppointmentService;
import com.medicare.common.CurrentUserService;
import com.medicare.identity.User;
import com.medicare.medicine.*;
import com.medicare.notification.NotificationPreference;
import com.medicare.notification.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class RuleBasedAssistant {

    private static final String HELP =
        "Supported requests: "
            + "1. “show today's medicine schedule” "
            + "2. “summarize upcoming appointments” "
            + "3. “explain saved reminder settings” "
            + "4. “propose notification preferences” "
            + "5. “identify conflicting or incomplete schedules” "
            + "6. “plan daily reminder for <medicine name> at HH:mm” or “plan weekly reminder for <medicine name> on <weekday> at HH:mm”. "
            + "I am a deterministic assistant and only process these structured requests.";

    private final IntentParser intentParser;
    private final ConflictDetector conflictDetector;
    private final SchedulePlanner schedulePlanner;
    private final PlanValidator planValidator;
    private final ReminderService reminderService;
    private final AppointmentService appointmentService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final CurrentUserService currentUserService;
    private final AuditEventRepository auditRepository;

    public RuleBasedAssistant(IntentParser intentParser,
                              ConflictDetector conflictDetector,
                              SchedulePlanner schedulePlanner,
                              PlanValidator planValidator,
                              ReminderService reminderService,
                              AppointmentService appointmentService,
                              NotificationPreferenceRepository preferenceRepository,
                              CurrentUserService currentUserService,
                              AuditEventRepository auditRepository) {
        this.intentParser = intentParser;
        this.conflictDetector = conflictDetector;
        this.schedulePlanner = schedulePlanner;
        this.planValidator = planValidator;
        this.reminderService = reminderService;
        this.appointmentService = appointmentService;
        this.preferenceRepository = preferenceRepository;
        this.currentUserService = currentUserService;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public AssistantResult respond(String command) {
        IntentParser.ParsedIntent intent = intentParser.parse(command);
        User user = currentUserService.getCurrentUser();

        return switch (intent.type()) {
            case SHOW_TODAY_SCHEDULE -> handleTodaySchedule(user);
            case SUMMARIZE_APPOINTMENTS -> handleUpcomingAppointments(user);
            case EXPLAIN_REMINDER_SETTINGS -> handleExplainSettings(user);
            case PROPOSE_NOTIFICATION_PREFERENCES -> handleProposePreferences(user);
            case IDENTIFY_CONFLICTS -> handleIdentifyConflicts(user);
            case PREPARE_REMINDER_PLAN -> handlePreparePlan(intent, user);
            case UNKNOWN -> unsupported();
        };
    }

    private AssistantResult handleTodaySchedule(User user) {
        LocalDate today = reminderService.todayInUserTimeZone();
        ZoneId zone = ZoneId.of(user.getTimeZone());
        List<ReminderService.OccurrenceView> todaySchedule = reminderService.upcoming().stream()
            .filter(item -> item.dueAt().atZone(zone).toLocalDate().equals(today))
            .toList();

        String summary = todaySchedule.isEmpty()
            ? "There are no upcoming medicine reminders scheduled for today."
            : "Today's saved reminders: " + String.join(", ", todaySchedule.stream()
                .map(item -> item.medicineName() + " at " + item.dueAt().atZone(zone).toLocalTime())
                .toList()) + ".";

        recordAudit("ASSISTANT_SCHEDULE_SUMMARY");
        return new AssistantResult(summary, null);
    }

    private AssistantResult handleUpcomingAppointments(User user) {
        List<Appointment> myAppointments = appointmentService.mine().stream()
            .filter(a -> "BOOKED".equals(a.getStatus()))
            .toList();

        String summary = myAppointments.isEmpty()
            ? "You currently have no upcoming doctor appointments booked."
            : "Your upcoming appointments: " + String.join("; ", myAppointments.stream()
                .map(a -> (a.getDoctor() != null ? a.getDoctor().getFullName() + " (" + a.getDoctor().getSpecialty() + ")" : "Doctor")
                    + " on " + a.getAppointmentAt())
                .toList()) + ".";

        recordAudit("ASSISTANT_APPOINTMENTS_SUMMARY");
        return new AssistantResult(summary, null);
    }

    private AssistantResult handleExplainSettings(User user) {
        NotificationPreference pref = preferenceRepository.findByUserId(user.getId()).orElse(null);
        String emailStatus = pref != null && pref.isEmailEnabled() ? "Enabled" : "Disabled";
        String browserStatus = pref != null && pref.isBrowserEnabled() ? "Enabled (in active tab)" : "Disabled";
        String pushStatus = pref != null && pref.isPushEnabled() ? "Enabled" : "Disabled";
        String quietHours = (pref != null && pref.getQuietStart() != null && pref.getQuietEnd() != null)
            ? pref.getQuietStart() + " to " + pref.getQuietEnd() : "None configured";

        recordAudit("ASSISTANT_SETTINGS_EXPLAINED");
        return new AssistantResult("Your account time zone is " + user.getTimeZone()
            + ". Reminders are saved in that time zone and appear in the in-app notification center. "
            + "Email channel: " + emailStatus + ". "
            + "Browser alerts: " + browserStatus + ". "
            + "Web Push: " + pushStatus + ". "
            + "Quiet hours: " + quietHours + ".", null);
    }

    private AssistantResult handleProposePreferences(User user) {
        recordAudit("ASSISTANT_PREFERENCES_PROPOSED");
        return new AssistantResult("Proposed Notification Settings: "
            + "1. Enable Email Reminders for advance notice of medication doses. "
            + "2. Enable Browser In-App Notifications for immediate alerts while using MediCare AI. "
            + "3. Set Quiet Hours from 22:00 to 07:00 in " + user.getTimeZone() + " to prevent sleep disturbances. "
            + "To activate these changes, review and save them on your Settings page (/settings/notifications).", null);
    }

    private AssistantResult handleIdentifyConflicts(User user) {
        List<String> conflicts = conflictDetector.detectConflicts(user.getId());
        recordAudit("ASSISTANT_CONFLICTS_CHECKED");

        if (conflicts.isEmpty()) {
            return new AssistantResult("No conflicting, overlapping, or incomplete schedules were identified across your active reminders.", null);
        }

        String report = "Schedule Analysis Findings (" + conflicts.size() + "):\n- "
            + String.join("\n- ", conflicts)
            + "\nRecommended action: Review your schedule times on the Medicines page to ensure safe spacing between doses.";

        return new AssistantResult(report, null);
    }

    private AssistantResult handlePreparePlan(IntentParser.ParsedIntent intent, User user) {
        ProposedSchedule proposal = schedulePlanner.planFromIntent(intent, user);
        if (proposal == null) {
            return unsupported();
        }

        try {
            planValidator.validate(proposal, user);
        } catch (IllegalArgumentException ex) {
            return new AssistantResult("Cannot propose reminder plan: " + ex.getMessage(), null);
        }

        recordAudit("ASSISTANT_SCHEDULE_PROPOSED");
        return new AssistantResult(
            "Preview only — no reminder has been saved. Confirm this exact " + proposal.frequency().name().toLowerCase()
                + " reminder or cancel it.",
            proposal
        );
    }

    private AssistantResult unsupported() {
        return new AssistantResult(HELP, null);
    }

    private void recordAudit(String eventType) {
        auditRepository.save(new AuditEvent(currentUserService.getCurrentUser(), eventType, null));
    }

    public record AssistantResult(String message, ProposedSchedule proposal) {
    }

    public record ProposedSchedule(String medicineName, LocalTime localTime, LocalDate startDate,
                                    String timeZone, ScheduleFrequency frequency, Integer weekday) {
        public ProposedSchedule(String medicineName, LocalTime localTime, LocalDate startDate, String timeZone) {
            this(medicineName, localTime, startDate, timeZone, ScheduleFrequency.DAILY, null);
        }
    }
}
