package com.ecommerce.secure.shared.service;

import com.ecommerce.secure.shared.dto.TransactionContext;
import com.ecommerce.secure.shared.dto.TransactionRisk;
import com.ecommerce.secure.shared.dto.RiskDecision;
import com.ecommerce.secure.shared.enums.RiskLevel;
import com.ecommerce.secure.shared.enums.SecurityAction;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Transaction Security Service - Kiểm tra toàn diện giao dịch
 * Đặt giao dịch làm trung tâm bảo mật - Transaction-Centric Security
 */
@Service
public class TransactionSecurityService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionSecurityService.class);
    
    // Threshold Configuration
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000000"); // 1M VNĐ
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int SUSPICIOUS_ORDER_COUNT = 5;
    private static final long SESSION_TIMEOUT_MINUTES = 30;
    
    /**
     * Kiểm tra toàn diện giao dịch - Security Gate chính
     */
    public RiskDecision validateTransaction(TransactionContext context) {
        logger.info("Starting comprehensive transaction validation for user: {}, order: {}", 
                   context.getUserId(), context.getOrderId());
        
        TransactionRisk risk = TransactionRisk.builder()
            .orderId(context.getOrderId())
            .userId(context.getUserId())
            .timestamp(Instant.now())
            .build();
        
        // 1. Kiểm tra đúng người (User Identity Verification)
        UserVerificationResult userVerification = verifyUserIdentity(context);
        risk.setUserVerificationScore(userVerification.getScore());
        risk.addRiskFactor("user_identity", userVerification.getRiskLevel().name());
        
        // 2. Kiểm tra đúng session (Session Binding Validation)
        SessionValidationResult sessionValidation = validateSessionBinding(context);
        risk.setSessionValidityScore(sessionValidation.getScore());
        risk.addRiskFactor("session_binding", sessionValidation.getRiskLevel().name());
        
        // 3. Kiểm tra mặt hàng có bị sửa đổi (Product Integrity Check)
        ProductIntegrityResult productIntegrity = validateProductIntegrity(context);
        risk.setProductIntegrityScore(productIntegrity.getScore());
        risk.addRiskFactor("product_integrity", productIntegrity.getRiskLevel().name());
        
        // 4. Kiểm tra đúng giá tiền (Price Accuracy Verification)
        PriceValidationResult priceValidation = validatePriceAccuracy(context);
        risk.setPriceAccuracyScore(priceValidation.getScore());
        risk.addRiskFactor("price_accuracy", priceValidation.getRiskLevel().name());
        
        // 5. Kiểm tra trạng thái giao dịch (Transaction Status Verification)
        TransactionStatusResult statusCheck = validateTransactionStatus(context);
        risk.setTransactionStatusScore(statusCheck.getScore());
        risk.addRiskFactor("transaction_status", statusCheck.getRiskLevel().name());
        
        // 6. Fraud Detection - Behavioral Analysis
        FraudAnalysisResult fraudAnalysis = performFraudAnalysis(context);
        risk.setFraudScore(fraudAnalysis.getScore());
        risk.addRiskFactor("fraud_detection", fraudAnalysis.getRiskLevel().name());
        
        // 7. Calculate Overall Risk Score
        int overallScore = calculateOverallRiskScore(risk);
        risk.setOverallScore(overallScore);
        
        // 8. Make Security Decision
        RiskDecision decision = makeSecurityDecision(risk, context);
        
        logger.info("Transaction validation completed. Overall Score: {}, Decision: {}, Action: {}", 
                   overallScore, decision.getRiskLevel(), decision.getAction());
        
        return decision;
    }
    
    /**
     * 1. Kiểm tra đúng người - User Identity Verification
     */
    private UserVerificationResult verifyUserIdentity(TransactionContext context) {
        logger.debug("Verifying user identity for user: {}", context.getUserId());
        
        int score = 100; // Start with perfect score
        RiskLevel riskLevel = RiskLevel.LOW;
        List<String> issues = new ArrayList<>();
        
        // Device Fingerprint Check
        if (!isDeviceRecognized(context.getDeviceFingerprint(), context.getUserId())) {
            score -= 20;
            issues.add("Unknown device detected");
            riskLevel = RiskLevel.MEDIUM;
        }
        
        // IP Geolocation Check
        if (isSuspiciousLocation(context.getIpAddress(), context.getUserId())) {
            score -= 25;
            issues.add("Suspicious geolocation");
            riskLevel = RiskLevel.HIGH;
        }
        
        // Authentication Method Strength
        if (!hasStrongAuthentication(context)) {
            score -= 15;
            issues.add("Weak authentication method");
        }
        
        // Recent Failed Login Attempts
        int failedAttempts = getRecentFailedAttempts(context.getUserId());
        if (failedAttempts > MAX_FAILED_ATTEMPTS) {
            score -= 30;
            issues.add("Multiple failed login attempts detected");
            riskLevel = RiskLevel.CRITICAL;
        }
        
        // Account Age and Activity Pattern
        if (isNewOrInactiveAccount(context.getUserId())) {
            score -= 10;
            issues.add("New or inactive account");
        }
        
        return UserVerificationResult.builder()
            .score(Math.max(0, score))
            .riskLevel(riskLevel)
            .issues(issues)
            .deviceRecognized(isDeviceRecognized(context.getDeviceFingerprint(), context.getUserId()))
            .locationTrusted(!isSuspiciousLocation(context.getIpAddress(), context.getUserId()))
            .build();
    }
    
    /**
     * 2. Kiểm tra Session Binding - Đảm bảo session thuộc về đúng người
     */
    private SessionValidationResult validateSessionBinding(TransactionContext context) {
        logger.debug("Validating session binding for session: {}", context.getSessionId());
        
        int score = 100;
        RiskLevel riskLevel = RiskLevel.LOW;
        List<String> issues = new ArrayList<>();
        
        // Session Ownership Verification
        if (!isSessionOwnedByUser(context.getSessionId(), context.getUserId())) {
            score = 0;
            riskLevel = RiskLevel.CRITICAL;
            issues.add("Session does not belong to user");
            return SessionValidationResult.builder()
                .score(score)
                .riskLevel(riskLevel)
                .issues(issues)
                .sessionValid(false)
                .build();
        }
        
        // Session Timeout Check
        if (isSessionExpired(context.getSessionId())) {
            score -= 50;
            riskLevel = RiskLevel.HIGH;
            issues.add("Session has expired");
        }
        
        // Session Hijacking Detection
        if (detectSessionHijacking(context)) {
            score -= 40;
            riskLevel = RiskLevel.CRITICAL;
            issues.add("Potential session hijacking detected");
        }
        
        // Concurrent Session Check
        if (hasMultipleConcurrentSessions(context.getUserId())) {
            score -= 15;
            issues.add("Multiple concurrent sessions detected");
        }
        
        return SessionValidationResult.builder()
            .score(Math.max(0, score))
            .riskLevel(riskLevel)
            .issues(issues)
            .sessionValid(score > 50)
            .build();
    }
    
    /**
     * 3. Kiểm tra Product Integrity - Mặt hàng có bị sửa đổi không
     */
    private ProductIntegrityResult validateProductIntegrity(TransactionContext context) {
        logger.debug("Validating product integrity for order: {}", context.getOrderId());
        
        int score = 100;
        RiskLevel riskLevel = RiskLevel.LOW;
        List<String> issues = new ArrayList<>();
        Map<String, String> productHashes = new HashMap<>();
        
        for (var item : context.getOrderItems()) {
            // Generate current product hash
            String currentHash = generateProductHash(item.getProductId());
            String originalHash = item.getProductHash();
            
            productHashes.put(item.getProductId().toString(), currentHash);
            
            // Compare hashes
            if (!currentHash.equals(originalHash)) {
                score -= 30;
                riskLevel = RiskLevel.HIGH;
                issues.add("Product " + item.getProductId() + " has been modified");
            }
            
            // Check availability
            if (!isProductAvailable(item.getProductId(), item.getQuantity())) {
                score -= 25;
                riskLevel = RiskLevel.HIGH;
                issues.add("Product " + item.getProductId() + " is not available");
            }
            
            // Validate quantity limits
            if (exceedsQuantityLimits(item.getProductId(), item.getQuantity())) {
                score -= 20;
                issues.add("Quantity exceeds limits for product " + item.getProductId());
            }
        }
        
        return ProductIntegrityResult.builder()
            .score(Math.max(0, score))
            .riskLevel(riskLevel)
            .issues(issues)
            .productHashes(productHashes)
            .allProductsValid(issues.isEmpty())
            .build();
    }
    
    /**
     * 4. Kiểm tra Price Accuracy - Đúng giá tiền không
     */
    private PriceValidationResult validatePriceAccuracy(TransactionContext context) {
        logger.debug("Validating price accuracy for order: {}", context.getOrderId());
        
        int score = 100;
        RiskLevel riskLevel = RiskLevel.LOW;
        List<String> issues = new ArrayList<>();
        
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        BigDecimal clientTotal = context.getTotalAmount();
        
        // Recalculate total from current prices
        for (var item : context.getOrderItems()) {
            BigDecimal currentPrice = getCurrentPrice(item.getProductId());
            BigDecimal itemTotal = currentPrice.multiply(new BigDecimal(item.getQuantity()));
            calculatedTotal = calculatedTotal.add(itemTotal);
            
            // Check for price manipulation
            if (!currentPrice.equals(item.getPrice())) {
                score -= 25;
                riskLevel = RiskLevel.HIGH;
                issues.add("Price mismatch for product " + item.getProductId() + 
                          ": expected " + currentPrice + ", got " + item.getPrice());
            }
        }
        
        // Add taxes and shipping
        BigDecimal taxAmount = calculateTax(calculatedTotal);
        BigDecimal shippingAmount = calculateShipping(context);
        calculatedTotal = calculatedTotal.add(taxAmount).add(shippingAmount);
        
        // Compare with client-provided total
        BigDecimal tolerance = new BigDecimal("1.00"); // 1 VNĐ tolerance
        if (calculatedTotal.subtract(clientTotal).abs().compareTo(tolerance) > 0) {
            score = 0;
            riskLevel = RiskLevel.CRITICAL;
            issues.add("Total amount manipulation detected: expected " + 
                      calculatedTotal + ", got " + clientTotal);
        }
        
        return PriceValidationResult.builder()
            .score(score)
            .riskLevel(riskLevel)
            .issues(issues)
            .calculatedTotal(calculatedTotal)
            .clientTotal(clientTotal)
            .priceAccurate(issues.isEmpty())
            .build();
    }
    
    /**
     * 5. Kiểm tra Transaction Status - Giao dịch đã thành công chưa
     */
    private TransactionStatusResult validateTransactionStatus(TransactionContext context) {
        logger.debug("Validating transaction status for order: {}", context.getOrderId());
        
        int score = 100;
        RiskLevel riskLevel = RiskLevel.LOW;
        List<String> issues = new ArrayList<>();
        
        // Check for duplicate transactions
        if (isDuplicateTransaction(context)) {
            score -= 40;
            riskLevel = RiskLevel.HIGH;
            issues.add("Duplicate transaction detected");
        }
        
        // Check transaction timing
        if (isSuspiciousTransactionTiming(context)) {
            score -= 20;
            issues.add("Suspicious transaction timing");
        }
        
        // Validate cart state consistency
        if (!isCartStateValid(context)) {
            score -= 30;
            riskLevel = RiskLevel.HIGH;
            issues.add("Cart state inconsistency detected");
        }
        
        // Check for rapid succession orders
        if (hasRapidSuccessionOrders(context.getUserId())) {
            score -= 25;
            riskLevel = RiskLevel.MEDIUM;
            issues.add("Rapid succession orders detected");
        }
        
        return TransactionStatusResult.builder()
            .score(Math.max(0, score))
            .riskLevel(riskLevel)
            .issues(issues)
            .transactionValid(score > 70)
            .build();
    }
    
    /**
     * 6. Fraud Analysis - Phân tích gian lận
     */
    private FraudAnalysisResult performFraudAnalysis(TransactionContext context) {
        logger.debug("Performing fraud analysis for user: {}", context.getUserId());
        
        int score = 100;
        RiskLevel riskLevel = RiskLevel.LOW;
        List<String> fraudIndicators = new ArrayList<>();
        
        // High-value transaction check
        if (context.getTotalAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            score -= 15;
            fraudIndicators.add("High-value transaction");
        }
        
        // Velocity check - too many orders in short time
        int recentOrderCount = getRecentOrderCount(context.getUserId(), 24); // Last 24 hours
        if (recentOrderCount > SUSPICIOUS_ORDER_COUNT) {
            score -= 30;
            riskLevel = RiskLevel.HIGH;
            fraudIndicators.add("Unusual order velocity");
        }
        
        // Behavioral analysis
        if (isAnomalousBehavior(context)) {
            score -= 20;
            riskLevel = RiskLevel.MEDIUM;
            fraudIndicators.add("Anomalous user behavior");
        }
        
        // Payment method risk
        if (isRiskyPaymentMethod(context.getPaymentMethod())) {
            score -= 10;
            fraudIndicators.add("Risky payment method");
        }
        
        // Address verification
        if (!isShippingAddressValid(context)) {
            score -= 15;
            fraudIndicators.add("Invalid or suspicious shipping address");
        }
        
        return FraudAnalysisResult.builder()
            .score(Math.max(0, score))
            .riskLevel(riskLevel)
            .fraudIndicators(fraudIndicators)
            .recommendsManualReview(score < 60)
            .build();
    }
    
    /**
     * Calculate Overall Risk Score
     */
    private int calculateOverallRiskScore(TransactionRisk risk) {
        // Weighted average of all scores
        double userWeight = 0.25;
        double sessionWeight = 0.15;
        double productWeight = 0.20;
        double priceWeight = 0.25;
        double statusWeight = 0.10;
        double fraudWeight = 0.05;
        
        double weightedScore = 
            (risk.getUserVerificationScore() * userWeight) +
            (risk.getSessionValidityScore() * sessionWeight) +
            (risk.getProductIntegrityScore() * productWeight) +
            (risk.getPriceAccuracyScore() * priceWeight) +
            (risk.getTransactionStatusScore() * statusWeight) +
            (risk.getFraudScore() * fraudWeight);
        
        return (int) Math.round(weightedScore);
    }
    
    /**
     * Make Security Decision based on Risk Analysis
     */
    private RiskDecision makeSecurityDecision(TransactionRisk risk, TransactionContext context) {
        int overallScore = risk.getOverallScore();
        
        RiskLevel riskLevel;
        SecurityAction action;
        String reason;
        
        if (overallScore >= 80) {
            riskLevel = RiskLevel.LOW;
            action = SecurityAction.APPROVE;
            reason = "Transaction meets all security criteria";
            
        } else if (overallScore >= 60) {
            riskLevel = RiskLevel.MEDIUM;
            action = SecurityAction.APPROVE_WITH_MONITORING;
            reason = "Transaction approved with enhanced monitoring";
            
        } else if (overallScore >= 40) {
            riskLevel = RiskLevel.HIGH;
            action = SecurityAction.REQUIRE_ADDITIONAL_AUTH;
            reason = "Additional authentication required";
            
        } else if (overallScore >= 20) {
            riskLevel = RiskLevel.CRITICAL;
            action = SecurityAction.FLAG_FOR_REVIEW;
            reason = "Transaction flagged for manual review";
            
        } else {
            riskLevel = RiskLevel.CRITICAL;
            action = SecurityAction.BLOCK;
            reason = "Transaction blocked due to high risk";
        }
        
        // Override for high-value transactions
        if (context.getTotalAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0 && overallScore < 90) {
            action = SecurityAction.REQUIRE_ADDITIONAL_AUTH;
            reason = "High-value transaction requires additional verification";
        }
        
        return RiskDecision.builder()
            .riskLevel(riskLevel)
            .action(action)
            .overallScore(overallScore)
            .reason(reason)
            .riskFactors(risk.getRiskFactors())
            .recommendedActions(generateRecommendedActions(risk))
            .build();
    }
    
    // Helper Methods - Placeholder implementations
    private boolean isDeviceRecognized(String deviceFingerprint, Long userId) {
        // TODO: Implement device recognition logic
        return deviceFingerprint != null && !deviceFingerprint.isEmpty();
    }
    
    private boolean isSuspiciousLocation(String ipAddress, Long userId) {
        // TODO: Implement geolocation analysis
        return false;
    }
    
    private boolean hasStrongAuthentication(TransactionContext context) {
        // TODO: Check for MFA, biometric, etc.
        return context.getAuthenticationMethods().contains("MFA");
    }
    
    private int getRecentFailedAttempts(Long userId) {
        // TODO: Query failed authentication attempts
        return 0;
    }
    
    private boolean isNewOrInactiveAccount(Long userId) {
        // TODO: Check account age and activity
        return false;
    }
    
    private boolean isSessionOwnedByUser(String sessionId, Long userId) {
        // TODO: Verify session ownership
        return true;
    }
    
    private boolean isSessionExpired(String sessionId) {
        // TODO: Check session expiry
        return false;
    }
    
    private boolean detectSessionHijacking(TransactionContext context) {
        // TODO: Implement session hijacking detection
        return false;
    }
    
    private boolean hasMultipleConcurrentSessions(Long userId) {
        // TODO: Check for concurrent sessions
        return false;
    }
    
    private String generateProductHash(Long productId) {
        // TODO: Generate SHA-256 hash of product data
        return "hash_" + productId;
    }
    
    private boolean isProductAvailable(Long productId, int quantity) {
        // TODO: Check inventory
        return true;
    }
    
    private boolean exceedsQuantityLimits(Long productId, int quantity) {
        // TODO: Check quantity limits
        return quantity > 100;
    }
    
    private BigDecimal getCurrentPrice(Long productId) {
        // TODO: Get current price from catalog service
        return new BigDecimal("100000");
    }
    
    private BigDecimal calculateTax(BigDecimal amount) {
        // TODO: Calculate tax
        return amount.multiply(new BigDecimal("0.1"));
    }
    
    private BigDecimal calculateShipping(TransactionContext context) {
        // TODO: Calculate shipping cost
        return new BigDecimal("30000");
    }
    
    private boolean isDuplicateTransaction(TransactionContext context) {
        // TODO: Check for duplicate transactions
        return false;
    }
    
    private boolean isSuspiciousTransactionTiming(TransactionContext context) {
        // TODO: Analyze transaction timing
        return false;
    }
    
    private boolean isCartStateValid(TransactionContext context) {
        // TODO: Validate cart state
        return true;
    }
    
    private boolean hasRapidSuccessionOrders(Long userId) {
        // TODO: Check for rapid orders
        return false;
    }
    
    private int getRecentOrderCount(Long userId, int hours) {
        // TODO: Count recent orders
        return 0;
    }
    
    private boolean isAnomalousBehavior(TransactionContext context) {
        // TODO: Implement ML-based behavioral analysis
        return false;
    }
    
    private boolean isRiskyPaymentMethod(String paymentMethod) {
        // TODO: Assess payment method risk
        return false;
    }
    
    private boolean isShippingAddressValid(TransactionContext context) {
        // TODO: Validate shipping address
        return true;
    }
    
    private List<String> generateRecommendedActions(TransactionRisk risk) {
        List<String> actions = new ArrayList<>();
        if (risk.getUserVerificationScore() < 70) {
            actions.add("Require additional user verification");
        }
        if (risk.getPriceAccuracyScore() < 80) {
            actions.add("Manual price verification required");
        }
        if (risk.getFraudScore() < 60) {
            actions.add("Enhanced fraud monitoring recommended");
        }
        return actions;
    }
}