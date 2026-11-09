package com.ecommerce.notificationservice.web;

import com.ecommerce.notificationservice.dto.RegisterDeviceRequest;
import com.ecommerce.notificationservice.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.ecommerce.notificationservice.web.UserHeaders.USER_ID;

@RestController
@RequestMapping("/api/notifications/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@RequestHeader(USER_ID) String userId, @Valid @RequestBody RegisterDeviceRequest request) {
        deviceService.register(userId, request.token(), request.platform());
    }

    @DeleteMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@RequestHeader(USER_ID) String userId, @PathVariable String token) {
        deviceService.unregister(userId, token);
    }
}
