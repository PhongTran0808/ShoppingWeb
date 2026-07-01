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
        String timestamp = payload.get("timestamp");
        String signature = payload.get("signature");
        String publicKeyBase64 = payload.get("publicKey");

        // Bắt buộc xác thực chữ ký số bất đối xứng (ECDSA P-256)
        if (signature != null && publicKeyBase64 != null && timestamp != null) {
            try {
                // 1. Tái cấu trúc chuỗi payload để xác thực chữ ký
                String dataToVerify = address + ":" + phone + ":" + timestamp;
                byte[] dataBytes = dataToVerify.getBytes(java.nio.charset.StandardCharsets.UTF_8);

                // 2. Decode Public Key từ Base64
                byte[] keyBytes = java.util.Base64.getDecoder().decode(publicKeyBase64.trim());
                java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(keyBytes);
                java.security.KeyFactory kf = java.security.KeyFactory.getInstance("EC");
                java.security.PublicKey pubKey = kf.generatePublic(spec);

                // 3. Xác thực chữ ký số bằng SHA256withECDSA
                java.security.Signature ecdsaVerify = java.security.Signature.getInstance("SHA256withECDSA");
                ecdsaVerify.initVerify(pubKey);
                ecdsaVerify.update(dataBytes);
                
                byte[] sigBytes = java.util.Base64.getDecoder().decode(signature.trim());
                boolean isValid = ecdsaVerify.verify(sigBytes);

                System.out.println("\n=================================================");
                System.out.println("[CRYPTOGRAPHIC AUDIT] ORDER SIGNATURE VERIFICATION");
                System.out.println("Plaintext Payload : " + dataToVerify);
                System.out.println("Public Key Base64 : " + publicKeyBase64.substring(0, Math.min(publicKeyBase64.length(), 40)) + "...");
                System.out.println("Signature Base64  : " + signature.substring(0, Math.min(signature.length(), 40)) + "...");
                System.out.println("Verification Status: " + (isValid ? "SUCCESS (APPROVED)" : "FAILED (REJECTED)"));
                System.out.println("=================================================\n");

                if (!isValid) {
                    throw new SecurityException("Giao dịch bị từ chối! Chữ ký số không hợp lệ hoặc dữ liệu đơn hàng đã bị sửa đổi.");
                }
            } catch (Exception e) {
                System.err.println("[CRYPTOGRAPHIC ERROR] Signature verification failed: " + e.getMessage());
                throw new SecurityException("Lỗi xác thực mật mã: " + e.getMessage());
            }
        } else {
            throw new SecurityException("Giao dịch bị từ chối! Thiếu chữ ký số giao dịch chống chối bỏ.");
        }

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
