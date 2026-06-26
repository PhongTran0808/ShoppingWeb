package com.ecommerce.secure.shared.dto;

import com.ecommerce.secure.shared.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PriceValidationResult {
    private int score;
    private RiskLevel riskLevel;
    private List<String> issues;
    private BigDecimal calculatedTotal;
    private BigDecimal clientTotal;
    private boolean priceAccurate;
    private BigDecimal discrepancyAmount;
}
