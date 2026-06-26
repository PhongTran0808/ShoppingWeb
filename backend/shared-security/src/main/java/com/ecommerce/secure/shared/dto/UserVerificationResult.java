package com.ecommerce.secure.shared.dto;

import com.ecommerce.secure.shared.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserVerificationResult {
    private int score;
    private RiskLevel riskLevel;
    private List<String> issues;
    private boolean deviceRecognized;
    private boolean locationTrusted;
    private boolean hasStrongAuth;
    private String geolocationCountry;
    private String deviceType;
}
