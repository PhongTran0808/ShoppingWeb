package com.ecommerce.secure.payment.controller;

import com.ecommerce.secure.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> processPayment(
            @RequestHeader("Authorization") String bearerToken,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {

        Long orderId = Long.valueOf(payload.get("orderId").toString());
        String paymentToken = payload.get("paymentToken").toString();

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }

        String userAgent = request.getHeader("User-Agent");

        paymentService.processPayment(orderId, paymentToken, bearerToken, signature, ipAddress, userAgent);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Payment processed successfully");
        response.put("orderId", orderId);

        return ResponseEntity.ok(response);
    }
}
