# KIẾN TRÚC BẢO MẬT TOÀN DIỆN - SHOPPING WEB SECURITY ARCHITECTURE

## 🛡️ 1. TRANSACTION SECURITY FRAMEWORK - GIAO DỊCH LÀM TRUNG TÂM

### 1.1 Rủi Ro Giao Dịch - Tiêu Chí Đầu Tiên

#### A. Mặt Hàng Bị Sửa Đổi (Product Integrity)
```
SECURITY FLOW:
User Selection → Product Hash Verification → Cart Validation → Order Confirmation
```

**Kiểm Soát:**
- **Product Fingerprinting**: Mỗi sản phẩm có hash SHA-256 (price + description + availability)
- **Immutable Cart**: Cart items được sign bằng HMAC, không thể modify từ client
- **Real-time Price Validation**: Kiểm tra giá realtime trước checkout
- **Inventory Lock**: Reserve sản phẩm trong 15 phút khi add to cart

#### B. Xác Thực Giao Dịch (Transaction Authentication)
```
AUTHENTICATION CHAIN:
User Identity → Session Binding → Device Fingerprint → Transaction Authorization
```

**Kiểm Soát:**
- **Multi-Factor Transaction**: SMS OTP cho orders > 500k VNĐ
- **Device Binding**: JWT token bind với device fingerprint
- **IP Geolocation**: Cảnh báo khi transaction từ IP bất thường
- **Behavioral Analysis**: AI phát hiện pattern mua hàng bất thường

#### C. Session & Payment Transition Security
```
TRANSACTION FLOW:
Cart → Order Creation → Payment Authorization → Payment Processing → Fulfillment
```

**Security Gates:**
1. **Session Validation**: Kiểm tra session còn valid & belong to user
2. **Amount Verification**: Cross-check cart total vs payment amount
3. **Product Availability**: Final check sản phẩm còn stock
4. **Fraud Scoring**: ML model tính risk score realtime
5. **3D Secure**: Bank authentication cho high-value transactions

## 🔐 2. BA YẾU TỐ BẢO MẬT CỐT LÕI

### 2.1 Giao Tiếp An Toàn (Secure Communication)

#### A. Service-to-Service Security
```
HMAC-SHA256 Authentication:
- Timestamp window: 5 minutes
- Nonce tracking: Prevent replay
- Service identity verification
```

#### B. Client-Server Security  
```
HTTPS + Certificate Pinning:
- TLS 1.3 minimum
- HSTS headers
- Certificate transparency monitoring
```

#### C. API Gateway Protection
```
Rate Limiting + DDoS Protection:
- Redis-based sliding window
- JWT validation caching
- Request sanitization
```

### 2.2 Xử Lý Logic An Toàn (Secure Business Logic)

#### A. Input Validation & Sanitization
```java
@Component
public class SecurityValidator {
    
    @Validated
    public OrderRequest validateOrder(@Valid OrderRequest request, 
                                    @AuthenticationPrincipal UserPrincipal user) {
        // 1. Ownership validation
        validateCartOwnership(request.getCartId(), user.getId());
        
        // 2. Product integrity check
        validateProductIntegrity(request.getItems());
        
        // 3. Price tampering detection
        validatePriceConsistency(request.getItems());
        
        // 4. Business rule validation
        validateBusinessRules(request, user);
        
        return request;
    }
}
```

#### B. Fraud Detection Engine
```java
@Service
public class FraudDetectionService {
    
    public RiskScore calculateRiskScore(TransactionContext context) {
        RiskScore score = new RiskScore();
        
        // Device analysis
        score.addFactor(analyzeDevice(context.getDeviceFingerprint()));
        
        // Behavioral analysis  
        score.addFactor(analyzeBehavior(context.getUserHistory()));
        
        // Geolocation analysis
        score.addFactor(analyzeLocation(context.getIpAddress()));
        
        // Amount analysis
        score.addFactor(analyzeAmount(context.getAmount(), context.getUserProfile()));
        
        return score;
    }
}
```

### 2.3 Lưu Trữ An Toàn (Secure Storage)

#### A. Field-Level Encryption
```java
@Entity
public class SecureOrder {
    
    @Convert(converter = AesCryptoConverter.class)
    private String shippingAddress;
    
    @Convert(converter = VaultEncryptionConverter.class) 
    private String paymentDetails;
    
    // Hash cho integrity checking
    @Column(name = "integrity_hash")
    private String integrityHash;
}
```

