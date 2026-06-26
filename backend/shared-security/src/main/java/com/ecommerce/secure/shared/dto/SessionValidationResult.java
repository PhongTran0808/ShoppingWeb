package com.ecommerce.secure.shared.dto;

import com.ecommerce.secure.shared.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SessionValidationResult {
    private int score;
    private RiskLevel riskLevel;
    private List<String> issues;
    private boolean sessionValid;
    private boolean sessionExpired;
    private boolean hijackingDetected;
    private int concurrentSessionCount;
}
