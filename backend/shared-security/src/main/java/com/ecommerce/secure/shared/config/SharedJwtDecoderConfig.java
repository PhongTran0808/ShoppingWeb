package com.ecommerce.secure.shared.config;

import com.ecommerce.secure.shared.service.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import javax.crypto.spec.SecretKeySpec;

@Configuration
public class SharedJwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(JwtUtil.SECRET.getBytes(), "HMACSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}
