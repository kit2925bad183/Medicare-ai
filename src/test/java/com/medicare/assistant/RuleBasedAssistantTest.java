package com.medicare.assistant;

import com.medicare.appointment.Appointment;
import com.medicare.appointment.AppointmentService;
import com.medicare.appointment.Doctor;
import com.medicare.common.CurrentUserService;
import com.medicare.identity.Role;
import com.medicare.identity.User;
import com.medicare.medicine.*;
import com.medicare.notification.NotificationPreference;
import com.medicare.notification.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleBasedAssistantTest {

    @Mock
    private ReminderService reminderService;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditEventRepository auditRepository;

    @Mock
    private MedicineScheduleRepository scheduleRepository;

    private IntentParser intentParser;
    private ConflictDetector conflictDetector;
    private SchedulePlanner schedulePlanner;
    private PlanValidator planValidator;
    private RuleBasedAssistant assistant;
    private User patient;

    @BeforeEach
    void setUp() {
        intentParser = new IntentParser();
        conflictDetector = new ConflictDetector(scheduleRepository);
        planValidator = new PlanValidator(scheduleRepository);
        schedulePlanner = new SchedulePlanner(reminderService);

        assistant = new RuleBasedAssistant(
            intentParser, conflictDetector, schedulePlanner, planValidator,
            reminderService, appointmentService, preferenceRepository,
            currentUserService, auditRepository
        );

        patient = new User("patient@test.local", "hash", "Jane", "Doe", Role.PATIENT);
        patient.setId(101L);
        patient.setTimeZone("UTC");
    }

    @Test
    void testShowTodayScheduleIntent() {
        when(currentUserService.getCurrentUser()).thenReturn(patient);
        when(reminderService.todayInUserTimeZone()).thenReturn(LocalDate.of(2026, 11, 1));

        Instant dueInstant = LocalDate.of(2026, 11, 1).atTime(8, 0).atZone(ZoneId.of("UTC")).toInstant();
        ReminderService.OccurrenceView item = new ReminderService.OccurrenceView(
            1L, "Amoxicillin", "Take with meal", dueInstant, "UTC", "AVAILABLE"
        );
        when(reminderService.upcoming()).thenReturn(List.of(item));

        RuleBasedAssistant.AssistantResult result = assistant.respond("show today schedule");

        assertNotNull(result);
        assertTrue(result.message().contains("Amoxicillin"));
        assertTrue(result.message().contains("08:00"));
        assertNull(result.proposal());
        verify(auditRepository).save(argThat(e -> "ASSISTANT_SCHEDULE_SUMMARY".equals(e.getEventType())));
    }

    @Test
    void testSummarizeAppointmentsIntent() {
        when(currentUserService.getCurrentUser()).thenReturn(patient);

        Doctor doc = new Doctor("Dr. Robert Smith", "Cardiology");
        Appointment appt = new Appointment(patient, doc, Instant.parse("2026-11-05T14:00:00Z"), "UTC", "Cardiac review");
        when(appointmentService.mine()).thenReturn(List.of(appt));

        RuleBasedAssistant.AssistantResult result = assistant.respond("upcoming appointments");

        assertNotNull(result);
        assertTrue(result.message().contains("Dr. Robert Smith"));
        assertTrue(result.message().contains("Cardiology"));
        assertNull(result.proposal());
        verify(auditRepository).save(argThat(e -> "ASSISTANT_APPOINTMENTS_SUMMARY".equals(e.getEventType())));
    }

    @Test
    void testExplainSettingsIntent() {
        when(currentUserService.getCurrentUser()).thenReturn(patient);

        NotificationPreference pref = new NotificationPreference(patient);
        pref.setEmailEnabled(true);
        pref.setBrowserEnabled(false);
        pref.setPushEnabled(true);
        pref.setQuietStart(LocalTime.of(22, 0));
        pref.setQuietEnd(LocalTime.of(7, 0));
        when(preferenceRepository.findByUserId(patient.getId())).thenReturn(Optional.of(pref));

        RuleBasedAssistant.AssistantResult result = assistant.respond("explain saved reminder settings");

        assertNotNull(result);
        assertTrue(result.message().contains("Email channel: Enabled"));
        assertTrue(result.message().contains("Quiet hours: 22:00 to 07:00"));
        assertNull(result.proposal());
        verify(auditRepository).save(argThat(e -> "ASSISTANT_SETTINGS_EXPLAINED".equals(e.getEventType())));
    }

    @Test
    void testProposePreferencesIntent() {
        when(currentUserService.getCurrentUser()).thenReturn(patient);

        RuleBasedAssistant.AssistantResult result = assistant.respond("propose notification preferences");

        assertNotNull(result);
        assertTrue(result.message().contains("22:00 to 07:00"));
        assertNull(result.proposal());
        verify(auditRepository).save(argThat(e -> "ASSISTANT_PREFERENCES_PROPOSED".equals(e.getEventType())));
    }

    @Test
    void testIdentifyConflictsIntent() {
        when(currentUserService.getCurrentUser()).thenReturn(patient);

        MedicineSchedule s1 = new MedicineSchedule(patient, "Metformin", "take with food", ScheduleFrequency.DAILY,
            LocalTime.of(8, 0), null, LocalDate.now(), "UTC");
        MedicineSchedule s2 = new MedicineSchedule(patient, "Lisinopril", "morning", ScheduleFrequency.DAILY,
            LocalTime.of(8, 10), null, LocalDate.now(), "UTC");

        when(scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(patient.getId())).thenReturn(List.of(s1, s2));

        RuleBasedAssistant.AssistantResult result = assistant.respond("check schedule conflicts");

        assertNotNull(result);
        assertTrue(result.message().contains("Close interval alert") || result.message().contains("within 15 minutes") || result.message().contains("min apart"));
        assertNull(result.proposal());
        verify(auditRepository).save(argThat(e -> "ASSISTANT_CONFLICTS_CHECKED".equals(e.getEventType())));
    }

    @Test
    void testPrepareReminderPlanProducesStructuredProposalWithoutPersisting() {
        when(currentUserService.getCurrentUser()).thenReturn(patient);
        when(reminderService.todayInUserTimeZone()).thenReturn(LocalDate.of(2026, 11, 1));
        when(scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(patient.getId())).thenReturn(List.of());

        RuleBasedAssistant.AssistantResult result = assistant.respond("plan daily reminder for Atorvastatin at 09:30");

        assertNotNull(result);
        assertNotNull(result.proposal());
        assertEquals("Atorvastatin", result.proposal().medicineName());
        assertEquals(LocalTime.of(9, 30), result.proposal().localTime());
        assertEquals(ScheduleFrequency.DAILY, result.proposal().frequency());
        assertTrue(result.message().contains("Preview only — no reminder has been saved"));
        verify(auditRepository).save(argThat(e -> "ASSISTANT_SCHEDULE_PROPOSED".equals(e.getEventType())));
    }

    @Test
    void testAmbiguousCommandReturnsClearHelpGuidance() {
        RuleBasedAssistant.AssistantResult result = assistant.respond("diagnose my persistent headache and prescribe pills");

        assertNotNull(result);
        assertNull(result.proposal());
        assertTrue(result.message().contains("Supported requests:"));
        assertTrue(result.message().contains("deterministic assistant"));
        verifyNoInteractions(auditRepository);
    }

    @Test
    void testPlanValidatorRejectsBlankMedicineName() {
        RuleBasedAssistant.ProposedSchedule invalid = new RuleBasedAssistant.ProposedSchedule(
            "", LocalTime.of(9, 0), LocalDate.now(), "UTC"
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> planValidator.validate(invalid, patient));
        assertEquals("Medicine name cannot be empty.", ex.getMessage());
    }

    @Test
    void testPlanValidatorRejectsPastStartDate() {
        RuleBasedAssistant.ProposedSchedule past = new RuleBasedAssistant.ProposedSchedule(
            "Aspirin", LocalTime.of(9, 0), LocalDate.now().minusDays(2), "UTC"
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> planValidator.validate(past, patient));
        assertEquals("Start date cannot be in the past.", ex.getMessage());
    }
}
