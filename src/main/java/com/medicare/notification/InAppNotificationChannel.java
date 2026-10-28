package com.medicare.notification;

import com.medicare.medicine.NotificationDelivery;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InAppNotificationChannel implements NotificationChannel {

    @Override
    public String getChannelName() {
        return "IN_APP";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void deliver(NotificationDelivery delivery) {
        delivery.setState("AVAILABLE");
        delivery.setSentAt(Instant.now());
    }
}
