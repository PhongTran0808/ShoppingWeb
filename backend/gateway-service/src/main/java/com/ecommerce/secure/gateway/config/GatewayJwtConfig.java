package com.ecommerce.secure.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import javax.crypto.spec.SecretKeySpec;

@Configuration
public class GatewayJwtConfig {
    private static final String SECRET = "SecureECommercePlatformSecretKeyForJWTAuthTokenSigningHMAC256";

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET.getBytes(), "HMACSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }
}
