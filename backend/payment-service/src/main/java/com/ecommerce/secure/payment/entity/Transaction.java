package com.ecommerce.secure.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency = "VND";

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "payment_token", nullable = false)
    private String paymentToken;

    @Column(name = "hmac_signature", nullable = false, length = 500)
    private String hmacSignature;

    @Column(name = "encrypted_log", columnDefinition = "TEXT")
    private String encryptedLog;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
