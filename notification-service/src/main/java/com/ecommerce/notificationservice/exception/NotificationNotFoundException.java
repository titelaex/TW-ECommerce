package com.ecommerce.notificationservice.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String id) {
        super("Notificarea " + id + " nu exista");
    }
}
