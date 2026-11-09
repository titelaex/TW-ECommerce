package com.ecommerce.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterDeviceRequest(
        @NotBlank String token,
        @NotBlank @Pattern(regexp = "WEB|ANDROID|IOS") String platform) {
}
