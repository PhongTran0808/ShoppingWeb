package com.ecommerce.secure.order.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@FeignClient(name = "payment-service", url = "http://localhost:8084", configuration = PaymentFeignClient.Configuration.class)
public interface PaymentFeignClient {

    @PostMapping("/api/payments")
    String processPayment(String paymentToken);

    class Configuration {
        @Value("${hmac_secret:default_secret_fallback_do_not_use_in_prod}")
        private String hmacSecret;

        // Tạo RequestInterceptor để tự động ký số (HMAC) cho mọi request gửi sang payment-service
        @Bean
        public RequestInterceptor hmacRequestInterceptor() {
            return template -> {
                String timestamp = String.valueOf(System.currentTimeMillis());
                
                // Read request body
                byte[] bodyBytes = template.body();
                String body = bodyBytes != null ? new String(bodyBytes, StandardCharsets.UTF_8) : "";
                
                String dataToSign = template.path() + body + timestamp;
                
                String signature = calculateHmac(dataToSign, hmacSecret);
                template.header("X-Timestamp", timestamp);
                template.header("X-Signature", signature);
            };
        }

        private String calculateHmac(String data, String key) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                mac.init(secretKeySpec);
                byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hmacBytes);
            } catch (Exception e) {
                throw new RuntimeException("Failed to calculate HMAC signature", e);
            }
        }
    }
}
