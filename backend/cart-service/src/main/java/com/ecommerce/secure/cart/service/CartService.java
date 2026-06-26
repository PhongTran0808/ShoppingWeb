package com.ecommerce.secure.cart.service;

import com.ecommerce.secure.cart.entity.Cart;
import com.ecommerce.secure.cart.entity.CartItem;
import com.ecommerce.secure.cart.repository.CartItemRepository;
import com.ecommerce.secure.cart.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    public Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUserId(userId);
                    return cartRepository.save(cart);
                });
    }

    @Transactional
    public Cart addItemToCart(String userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateItemQuantity(String userId, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new SecurityException("Unauthorized access to cart item");
        }

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItemFromCart(String userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new SecurityException("Unauthorized access to cart item");
        }

        cart.getItems().remove(item);
        return cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
