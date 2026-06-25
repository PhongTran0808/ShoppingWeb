package com.ecommerce.secure.shared.enums;

/**
 * Security Actions - Hành động được khuyến nghị dựa trên risk assessment
 */
public enum SecurityAction {
    
    /**
     * Cho phép giao dịch tiếp tục bình thường
     */
    APPROVE("Approve Transaction", "Transaction approved - proceed normally"),
    
    /**
     * Cho phép nhưng tăng cường monitoring
     */
    APPROVE_WITH_MONITORING("Approve with Enhanced Monitoring", 
                           "Transaction approved but requires enhanced monitoring"),
    
    /**
     * Yêu cầu xác thực bổ sung (SMS OTP, 3D Secure, etc.)
     */
    REQUIRE_ADDITIONAL_AUTH("Require Additional Authentication", 
                           "Additional authentication required before proceeding"),
    
    /**
     * Đánh dấu để review thủ công
     */
    FLAG_FOR_REVIEW("Flag for Manual Review", 
                    "Transaction flagged for manual review by security team"),
    
    /**
     * Chặn giao dịch hoàn toàn
     */
    BLOCK("Block Transaction", 
          "Transaction blocked due to high security risk"),
    
    /**
     * Tạm dừng user account
     */
    SUSPEND_ACCOUNT("Suspend Account", 
                   "User account suspended due to suspicious activity"),
    
    /**
     * Yêu cầu KYC/Identity verification
     */
    REQUIRE_KYC("Require KYC Verification", 
               "Know Your Customer verification required");
    
    private final String action;
    private final String description;
    
    SecurityAction(String action, String description) {
        this.action = action;
        this.description = description;
    }
    
    public String getAction() {
        return action;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if action allows transaction to proceed
     */
    public boolean allowsTransaction() {
        return this == APPROVE || this == APPROVE_WITH_MONITORING;
    }
    
    /**
     * Check if action requires user intervention
     */
    public boolean requiresUserAction() {
        return this == REQUIRE_ADDITIONAL_AUTH || this == REQUIRE_KYC;
    }
    
    /**
     * Check if action is blocking/restrictive
     */
    public boolean isRestrictive() {
        return this == BLOCK || this == SUSPEND_ACCOUNT;
    }
    
    /**
     * Get recommended timeout in minutes for delayed actions
     */
    public int getRecommendedTimeoutMinutes() {
        switch (this) {
            case REQUIRE_ADDITIONAL_AUTH:
                return 5;  // 5 minutes for OTP/3DS
            case FLAG_FOR_REVIEW:
                return 60; // 1 hour for manual review
            case REQUIRE_KYC:
                return 1440; // 24 hours for KYC
            case BLOCK:
            case SUSPEND_ACCOUNT:
                return -1; // Indefinite
            default:
                return 0; // No timeout
        }
    }
}