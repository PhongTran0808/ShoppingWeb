package com.ecommerce.secure.shared.monitoring;

import com.ecommerce.secure.shared.enums.SecurityAlertType;
import com.ecommerce.secure.shared.enums.SecuritySeverity;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class SecurityAlert {
    private String alertId;
    private Instant timestamp;
    private SecurityAlertType alertType;
    private SecuritySeverity severity;
    private SecurityEvent sourceEvent;
    private String title;
    private String description;
    private String actionRequired;
    private Map<String, Object> metadata;
}
