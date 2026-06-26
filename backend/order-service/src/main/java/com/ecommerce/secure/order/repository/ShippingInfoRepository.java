package com.ecommerce.secure.order.repository;

import com.ecommerce.secure.order.entity.ShippingInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingInfoRepository extends JpaRepository<ShippingInfo, Long> {
    Optional<ShippingInfo> findByOrderId(Long orderId);
    Optional<ShippingInfo> findByPhoneBlindIndex(String phoneBlindIndex);
}
