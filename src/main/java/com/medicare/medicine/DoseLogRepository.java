package com.medicare.medicine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DoseLogRepository extends JpaRepository<DoseLog, Long> {

    List<DoseLog> findAllByUserIdAndActionTimeBetweenOrderByActionTimeDesc(Long userId, Instant start, Instant end);

    List<DoseLog> findAllByOccurrenceId(Long occurrenceId);

    boolean existsByOccurrenceIdAndAction(Long occurrenceId, String action);

    long countByUserIdAndActionAndActionTimeBetween(Long userId, String action, Instant start, Instant end);

    long countByUserIdAndActionTimeBetween(Long userId, Instant start, Instant end);
}
