package com.ecommerce.notificationservice.domain;

import java.util.Map;

/**
 * Textele notificarilor stau aici, nu in order-service: acesta trimite doar tipul + datele.
 * Placeholder-ele {cheie} se inlocuiesc cu valorile din `data`.
 */
public enum NotificationType {
    ORDER_CONFIRMED("Comanda confirmata", "Comanda {orderId} a fost platita. Total: {total}."),
    ORDER_REJECTED("Stoc insuficient", "Comanda {orderId} nu a putut fi plasata: unele produse nu mai sunt in stoc."),
    PAYMENT_FAILED("Plata esuata", "Plata pentru comanda {orderId} nu a reusit. Poti incerca din nou."),
    ORDER_CANCELLED("Comanda anulata", "Comanda {orderId} a fost anulata."),
    ORDER_SHIPPED("Comanda expediata", "Comanda {orderId} este in drum spre tine."),
    ORDER_DELIVERED("Comanda livrata", "Comanda {orderId} a fost livrata.");

    private final String titleTemplate;
    private final String bodyTemplate;

    NotificationType(String titleTemplate, String bodyTemplate) {
        this.titleTemplate = titleTemplate;
        this.bodyTemplate = bodyTemplate;
    }

    public String title(Map<String, String> data) {
        return render(titleTemplate, data);
    }

    public String body(Map<String, String> data) {
        return render(bodyTemplate, data);
    }

    private static String render(String template, Map<String, String> data) {
        String result = template;
        for (var entry : data.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
