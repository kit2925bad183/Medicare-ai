package com.medicare.medicine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderOccurrenceRepository extends JpaRepository<ReminderOccurrence, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<ReminderOccurrence> findByIdAndScheduleOwnerId(Long id, Long ownerId);

    List<ReminderOccurrence> findAllByScheduleIdAndStateOrderByDueAtAsc(Long scheduleId, OccurrenceState state);

    @Query("select o from ReminderOccurrence o where o.schedule.owner.id = :userId and o.dueAt between :start and :end order by o.dueAt asc")
    List<ReminderOccurrence> findAllByScheduleOwnerIdAndDueAtBetweenOrderByDueAtAsc(@Param("userId") Long userId,
                                                                                      @Param("start") Instant start,
                                                                                      @Param("end") Instant end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ReminderOccurrence o where o.state = :state and o.dueAt <= :now order by o.dueAt asc")
    List<ReminderOccurrence> findDueForUpdate(@Param("state") OccurrenceState state, @Param("now") Instant now, Pageable pageable);

    @Modifying
    @Query("update ReminderOccurrence o set o.state = :cancelled where o.schedule.id = :scheduleId and o.state = :pending")
    int cancelPendingForSchedule(@Param("scheduleId") Long scheduleId, @Param("pending") OccurrenceState pending, @Param("cancelled") OccurrenceState cancelled);
}
