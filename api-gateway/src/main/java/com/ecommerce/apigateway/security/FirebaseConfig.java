package com.ecommerce.apigateway.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final String DEV_MODE = "firebase.dev-mode";

    @Bean
    @ConditionalOnProperty(name = DEV_MODE, havingValue = "false", matchIfMissing = true)
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
    @ConditionalOnProperty(name = DEV_MODE, havingValue = "false", matchIfMissing = true)
    FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    @Bean
    @ConditionalOnProperty(name = DEV_MODE, havingValue = "false", matchIfMissing = true)
    TokenVerifier tokenVerifier(FirebaseAuth auth) {
        return idToken -> {
            FirebaseToken token = auth.verifyIdToken(idToken);
            return new AuthenticatedUser(token.getUid(), token.getEmail(), extractRole(token));
        };
    }

    /** In mod dev Firebase nu e initializat (nu e nevoie de cheie), deci orice token care nu e dev-token e respins. */
    @Bean
    @ConditionalOnProperty(name = DEV_MODE, havingValue = "true")
    TokenVerifier devModeTokenVerifier() {
        return idToken -> {
            throw new IllegalArgumentException("In mod dev sunt acceptate doar token-uri dev-token:<userId>:<role>");
        };
    }

    private static String extractRole(FirebaseToken token) {
        Object role = token.getClaims().get("role");
        return role != null ? role.toString() : AuthenticatedUser.DEFAULT_ROLE;
    }
}
