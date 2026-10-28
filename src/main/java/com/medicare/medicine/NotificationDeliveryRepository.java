package com.medicare.medicine;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
    List<NotificationDelivery> findTop50ByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    boolean existsByOccurrenceIdAndChannel(Long occurrenceId, String channel);
    boolean existsByAppointmentIdAndChannel(Long appointmentId, String channel);
    Optional<NotificationDelivery> findByIdAndRecipientId(Long id, Long recipientId);

    @Query("SELECT nd FROM NotificationDelivery nd WHERE nd.state = 'RETRYING' AND nd.nextRetryAt <= :now ORDER BY nd.nextRetryAt ASC")
    List<NotificationDelivery> findPendingRetries(@Param("now") Instant now, Pageable pageable);

    List<NotificationDelivery> findAllByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
}
