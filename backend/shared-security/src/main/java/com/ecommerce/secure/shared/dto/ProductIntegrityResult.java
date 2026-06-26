package com.ecommerce.secure.shared.dto;

import com.ecommerce.secure.shared.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductIntegrityResult {
    private int score;
    private RiskLevel riskLevel;
    private List<String> issues;
    private Map<String, String> productHashes;
    private boolean allProductsValid;
    private List<String> unavailableProducts;
}
