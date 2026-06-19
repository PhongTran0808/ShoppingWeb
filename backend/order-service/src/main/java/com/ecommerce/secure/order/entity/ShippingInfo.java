package com.ecommerce.secure.order.entity;

import com.ecommerce.secure.order.crypto.AesCryptoConverter;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ShippingInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String customerName;

    // Sử dụng AttributeConverter để mã hóa dữ liệu trước khi lưu và giải mã khi đọc
    @Convert(converter = AesCryptoConverter.class)
    @Column(columnDefinition = "TEXT")
    private String encryptedAddress;

    @Convert(converter = AesCryptoConverter.class)
    private String encryptedPhone;
}
