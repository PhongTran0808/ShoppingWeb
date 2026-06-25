package com.ecommerce.secure.order.security;

import com.ecommerce.secure.shared.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

/**
 * Payment Security Requirements - Yêu cầu bảo mật cho payment processing
 */
@Data
@Builder
public class PaymentSecurityRequirements {
    
    // Validation Requirements
    private boolean requiresValidation;
    private boolean approved;
    private String reason;
    
    // Risk Assessment
    private RiskLevel riskLevel;
    private int fraudScore;
    
    // Processing Requirements  
    private boolean requiresMonitoring;
    private boolean requires3DSecure;
    private boolean requiresStepUpAuth;
    
    // Payment Processor Guidance
    private String paymentProcessorRecommendation;
    private String processingMode; // NORMAL, ENHANCED, SECURE
    
    // Fraud Prevention
    private boolean enableFraudChecks;
    private boolean requiresVelocityCheck;
    private boolean requiresDeviceVerification;
    
    // Compliance Requirements
    private boolean requiresPCICompliance;
    private boolean requiresAuditLogging;
    private String complianceLevel; // BASIC, ENHANCED, STRICT
    
    /**
     * Get recommended processing configuration
     */
    public PaymentProcessingConfig getProcessingConfig() {
        return PaymentProcessingConfig.builder()
            .mode(determineProcessingMode())
            .fraudChecksEnabled(enableFraudChecks)
            .velocityChecksEnabled(requiresVelocityCheck)
            .deviceVerificationEnabled(requiresDeviceVerification)
            .realTimeMonitoring(requiresMonitoring)
            .threeDSecureRequired(requires3DSecure)
            .auditLevel(determineAuditLevel())
            .build();
    }
    
    private String determineProcessingMode() {
        if (riskLevel == RiskLevel.CRITICAL || riskLevel == RiskLevel.HIGH) {
            return "SECURE";
        } else if (requiresMonitoring || riskLevel == RiskLevel.MEDIUM) {
            return "ENHANCED";
        } else {
            return "NORMAL";
        }
    }
    
    private String determineAuditLevel() {
        if (riskLevel == RiskLevel.CRITICAL) {
            return "DETAILED";
        } else if (riskLevel == RiskLevel.HIGH || requiresMonitoring) {
            return "STANDARD";
        } else {
            return "BASIC";
        }
    }
}

/**
 * Payment Processing Configuration
 */
@Data
@Builder
class PaymentProcessingConfig {
    private String mode;
    private boolean fraudChecksEnabled;
    private boolean velocityChecksEnabled;
    private boolean deviceVerificationEnabled;
    private boolean realTimeMonitoring;
    private boolean threeDSecureRequired;
    private String auditLevel;
}