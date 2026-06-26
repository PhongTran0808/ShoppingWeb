package com.ecommerce.secure.shared.monitoring;

import org.springframework.stereotype.Component;

@Component
public class SecurityMetricsCollector {
    
    public void recordTransactionSecurityEvent(SecurityEvent event) {
        // Mock metrics recording
    }
    
    public void recordAuthenticationEvent(SecurityEvent event) {
        // Mock metrics recording
    }
    
    public void recordPaymentEvent(SecurityEvent event) {
        // Mock metrics recording
    }
    
    public void recordSystemEvent(SecurityEvent event) {
        // Mock metrics recording
    }
    
    public int getRecentFailedAttempts(String userId, String ipAddress) {
        return 0; // Mock response
    }
}
