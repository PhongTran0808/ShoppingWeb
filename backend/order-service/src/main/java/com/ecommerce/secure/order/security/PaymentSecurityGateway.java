package com.ecommerce.secure.order.security;

import com.ecommerce.secure.shared.dto.TransactionContext;
import com.ecommerce.secure.shared.dto.RiskDecision;
import com.ecommerce.secure.shared.enums.SecurityAction;
import com.ecommerce.secure.shared.service.TransactionSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Payment Security Gateway - Cổng kiểm soát chuyển đổi từ Order sang Payment
 * Đây là security gate cuối cùng trước khi chuyển sang payment processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSecurityGateway {
    
    private final TransactionSecurityService transactionSecurityService;
    
    // Cache cho approved transactions (tránh duplicate validation)
    private final Map<String, ApprovedTransaction> approvedTransactions = new ConcurrentHashMap<>();
    
    // Temporary hold cho pending authentications
    private final Map<String, PendingAuthentication> pendingAuthentications = new ConcurrentHashMap<>();
    
    /**
     * Main Security Gate - Kiểm tra toàn diện trước khi chuyển sang payment
     * 
     * @param orderContext Transaction context từ order
     * @return PaymentGateResult với decision và required actions
     */
    public PaymentGateResult validatePaymentTransition(TransactionContext orderContext) {
        log.info("🔒 Payment Security Gate - Validating transition for Order: {}, User: {}", 
                orderContext.getOrderId(), orderContext.getUserId());
        
        String transactionKey = generateTransactionKey(orderContext);
        
        // 1. Kiểm tra transaction đã được approve trước đó chưa (cache check)
        ApprovedTransaction existingApproval = approvedTransactions.get(transactionKey);
        if (existingApproval != null && isApprovalStillValid(existingApproval)) {
            log.info("✅ Transaction already approved and cached: {}", transactionKey);
            return PaymentGateResult.approved(existingApproval.getDecision());
        }
        
        // 2. Chạy comprehensive security validation
        RiskDecision decision = transactionSecurityService.validateTransaction(orderContext);
        
        // 3. Process decision theo security action
        PaymentGateResult result = processSecurityDecision(decision, orderContext);
        
        // 4. Cache approved transactions
        if (result.isApproved()) {
            cacheApprovedTransaction(transactionKey, decision);
        }
        
        // 5. Log security event cho audit
        logSecurityEvent(orderContext, decision, result);
        
        log.info("🔍 Payment Security Gate Result: {} - {}", result.getStatus(), result.getReason());
        return result;
    }
    
    /**
     * Xử lý Security Decision và tạo Payment Gate Result
     */
    private PaymentGateResult processSecurityDecision(RiskDecision decision, TransactionContext context) {
        
        switch (decision.getAction()) {
            case APPROVE:
                return PaymentGateResult.builder()
                    .status(PaymentGateStatus.APPROVED)
                    .approved(true)
                    .reason("Transaction meets all security criteria")
                    .riskDecision(decision)
                    .paymentProcessorRecommendation("PROCEED_NORMAL")
                    .build();
                    
            case APPROVE_WITH_MONITORING:
                return PaymentGateResult.builder()
                    .status(PaymentGateStatus.APPROVED_WITH_MONITORING)
                    .approved(true)
                    .reason("Transaction approved with enhanced monitoring")
                    .riskDecision(decision)
                    .paymentProcessorRecommendation("PROCEED_WITH_MONITORING")
                    .monitoringRequired(true)
                    .build();
                    
            case REQUIRE_ADDITIONAL_AUTH:
                return handleAdditionalAuthRequired(decision, context);
                
            case FLAG_FOR_REVIEW:
                return PaymentGateResult.builder()
                    .status(PaymentGateStatus.PENDING_REVIEW)
                    .approved(false)
                    .reason("Transaction requires manual review")
                    .riskDecision(decision)
                    .requiresManualReview(true)
                    .estimatedReviewTimeMinutes(60)
                    .build();
                    
            case BLOCK:
                return PaymentGateResult.builder()
                    .status(PaymentGateStatus.BLOCKED)
                    .approved(false)
                    .reason("Transaction blocked due to security concerns")
                    .riskDecision(decision)
                    .blockingReason(decision.getBlockingReason())
                    .build();
                    
            default:
                return PaymentGateResult.builder()
                    .status(PaymentGateStatus.ERROR)
                    .approved(false)
                    .reason("Unknown security action: " + decision.getAction())
                    .riskDecision(decision)
                    .build();
        }
    }
    
    /**
     * Xử lý trường hợp cần additional authentication
     */
    private PaymentGateResult handleAdditionalAuthRequired(RiskDecision decision, TransactionContext context) {
        String authToken = generateAuthenticationToken(context);
        
        // Determine loại authentication cần thiết
        String authMethod = determineRequiredAuthMethod(decision, context);
        
        // Store pending authentication
        PendingAuthentication pending = PendingAuthentication.builder()
            .token(authToken)
            .userId(context.getUserId())
            .orderId(context.getOrderId())
            .authMethod(authMethod)
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300)) // 5 minutes
            .transactionContext(context)
            .riskDecision(decision)
            .build();
            
        pendingAuthentications.put(authToken, pending);
        
        return PaymentGateResult.builder()
            .status(PaymentGateStatus.REQUIRES_ADDITIONAL_AUTH)
            .approved(false)
            .reason("Additional authentication required")
            .riskDecision(decision)
            .authenticationToken(authToken)
            .requiredAuthMethod(authMethod)
            .authTimeoutSeconds(300)
            .build();
    }
    
    /**
     * Xác định phương thức authentication cần thiết
     */
    private String determineRequiredAuthMethod(RiskDecision decision, TransactionContext context) {
        
        // High-value transactions -> 3D Secure
        if (context.getTotalAmount().compareTo(new BigDecimal("1000000")) > 0) {
            return "3DS"; // 3D Secure
        }
        
        // Device/location issues -> SMS OTP
        if (decision.getRiskFactors().containsKey("user_identity")) {
            return "SMS_OTP";
        }
        
        // Price/product integrity issues -> Email confirmation
        if (decision.getRiskFactors().containsKey("price_accuracy") || 
            decision.getRiskFactors().containsKey("product_integrity")) {
            return "EMAIL_CONFIRMATION";
        }
        
        // Default -> SMS OTP
        return "SMS_OTP";
    }
    
    /**
     * Verify Additional Authentication - Được gọi sau khi user hoàn thành auth
     */
    public PaymentGateResult verifyAdditionalAuthentication(String authToken, String authCode) {
        log.info("🔐 Verifying additional authentication: {}", authToken);
        
        PendingAuthentication pending = pendingAuthentications.get(authToken);
        if (pending == null) {
            return PaymentGateResult.builder()
                .status(PaymentGateStatus.ERROR)
                .approved(false)
                .reason("Invalid or expired authentication token")
                .build();
        }
        
        // Check expiry
        if (Instant.now().isAfter(pending.getExpiresAt())) {
            pendingAuthentications.remove(authToken);
            return PaymentGateResult.builder()
                .status(PaymentGateStatus.ERROR)
                .approved(false)
                .reason("Authentication token expired")
                .build();
        }
        
        // Verify auth code
        boolean authValid = verifyAuthenticationCode(pending.getAuthMethod(), authCode, pending);
        
        if (!authValid) {
            // Increment failed attempts
            pending.setFailedAttempts(pending.getFailedAttempts() + 1);
            
            if (pending.getFailedAttempts() >= 3) {
                pendingAuthentications.remove(authToken);
                return PaymentGateResult.builder()
                    .status(PaymentGateStatus.BLOCKED)
                    .approved(false)
                    .reason("Too many failed authentication attempts")
                    .build();
            }
            
            return PaymentGateResult.builder()
                .status(PaymentGateStatus.AUTH_FAILED)
                .approved(false)
                .reason("Invalid authentication code")
                .remainingAttempts(3 - pending.getFailedAttempts())
                .build();
        }
        
        // Authentication successful - approve transaction
        pendingAuthentications.remove(authToken);
        
        String transactionKey = generateTransactionKey(pending.getTransactionContext());
        cacheApprovedTransaction(transactionKey, pending.getRiskDecision());
        
        return PaymentGateResult.builder()
            .status(PaymentGateStatus.APPROVED)
            .approved(true)
            .reason("Transaction approved after additional authentication")
            .riskDecision(pending.getRiskDecision())
            .paymentProcessorRecommendation("PROCEED_NORMAL")
            .build();
    }
    
    /**
     * Get Payment Security Requirements - Để payment service biết requirements
     */
    public PaymentSecurityRequirements getPaymentRequirements(Long orderId, Long userId) {
        String transactionKey = orderId + "_" + userId;
        ApprovedTransaction approval = approvedTransactions.get(transactionKey);
        
        if (approval == null) {
            return PaymentSecurityRequirements.builder()
                .requiresValidation(true)
                .reason("No prior approval found - full validation required")
                .build();
        }
        
        RiskDecision decision = approval.getDecision();
        
        return PaymentSecurityRequirements.builder()
            .requiresValidation(false)
            .approved(true)
            .riskLevel(decision.getRiskLevel())
            .requiresMonitoring(decision.isRequiresRealTimeMonitoring())
            .requires3DSecure(approval.getDecision().getAdditionalAuthRequired() != null && 
                             approval.getDecision().getAdditionalAuthRequired().contains("3DS"))
            .fraudScore(decision.getOverallScore())
            .paymentProcessorRecommendation(determinePaymentProcessorAction(decision))
            .build();
    }
    
    // Helper Methods
    
    private String generateTransactionKey(TransactionContext context) {
        return context.getOrderId() + "_" + context.getUserId() + "_" + 
               context.getTotalAmount().toString();
    }
    
    private String generateAuthenticationToken(TransactionContext context) {
        return "auth_" + System.currentTimeMillis() + "_" + 
               context.getOrderId() + "_" + context.getUserId();
    }
    
    private boolean isApprovalStillValid(ApprovedTransaction approval) {
        // Approval valid for 15 minutes
        return Instant.now().isBefore(approval.getExpiresAt());
    }
    
    private void cacheApprovedTransaction(String key, RiskDecision decision) {
        ApprovedTransaction approval = ApprovedTransaction.builder()
            .decision(decision)
            .approvedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(900)) // 15 minutes
            .build();
        approvedTransactions.put(key, approval);
    }
    
    private boolean verifyAuthenticationCode(String authMethod, String code, PendingAuthentication pending) {
        // TODO: Implement actual verification based on auth method
        switch (authMethod) {
            case "SMS_OTP":
                return verifySmsOtp(code, pending.getUserId());
            case "EMAIL_CONFIRMATION":
                return verifyEmailConfirmation(code, pending.getUserId());
            case "3DS":
                return verify3DSecure(code, pending);
            default:
                return false;
        }
    }
    
    private boolean verifySmsOtp(String code, String userId) {
        // TODO: Implement SMS OTP verification
        return "123456".equals(code); // Mock implementation
    }
    
    private boolean verifyEmailConfirmation(String code, String userId) {
        // TODO: Implement email confirmation
        return code != null && code.length() == 6;
    }
    
    private boolean verify3DSecure(String code, PendingAuthentication pending) {
        // TODO: Implement 3D Secure verification  
        return code != null && !code.isEmpty();
    }
    
    private String determinePaymentProcessorAction(RiskDecision decision) {
        if (decision.isHighRisk()) {
            return "ENHANCED_VERIFICATION";
        } else if (decision.isRequiresRealTimeMonitoring()) {
            return "PROCEED_WITH_MONITORING";
        } else {
            return "PROCEED_NORMAL";
        }
    }
    
    private void logSecurityEvent(TransactionContext context, RiskDecision decision, PaymentGateResult result) {
        log.info("🔍 Security Event - Order: {}, User: {}, Score: {}, Action: {}, Result: {}", 
                context.getOrderId(), 
                context.getUserId(),
                decision.getOverallScore(),
                decision.getAction(),
                result.getStatus());
    }
    
    // Inner Classes
    
    @lombok.Data
    @lombok.Builder
    private static class ApprovedTransaction {
        private RiskDecision decision;
        private Instant approvedAt;
        private Instant expiresAt;
    }
    
    @lombok.Data
    @lombok.Builder
    private static class PendingAuthentication {
        private String token;
        private String userId;
        private Long orderId;
        private String authMethod;
        private Instant createdAt;
        private Instant expiresAt;
        private int failedAttempts;
        private TransactionContext transactionContext;
        private RiskDecision riskDecision;
    }
}