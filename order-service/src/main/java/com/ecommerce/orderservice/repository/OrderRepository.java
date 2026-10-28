package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** Istoricul comenzilor unui cumparator. */
    @EntityGraph(attributePaths = "items")
    Page<Order> findByUserId(String userId, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(UUID id);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    // TODO: istoricul vanzatorului (comenzi care contin produse ale lui) pentru "isi urmaresc incasarile"
}
