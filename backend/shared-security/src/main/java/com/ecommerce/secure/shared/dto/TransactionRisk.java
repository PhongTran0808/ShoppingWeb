package com.ecommerce.secure.shared.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Transaction Risk Assessment Result
 */
@Data
@Builder
public class TransactionRisk {
    
    private Long orderId;
    private Long userId;
    private Instant timestamp;
    
    // Individual Risk Scores (0-100, higher is better)
    private int userVerificationScore;
    private int sessionValidityScore;
    private int productIntegrityScore;
    private int priceAccuracyScore;
    private int transactionStatusScore;
    private int fraudScore;
    
    // Overall Risk Assessment
    private int overallScore;
    
    // Risk Factors Map
    @Builder.Default
    private Map<String, String> riskFactors = new HashMap<>();
    
    // Helper method to add risk factors
    public void addRiskFactor(String category, String value) {
        if (riskFactors == null) {
            riskFactors = new HashMap<>();
        }
        riskFactors.put(category, value);
    }
}

/**
 * User Verification Result
 */
@Data
@Builder
public class UserVerificationResult {
    private int score;
    private RiskLevel riskLevel;
    private java.util.List<String> issues;
    private boolean deviceRecognized;
    private boolean locationTrusted;
    private boolean hasStrongAuth;
    private String geolocationCountry;
    private String deviceType;
}

/**
 * Session Validation Result
 */
@Data
@Builder
public class SessionValidationResult {
    private int score;
    private RiskLevel riskLevel;
    private java.util.List<String> issues;
    private boolean sessionValid;
    private boolean sessionExpired;
    private boolean hijackingDetected;
    private int concurrentSessionCount;
}

/**
 * Product Integrity Result
 */
@Data
@Builder
public class ProductIntegrityResult {
    private int score;
    private RiskLevel riskLevel;
    private java.util.List<String> issues;
    private Map<String, String> productHashes;
    private boolean allProductsValid;
    private java.util.List<String> unavailableProducts;
}

/**
 * Price Validation Result
 */
@Data
@Builder  
public class PriceValidationResult {
    private int score;
    private RiskLevel riskLevel;
    private java.util.List<String> issues;
    private java.math.BigDecimal calculatedTotal;
    private java.math.BigDecimal clientTotal;
    private boolean priceAccurate;
    private java.math.BigDecimal discrepancyAmount;
}

/**
 * Transaction Status Result
 */
@Data
@Builder
public class TransactionStatusResult {
    private int score;
    private RiskLevel riskLevel;
    private java.util.List<String> issues;
    private boolean transactionValid;
    private boolean isDuplicate;
    private boolean suspiciousTiming;
}

/**
 * Fraud Analysis Result
 */
@Data
@Builder
public class FraudAnalysisResult {
    private int score;
    private RiskLevel riskLevel;
    private java.util.List<String> fraudIndicators;
    private boolean recommendsManualReview;
    private boolean highValueTransaction;
    private boolean velocityViolation;
    private boolean anomalousBehavior;
}