#### B. Database Security
```sql
-- Row-Level Security
CREATE POLICY order_policy ON orders 
FOR ALL TO ecom_user 
USING (user_id = current_user_id());

-- Audit Logging
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY,
    table_name VARCHAR(50),
    operation VARCHAR(10),
    old_values JSONB,
    new_values JSONB,
    user_id BIGINT,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    ip_address INET
);
```

## 🚨 3. TRANSACTION RISK DETECTION SYSTEM

### 3.1 Real-time Risk Scoring
```java
@Component
public class TransactionRiskAnalyzer {
    
    @EventListener
    public void analyzeTransaction(OrderCreatedEvent event) {
        
        TransactionRisk risk = TransactionRisk.builder()
            .orderId(event.getOrderId())
            .userId(event.getUserId())
            .build();
            
        // 1. Kiểm tra đúng người
        risk.setUserVerification(verifyUserIdentity(event));
        
        // 2. Kiểm tra đúng sản phẩm  
        risk.setProductIntegrity(verifyProductIntegrity(event));
        
        // 3. Kiểm tra đúng giá
        risk.setPriceAccuracy(verifyPriceAccuracy(event));
        
        // 4. Kiểm tra transaction status
        risk.setTransactionStatus(verifyTransactionStatus(event));
        
        // Decision engine
        RiskDecision decision = riskDecisionEngine.evaluate(risk);
        
        switch(decision.getAction()) {
            case APPROVE -> processOrderNormally(event.getOrderId());
            case REVIEW -> flagForManualReview(event.getOrderId());
            case BLOCK -> blockSuspiciousTransaction(event.getOrderId());
        }
    }
}
```

### 3.2 Session & Payment Validation Pipeline
```java
@Component 
public class PaymentSecurityPipeline {
    
    public PaymentValidationResult validatePayment(PaymentRequest request) {
        
        ValidationPipeline pipeline = ValidationPipeline.create()
            // 1. Session validation
            .addStep(ctx -> validateSession(ctx.getSessionId(), ctx.getUserId()))
            
            // 2. Product consistency
            .addStep(ctx -> validateProductConsistency(ctx.getOrderItems()))
            
            // 3. Amount verification  
            .addStep(ctx -> validateAmountConsistency(ctx.getCartTotal(), ctx.getPaymentAmount()))
            
            // 4. Fraud detection
            .addStep(ctx -> runFraudDetection(ctx))
            
            // 5. 3D Secure check
            .addStep(ctx -> validate3DSecure(ctx))
            
            // 6. Final authorization
            .addStep(ctx -> authorizePayment(ctx));
            
        return pipeline.execute(PaymentContext.from(request));
    }
}
```

## 🔑 4. ACCESS CONTROL & AUTHORIZATION

### 4.1 Fine-grained RBAC
```java
@PreAuthorize("hasRole('USER') and @orderSecurityService.canAccessOrder(#orderId, authentication.name)")
@GetMapping("/orders/{orderId}")
public OrderResponse getOrder(@PathVariable Long orderId) {
    return orderService.getOrder(orderId);
}

@PreAuthorize("@paymentSecurityService.canProcessPayment(#paymentRequest, authentication)")  
@PostMapping("/payments")
public PaymentResponse processPayment(@RequestBody PaymentRequest paymentRequest) {
    return paymentService.processPayment(paymentRequest);
}
```

### 4.2 Dynamic Security Policies
```java
@Component
public class DynamicSecurityPolicy {
    
    public boolean evaluateTransactionPolicy(TransactionContext context) {
        
        // High-value transaction policy
        if (context.getAmount().compareTo(BigDecimal.valueOf(1000000)) > 0) {
            return context.hasMultiFactorAuth() && 
                   context.has3DSecure() && 
                   context.getRiskScore().isLow();
        }
        
        // Suspicious user policy  
        if (context.getUser().isFlagged()) {
            return context.hasManagerApproval();
        }
        
        // Default policy
        return context.isAuthenticated() && 
               context.getRiskScore().isAcceptable();
    }
}
```

## 🐳 5. DOCKER SECURITY CONFIGURATION

