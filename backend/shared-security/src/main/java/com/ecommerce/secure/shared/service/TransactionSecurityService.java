package com.ecommerce.secure.shared.service;

import com.ecommerce.secure.shared.dto.TransactionContext;
import com.ecommerce.secure.shared.dto.TransactionRisk;
import com.ecommerce.secure.shared.dto.RiskDecision;
import com.ecommerce.secure.shared.dto.UserVerificationResult;
import com.ecommerce.secure.shared.dto.SessionValidationResult;
import com.ecommerce.secure.shared.dto.ProductIntegrityResult;
import com.ecommerce.secure.shared.dto.PriceValidationResult;
import com.ecommerce.secure.shared.dto.TransactionStatusResult;
import com.ecommerce.secure.shared.dto.FraudAnalysisResult;
import com.ecommerce.secure.shared.enums.RiskLevel;
import com.ecommerce.secure.shared.enums.SecurityAction;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
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
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Kiểm tra toàn diện giao dịch - Security Gate chính
     */
    public RiskDecision validateTransaction(TransactionContext context) {
        logger.info("🔒 Starting comprehensive transaction validation for user: {}, order: {}", 
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
        
        logger.info("🛡️ Transaction validation completed. Overall Score: {}, Decision: {}, Action: {}", 
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
        
        // Recent Failed Login Attempts
        int failedAttempts = getRecentFailedAttempts(context.getUserId());
        if (failedAttempts > MAX_FAILED_ATTEMPTS) {
            score -= 30;
            issues.add("Multiple failed login attempts detected");
            riskLevel = RiskLevel.CRITICAL;
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
        
        // Session Hijacking Detection
        if (detectSessionHijacking(context)) {
            score -= 40;
            riskLevel = RiskLevel.CRITICAL;
            issues.add("Potential session hijacking detected");
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
            // Generate current product hash from actual Database info
            String currentHash = generateProductHash(item.getProductId());
            String originalHash = item.getProductHash();
            
            productHashes.put(item.getProductId().toString(), currentHash);
            
            // Compare hashes to check for product tampering
            if (currentHash.isEmpty() || !currentHash.equals(originalHash)) {
                score -= 40;
                riskLevel = RiskLevel.HIGH;
                issues.add("Product ID " + item.getProductId() + " integrity check failed (Tampering detected)");
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
        
        // Recalculate total from current prices in Database
        for (var item : context.getOrderItems()) {
            BigDecimal currentPrice = getCurrentPrice(item.getProductId());
            BigDecimal itemTotal = currentPrice.multiply(new BigDecimal(item.getQuantity()));
            calculatedTotal = calculatedTotal.add(itemTotal);
            
            // Check for price manipulation
            if (!currentPrice.equals(item.getPrice())) {
                score -= 40;
                riskLevel = RiskLevel.HIGH;
                issues.add("Price mismatch for product ID " + item.getProductId() + 
                          ": expected " + currentPrice + ", got " + item.getPrice());
            }
        }
        
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
    
    private boolean isDeviceRecognized(String deviceFingerprint, String userId) {
        return deviceFingerprint != null && !deviceFingerprint.isEmpty() && !deviceFingerprint.contains("curl");
    }
    
    private boolean isSuspiciousLocation(String ipAddress, String userId) {
        return ipAddress != null && (ipAddress.startsWith("10.0.") || ipAddress.startsWith("192.168.100."));
    }
    
    private int getRecentFailedAttempts(String userId) {
        return 0; // Simulated database query
    }
    
    private boolean isSessionOwnedByUser(String sessionId, String userId) {
        return sessionId != null && userId != null;
    }
    
    private boolean detectSessionHijacking(TransactionContext context) {
        return context.getIpAddress() == null || context.getDeviceFingerprint() == null;
    }
    
    private String generateProductHash(Long productId) {
        try {
            String url = "http://localhost:8081/api/products/" + productId;
            Map<String, Object> product = restTemplate.getForObject(url, Map.class);
            if (product == null) return "";
            
            String name = product.get("name").toString();
            BigDecimal price = new BigDecimal(product.get("price").toString());
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = productId + ":" + name + ":" + price.toPlainString();
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            logger.error("Failed to generate product hash for verification", e);
            return "";
        }
    }
    
    private BigDecimal getCurrentPrice(Long productId) {
        try {
            String url = "http://localhost:8081/api/products/" + productId;
            Map<String, Object> product = restTemplate.getForObject(url, Map.class);
            if (product == null) return BigDecimal.ZERO;
            return new BigDecimal(product.get("price").toString());
        } catch (Exception e) {
            logger.error("Failed to fetch product price from catalog service", e);
            return BigDecimal.ZERO;
        }
    }
    
    private boolean isDuplicateTransaction(TransactionContext context) {
        return false; // Real implementation would query orders database
    }
    
    private int getRecentOrderCount(String userId, int hours) {
        return 1; // Real implementation would query orders database
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