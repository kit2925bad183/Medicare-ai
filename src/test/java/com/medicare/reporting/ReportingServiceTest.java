package com.medicare.reporting;

import com.medicare.appointment.AppointmentRepository;
import com.medicare.identity.Role;
import com.medicare.identity.User;
import com.medicare.identity.UserRepository;
import com.medicare.medicine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private ReminderOccurrenceRepository occurrenceRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private DoseLogRepository doseLogRepository;

    @Mock
    private MedicineScheduleRepository scheduleRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AuditEventRepository auditRepository;

    @Mock
    private UserRepository userRepository;

    private Clock clock;
    private ReportingService reportingService;
    private User testUser;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneId.of("UTC"));
        reportingService = new ReportingService(
            occurrenceRepository, deliveryRepository, doseLogRepository,
            scheduleRepository, appointmentRepository, auditRepository,
            userRepository, clock
        );

        testUser = new User("patient@example.com", "hash", "Bob", "Smith", Role.PATIENT);
        testUser.setId(42L);
        testUser.setTimeZone("America/New_York");
    }

    @Test
    void testAdherenceRateDerivedStrictlyFromRecordedDoseActions() {
        Instant now = clock.instant();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        // 7 days: 6 taken out of 8 total recorded actions = 75.0%
        when(doseLogRepository.countByUserIdAndActionAndActionTimeBetween(42L, "TAKEN", sevenDaysAgo, now)).thenReturn(6L);
        when(doseLogRepository.countByUserIdAndActionTimeBetween(42L, sevenDaysAgo, now)).thenReturn(8L);

        // 30 days: 27 taken out of 30 total recorded actions = 90.0%
        when(doseLogRepository.countByUserIdAndActionAndActionTimeBetween(42L, "TAKEN", thirtyDaysAgo, now)).thenReturn(27L);
        when(doseLogRepository.countByUserIdAndActionTimeBetween(42L, thirtyDaysAgo, now)).thenReturn(30L);

        ReportingService.AdherenceSummary summary = reportingService.getAdherenceSummary(42L);

        assertEquals(75.0, summary.sevenDayRate());
        assertEquals(6, summary.sevenDayTaken());
        assertEquals(8, summary.sevenDayTotal());

        assertEquals(90.0, summary.thirtyDayRate());
        assertEquals(27, summary.thirtyDayTaken());
        assertEquals(30, summary.thirtyDayTotal());

        // Must include mandatory disclaimer distinguishing reminder delivery from dose adherence
        assertNotNull(summary.disclaimer());
        assertTrue(summary.disclaimer().contains("not proof that medication was administered"));
    }

    @Test
    void testAdherenceRateDefaultsTo100WhenNoActionsRecordedYet() {
        Instant now = clock.instant();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        when(doseLogRepository.countByUserIdAndActionAndActionTimeBetween(42L, "TAKEN", sevenDaysAgo, now)).thenReturn(0L);
        when(doseLogRepository.countByUserIdAndActionTimeBetween(42L, sevenDaysAgo, now)).thenReturn(0L);
        when(doseLogRepository.countByUserIdAndActionAndActionTimeBetween(42L, "TAKEN", thirtyDaysAgo, now)).thenReturn(0L);
        when(doseLogRepository.countByUserIdAndActionTimeBetween(42L, thirtyDaysAgo, now)).thenReturn(0L);

        ReportingService.AdherenceSummary summary = reportingService.getAdherenceSummary(42L);

        assertEquals(100.0, summary.sevenDayRate());
        assertEquals(100.0, summary.thirtyDayRate());
    }

    @Test
    void testExportUserDataContainsOnlyTargetUserData() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(testUser));
        when(scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(42L)).thenReturn(List.of());
        when(appointmentRepository.findAllByPatientIdOrderByAppointmentAtDesc(42L)).thenReturn(List.of());
        when(doseLogRepository.findAllByUserIdAndActionTimeBetweenOrderByActionTimeDesc(eq(42L), any(), any())).thenReturn(List.of());

        Map<String, Object> export = reportingService.exportUserData(42L);

        assertNotNull(export);
        assertEquals("2026-11-01T10:00:00Z", export.get("exportTimestamp"));

        @SuppressWarnings("unchecked")
        Map<String, String> userProfile = (Map<String, String>) export.get("userProfile");
        assertEquals("patient@example.com", userProfile.get("email"));
        assertEquals("Bob", userProfile.get("firstName"));
        assertEquals("America/New_York", userProfile.get("timeZone"));

        assertTrue(export.containsKey("medicineSchedules"));
        assertTrue(export.containsKey("appointments"));
        assertTrue(export.containsKey("doseHistory"));
        assertTrue(export.containsKey("adherenceMetrics"));
    }
}
