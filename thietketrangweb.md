# SYSTEM SPECIFICATION AND IMPLEMENTATION GUIDE: SECURE E-COMMERCE MICROSERVICES PROTOTYPE

## 1. Project Context & Goals
You are an expert Software Engineer and Security Architect. Your task is to generate the scaffolding, configuration, and core business logic for a secure, microservices-based e-commerce platform. This is a prototype focused heavily on **Applied Cryptography, API Security, and Data Protection**.

## 2. Technology Stack
* **Backend Framework:** Java 21 with Spring Boot 3.x (Spring Web, Spring Security, Spring Data JPA).
* **API Gateway:** Spring Cloud Gateway (or Envoy config if strictly required, but Spring Cloud Gateway is preferred for seamless Java integration).
* **Identity Provider (IdP):** Keycloak (Dockerized).
* **Secrets & Key Management (KMS):** HashiCorp Vault.
* **Database:** PostgreSQL (with pg_crypto for field-level encryption).
* **Infrastructure:** Docker & Docker Compose (for local lab environment).

## 3. Microservices Architecture
The system consists of the following isolated services. Generate a multi-module Maven or Gradle project containing these components:

### 3.1. API Gateway (`gateway-service`)
* **Port:** 8080
* **Role:** Single entry point, routing, TLS termination.
* **Security:** Token Relay, Global Rate Limiting (Redis-based), standard WAF-like headers.

### 3.2. Catalog Service (`catalog-service`)
* **Port:** 8081
* **Role:** Manage product listings.
* **Security:** Public read access. Endpoints for CRUD require `SCOPE_admin`.

### 3.3. Cart Service (`cart-service`)
* **Port:** 8082
* **Role:** Manage user shopping carts.
* **Security:** Requires valid OAuth2/OIDC token. Users can only access their own cart (extracted from JWT `sub` claim).

### 3.4. Order Service (`order-service`)
* **Port:** 8083
* **Role:** Orchestrate checkout and create orders.
* **Security:** Requires authentication. Must implement programmatic mTLS or OAuth2 Client Credentials flow when communicating with the Payment Service.

### 3.5. Payment Service (`payment-service`)
* **Port:** 8084
* **Role:** Handle payment processing using tokenized card data.
* **Security:** * STRICTLY NO PAN (Primary Account Number) storage. Use dummy payment tokens (e.g., `tok_abc123`).
    * Verify HMAC-SHA256 signatures for incoming payment requests to ensure integrity and non-repudiation.
    * Integrate with HashiCorp Vault Transit Engine to encrypt payment logs/sensitive transaction details (Envelope Encryption simulation).

## 4. Cryptography & Security Implementation Rules
When generating the code, you MUST adhere to the following security constraints:

1.  **OAuth2 Resource Server Integration:** Configure all backend services (except Gateway and Catalog read endpoints) as Spring Security OAuth2 Resource Servers. Validate JWTs using the JWK Set URI from Keycloak.
2.  **Service-to-Service Authentication:** Implement a Feign Client or WebClient configuration in `order-service` that automatically attaches a Service Account JWT (Client Credentials grant) when calling `payment-service`.
3.  **Field-Level Encryption:** In the `order-service`, create an Entity listener or use PostgreSQL `pg_crypto` to encrypt the user's shipping address before saving it to the database. Use AES-256-GCM.
4.  **HashiCorp Vault Integration:** Use Spring Cloud Vault. Database credentials must be injected dynamically via Vault, NOT hardcoded in `application.yml`.
5.  **Request Signing:** Implement an Interceptor/Filter between `order-service` and `payment-service` that generates and validates an HMAC-SHA256 signature using a shared secret stored in Vault. The signature must cover the request body and a timestamp header to prevent Replay Attacks.

## 5. Execution Steps for the AI Agent
Please generate the project in the following order:

* **Step 1:** Generate the `docker-compose.yml` file including PostgreSQL, Keycloak, HashiCorp Vault, and Redis. Include initialization scripts for Keycloak (realm, client) and Vault (dev mode, secrets engine).
* **Step 2:** Create the parent `pom.xml` (or `build.gradle`) with dependency management for Spring Boot, Spring Cloud, Spring Security, and Spring Cloud Vault.
* **Step 3:** Implement the `gateway-service` with routing configurations and CORS setup.
* **Step 4:** Implement the `payment-service` first, focusing on the Vault integration (Transit Secrets Engine) and HMAC verification logic.
* **Step 5:** Implement the `order-service`, including the REST endpoints, PostgreSQL entity with field-level encryption logic, and the secure client to communicate with the `payment-service`.
* **Step 6:** Implement the `catalog-service` and `cart-service` with basic CRUD and JWT extraction logic.
* **Step 7:** Output a `README.md` containing commands to spin up the infrastructure, obtain a token from Keycloak, and test the secure checkout flow via `curl`.

Please begin by outputting Step 1 and Step 2. Wait for my confirmation, then proceed to the subsequent steps. Ensure all code is production-ready, well-commented, and explicitly highlights where the cryptographic operations occur.



Ví dụ về các khía cạnh của trang web:
1. Phân luồng trải nghiệm khách hàng (User Flow)Thanh tìm kiếm thông minh (Search Bar): Cần có tính năng gợi ý từ khóa tự động (Autocomplete) giúp tìm kiếm nhanh.Bộ lọc & Sắp xếp (Filters & Sorting): Cho phép người dùng lọc sản phẩm theo giá cả, thương hiệu, màu sắc, kích thước.Trang chi tiết sản phẩm (Product Page): Phải có hình ảnh sắc nét, phần chọn biến thể (màu/size), mô tả, đánh giá (review), và nút Thêm vào giỏ hàng (Add to Cart) nổi bật.2. Hệ thống giỏ hàng và Thanh toán (Cart & Checkout)Tương tác giỏ hàng: Cho phép người dùng xem, cập nhật số lượng, thêm mã giảm giá (Promo Code) và xem tổng tiền (Subtotal).Thanh toán mô phỏng: Cung cấp quy trình thanh toán dễ dàng với các lựa chọn như thẻ tín dụng (Visa/Mastercard) hoặc ví điện tử. Trong bản demo, bạn có thể thiết lập phương thức thanh toán thử nghiệm (như Sandbox) để tránh trừ tiền thật.3. Trải nghiệm người dùng bổ sungTương thích đa thiết bị (Responsive Design): Website demo phải hiển thị chuẩn xác trên cả máy tính (Desktop) và điện thoại thông minh (Mobile).Hỗ trợ khách hàng (Live Chat): Tích hợp chatbox mô phỏng để người dùng trải nghiệm cách nhận hỗ trợ khi cần