package com.ecommerce.secure.payment.service;

import com.ecommerce.secure.payment.entity.AuditLog;
import com.ecommerce.secure.payment.entity.Transaction;
import com.ecommerce.secure.payment.repository.AuditLogRepository;
import com.ecommerce.secure.payment.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private VaultEncryptionService vaultEncryptionService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void processPayment(Long orderId, String paymentToken, String authorizationHeader, String hmacSignature, String ipAddress, String userAgent) {
        // 1. Fetch order details from order-service
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String orderServiceUrl = "http://localhost:8083/api/orders/" + orderId;
        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(orderServiceUrl, HttpMethod.GET, requestEntity, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch order details from order-service", e);
        }

        Map<String, Object> order = response.getBody();
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }

        BigDecimal amount = new BigDecimal(order.get("totalAmount").toString());

        // 2. Encrypt log payload using Vault Transit Engine
        String logPayload = String.format("Payment processed for orderId: %d with token: %s, amount: %s", orderId, paymentToken, amount);
        String encryptedLog = vaultEncryptionService.encryptData(logPayload);

        // 3. Save transaction record
        Transaction transaction = new Transaction();
        transaction.setOrderId(orderId);
        transaction.setAmount(amount);
        transaction.setPaymentToken(paymentToken);
        transaction.setStatus("SUCCESS");
        transaction.setHmacSignature(hmacSignature != null ? hmacSignature : "N/A");
        transaction.setEncryptedLog(encryptedLog);
        transactionRepository.save(transaction);

        // 4. Save Audit Log
        AuditLog auditLog = new AuditLog();
        auditLog.setEventType("PAYMENT_SUCCESS");
        auditLog.setOrderId(orderId);
        auditLog.setIpAddress(ipAddress);
        auditLog.setDeviceFingerprint(userAgent);
        auditLog.setRiskScore(BigDecimal.ZERO); // normal risk
        auditLog.setNotes("Payment completed successfully");
        auditLogRepository.save(auditLog);

        // 5. Update order status in order-service
        String updateStatusUrl = "http://localhost:8083/api/orders/" + orderId + "/status?status=PAID";
        try {
            restTemplate.exchange(updateStatusUrl, HttpMethod.PUT, requestEntity, Void.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update order status in order-service", e);
        }
    }
}
