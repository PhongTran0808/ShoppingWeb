package com.ecommerce.secure.shared.monitoring;

import com.ecommerce.secure.shared.enums.AuthenticationEventType;
import com.ecommerce.secure.shared.enums.PaymentEventType;
import com.ecommerce.secure.shared.enums.RiskLevel;
import com.ecommerce.secure.shared.enums.SecurityAction;
import com.ecommerce.secure.shared.enums.SecurityEventType;
import com.ecommerce.secure.shared.enums.SecuritySeverity;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class SecurityEvent {
    private String eventId;
    private Instant timestamp;
    private SecurityEventType eventType;
    private SecuritySeverity severity;
    private Object userId;
    private String sessionId;
    private Long orderId;
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;
    private int riskScore;
    private RiskLevel riskLevel;
    private SecurityAction securityAction;
    private Map<String, String> riskFactors;
    private Map<String, Object> additionalData;
    private AuthenticationEventType authenticationEventType;
    private boolean authenticationSuccess;
    private String reason;
    private PaymentEventType paymentEventType;
    private boolean paymentSuccess;
    private String paymentMethod;
    private String component;
}
