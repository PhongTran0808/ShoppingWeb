package com.ecommerce.secure.cart.controller;

import com.ecommerce.secure.cart.entity.Cart;
import com.ecommerce.secure.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> getCart(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(cartService.getOrCreateCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, Object> payload) {
        String userId = jwt.getSubject();
        Long productId = Long.valueOf(payload.get("productId").toString());
        Integer quantity = Integer.valueOf(payload.get("quantity").toString());
        return ResponseEntity.ok(cartService.addItemToCart(userId, productId, quantity));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<Cart> updateItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId, @RequestBody Map<String, Object> payload) {
        String userId = jwt.getSubject();
        Integer quantity = Integer.valueOf(payload.get("quantity").toString());
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, itemId, quantity));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Cart> removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(cartService.removeItemFromCart(userId, itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        cartService.clearCart(userId);
        return ResponseEntity.ok().build();
    }
}
