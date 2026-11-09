package com.ecommerce.notificationservice.repository;

import com.google.api.core.ApiFuture;

import java.util.concurrent.ExecutionException;

/** SDK-ul Firestore e asincron; in serviciile noastre (blocante) asteptam rezultatul. */
final class Futures {

    private Futures() {
    }

    static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FirestoreException(e);
        } catch (ExecutionException e) {
            throw new FirestoreException(e.getCause());
        }
    }

    static class FirestoreException extends RuntimeException {
        FirestoreException(Throwable cause) {
            super("Eroare Firestore: " + cause.getMessage(), cause);
        }
    }
}
