package com.ecommerce.apigateway.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    FirebaseApp firebaseApp(FirebaseProperties props) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        GoogleCredentials credentials;
        if (StringUtils.hasText(props.credentialsPath())) {
            try (InputStream in = new FileInputStream(props.credentialsPath())) {
                credentials = GoogleCredentials.fromStream(in);
            }
        } else {
            // fallback: variabila de mediu GOOGLE_APPLICATION_CREDENTIALS
            credentials = GoogleCredentials.getApplicationDefault();
        }
        FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(credentials);
        if (StringUtils.hasText(props.projectId())) {
            options.setProjectId(props.projectId());
        }
        return FirebaseApp.initializeApp(options.build());
    }

    @Bean
    TokenVerifier tokenVerifier(FirebaseApp app) {
        FirebaseAuth auth = FirebaseAuth.getInstance(app);
        return idToken -> {
            var token = auth.verifyIdToken(idToken);
            Object role = token.getClaims().get("role");
            return new AuthenticatedUser(
                    token.getUid(),
                    token.getEmail(),
                    role != null ? role.toString() : AuthenticatedUser.DEFAULT_ROLE);
        };
    }
}
