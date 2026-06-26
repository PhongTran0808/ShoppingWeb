package com.ecommerce.secure.order.service;

import com.ecommerce.secure.order.client.CartFeignClient;
import com.ecommerce.secure.order.client.CatalogFeignClient;
import com.ecommerce.secure.order.entity.Order;
import com.ecommerce.secure.order.entity.OrderItem;
import com.ecommerce.secure.order.entity.ShippingInfo;
import com.ecommerce.secure.order.repository.OrderItemRepository;
import com.ecommerce.secure.order.repository.OrderRepository;
import com.ecommerce.secure.order.repository.ShippingInfoRepository;
import com.ecommerce.secure.order.security.PaymentGateResult;
import com.ecommerce.secure.order.security.PaymentSecurityGateway;
import com.ecommerce.secure.shared.dto.TransactionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ShippingInfoRepository shippingInfoRepository;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private CatalogFeignClient catalogFeignClient;

    @Autowired
    private PaymentSecurityGateway paymentSecurityGateway;

    @Value("${hmac_secret:default_secret_fallback_do_not_use_in_prod}")
    private String hmacSecret;

    @Transactional
    public Order createOrder(String bearerToken, String userId, String customerName, String address, String phone, String deviceFingerprint, String ipAddress, String sessionId) {
        // 1. Retrieve cart
        Map<String, Object> cart = cartFeignClient.getCart(bearerToken);
        if (cart == null || !cart.containsKey("items")) {
            throw new IllegalArgumentException("Cart is empty");
        }

        List<Map<String, Object>> cartItems = (List<Map<String, Object>>) cart.get("items");
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // 2. Build items and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<TransactionContext.OrderItem> orderContextItems = new ArrayList<>();
        List<OrderItem> jpaOrderItems = new ArrayList<>();

        for (Map<String, Object> cartItem : cartItems) {
            Long productId = Long.valueOf(cartItem.get("productId").toString());
            Integer quantity = Integer.valueOf(cartItem.get("quantity").toString());

            // Fetch product details
            Map<String, Object> product = catalogFeignClient.getProduct(productId);
            String productName = product.get("name").toString();
            BigDecimal price = new BigDecimal(product.get("price").toString());

            totalAmount = totalAmount.add(price.multiply(new BigDecimal(quantity)));

            // Compute product hash for integrity check
            String productHash = calculateProductHash(productId, productName, price);

            orderContextItems.add(TransactionContext.OrderItem.builder()
                    .productId(productId)
                    .productName(productName)
                    .price(price)
                    .quantity(quantity)
                    .productHash(productHash)
                    .build());

            OrderItem jpaItem = new OrderItem();
            jpaItem.setProductId(productId);
            jpaItem.setProductName(productName);
            jpaItem.setQuantity(quantity);
            jpaItem.setPrice(price);
            jpaOrderItems.add(jpaItem);
        }

        // 3. Build TransactionContext
        TransactionContext transactionContext = TransactionContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .totalAmount(totalAmount)
                .currency("VND")
                .timestamp(Instant.now())
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress)
                .orderItems(orderContextItems)
                .shippingDetails(TransactionContext.ShippingDetails.builder()
                        .fullName(customerName)
                        .address(address)
                        .phoneNumber(phone)
                        .build())
                .build();

        // 4. Validate through security gateway
        PaymentGateResult result = paymentSecurityGateway.validatePaymentTransition(transactionContext);
        if (!result.isApproved()) {
            throw new SecurityException("Transaction security check failed: " + result.getReason());
        }

        // 5. Save Order & items
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING_PAYMENT");
        order.setTrackingNumber("TRK" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        
        final Order savedOrder = orderRepository.save(order);

        for (OrderItem item : jpaOrderItems) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }

        // 6. Save ShippingInfo with blind index
        ShippingInfo shippingInfo = new ShippingInfo();
        shippingInfo.setOrderId(savedOrder.getId());
        shippingInfo.setCustomerName(customerName);
        shippingInfo.setEncryptedAddress(address);
        shippingInfo.setEncryptedPhone(phone);
        shippingInfo.setPhoneBlindIndex(calculateBlindIndex(phone, hmacSecret));
        shippingInfoRepository.save(shippingInfo);

        // 7. Clear cart
        cartFeignClient.clearCart(bearerToken);

        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long id, String userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized access to order");
        }
        return order;
    }

    public Order getOrderByIdWithoutUserCheck(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    private String calculateProductHash(Long id, String name, BigDecimal price) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = id + ":" + name + ":" + price.toPlainString();
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate product hash", e);
        }
    }

    private String calculateBlindIndex(String phone, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(phone.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate blind index", e);
        }
    }
}
