package com.medicare.caregiver;

import com.medicare.appointment.Appointment;
import com.medicare.appointment.AppointmentRepository;
import com.medicare.common.CurrentUserService;
import com.medicare.identity.Role;
import com.medicare.identity.User;
import com.medicare.identity.UserRepository;
import com.medicare.medicine.AuditEventRepository;
import com.medicare.medicine.MedicineSchedule;
import com.medicare.medicine.MedicineScheduleRepository;
import com.medicare.medicine.ReminderOccurrenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaregiverServiceTest {

    @Mock
    private CaregiverRepository caregiverRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MedicineScheduleRepository scheduleRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ReminderOccurrenceRepository occurrenceRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditEventRepository auditRepository;

    private CaregiverService caregiverService;

    private User patient;
    private User caregiver;
    private User stranger;

    @BeforeEach
    void setUp() {
        caregiverService = new CaregiverService(
            caregiverRepository, userRepository, scheduleRepository,
            appointmentRepository, occurrenceRepository, currentUserService, auditRepository
        );

        patient = new User("patient@care.local", "hash", "Alice", "Smith", Role.PATIENT);
        patient.setId(100L);

        caregiver = new User("caregiver@care.local", "hash", "Bob", "Jones", Role.PATIENT);
        caregiver.setId(200L);

        stranger = new User("stranger@care.local", "hash", "Eve", "Hacker", Role.PATIENT);
        stranger.setId(300L);
    }

    @Test
    void testPatientCanInviteCaregiver() {
        when(currentUserService.getCurrentUser()).thenReturn(patient);
        when(userRepository.findByEmailIgnoreCase("caregiver@care.local")).thenReturn(Optional.of(caregiver));
        when(caregiverRepository.findByPatientIdAndCaregiverId(patient.getId(), caregiver.getId())).thenReturn(Optional.empty());

        CaregiverRelation created = new CaregiverRelation(patient, caregiver);
        when(caregiverRepository.save(any(CaregiverRelation.class))).thenReturn(created);

        CaregiverRelation result = caregiverService.invite("caregiver@care.local");

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        verify(auditRepository).save(argThat(event -> "CAREGIVER_INVITED".equals(event.getEventType())));
    }

    @Test
    void testCannotInviteSelf() {
        when(currentUserService.getCurrentUser()).thenReturn(patient);
        when(userRepository.findByEmailIgnoreCase("patient@care.local")).thenReturn(Optional.of(patient));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> caregiverService.invite("patient@care.local"));
        assertEquals("Cannot add yourself as caregiver.", ex.getMessage());
    }

    @Test
    void testCaregiverCanApproveInvitation() {
        when(currentUserService.getCurrentUser()).thenReturn(caregiver);
        CaregiverRelation relation = new CaregiverRelation(patient, caregiver);
        relation.setId(50L);
        when(caregiverRepository.findByIdAndCaregiverId(50L, caregiver.getId())).thenReturn(Optional.of(relation));

        caregiverService.approve(50L);

        assertEquals("ACTIVE", relation.getStatus());
        assertNotNull(relation.getRespondedAt());
        verify(auditRepository).save(argThat(event -> "CAREGIVER_APPROVED".equals(event.getEventType())));
    }

    @Test
    void testPatientOrCaregiverCanRevokeRelationship() {
        when(currentUserService.getCurrentUser()).thenReturn(patient);
        CaregiverRelation relation = new CaregiverRelation(patient, caregiver);
        relation.setId(55L);
        relation.setStatus("ACTIVE");
        when(caregiverRepository.findById(55L)).thenReturn(Optional.of(relation));

        caregiverService.revoke(55L);

        assertEquals("REVOKED", relation.getStatus());
        assertNotNull(relation.getRevokedAt());
        verify(auditRepository).save(argThat(event -> "CAREGIVER_REVOKED".equals(event.getEventType())));
    }

    @Test
    void testUnauthorizedUserCannotRevokeRelationship() {
        when(currentUserService.getCurrentUser()).thenReturn(stranger);
        CaregiverRelation relation = new CaregiverRelation(patient, caregiver);
        relation.setId(55L);
        when(caregiverRepository.findById(55L)).thenReturn(Optional.of(relation));

        assertThrows(AccessDeniedException.class, () -> caregiverService.revoke(55L));
    }

    @Test
    void testActiveCaregiverCanAccessPatientSchedules() {
        when(currentUserService.getCurrentUser()).thenReturn(caregiver);
        CaregiverRelation relation = new CaregiverRelation(patient, caregiver);
        relation.setStatus("ACTIVE");
        when(caregiverRepository.findByPatientIdAndCaregiverIdAndStatus(patient.getId(), caregiver.getId(), "ACTIVE"))
            .thenReturn(Optional.of(relation));

        MedicineSchedule schedule = new MedicineSchedule();
        schedule.setMedicineName("Blood Pressure Med");
        when(scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(patient.getId())).thenReturn(List.of(schedule));

        List<MedicineSchedule> schedules = caregiverService.getPatientSchedules(patient.getId());

        assertEquals(1, schedules.size());
        assertEquals("Blood Pressure Med", schedules.get(0).getMedicineName());
        verify(auditRepository).save(argThat(event -> "CAREGIVER_VIEWED_PATIENT_SCHEDULES".equals(event.getEventType())));
    }

    @Test
    void testNonActiveCaregiverIsDeniedAccessToPatientData() {
        when(currentUserService.getCurrentUser()).thenReturn(caregiver);
        when(caregiverRepository.findByPatientIdAndCaregiverIdAndStatus(patient.getId(), caregiver.getId(), "ACTIVE"))
            .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> caregiverService.getPatientSchedules(patient.getId()));
        assertThrows(AccessDeniedException.class, () -> caregiverService.getPatientAppointments(patient.getId()));
        assertThrows(AccessDeniedException.class, () -> caregiverService.getPatientUpcomingDoses(patient.getId()));
    }
}
