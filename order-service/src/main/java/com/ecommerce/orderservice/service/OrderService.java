package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.CartClient;
import com.ecommerce.orderservice.client.InventoryClient;
import com.ecommerce.orderservice.client.NotificationClient;
import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.dto.ClientDtos.CartResponse;
import com.ecommerce.orderservice.client.dto.ClientDtos.PaymentRequest;
import com.ecommerce.orderservice.client.dto.ClientDtos.PaymentResponse;
import com.ecommerce.orderservice.client.dto.ClientDtos.ReservationItem;
import com.ecommerce.orderservice.client.dto.ClientDtos.ReservationRequest;
import com.ecommerce.orderservice.domain.Order;
import com.ecommerce.orderservice.domain.OrderItem;
import com.ecommerce.orderservice.domain.OrderStatus;
import com.ecommerce.orderservice.domain.ShippingAddress;
import com.ecommerce.orderservice.dto.CheckoutRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.exception.EmptyCartException;
import com.ecommerce.orderservice.exception.ForbiddenException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.OutOfStockException;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.ecommerce.orderservice.web.UserHeaders.ROLE_ADMIN;
import static com.ecommerce.orderservice.web.UserHeaders.ROLE_SELLER;

/**
 * Orchestreaza checkout-ul (saga): rezervare stoc -> plata -> confirmare / compensare.
 * Apelurile catre alte servicii NU se fac in interiorul unei tranzactii DB;
 * fiecare pas isi salveaza separat starea, ca sa stim mereu unde a ramas comanda.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;
    private final TransactionTemplate tx;
    private final String currency;

    public OrderService(OrderRepository orderRepository,
                        CartClient cartClient,
                        InventoryClient inventoryClient,
                        PaymentClient paymentClient,
                        NotificationClient notificationClient,
                        TransactionTemplate tx,
                        @Value("${order.currency}") String currency) {
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.notificationClient = notificationClient;
        this.tx = tx;
        this.currency = currency;
    }

    public OrderResponse checkout(String userId, CheckoutRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return OrderResponse.from(load(existing.get().getId()));
            }
        }

        // 1. Construim comanda din cos (preturile vin din cart-service, nu de la client)
        CartResponse cart = cartClient.getCart(userId);
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw new EmptyCartException();
        }
        UUID orderId = createPendingOrder(userId, request, idempotencyKey, cart);

        // 2. Rezervam stocul
        try {
            var items = cart.items().stream()
                    .map(i -> new ReservationItem(i.productId(), i.quantity()))
                    .toList();
            var reservation = inventoryClient.reserve(new ReservationRequest(orderId, items));
            update(orderId, o -> {
                o.setReservationId(reservation.reservationId());
                o.changeStatus(OrderStatus.STOCK_RESERVED);
            });
        } catch (OutOfStockException e) {
            update(orderId, o -> {
                o.setFailureReason(e.getMessage());
                o.changeStatus(OrderStatus.REJECTED);
            });
            notificationClient.notify(userId, "ORDER_REJECTED", Map.of("orderId", orderId.toString()));
            throw e;
        }

        // 3. Plata + confirmare sau compensare
        Order order = load(orderId);
        PaymentResponse payment = pay(order);
        if (payment.succeeded()) {
            inventoryClient.confirm(order.getReservationId());
            update(orderId, o -> {
                o.setPaymentId(payment.paymentId());
                o.changeStatus(OrderStatus.CONFIRMED);
            });
            clearCartQuietly(userId);
            notificationClient.notify(userId, "ORDER_CONFIRMED", Map.of(
                    "orderId", orderId.toString(),
                    "total", order.getTotalAmount().toPlainString() + " " + order.getCurrency()));
        } else {
            inventoryClient.release(order.getReservationId());
            update(orderId, o -> {
                o.setPaymentId(payment.paymentId());
                o.setFailureReason(payment.failureReason());
                o.changeStatus(OrderStatus.PAYMENT_FAILED);
            });
            notificationClient.notify(userId, "PAYMENT_FAILED", Map.of("orderId", orderId.toString()));
        }

        return OrderResponse.from(load(orderId));
    }

    public Page<OrderResponse> getHistory(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(OrderResponse::from);
    }

    public OrderResponse getOrder(UUID orderId, String userId, String role) {
        Order order = load(orderId);
        checkCanView(order, userId, role);
        return OrderResponse.from(order);
    }

    public OrderResponse cancel(UUID orderId, String userId, String role) {
        Order order = load(orderId);
        if (!order.getUserId().equals(userId) && !ROLE_ADMIN.equals(role)) {
            throw new ForbiddenException("Doar cumparatorul sau un admin poate anula comanda");
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            // TODO: paymentClient.refund(order.getPaymentId()) + repunere stoc in inventory-service
            log.warn("Anulare comanda {} CONFIRMED fara refund -- de implementat", orderId);
        } else if (order.getReservationId() != null && order.getStatus() == OrderStatus.STOCK_RESERVED) {
            inventoryClient.release(order.getReservationId());
        }
        update(orderId, o -> o.changeStatus(OrderStatus.CANCELLED));
        notificationClient.notify(order.getUserId(), "ORDER_CANCELLED", Map.of("orderId", orderId.toString()));
        return OrderResponse.from(load(orderId));
    }

    /** Folosit de vanzator/admin pentru SHIPPED / DELIVERED. */
    public OrderResponse updateStatus(UUID orderId, OrderStatus next, String userId, String role) {
        Order order = load(orderId);
        boolean allowed = ROLE_ADMIN.equals(role)
                || (ROLE_SELLER.equals(role) && order.hasItemsFromSeller(userId));
        if (!allowed) {
            throw new ForbiddenException("Nu ai dreptul sa modifici statusul acestei comenzi");
        }
        update(orderId, o -> o.changeStatus(next));
        switch (next) {
            case SHIPPED -> notificationClient.notify(order.getUserId(), "ORDER_SHIPPED", Map.of("orderId", orderId.toString()));
            case DELIVERED -> notificationClient.notify(order.getUserId(), "ORDER_DELIVERED", Map.of("orderId", orderId.toString()));
            default -> { }
        }
        return OrderResponse.from(load(orderId));
    }

    // ---------------------------------------------------------------------

    private UUID createPendingOrder(String userId, CheckoutRequest request, String idempotencyKey, CartResponse cart) {
        var a = request.shippingAddress();
        var address = new ShippingAddress(a.fullName(), a.phone(), a.street(), a.city(), a.county(), a.postalCode());
        Order order = new Order(userId, currency, address, idempotencyKey);
        cart.items().forEach(i -> order.addItem(
                new OrderItem(i.productId(), i.sellerId(), i.productName(), i.unitPrice(), i.quantity())));
        return orderRepository.save(order).getId();
    }

    private PaymentResponse pay(Order order) {
        try {
            return paymentClient.pay(new PaymentRequest(
                    order.getId(), order.getUserId(), order.getTotalAmount(), order.getCurrency()));
        } catch (RestClientException e) {
            // payment-service indisponibil: tratam ca plata esuata ca sa eliberam stocul
            // TODO: diferentiat timeout (plata poate sa fi trecut!) de eroare clara
            log.error("Plata pentru comanda {} a esuat: {}", order.getId(), e.getMessage());
            return new PaymentResponse(null, "FAILED", "payment-service indisponibil");
        }
    }

    private void clearCartQuietly(String userId) {
        try {
            cartClient.clearCart(userId);
        } catch (RestClientException e) {
            log.warn("Cosul userului {} nu a putut fi golit: {}", userId, e.getMessage());
        }
    }

    private void checkCanView(Order order, String userId, String role) {
        boolean allowed = order.getUserId().equals(userId)
                || ROLE_ADMIN.equals(role)
                || (ROLE_SELLER.equals(role) && order.hasItemsFromSeller(userId));
        if (!allowed) {
            throw new ForbiddenException("Nu ai acces la aceasta comanda");
        }
    }

    private Order load(UUID orderId) {
        return orderRepository.findWithItemsById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void update(UUID orderId, Consumer<Order> change) {
        tx.executeWithoutResult(status -> {
            Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
            change.accept(order);
        });
    }
}
