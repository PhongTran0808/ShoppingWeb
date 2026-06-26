package com.ecommerce.secure.shared.dto;

import com.ecommerce.secure.shared.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class FraudAnalysisResult {
    private int score;
    private RiskLevel riskLevel;
    private List<String> fraudIndicators;
    private boolean recommendsManualReview;
    private boolean highValueTransaction;
    private boolean velocityViolation;
    private boolean anomalousBehavior;
}
