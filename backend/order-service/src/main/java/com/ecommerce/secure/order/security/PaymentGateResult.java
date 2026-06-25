package com.ecommerce.secure.order.security;

import com.ecommerce.secure.shared.dto.RiskDecision;
import lombok.Builder;
import lombok.Data;

/**
 * Payment Gate Result - Kết quả từ Payment Security Gateway
 */
@Data
@Builder
public class PaymentGateResult {
    
    private PaymentGateStatus status;
    private boolean approved;
    private String reason;
    private RiskDecision riskDecision;
    
    // Authentication Requirements
    private String authenticationToken;
    private String requiredAuthMethod;
    private int authTimeoutSeconds;
    private Integer remainingAttempts;
    
    // Payment Processing Guidance
    private String paymentProcessorRecommendation;
    private boolean monitoringRequired;
    private boolean requires3DSecure;
    
    // Review & Blocking
    private boolean requiresManualReview;
    private int estimatedReviewTimeMinutes;
    private String blockingReason;
    
    // Helper Methods
    public boolean isApproved() {
        return approved;
    }
    
    public boolean needsAdditionalAuth() {
        return status == PaymentGateStatus.REQUIRES_ADDITIONAL_AUTH;
    }
    
    public boolean needsManualReview() {
        return requiresManualReview;
    }
    
    public boolean isBlocked() {
        return status == PaymentGateStatus.BLOCKED;
    }
    
    public static PaymentGateResult approved(RiskDecision decision) {
        return PaymentGateResult.builder()
            .status(PaymentGateStatus.APPROVED)
            .approved(true)
            .reason("Transaction approved")
            .riskDecision(decision)
            .paymentProcessorRecommendation("PROCEED_NORMAL")
            .build();
    }
    
    public static PaymentGateResult blocked(String reason) {
        return PaymentGateResult.builder()
            .status(PaymentGateStatus.BLOCKED)
            .approved(false)
            .reason(reason)
            .blockingReason(reason)
            .build();
    }
}

/**
 * Payment Gate Status Enumeration
 */
enum PaymentGateStatus {
    APPROVED,
    APPROVED_WITH_MONITORING,
    REQUIRES_ADDITIONAL_AUTH,
    PENDING_REVIEW,
    BLOCKED,
    AUTH_FAILED,
    ERROR
}