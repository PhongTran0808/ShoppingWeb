package com.ecommerce.secure.payment.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class HmacVerificationFilter extends OncePerRequestFilter {

    @Value("${hmac_secret}")
    private String hmacSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().startsWith("/api/payments")) {
            String signature = request.getHeader("X-Signature");
            String timestamp = request.getHeader("X-Timestamp");

            if (signature == null || timestamp == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing HMAC Signature or Timestamp");
                return;
            }

            // Chống Replay Attack (Giới hạn timestamp trong vòng 5 phút)
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            if (currentTime - requestTime > 300000) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Timestamp expired (Replay Attack Prevention)");
                return;
            }

            // Xác thực toàn vẹn bằng cách băm lại dữ liệu URI + Timestamp
            // Ghi chú: Trong thực tế nên dùng ContentCachingRequestWrapper để băm cả body JSON.
            String dataToSign = request.getRequestURI() + timestamp;
            String calculatedSignature = calculateHmac(dataToSign, hmacSecret);

            if (!calculatedSignature.equals(signature)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid HMAC Signature");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String calculateHmac(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC", e);
        }
    }
}
