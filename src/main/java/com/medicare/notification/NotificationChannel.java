package com.medicare.notification;

import com.medicare.medicine.NotificationDelivery;

public interface NotificationChannel {
    String getChannelName();
    boolean isAvailable();
    void deliver(NotificationDelivery delivery) throws NotificationDeliveryException;
}
