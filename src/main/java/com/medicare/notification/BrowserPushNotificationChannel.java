package com.medicare.notification;

import com.medicare.identity.User;
import com.medicare.medicine.NotificationDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class BrowserPushNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(BrowserPushNotificationChannel.class);

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public BrowserPushNotificationChannel(PushSubscriptionRepository pushSubscriptionRepository,
                                          NotificationPreferenceRepository preferenceRepository) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Override
    public String getChannelName() {
        return "PUSH";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void deliver(NotificationDelivery delivery) throws NotificationDeliveryException {
        User recipient = delivery.getRecipient();
        var pref = preferenceRepository.findByUserId(recipient.getId()).orElse(null);
        if (pref != null && !pref.isPushEnabled()) {
            throw new NotificationDeliveryException("CHANNEL_DISABLED", "Web Push is disabled by user preference.");
        }

        List<PushSubscription> subs = pushSubscriptionRepository.findAllByUserIdAndRevokedFalse(recipient.getId());
        if (subs.isEmpty()) {
            throw new NotificationDeliveryException("NO_SUBSCRIPTION", "No active browser push subscription registered.");
        }

        // Clean up revoked subscriptions
        boolean sentAny = false;
        for (PushSubscription sub : subs) {
            if (sub.isRevoked()) {
                continue;
            }
            // Mark last seen
            sub.setLastSeenAt(Instant.now());
            pushSubscriptionRepository.save(sub);
            sentAny = true;
        }

        if (!sentAny) {
            throw new NotificationDeliveryException("SUBSCRIPTION_EXPIRED", "All registered push subscriptions are expired or revoked.");
        }

        delivery.setState("SENT");
        delivery.setSentAt(Instant.now());
        delivery.setNextRetryAt(null);
        log.info("Push notification recorded for delivery ID {}", delivery.getId());
    }
}
