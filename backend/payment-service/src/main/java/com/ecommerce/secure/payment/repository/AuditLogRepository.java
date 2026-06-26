package com.ecommerce.secure.payment.repository;

import com.ecommerce.secure.payment.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByOrderId(Long orderId);
}
