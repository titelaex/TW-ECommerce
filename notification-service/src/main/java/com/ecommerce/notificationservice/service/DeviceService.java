package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.repository.DeviceTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

    private final DeviceTokenRepository devices;

    public DeviceService(DeviceTokenRepository devices) {
        this.devices = devices;
    }

    /** Apelat de frontend dupa login, cu token-ul primit de la Firebase Messaging (getToken()). */
    public void register(String userId, String token, String platform) {
        devices.save(token, userId, platform);
    }

    /** Apelat la logout, ca userul sa nu mai primeasca push-uri pe acel device. */
    public void unregister(String userId, String token) {
        devices.findByToken(token)
                .filter(d -> d.userId().equals(userId))
                .ifPresent(d -> devices.delete(token));
    }
}
