package com.ecommerce.apigateway.web;

import com.ecommerce.apigateway.security.UserHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Endpoint de test: arata ce utilizator a extras gateway-ul din token. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    Map<String, String> me(@RequestHeader(UserHeaders.ID) String uid,
                           @RequestHeader(value = UserHeaders.EMAIL, required = false) String email,
                           @RequestHeader(UserHeaders.ROLE) String role) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("uid", uid);
        body.put("email", email);
        body.put("role", role);
        return body;
    }
}
