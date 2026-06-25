# Backend Monitoring Setup

## Thêm Prometheus Metrics vào Spring Boot Services

### 1. Thêm dependencies vào pom.xml của mỗi service

Thêm vào `backend/gateway-service/pom.xml` (và tương tự cho các service khác):

```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Micrometer Tracing (Jaeger) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
```

### 2. Cấu hình Actuator endpoints

Thêm vào `application.yml` của mỗi service:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
    tags:
      application: ${spring.application.name}
      environment: development
```

### 3. Custom Metrics trong Code

Tạo class `MetricsConfig.java` trong mỗi service:

```java
package com.ecommerce.secure.[service].config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    
    @Bean
    public Counter securityEventCounter(MeterRegistry registry) {
        return Counter.builder("security.events")
                .description("Security events counter")
                .tag("type", "general")
                .register(registry);
    }
    
    @Bean
    public Timer requestTimer(MeterRegistry registry) {
        return Timer.builder("custom.request.duration")
                .description("Request processing time")
                .register(registry);
    }
}
```

### 4. Sử dụng Metrics trong Controllers

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final Counter orderCounter;
    private final Timer requestTimer;
    
    @Autowired
    public OrderController(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.created")
                .description("Number of orders created")
                .register(registry);
        this.requestTimer = Timer.builder("orders.processing.time")
                .description("Order processing time")
                .register(registry);
    }
    
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        return requestTimer.record(() -> {
            // Process order
            Order order = orderService.createOrder(request);
            orderCounter.increment();
            return ResponseEntity.ok(order);
        });
    }
}
```

### 5. Logging to ELK Stack

Thêm Logstash appender vào `logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Logstash TCP Appender -->
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>localhost:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"${spring.application.name}"}</customFields>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="LOGSTASH"/>
    </root>
</configuration>
```

Thêm dependency:

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### 6. Structured Logging for Security Events

```java
@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    public void logSecurityEvent(String eventType, String userId, Map<String, Object> details) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("event_type", "security");
        logEntry.put("security_event_type", eventType);
        logEntry.put("user_id", userId);
        logEntry.put("timestamp", Instant.now().toString());
        logEntry.put("details", details);
        
        logger.info("Security Event: {}", new ObjectMapper().writeValueAsString(logEntry));
    }
}
```

### 7. Kafka Event Publishing

Thêm vào payment service để publish fraud detection events:

```java
@Service
public class FraudDetectionService {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    public void publishFraudAlert(String orderId, double riskScore) {
        Map<String, Object> event = Map.of(
            "event_type", "fraud_detection",
            "order_id", orderId,
            "risk_score", riskScore,
            "timestamp", Instant.now().toString()
        );
        
        kafkaTemplate.send("fraud-alerts", new ObjectMapper().writeValueAsString(event));
    }
}
```

Configuration:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

### 8. Health Checks

Thêm custom health indicators:

```java
@Component
public class VaultHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check Vault connectivity
            // vaultClient.status()
            return Health.up()
                    .withDetail("vault", "Connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("vault", "Disconnected")
                    .withException(e)
                    .build();
        }
    }
}
```

## Testing Metrics

### 1. Verify Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# All metrics
curl http://localhost:8080/actuator/metrics
```

### 2. Query Prometheus

```bash
# Open Prometheus UI
http://localhost:9091

# Query examples:
# - http_server_requests_seconds_count
# - jvm_memory_used_bytes
# - orders_created_total
```

### 3. View in Grafana

1. Open http://localhost:3001 (admin/admin)
2. Add Prometheus data source: http://prometheus:9090
3. Import dashboard or create custom panels

## Grafana Dashboard JSON

Save as `monitoring/grafana/dashboards/service-overview.json`:

```json
{
  "dashboard": {
    "title": "Service Overview",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])"
          }
        ]
      }
    ]
  }
}
```

## Next Steps

1. Apply changes to all 5 services
2. Rebuild and restart services
3. Verify metrics in Prometheus
4. Create Grafana dashboards
5. Set up alerting rules
