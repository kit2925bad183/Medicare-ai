package com.medicare.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    List<PushSubscription> findAllByUserIdAndRevokedFalse(Long userId);
    void deleteByEndpoint(String endpoint);
}
