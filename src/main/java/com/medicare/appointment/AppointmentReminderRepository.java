package com.medicare.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;

public interface AppointmentReminderRepository extends JpaRepository<AppointmentReminder, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AppointmentReminder r where r.state = :state and r.dueAt <= :now order by r.dueAt asc")
    List<AppointmentReminder> findDueForUpdate(@Param("state") String state, @Param("now") Instant now);
}
