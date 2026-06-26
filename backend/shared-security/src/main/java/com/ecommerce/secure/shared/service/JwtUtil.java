package com.ecommerce.secure.shared.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class JwtUtil {
    public static final String SECRET = "SecureECommercePlatformSecretKeyForJWTAuthTokenSigningHMAC256";

    public static String generateToken(String userId, String username, String role) throws Exception {
        JWSSigner signer = new MACSigner(SECRET.getBytes());
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(userId)
            .claim("preferred_username", username)
            .claim("roles", List.of(role))
            .claim("scope", role.equals("ROLE_ADMIN") ? "admin" : "user")
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + 86400 * 1000)) // 24 hours
            .build();
            
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
