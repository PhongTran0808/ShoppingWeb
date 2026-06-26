package com.ecommerce.secure.order.controller;

import com.ecommerce.secure.order.entity.Order;
import com.ecommerce.secure.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestHeader("Authorization") String bearerToken,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {

        String userId = jwt.getSubject();
        String customerName = jwt.getClaimAsString("preferred_username");
        if (customerName == null) {
            customerName = userId;
        }

        String address = payload.get("shippingAddress");
        String phone = payload.get("phoneNumber");

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }

        String deviceFingerprint = request.getHeader("User-Agent");
        String sessionId = request.getSession().getId();

        Order order = orderService.createOrder(
                bearerToken,
                userId,
                customerName,
                address,
                phone,
                deviceFingerprint,
                ipAddress,
                sessionId
        );

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getId());
        response.put("trackingNumber", order.getTrackingNumber());
        response.put("totalAmount", order.getTotalAmount());
        response.put("status", order.getStatus());

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Order>> getAllOrders(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(orderService.getOrderById(id, userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderService.getOrderByIdWithoutUserCheck(id);
        order.setStatus(status);
        orderService.saveOrder(order);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
