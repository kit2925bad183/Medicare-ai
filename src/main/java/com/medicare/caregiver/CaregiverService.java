package com.medicare.caregiver;

import com.medicare.appointment.Appointment;
import com.medicare.appointment.AppointmentRepository;
import com.medicare.common.CurrentUserService;
import com.medicare.identity.User;
import com.medicare.identity.UserRepository;
import com.medicare.medicine.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CaregiverService {

    private final CaregiverRepository caregiverRepository;
    private final UserRepository userRepository;
    private final MedicineScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReminderOccurrenceRepository occurrenceRepository;
    private final CurrentUserService currentUserService;
    private final AuditEventRepository auditRepository;

    public CaregiverService(CaregiverRepository caregiverRepository,
                            UserRepository userRepository,
                            MedicineScheduleRepository scheduleRepository,
                            AppointmentRepository appointmentRepository,
                            ReminderOccurrenceRepository occurrenceRepository,
                            CurrentUserService currentUserService,
                            AuditEventRepository auditRepository) {
        this.caregiverRepository = caregiverRepository;
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.currentUserService = currentUserService;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public CaregiverRelation invite(String caregiverEmail) {
        User patient = currentUserService.getCurrentUser();
        User caregiver = userRepository.findByEmailIgnoreCase(caregiverEmail.trim())
            .orElseThrow(() -> new IllegalArgumentException("Caregiver account not found."));
        if (caregiver.getId().equals(patient.getId())) {
            throw new IllegalArgumentException("Cannot add yourself as caregiver.");
        }

        var existing = caregiverRepository.findByPatientIdAndCaregiverId(patient.getId(), caregiver.getId());
        if (existing.isPresent()) {
            CaregiverRelation rel = existing.get();
            if ("ACTIVE".equals(rel.getStatus())) {
                throw new IllegalArgumentException("This caregiver relationship is already active.");
            }
            rel.setStatus("PENDING");
            rel.setRevokedAt(null);
            rel.setRespondedAt(null);
            auditRepository.save(new AuditEvent(patient, "CAREGIVER_INVITED", rel.getId()));
            return rel;
        }

        CaregiverRelation relation = caregiverRepository.save(new CaregiverRelation(patient, caregiver));
        auditRepository.save(new AuditEvent(patient, "CAREGIVER_INVITED", relation.getId()));
        return relation;
    }

    @Transactional
    public void approve(Long relationId) {
        User caregiver = currentUserService.getCurrentUser();
        CaregiverRelation relation = caregiverRepository.findByIdAndCaregiverId(relationId, caregiver.getId())
            .orElseThrow(() -> new EntityNotFoundException("Invitation not found."));
        if ("ACTIVE".equals(relation.getStatus())) {
            return;
        }
        relation.setStatus("ACTIVE");
        relation.setRespondedAt(Instant.now());
        auditRepository.save(new AuditEvent(caregiver, "CAREGIVER_APPROVED", relation.getId()));
    }

    @Transactional
    public void revoke(Long relationId) {
        User user = currentUserService.getCurrentUser();
        CaregiverRelation relation = caregiverRepository.findById(relationId)
            .orElseThrow(() -> new EntityNotFoundException("Caregiver relation not found."));

        if (!relation.getPatient().getId().equals(user.getId()) && !relation.getCaregiver().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not authorized to modify this caregiver relation.");
        }

        relation.setStatus("REVOKED");
        relation.setRevokedAt(Instant.now());
        auditRepository.save(new AuditEvent(user, "CAREGIVER_REVOKED", relation.getId()));
    }

    @Transactional(readOnly = true)
    public List<CaregiverRelation> invited() {
        return caregiverRepository.findAllByPatientId(currentUserService.getCurrentUser().getId());
    }

    @Transactional(readOnly = true)
    public List<CaregiverRelation> pending() {
        return caregiverRepository.findAllByCaregiverIdAndStatus(currentUserService.getCurrentUser().getId(), "PENDING");
    }

    @Transactional(readOnly = true)
    public List<CaregiverRelation> authorizedPatients() {
        return caregiverRepository.findAllByCaregiverIdAndStatus(currentUserService.getCurrentUser().getId(), "ACTIVE");
    }

    @Transactional(readOnly = true)
    public List<MedicineSchedule> getPatientSchedules(Long patientId) {
        verifyActiveCaregiver(patientId);
        auditRepository.save(new AuditEvent(currentUserService.getCurrentUser(), "CAREGIVER_VIEWED_PATIENT_SCHEDULES", patientId));
        return scheduleRepository.findAllByOwnerIdOrderByLocalTimeAsc(patientId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getPatientAppointments(Long patientId) {
        verifyActiveCaregiver(patientId);
        auditRepository.save(new AuditEvent(currentUserService.getCurrentUser(), "CAREGIVER_VIEWED_PATIENT_APPOINTMENTS", patientId));
        return appointmentRepository.findAllByPatientIdOrderByAppointmentAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<ReminderOccurrence> getPatientUpcomingDoses(Long patientId) {
        verifyActiveCaregiver(patientId);
        auditRepository.save(new AuditEvent(currentUserService.getCurrentUser(), "CAREGIVER_VIEWED_PATIENT_DOSES", patientId));
        Instant now = Instant.now();
        return occurrenceRepository.findAllByScheduleOwnerIdAndDueAtBetweenOrderByDueAtAsc(patientId, now, now.plus(7, ChronoUnit.DAYS));
    }

    private void verifyActiveCaregiver(Long patientId) {
        User caregiver = currentUserService.getCurrentUser();
        caregiverRepository.findByPatientIdAndCaregiverIdAndStatus(patientId, caregiver.getId(), "ACTIVE")
            .orElseThrow(() -> new AccessDeniedException("You are not an authorized active caregiver for this patient."));
    }
}
