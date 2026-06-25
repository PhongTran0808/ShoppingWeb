package com.ecommerce.secure.shared.enums;

/**
 * Risk Level Enumeration
 */
public enum RiskLevel {
    LOW("Low Risk", 0),
    MEDIUM("Medium Risk", 1), 
    HIGH("High Risk", 2),
    CRITICAL("Critical Risk", 3);
    
    private final String description;
    private final int severity;
    
    RiskLevel(String description, int severity) {
        this.description = description;
        this.severity = severity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getSeverity() {
        return severity;
    }
    
    public boolean isHigherThan(RiskLevel other) {
        return this.severity > other.severity;
    }
    
    public boolean isAtLeast(RiskLevel other) {
        return this.severity >= other.severity;
    }
}