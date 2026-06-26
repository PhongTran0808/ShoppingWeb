package com.ecommerce.secure.shared.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.Data;

/**
 * Transaction Context - Chứa toàn bộ thông tin cần thiết cho security validation
 */
@Data
@Builder
public class TransactionContext {
    
    // Basic Transaction Info
    private Long orderId;
    private String userId;
    private String sessionId;
    private BigDecimal totalAmount;
    private String currency;
    private Instant timestamp;
    
    // Security Context
    private String deviceFingerprint;
    private String ipAddress;
    private String userAgent;
    private Set<String> authenticationMethods; // MFA, SMS, BIOMETRIC, etc.
    private String paymentMethod;
    
    // Order Details  
    private List<OrderItem> orderItems;
    private ShippingDetails shippingDetails;
    private PaymentDetails paymentDetails;
    
    // Session & Authentication
    private String accessToken;
    private String refreshToken;
    private Instant tokenExpiry;
    private boolean isNewDevice;
    private boolean isNewLocation;
    
    // Risk Context
    private String previousOrderId; // For comparison
    private Integer recentOrderCount;
    private BigDecimal averageOrderValue;
    private String customerSegment; // VIP, REGULAR, NEW
    
    @Data
    @Builder
    public static class OrderItem {
        private Long productId;
        private String productName;
        private String productSku;
        private BigDecimal price;
        private Integer quantity;
        private String productHash; // SHA-256 hash for integrity
        private String category;
        private Boolean isDigital;
        private BigDecimal weight;
    }
    
    @Data
    @Builder
    public static class ShippingDetails {
        private String fullName;
        private String address;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phoneNumber;
        private String shippingMethod;
        private boolean isNewAddress;
        private boolean addressVerified;
    }
    
    @Data
    @Builder  
    public static class PaymentDetails {
        private String paymentMethod;
        private String cardType;
        private String cardLast4;
        private String billingAddress;
        private String paymentProcessor;
        private boolean requires3DS;
        private String bankCode;
        private boolean isNewPaymentMethod;
    }
}