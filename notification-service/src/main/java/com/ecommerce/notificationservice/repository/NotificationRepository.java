package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.domain.DeliveryStatus;
import com.ecommerce.notificationservice.domain.Notification;
import com.ecommerce.notificationservice.domain.NotificationType;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ecommerce.notificationservice.repository.Futures.await;

@Repository
public class NotificationRepository {

    static final String COLLECTION = "notifications";

    private final CollectionReference collection;

    public NotificationRepository(Firestore firestore) {
        this.collection = firestore.collection(COLLECTION);
    }

    /** Salveaza o notificare noua si intoarce id-ul generat de Firestore. */
    public String create(Notification n) {
        DocumentReference ref = collection.document();
        await(ref.set(toMap(n)));
        return ref.getId();
    }

    public void updateDeliveryStatus(String id, DeliveryStatus status) {
        await(collection.document(id).update("deliveryStatus", status.name()));
    }

    public void markRead(String id) {
        await(collection.document(id).update("read", true));
    }

    public Optional<Notification> findById(String id) {
        DocumentSnapshot doc = await(collection.document(id).get());
        return doc.exists() ? Optional.of(fromSnapshot(doc)) : Optional.empty();
    }

    /** Necesita index compus (userId ASC, createdAt DESC) -- vezi firestore.indexes.json. */
    public List<Notification> findByUser(String userId, int limit) {
        return await(collection.whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get())
                .getDocuments().stream()
                .map(this::fromSnapshot)
                .toList();
    }

    // TODO: countUnread(userId) pentru badge-ul din UI

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", n.userId());
        map.put("type", n.type().name());
        map.put("title", n.title());
        map.put("body", n.body());
        map.put("data", n.data());
        map.put("read", n.read());
        map.put("deliveryStatus", n.deliveryStatus().name());
        map.put("createdAt", Timestamp.ofTimeSecondsAndNanos(n.createdAt().getEpochSecond(), n.createdAt().getNano()));
        return map;
    }

    @SuppressWarnings("unchecked")
    private Notification fromSnapshot(DocumentSnapshot doc) {
        Timestamp createdAt = doc.getTimestamp("createdAt");
        return new Notification(
                doc.getId(),
                doc.getString("userId"),
                NotificationType.valueOf(doc.getString("type")),
                doc.getString("title"),
                doc.getString("body"),
                (Map<String, String>) doc.get("data"),
                Boolean.TRUE.equals(doc.getBoolean("read")),
                DeliveryStatus.valueOf(doc.getString("deliveryStatus")),
                createdAt == null ? null : Instant.ofEpochSecond(createdAt.getSeconds(), createdAt.getNanos()));
    }
}
