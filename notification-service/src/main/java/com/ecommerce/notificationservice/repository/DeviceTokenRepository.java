package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.domain.DeviceToken;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ecommerce.notificationservice.repository.Futures.await;

@Repository
public class DeviceTokenRepository {

    static final String COLLECTION = "device_tokens";

    private final Firestore firestore;
    private final CollectionReference collection;

    public DeviceTokenRepository(Firestore firestore) {
        this.firestore = firestore;
        this.collection = firestore.collection(COLLECTION);
    }

    /**
     * Token-ul e id-ul documentului: acelasi device inregistrat de doua ori nu se dubleaza,
     * iar daca alt user se logheaza pe acelasi device, token-ul trece la el.
     */
    public void save(String token, String userId, String platform) {
        await(collection.document(token).set(Map.of(
                "userId", userId,
                "platform", platform,
                "updatedAt", Timestamp.now())));
    }

    public Optional<DeviceToken> findByToken(String token) {
        DocumentSnapshot doc = await(collection.document(token).get());
        if (!doc.exists()) {
            return Optional.empty();
        }
        Timestamp updatedAt = doc.getTimestamp("updatedAt");
        return Optional.of(new DeviceToken(doc.getId(), doc.getString("userId"), doc.getString("platform"),
                updatedAt == null ? null : Instant.ofEpochSecond(updatedAt.getSeconds(), updatedAt.getNanos())));
    }

    public List<String> findTokensByUser(String userId) {
        return await(collection.whereEqualTo("userId", userId).get())
                .getDocuments().stream()
                .map(DocumentSnapshot::getId)
                .toList();
    }

    public void delete(String token) {
        await(collection.document(token).delete());
    }

    public void deleteAll(Collection<String> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        WriteBatch batch = firestore.batch();
        tokens.forEach(t -> batch.delete(collection.document(t)));
        await(batch.commit());
    }
}
