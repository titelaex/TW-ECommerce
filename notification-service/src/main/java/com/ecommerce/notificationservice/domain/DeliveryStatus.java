package com.ecommerce.notificationservice.domain;

public enum DeliveryStatus {
    PENDING,
    SENT,
    /** Userul nu are niciun device inregistrat; notificarea ramane doar in istoric (in-app). */
    NO_DEVICE,
    FAILED
}
