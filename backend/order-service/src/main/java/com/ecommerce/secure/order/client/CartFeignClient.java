package com.ecommerce.secure.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "cart-service", url = "http://localhost:8082")
public interface CartFeignClient {

    @GetMapping("/api/cart")
    Map<String, Object> getCart(@RequestHeader("Authorization") String bearerToken);

    @DeleteMapping("/api/cart")
    void clearCart(@RequestHeader("Authorization") String bearerToken);
}
