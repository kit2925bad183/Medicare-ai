package com.medicare.notification;

import com.medicare.medicine.NotificationDelivery;
import com.medicare.medicine.NotificationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcherService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final Map<String, NotificationChannel> channels;
    private final NotificationDeliveryRepository deliveryRepository;
    private final Clock clock;

    public NotificationDispatcherService(List<NotificationChannel> channelList,
                                         NotificationDeliveryRepository deliveryRepository,
                                         Clock clock) {
        this.channels = channelList.stream()
            .collect(Collectors.toMap(NotificationChannel::getChannelName, Function.identity()));
        this.deliveryRepository = deliveryRepository;
        this.clock = clock;
    }

    @Transactional
    public void dispatch(NotificationDelivery delivery) {
        NotificationChannel channel = channels.get(delivery.getChannel());
        if (channel == null) {
            delivery.setState("FAILED");
            delivery.setLastErrorCategory("UNKNOWN_CHANNEL");
            deliveryRepository.save(delivery);
            return;
        }

        try {
            channel.deliver(delivery);
            delivery.setAttemptCount(delivery.getAttemptCount() + 1);
            delivery.setLastErrorCategory(null);
            delivery.setNextRetryAt(null);
            deliveryRepository.save(delivery);
        } catch (NotificationDeliveryException ex) {
            int attempts = delivery.getAttemptCount() + 1;
            delivery.setAttemptCount(attempts);
            delivery.setLastErrorCategory(ex.getErrorCategory());

            if (attempts >= MAX_ATTEMPTS || "CHANNEL_DISABLED".equals(ex.getErrorCategory())) {
                delivery.setState("FAILED");
                delivery.setNextRetryAt(null);
                log.warn("Delivery ID {} reached terminal failure state. Category: {}", delivery.getId(), ex.getErrorCategory());
            } else {
                delivery.setState("RETRYING");
                long backoffMinutes = calculateBackoff(attempts);
                Instant nextRetry = clock.instant().plus(backoffMinutes, ChronoUnit.MINUTES);
                delivery.setNextRetryAt(nextRetry);
                log.info("Delivery ID {} scheduled for retry in {} minutes. Category: {}", delivery.getId(), backoffMinutes, ex.getErrorCategory());
            }
            deliveryRepository.save(delivery);
        } catch (Exception ex) {
            int attempts = delivery.getAttemptCount() + 1;
            delivery.setAttemptCount(attempts);
            delivery.setLastErrorCategory("UNEXPECTED_ERROR");
            delivery.setState(attempts >= MAX_ATTEMPTS ? "FAILED" : "RETRYING");
            delivery.setNextRetryAt(attempts >= MAX_ATTEMPTS ? null : clock.instant().plus(5, ChronoUnit.MINUTES));
            deliveryRepository.save(delivery);
            log.error("Unexpected error dispatching delivery ID {}", delivery.getId(), ex);
        }
    }

    private long calculateBackoff(int attempt) {
        return switch (attempt) {
            case 1 -> 2;
            case 2 -> 5;
            default -> 15;
        };
    }
}