### 5.1 Multi-stage Production Build
```dockerfile
# Security-hardened base
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user
RUN addgroup -g 1001 -S ecommerce && \
    adduser -u 1001 -S ecommerce -G ecommerce

# Install security tools
RUN apk add --no-cache \
    dumb-init \
    tini \
    curl \
    && rm -rf /var/cache/apk/*

# Security hardening
RUN chmod 755 /usr/bin/dumb-init

USER ecommerce
WORKDIR /app

COPY --chown=ecommerce:ecommerce --from=builder /app/target/*.jar app.jar

# Security headers
EXPOSE 8080
ENTRYPOINT ["dumb-init", "--"]
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### 5.2 Secure Docker Compose
```yaml
version: '3.8'

services:
  gateway:
    image: ecommerce/gateway:latest
    container_name: ecom-gateway
    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
    read_only: true
    tmpfs:
      - /tmp:noexec,nosuid,size=100m
    environment:
      - SPRING_PROFILES_ACTIVE=production
    networks:
      - ecom-secure
    restart: unless-stopped
    
networks:
  ecom-secure:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

## 📊 6. MONITORING & INCIDENT RESPONSE

### 6.1 Security Event Monitoring
```java
@Component
public class SecurityEventPublisher {
    
    @EventListener
    public void onSuspiciousActivity(SuspiciousActivityEvent event) {
        
        SecurityAlert alert = SecurityAlert.builder()
            .severity(determineSeverity(event))
            .userId(event.getUserId())
            .ipAddress(event.getIpAddress())
            .eventType(event.getType())
            .details(event.getDetails())
            .timestamp(Instant.now())
            .build();
            
        // Real-time alerting
        alertService.sendAlert(alert);
        
        // SIEM integration
        siemService.logSecurityEvent(alert);
        
        // Auto-response if critical
        if (alert.getSeverity() == Severity.CRITICAL) {
            autoResponseService.triggerResponse(alert);
        }
    }
}
```

### 6.2 Fraud Detection Dashboard
```yaml
# Grafana Dashboard Configuration
dashboard:
  title: "E-commerce Security Dashboard"
  panels:
    - title: "Transaction Risk Score"
      type: "timeseries"
      targets:
        - expr: 'avg(transaction_risk_score) by (user_id)'
    
    - title: "Failed Authentication Attempts"  
      type: "stat"
      targets:
        - expr: 'sum(rate(auth_failures[5m]))'
        
    - title: "Suspicious IP Activity"
      type: "table" 
      targets:
        - expr: 'topk(10, sum by (ip_address) (rate(suspicious_requests[1h])))'
```

## 🎖️ 7. COMPLIANCE & AUDIT

### 7.1 Audit Trail System
```java
@Entity
@Table(name = "security_audit_log")
public class SecurityAuditLog {
    
    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;
    
    private String userId;
    private String sessionId;
    private String ipAddress;
    private String userAgent;
    private String resource;
    private String action;
    private String result;
    
    @Type(JsonType.class)
    private Map<String, Object> additionalData;
    
    private Instant timestamp;
}
```

### 7.2 Security Metrics & KPIs
```java
@Component
public class SecurityMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void recordSecurityEvent(SecurityEvent event) {
        
        // Transaction security metrics
        Counter.builder("transaction.security.events")
            .tag("type", event.getType())
            .tag("severity", event.getSeverity())
            .register(meterRegistry)
            .increment();
            
        // Authentication metrics
        Timer.builder("authentication.duration")
            .register(meterRegistry)
            .record(event.getDuration());
            
        // Risk score distribution
        Gauge.builder("risk.score.current")
            .tag("user_segment", event.getUserSegment())
            .register(meterRegistry, () -> event.getRiskScore());
    }
}
```

---

## 🏆 SECURITY BENEFITS

1. **Transaction-Centric Security**: Giao dịch được bảo vệ ở mọi bước
2. **Real-time Fraud Detection**: Phát hiện gian lận theo thời gian thực  
3. **Comprehensive Access Control**: Kiểm soát truy cập chi tiết
4. **Secure Communication**: Giao tiếp mã hóa end-to-end
5. **Audit & Compliance**: Đáp ứng yêu cầu tuân thủ
6. **Incident Response**: Phản ứng tự động với threat
7. **Container Security**: Bảo mật containerized deployment

Kiến trúc này đảm bảo **đúng người → đúng sản phẩm → đúng giá → giao dịch thành công** với security làm foundation.