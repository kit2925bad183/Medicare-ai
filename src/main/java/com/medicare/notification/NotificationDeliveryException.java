package com.medicare.notification;

public class NotificationDeliveryException extends Exception {

    private final String errorCategory;

    public NotificationDeliveryException(String errorCategory, String message) {
        super(message);
        this.errorCategory = errorCategory;
    }

    public NotificationDeliveryException(String errorCategory, String message, Throwable cause) {
        super(message, cause);
        this.errorCategory = errorCategory;
    }

    public String getErrorCategory() {
        return errorCategory;
    }
}
