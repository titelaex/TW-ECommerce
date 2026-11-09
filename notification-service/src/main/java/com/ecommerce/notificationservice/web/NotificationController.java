package com.ecommerce.notificationservice.web;

import com.ecommerce.notificationservice.domain.Notification;
import com.ecommerce.notificationservice.dto.SendNotificationRequest;
import com.ecommerce.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.ecommerce.notificationservice.web.UserHeaders.USER_ID;

@RestController
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Intern: apelat de order-service. Gateway-ul NU trebuie sa ruteze /internal/**. */
    @PostMapping("/internal/notifications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> send(@Valid @RequestBody SendNotificationRequest request) {
        return Map.of("id", notificationService.send(request));
    }

    /** Istoricul notificarilor userului curent (clopotelul din UI). */
    @GetMapping("/api/notifications")
    public List<Notification> list(@RequestHeader(USER_ID) String userId,
                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return notificationService.listForUser(userId, limit);
    }

    @PatchMapping("/api/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@RequestHeader(USER_ID) String userId, @PathVariable String id) {
        notificationService.markRead(userId, id);
    }
}
