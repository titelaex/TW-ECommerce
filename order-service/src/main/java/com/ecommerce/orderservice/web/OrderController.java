package com.ecommerce.orderservice.web;

import com.ecommerce.orderservice.dto.CheckoutRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.UpdateStatusRequest;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.ecommerce.orderservice.web.UserHeaders.IDEMPOTENCY_KEY;
import static com.ecommerce.orderservice.web.UserHeaders.USER_ID;
import static com.ecommerce.orderservice.web.UserHeaders.USER_ROLE;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@RequestHeader(USER_ID) String userId,
                                  @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String idempotencyKey,
                                  @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(userId, request, idempotencyKey);
    }

    /** Istoricul comenzilor cumparatorului curent. */
    @GetMapping
    public Page<OrderResponse> history(@RequestHeader(USER_ID) String userId,
                                       @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                                       Pageable pageable) {
        return orderService.getHistory(userId, pageable);
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable UUID orderId,
                             @RequestHeader(USER_ID) String userId,
                             @RequestHeader(USER_ROLE) String role) {
        return orderService.getOrder(orderId, userId, role);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable UUID orderId,
                                @RequestHeader(USER_ID) String userId,
                                @RequestHeader(USER_ROLE) String role) {
        return orderService.cancel(orderId, userId, role);
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(@PathVariable UUID orderId,
                                      @RequestHeader(USER_ID) String userId,
                                      @RequestHeader(USER_ROLE) String role,
                                      @Valid @RequestBody UpdateStatusRequest request) {
        return orderService.updateStatus(orderId, request.status(), userId, role);
    }
}
