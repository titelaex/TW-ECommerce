package com.ecommerce.notificationservice.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Izoleaza apelul catre Firebase Cloud Messaging (usor de inlocuit cu un mock in teste). */
@Component
public class PushSender {

    private final FirebaseMessaging messaging;

    public PushSender(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    /**
     * @return rezultatul trimiterii: cate au reusit si ce token-uri sunt invalide (de sters)
     */
    public Result send(List<String> tokens, String title, String body, Map<String, String> data) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();
        try {
            BatchResponse response = messaging.sendEachForMulticast(message);
            List<String> invalid = new ArrayList<>();
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful() && isInvalidToken(responses.get(i).getException())) {
                    invalid.add(tokens.get(i));
                }
            }
            return new Result(response.getSuccessCount(), invalid);
        } catch (FirebaseMessagingException e) {
            throw new IllegalStateException("FCM indisponibil: " + e.getMessage(), e);
        }
    }

    private static boolean isInvalidToken(FirebaseMessagingException e) {
        return e != null && (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT);
    }

    public record Result(int successCount, List<String> invalidTokens) {
    }
}
