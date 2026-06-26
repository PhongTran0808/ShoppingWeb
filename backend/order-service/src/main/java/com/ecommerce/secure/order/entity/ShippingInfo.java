package com.ecommerce.secure.order.entity;

import com.ecommerce.secure.order.crypto.AesCryptoConverter;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "shipping_info")
@Data
public class ShippingInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    // Sử dụng AttributeConverter để mã hóa dữ liệu trước khi lưu và giải mã khi đọc
    @Convert(converter = AesCryptoConverter.class)
    @Column(name = "encrypted_address", nullable = false, columnDefinition = "TEXT")
    private String encryptedAddress;

    @Convert(converter = AesCryptoConverter.class)
    @Column(name = "encrypted_phone", nullable = false, columnDefinition = "VARCHAR(500)")
    private String encryptedPhone;

    @Column(name = "phone_blind_index", nullable = false)
    private String phoneBlindIndex;
}
