package com.medicare.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByPatientIdOrderByAppointmentAtDesc(Long patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.doctor.id = :doctorId and a.status <> 'CANCELLED' and a.appointmentAt = :appointmentAt")
    List<Appointment> findConflictingForUpdate(@Param("doctorId") Long doctorId, @Param("appointmentAt") Instant appointmentAt);

    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);
}
