package com.medicare.appointment;

import com.medicare.common.CurrentUserService;
import com.medicare.identity.User;
import com.medicare.medicine.AuditEvent;
import com.medicare.medicine.AuditEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class AppointmentService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentReminderRepository appointmentReminderRepository;
    private final AuditEventRepository auditRepository;
    private final CurrentUserService currentUserService;

    public AppointmentService(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository,
                              AppointmentReminderRepository appointmentReminderRepository,
                              AuditEventRepository auditRepository, CurrentUserService currentUserService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentReminderRepository = appointmentReminderRepository;
        this.auditRepository = auditRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Doctor> doctors(String specialty) {
        if (specialty == null || specialty.isBlank()) {
            return doctorRepository.findAllByActiveTrueOrderByFullNameAsc();
        }
        return doctorRepository.findAllByActiveTrueAndSpecialtyContainingIgnoreCaseOrderByFullNameAsc(specialty.trim());
    }

    @Transactional
    public Appointment book(BookAppointmentRequest request) {
        User patient = currentUserService.getCurrentUser();
        Doctor doctor = doctorRepository.findById(request.doctorId())
            .orElseThrow(() -> new EntityNotFoundException("Doctor not found."));
        if (!doctor.isActive()) {
            throw new IllegalArgumentException("Doctor is not available.");
        }
        ZoneId zone = ZoneId.of(patient.getTimeZone());
        ZonedDateTime local = LocalDateTime.of(request.appointmentDate(), request.appointmentTime()).atZone(zone);
        Instant when = local.toInstant();
        if (when.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Appointment must be in the future.");
        }
        List<Appointment> conflicts = appointmentRepository.findConflictingForUpdate(doctor.getId(), when);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("This time slot is already booked.");
        }
        Appointment appointment = appointmentRepository.save(new Appointment(patient, doctor, when, patient.getTimeZone(),
            request.reason() == null ? null : request.reason().trim()));
        String key = "appointment:" + appointment.getId();
        if (!appointmentReminderRepository.existsByIdempotencyKey(key)) {
            appointmentReminderRepository.save(new AppointmentReminder(appointment, when, key));
        }
        auditRepository.save(new AuditEvent(patient, "APPOINTMENT_BOOKED", appointment.getId()));
        return appointment;
    }

    @Transactional
    public void cancel(Long appointmentId) {
        User patient = currentUserService.getCurrentUser();
        Appointment appointment = appointmentRepository.findByIdAndPatientId(appointmentId, patient.getId())
            .orElseThrow(() -> new EntityNotFoundException("Appointment not found."));
        if ("CANCELLED".equals(appointment.getStatus())) {
            return;
        }
        appointment.setStatus("CANCELLED");
        auditRepository.save(new AuditEvent(patient, "APPOINTMENT_CANCELLED", appointment.getId()));
    }

    @Transactional(readOnly = true)
    public List<Appointment> mine() {
        User patient = currentUserService.getCurrentUser();
        return appointmentRepository.findAllByPatientIdOrderByAppointmentAtDesc(patient.getId());
    }

    public record BookAppointmentRequest(Long doctorId, java.time.LocalDate appointmentDate,
                                         java.time.LocalTime appointmentTime, String reason) {
    }
}
