package dev.elvis.orders.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
class OrderController {

    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        var order = service.create(request.customerId(), request.amount());
        return ResponseEntity.created(URI.create("/orders/" + order.getId())).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.from(service.find(id));
    }

    @PostMapping("/{id}/pay")
    OrderResponse pay(@PathVariable UUID id) {
        return OrderResponse.from(service.pay(id));
    }

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(@PathVariable UUID id) {
        return OrderResponse.from(service.cancel(id));
    }

    record CreateOrderRequest(
            @NotBlank String customerId,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount) {}

    record OrderResponse(
            UUID id, String customerId, BigDecimal amount, OrderStatus status, Instant createdAt, Instant updatedAt) {

        static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.getId(),
                    order.getCustomerId(),
                    order.getAmount(),
                    order.getStatus(),
                    order.getCreatedAt(),
                    order.getUpdatedAt());
        }
    }
}
