package com.ecommerce.secure.shared.monitoring;

import com.ecommerce.secure.shared.dto.RiskDecision;
import com.ecommerce.secure.shared.dto.TransactionContext;
import com.ecommerce.secure.shared.enums.RiskLevel;
import com.ecommerce.secure.shared.enums.AuthenticationEventType;
import com.ecommerce.secure.shared.enums.PaymentEventType;
import com.ecommerce.secure.shared.enums.SecurityEventType;
import com.ecommerce.secure.shared.enums.SecuritySeverity;
import com.ecommerce.secure.shared.enums.SecurityAlertType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Security Event Publisher - Hệ thống monitoring và alerting security events
 * Tập trung vào real-time detection và response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    private final SecurityMetricsCollector metricsCollector;
    
    @Value("${security.monitoring.kafka.topic:security-events}")
    private String securityEventsTopic;
    
    @Value("${security.monitoring.alerts.topic:security-alerts}")
    private String securityAlertsTopic;
    
    /**
     * Publish Transaction Security Event
     */
    public void publishTransactionSecurityEvent(TransactionContext context, RiskDecision decision) {
        
        SecurityEvent event = SecurityEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .eventType(SecurityEventType.TRANSACTION_RISK_ASSESSMENT)
            .severity(mapRiskLevelToSeverity(decision.getRiskLevel()))
            .userId(context.getUserId())
            .sessionId(context.getSessionId())
            .orderId(context.getOrderId())
            .ipAddress(context.getIpAddress())
            .userAgent(context.getUserAgent())
            .deviceFingerprint(context.getDeviceFingerprint())
            .riskScore(decision.getOverallScore())
            .riskLevel(decision.getRiskLevel())
            .securityAction(decision.getAction())
            .riskFactors(decision.getRiskFactors())
            .additionalData(buildTransactionEventData(context, decision))
            .build();
            
        // Publish to multiple channels
        publishEvent(event);
        
        // Update metrics
        metricsCollector.recordTransactionSecurityEvent(event);
        
        // Trigger alerts if high risk
        if (shouldTriggerAlert(decision)) {
            triggerSecurityAlert(event, context, decision);
        }
        
        log.info("🔍 Transaction Security Event Published: {} - Risk: {}, Score: {}", 
                event.getEventId(), decision.getRiskLevel(), decision.getOverallScore());
    }
    
    /**
     * Publish Authentication Security Event
     */
    public void publishAuthenticationEvent(String userId, String sessionId, String ipAddress, 
                                          AuthenticationEventType authType, boolean success, String reason) {
        
        SecurityEvent event = SecurityEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .eventType(SecurityEventType.AUTHENTICATION)
            .severity(success ? SecuritySeverity.INFO : SecuritySeverity.WARNING)
            .userId(Long.valueOf(userId))
            .sessionId(sessionId)
            .ipAddress(ipAddress)
            .authenticationEventType(authType)
            .authenticationSuccess(success)
            .reason(reason)
            .additionalData(Map.of(
                "auth_type", authType.name(),
                "success", success,
                "reason", reason != null ? reason : "N/A"
            ))
            .build();
            
        publishEvent(event);
        metricsCollector.recordAuthenticationEvent(event);
        
        // Alert on failed authentication patterns
        if (!success && shouldAlertOnAuthFailure(userId, ipAddress)) {
            triggerAuthenticationAlert(event);
        }
        
        log.info("🔐 Authentication Event: User: {}, Type: {}, Success: {}", userId, authType, success);
    }
    
    /**
     * Publish Payment Security Event
     */
    public void publishPaymentSecurityEvent(Long orderId, Long userId, String paymentMethod, 
                                           PaymentEventType eventType, boolean success, Map<String, Object> details) {
        
        SecurityEvent event = SecurityEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .eventType(SecurityEventType.PAYMENT_PROCESSING)
            .severity(success ? SecuritySeverity.INFO : SecuritySeverity.HIGH)
            .userId(userId)
            .orderId(orderId)
            .paymentEventType(eventType)
            .paymentSuccess(success)
            .paymentMethod(paymentMethod)
            .additionalData(details != null ? details : new HashMap<>())
            .build();
            
        publishEvent(event);
        metricsCollector.recordPaymentEvent(event);
        
        // Alert on payment failures or suspicious activity
        if (!success || isPaymentEventSuspicious(eventType, details)) {
            triggerPaymentAlert(event);
        }
        
        log.info("💳 Payment Security Event: Order: {}, Type: {}, Success: {}", orderId, eventType, success);
    }
    
    /**
     * Publish System Security Event
     */
    public void publishSystemSecurityEvent(SecurityEventType eventType, SecuritySeverity severity, 
                                          String component, String details, Map<String, Object> metadata) {
        
        SecurityEvent event = SecurityEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .eventType(eventType)
            .severity(severity)
            .component(component)
            .reason(details)
            .additionalData(metadata != null ? metadata : new HashMap<>())
            .build();
            
        publishEvent(event);
        metricsCollector.recordSystemEvent(event);
        
        // Alert on critical system events
        if (severity == SecuritySeverity.CRITICAL || severity == SecuritySeverity.HIGH) {
            triggerSystemAlert(event);
        }
        
        log.warn("⚠️ System Security Event: {} - {} - {}", component, eventType, details);
    }
    
    /**
     * Core event publishing method
     */
    private void publishEvent(SecurityEvent event) {
        try {
            // Publish to Spring Application Events
            applicationEventPublisher.publishEvent(event);
            
            // Store in security audit log
            storeSecurityAuditLog(event);
            
        } catch (Exception e) {
            log.error("Failed to publish security event: {}", event.getEventId(), e);
            // Fallback logging to ensure event is not lost
            logSecurityEventFallback(event);
        }
    }
    
    /**
     * Trigger Security Alert
     */
    private void triggerSecurityAlert(SecurityEvent event, TransactionContext context, RiskDecision decision) {
        
        SecurityAlert alert = SecurityAlert.builder()
            .alertId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .alertType(SecurityAlertType.HIGH_RISK_TRANSACTION)
            .severity(mapRiskLevelToSeverity(decision.getRiskLevel()))
            .sourceEvent(event)
            .title("High Risk Transaction Detected")
            .description(String.format("Transaction %s from user %s has risk score %d", 
                        context.getOrderId(), context.getUserId(), decision.getOverallScore()))
            .actionRequired(decision.getAction().getDescription())
            .metadata(Map.of(
                "risk_score", decision.getOverallScore(),
                "risk_factors", decision.getRiskFactors(),
                "recommended_actions", decision.getRecommendedActions()
            ))
            .build();
            
        publishAlert(alert);
    }
    
    /**
     * Trigger Authentication Alert
     */
    private void triggerAuthenticationAlert(SecurityEvent event) {
        SecurityAlert alert = SecurityAlert.builder()
            .alertId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .alertType(SecurityAlertType.AUTHENTICATION_FAILURE)
            .severity(SecuritySeverity.HIGH)
            .sourceEvent(event)
            .title("Suspicious Authentication Activity")
            .description(String.format("Multiple failed authentication attempts from IP: %s", event.getIpAddress()))
            .actionRequired("Investigate and potentially block IP address")
            .build();
            
        publishAlert(alert);
    }
    
    /**
     * Trigger Payment Alert
     */
    private void triggerPaymentAlert(SecurityEvent event) {
        SecurityAlert alert = SecurityAlert.builder()
            .alertId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .alertType(SecurityAlertType.PAYMENT_FRAUD)
            .severity(SecuritySeverity.CRITICAL)
            .sourceEvent(event)
            .title("Payment Security Incident")
            .description(String.format("Suspicious payment activity detected for order: %s", event.getOrderId()))
            .actionRequired("Immediate investigation required")
            .build();
            
        publishAlert(alert);
    }
    
    /**
     * Trigger System Alert
     */
    private void triggerSystemAlert(SecurityEvent event) {
        SecurityAlert alert = SecurityAlert.builder()
            .alertId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .alertType(SecurityAlertType.SYSTEM_SECURITY)
            .severity(event.getSeverity())
            .sourceEvent(event)
            .title("System Security Event")
            .description(String.format("Security event in component: %s", event.getComponent()))
            .actionRequired("System administrator attention required")
            .build();
            
        publishAlert(alert);
    }
    
    /**
     * Publish Alert to Alert System
     */
    private void publishAlert(SecurityAlert alert) {
        try {
            // Store alert in database
            storeSecurityAlert(alert);
            
            // Send notifications based on severity
            if (alert.getSeverity() == SecuritySeverity.CRITICAL) {
                sendCriticalAlert(alert);
            }
            
            log.warn("🚨 Security Alert Triggered: {} - {}", alert.getAlertType(), alert.getTitle());
            
        } catch (Exception e) {
            log.error("Failed to publish security alert: {}", alert.getAlertId(), e);
        }
    }
    
    // Helper Methods
    
    private SecuritySeverity mapRiskLevelToSeverity(RiskLevel riskLevel) {
        switch (riskLevel) {
            case LOW: return SecuritySeverity.INFO;
            case MEDIUM: return SecuritySeverity.WARNING;
            case HIGH: return SecuritySeverity.HIGH;
            case CRITICAL: return SecuritySeverity.CRITICAL;
            default: return SecuritySeverity.INFO;
        }
    }
    
    private boolean shouldTriggerAlert(RiskDecision decision) {
        return decision.getRiskLevel() == RiskLevel.HIGH || 
               decision.getRiskLevel() == RiskLevel.CRITICAL ||
               decision.getOverallScore() < 40;
    }
    
    private boolean shouldAlertOnAuthFailure(String userId, String ipAddress) {
        // TODO: Implement logic to detect authentication failure patterns
        return metricsCollector.getRecentFailedAttempts(userId, ipAddress) > 3;
    }
    
    private boolean isPaymentEventSuspicious(PaymentEventType eventType, Map<String, Object> details) {
        // TODO: Implement payment fraud detection logic
        return eventType == PaymentEventType.FRAUD_DETECTED || 
               eventType == PaymentEventType.CHARGEBACK_RECEIVED;
    }
    
    private Map<String, Object> buildTransactionEventData(TransactionContext context, RiskDecision decision) {
        Map<String, Object> data = new HashMap<>();
        data.put("order_id", context.getOrderId());
        data.put("total_amount", context.getTotalAmount());
        data.put("payment_method", context.getPaymentMethod());
        data.put("device_fingerprint", context.getDeviceFingerprint());
        data.put("risk_factors", decision.getRiskFactors());
        data.put("security_action", decision.getAction().name());
        return data;
    }
    
    private void storeSecurityAuditLog(SecurityEvent event) {
        // TODO: Store in database for audit purposes
        log.debug("Storing security audit log: {}", event.getEventId());
    }
    
    private void storeSecurityAlert(SecurityAlert alert) {
        // TODO: Store alert in database
        log.debug("Storing security alert: {}", alert.getAlertId());
    }
    
    private void logSecurityEventFallback(SecurityEvent event) {
        log.error("FALLBACK SECURITY LOG - Event: {}, Type: {}, Severity: {}, Details: {}", 
                 event.getEventId(), event.getEventType(), event.getSeverity(), event.getReason());
    }
    
    private void sendCriticalAlert(SecurityAlert alert) {
        // TODO: Send immediate notifications (email, SMS, Slack, etc.)
        log.error("🚨🚨 CRITICAL SECURITY ALERT: {} - {}", alert.getTitle(), alert.getDescription());
    }
}