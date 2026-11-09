package com.ecommerce.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(FirebaseProperties props) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(credentials(props));
        if (StringUtils.hasText(props.projectId())) {
            options.setProjectId(props.projectId());
        }
        return FirebaseApp.initializeApp(options.build());
    }

    @Bean
    public Firestore firestore(FirebaseApp app) {
        return FirestoreClient.getFirestore(app);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }

    private GoogleCredentials credentials(FirebaseProperties props) throws IOException {
        if (!StringUtils.hasText(props.credentialsPath())) {
            return GoogleCredentials.getApplicationDefault();
        }
        try (InputStream in = new FileInputStream(props.credentialsPath())) {
            return GoogleCredentials.fromStream(in);
        }
    }
}
