package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.domain.DeliveryStatus;
import com.ecommerce.notificationservice.domain.Notification;
import com.ecommerce.notificationservice.dto.SendNotificationRequest;
import com.ecommerce.notificationservice.exception.NotificationNotFoundException;
import com.ecommerce.notificationservice.repository.DeviceTokenRepository;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final DeviceTokenRepository devices;
    private final PushSender pushSender;

    public NotificationService(NotificationRepository notifications, DeviceTokenRepository devices, PushSender pushSender) {
        this.notifications = notifications;
        this.devices = devices;
        this.pushSender = pushSender;
    }

    /**
     * 1. salveaza notificarea in Firestore (apare in istoricul din aplicatie chiar daca push-ul esueaza)
     * 2. trimite push pe toate device-urile userului
     * 3. curata token-urile expirate
     */
    public String send(SendNotificationRequest request) {
        Map<String, String> data = request.data() == null ? Map.of() : request.data();
        String title = request.type().title(data);
        String body = request.type().body(data);

        String id = notifications.create(new Notification(null, request.userId(), request.type(), title, body,
                data, false, DeliveryStatus.PENDING, Instant.now()));

        List<String> tokens = devices.findTokensByUser(request.userId());
        if (tokens.isEmpty()) {
            notifications.updateDeliveryStatus(id, DeliveryStatus.NO_DEVICE);
            return id;
        }

        try {
            PushSender.Result result = pushSender.send(tokens, title, body, withNotificationId(data, id));
            devices.deleteAll(result.invalidTokens());
            notifications.updateDeliveryStatus(id, result.successCount() > 0 ? DeliveryStatus.SENT : DeliveryStatus.FAILED);
        } catch (IllegalStateException e) {
            // TODO: retry programat (@Scheduled) pentru notificarile FAILED
            log.error("Push pentru notificarea {} a esuat: {}", id, e.getMessage());
            notifications.updateDeliveryStatus(id, DeliveryStatus.FAILED);
        }
        return id;
    }

    public List<Notification> listForUser(String userId, int limit) {
        return notifications.findByUser(userId, limit);
    }

    public void markRead(String userId, String notificationId) {
        Notification n = notifications.findById(notificationId)
                .filter(found -> found.userId().equals(userId))
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!n.read()) {
            notifications.markRead(notificationId);
        }
    }

    private static Map<String, String> withNotificationId(Map<String, String> data, String id) {
        var copy = new HashMap<>(data);
        copy.put("notificationId", id);
        return copy;
    }
}
