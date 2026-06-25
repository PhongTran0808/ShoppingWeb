package com.ecommerce.secure.shared.dto;

import com.ecommerce.secure.shared.enums.RiskLevel;
import com.ecommerce.secure.shared.enums.SecurityAction;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Risk Decision - Kết quả cuối cùng của việc đánh giá security
 */
@Data
@Builder
public class RiskDecision {
    
    private RiskLevel riskLevel;
    private SecurityAction action;
    private int overallScore;
    private String reason;
    private Instant timestamp;
    
    // Risk Analysis Details
    private Map<String, String> riskFactors;
    private List<String> recommendedActions;
    private List<String> securityWarnings;
    
    // Action-Specific Data
    private String additionalAuthRequired; // SMS, EMAIL, 3DS
    private String manualReviewReason;
    private String blockingReason;
    private int retryAfterSeconds;
    
    // Monitoring & Alerting
    private boolean requiresRealTimeMonitoring;
    private boolean triggerSecurityAlert;
    private String alertSeverity; // LOW, MEDIUM, HIGH, CRITICAL
    
    // Compliance & Audit
    private String complianceNote;
    private boolean requiresAuditLog;
    private String auditCategory;
    
    /**
     * Helper Methods
     */
    public boolean isApproved() {
        return action == SecurityAction.APPROVE || 
               action == SecurityAction.APPROVE_WITH_MONITORING;
    }
    
    public boolean requiresIntervention() {
        return action == SecurityAction.REQUIRE_ADDITIONAL_AUTH ||
               action == SecurityAction.FLAG_FOR_REVIEW ||
               action == SecurityAction.BLOCK;
    }
    
    public boolean isBlocked() {
        return action == SecurityAction.BLOCK;
    }
    
    public boolean needsManualReview() {
        return action == SecurityAction.FLAG_FOR_REVIEW;
    }
    
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }
}