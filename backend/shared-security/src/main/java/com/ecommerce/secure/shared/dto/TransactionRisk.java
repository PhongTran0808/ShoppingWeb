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
    private String userId;
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