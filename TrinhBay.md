# TÀI LIỆU TRÌNH BÀY ĐỒ ÁN
## Thiết kế, Triển khai & Đánh giá An toàn Mật mã cho Hệ thống Thương mại Điện tử
**Môn:** NT219 - Mật mã Ứng dụng (Cryptography) — HCMUTE  
**Nhóm:** Nguyễn Thành An (23162001) — Huỳnh Thanh Tú (23162111) — Xín Lợi Huy (23110231)

---

## 1. HIỂU GÌ VỀ ĐỀ TÀI

Đề tài yêu cầu xây dựng một **prototype hệ thống thương mại điện tử** (như Shopee, Amazon) tập trung vào **áp dụng mật mã học để bảo vệ giao dịch** xuyên suốt toàn bộ hành trình người dùng:

```
Đúng Người → Đúng Sản Phẩm → Đúng Giá → Giao Dịch Thành Công
```

Trọng tâm **không phải** là xây dựng website mua sắm thông thường, mà là **thiết kế và chứng minh** rằng các kỹ thuật mật mã (JWT, AES-GCM, HMAC, SHA3, Vault KMS) có thể ngăn chặn tấn công thực tế trong thương mại điện tử.

---

## 2. YÊU CẦU ĐỀ TÀI & THỰC HIỆN TƯƠNG ỨNG

| # | Yêu cầu đề tài | Đã thực hiện |
|---|---|---|
| 1 | **Xác thực người dùng** (OAuth2, JWT, MFA) | JWT HS256 (nimbus-jose-jwt), SHA3-512 password hash, RBAC (ROLE_USER / ROLE_ADMIN) |
| 2 | **Thanh toán an toàn** (tokenization, không lưu PAN) | Token hóa qua VietQR; payment token được xác thực HMAC trước khi xử lý |
| 3 | **Quản lý khóa (KMS/Vault)** + Envelope Encryption | HashiCorp Vault Transit Engine; fallback AES-GCM local nếu Vault offline |
| 4 | **Mã hóa dữ liệu PII** trong DB | AesCryptoConverter — AES-256-GCM với IV ngẫu nhiên cho address, phone |
| 5 | **Xác thực giữa microservices** (HMAC) | HmacVerificationFilter tại payment-service; Order-service tự ký HMAC |
| 6 | **API Gateway + Rate Limiting** | Spring Cloud Gateway (port 8080) kiểm tra JWT |
| 7 | **Kiến trúc Microservices** | 5 services: gateway, catalog, cart, order, payment |
| 8 | **Phát hiện gian lận** | PaymentSecurityGateway — tính risk score, quyết định APPROVE / BLOCK |
| 9 | **Blind Index** tìm kiếm trên dữ liệu mã hóa | HMAC-SHA256(phone, secret) lưu song song với phone đã AES |
| 10 | **Kiểm thử bảo mật** | experiments/token-replay-test, experiments/api-abuse-test |
| 11 | **HTTPS/TLS** end-to-end | Apache reverse proxy + self-signed cert; tienthienvienman.site.je |
| 12 | **Monitoring & Audit Log** | Bảng audit_log ghi mọi event thanh toán; Prometheus + Grafana |

---

## 3. CÁC RỦI RO & CÁCH PROJECT GIẢI QUYẾT

### Triết lý: Đúng Người → Đúng Sản Phẩm → Đúng Giá → Giao Dịch Thành Công

---

### RỦI RO 1: SAI NGƯỜI (Xác thực danh tính)

| Kịch bản tấn công | Giải pháp đã triển khai |
|---|---|
| Đăng nhập bằng mật khẩu bị lộ | Password hash bằng **SHA3-512** — không lưu mật khẩu gốc |
| Token bị đánh cắp & replay | JWT signed bằng HS256 + secret; TTL 24h; claims chuẩn RFC |
| Giả mạo JWT (alg:none attack) | nimbus-jose-jwt bắt buộc HS256; từ chối alg:none |
| Truy cập tài nguyên user khác | RBAC: ROLE_USER chỉ xem order của họ; ROLE_ADMIN xem tất cả |
| Credential Stuffing | Rate limiting tại API Gateway; is_active để vô hiệu hóa |

**Code minh chứng (UserController.java):**
```java
MessageDigest digest = MessageDigest.getInstance("SHA3-512");
byte[] hashedBytes = digest.digest(password.getBytes(UTF_8));
String computedHash = HexFormat.of().formatHex(hashedBytes);
if (!computedHash.equalsIgnoreCase(user.getPasswordHash())) {
    return ResponseEntity.status(401).body("Invalid credentials");
}
```

---

### RỦI RO 2: SAI SẢN PHẨM (Toàn vẹn sản phẩm)

| Kịch bản tấn công | Giải pháp đã triển khai |
|---|---|
| Giả mạo / đổi sản phẩm giữa chừng | Product Hash — SHA-256(productId + name + price) khi đưa vào đơn |
| Client gửi productId tùy ý | Order-service verify từ catalog-service trực tiếp (không tin client) |
| Sản phẩm hết hàng vẫn đặt | Real-time kiểm tra availability khi tạo đơn |

**Code minh chứng (OrderService.java):**
```java
// Không tin client — lấy giá thực từ catalog-service
Map<String, Object> product = catalogFeignClient.getProduct(productId);
BigDecimal price = new BigDecimal(product.get("price").toString());
String productHash = calculateProductHash(productId, productName, price);
```

---

### RỦI RO 3: SAI GIÁ (Price Tampering)

| Kịch bản tấn công | Giải pháp đã triển khai |
|---|---|
| Client tự gửi price thấp | Giá fetch từ catalog-service tại server — client không kiểm soát |
| Sửa giá trong cart | Total tính lại từ đầu tại order-service — không dùng giá từ cart |
| Man-in-the-middle sửa amount | HTTPS/TLS; HMAC ký toàn bộ request khi gọi payment-service |

**Code minh chứng (PaymentFeignClient.java):**
```java
// HMAC ký request order-service → payment-service
String dataToSign = "orderId=" + orderId + "&amount=" + amount;
String signature = calculateHmac(dataToSign, hmacSecret);
requestTemplate.header("X-HMAC-Signature", signature);
requestTemplate.header("X-Timestamp", String.valueOf(Instant.now().toEpochMilli()));
```

---

### RỦI RO 4: NGỤY TẠO GIAO DỊCH / GIẢ MẠO THANH TOÁN

| Kịch bản tấn công | Giải pháp đã triển khai |
|---|---|
| Replay giao dịch cũ | HMAC timestamp check — request quá 5 phút bị từ chối |
| Giả mạo HMAC | HmacVerificationFilter — 401 nếu thiếu, 403 nếu sai HMAC |
| Tạo đơn không trả tiền | State machine: PENDING_PAYMENT → PAID (payment-service confirm mới update) |
| Fraud đặt hàng bất thường | PaymentSecurityGateway — risk score → APPROVE / REQUIRE_AUTH / BLOCK |
| Log giả mạo | Log mã hóa bằng Vault Transit Engine trước khi lưu DB |
| Lộ địa chỉ, SĐT | AES-256-GCM field-level encryption — raw data không tồn tại trong DB |
| Tìm kiếm trên data mã hóa | Blind Index = HMAC-SHA256(phone, secret) — deterministic, không đảo ngược |

**Code minh chứng (HmacVerificationFilter.java + ShippingInfo.java):**
```java
// Filter chặn request không có HMAC (payment-service)
String hmacSignature = request.getHeader("X-HMAC-Signature");
String timestamp = request.getHeader("X-Timestamp");
if (hmacSignature == null || timestamp == null) {
    response.sendError(401, "Missing HMAC Signature or Timestamp");
    return;
}
// Kiểm tra timestamp không quá 5 phút (chống replay attack)

// AES-256-GCM field-level encryption (ShippingInfo entity)
@Convert(converter = AesCryptoConverter.class)
@Column(name = "encrypted_address", columnDefinition = "TEXT")
private String encryptedAddress; // DB chứa: IV(12B) + GCM_ciphertext, base64

// Blind Index để tìm kiếm mà không giải mã
shippingInfo.setPhoneBlindIndex(calculateBlindIndex(phone, hmacSecret));
```

---

### RỦI RO 5: LỘ DỮ LIỆU / KHÔNG TUÂN THỦ PCI-DSS

| Kịch bản tấn công | Giải pháp đã triển khai |
|---|---|
| DB bị dump toàn bộ | Data nhạy cảm đã mã hóa; mất DB không đủ để đọc vì cần key từ Vault |
| Lưu PAN (số thẻ) trực tiếp | Không lưu PAN — chỉ dùng payment token VietQR + tracking number |
| Secrets hardcode trong code | Keys inject qua @Value từ application.yml / Vault; Vault là nguồn truth |
| Logs chứa thông tin nhạy cảm | Audit log mã hóa bằng Vault Transit trước khi lưu |

---

## 4. KỊCH BẢN DEMO KHI BÁO CÁO

### Kịch bản 5 bước (~2 phút mỗi bước)

---

#### BƯỚC 1 — Đúng Người: Đăng ký & Đăng nhập an toàn

**Mục tiêu:** Chứng minh mật khẩu được hash SHA3-512, không lưu plaintext.

1. Mở **https://tienthienvienman.site.je** → Đăng ký user mới
2. Vào MySQL: `SELECT username, password_hash FROM users;`
3. **Thấy:** Chuỗi hex 128 ký tự — không phải mật khẩu gốc (SHA3-512)
4. Đăng nhập → DevTools → LocalStorage → `vault_token` chứa JWT
5. Decode tại **jwt.io** → thấy `sub`, `roles: ROLE_USER`, `exp`

> *"Dù DB bị tấn công, attacker không thể tìm lại mật khẩu từ hash SHA3-512"*

---

#### BƯỚC 2 — Đúng Sản Phẩm: Server-side price validation

**Mục tiêu:** Chứng minh server không tin giá từ client.

1. Dùng **Postman**: gửi checkout với `price: 1` (giả mạo giá thấp)
2. **Server recalculate** từ catalog-service → giá đúng hoặc báo lỗi
3. Log order-service: `Fetching product from catalog: productId=X, price=Y`

> *"Giá luôn lấy từ catalog-service tại server — client không thể can thiệp"*

---

#### BƯỚC 3 — Đúng Giá: HMAC bảo vệ service-to-service

**Mục tiêu:** Chứng minh giao tiếp giữa services được xác thực mật mã.

1. Checkout → quét QR → nhấn "Xác nhận thanh toán"
2. Log payment-service: thấy `X-HMAC-Signature` và `X-Timestamp`
3. **Demo attack:** Postman gọi thẳng `/api/payments/process` **không có HMAC**
4. Kết quả: `401 Unauthorized — Missing HMAC Signature`
5. Thử HMAC sai: `403 Forbidden — Invalid HMAC Signature`

> *"Chỉ order-service được phép gọi payment-service — attacker không thể giả mạo"*

---

#### BƯỚC 4 — Giao Dịch Thành Công + Mã hóa dữ liệu

**Mục tiêu:** Chứng minh thông tin giao hàng được mã hóa trong DB.

1. Sau thanh toán → Admin dashboard → Trạng thái "Đã thanh toán"
2. MySQL: `SELECT encrypted_address, encrypted_phone FROM shipping_info;`
3. **Thấy:** Chuỗi Base64 ngẫu nhiên — không đọc được địa chỉ hay SĐT
4. Bảng `transactions`: `encrypted_log` = chuỗi mã hóa từ Vault

> *"Dù dump DB, địa chỉ & SĐT vẫn an toàn — AES-256-GCM với IV ngẫu nhiên"*

---

#### BƯỚC 5 — Kiểm thử tấn công: Token Replay & API Abuse

**Mục tiêu:** Chứng minh hệ thống chống tấn công tự động.

1. `python experiments/token-replay-test/test_token_replay.py`
   → Token từ device A bị từ chối khi dùng từ IP khác
2. `python experiments/api-abuse-test/test_rate_limiting.py`
   → Sau nhiều request: `429 Too Many Requests`
3. Grafana (`localhost:3001`) → audit log real-time

> *"Hệ thống phát hiện và chặn tấn công tự động trong thời gian thực"*

---

## 5. TRẢ LỜI CÁC CÂU HỎI NGHIÊN CỨU (YEUCAU.md)

---

### RQ1: Điểm yếu mật mã nào thường gây compromise trong e-commerce?

1. **JWT misconfiguration** — alg:none, HS256 secret yếu. **Giải pháp:** nimbus-jose-jwt buộc HS256, secret 64 ký tự.
2. **Password lưu plaintext / MD5** — **Giải pháp:** SHA3-512 (Keccak family, quantum-resistant hơn SHA-256).
3. **PAN lưu trực tiếp** — **Giải pháp:** Không lưu PAN, dùng VietQR token.
4. **PII không mã hóa trong DB** — **Giải pháp:** AES-256-GCM field-level cho address, phone.
5. **Service-to-service không xác thực** — **Giải pháp:** HMAC-SHA256 với timestamp (chống replay).

---

### RQ2: Tokenization có giảm rủi ro gian lận thanh toán không?

**Có, nhưng phụ thuộc triển khai.**

- **VietQR token** — số thẻ không bao giờ đến server hệ thống (zero PAN)
- **HMAC timestamp window 5 phút** → replay attack bị chặn hoàn toàn
- **Trade-off:** Latency tăng ~50-100ms (Vault calls), đổi lại attack surface giảm đáng kể
- **PaymentSecurityGateway** thêm lớp risk scoring — quyết định APPROVE / BLOCK real-time

---

### RQ3: HSM/KMS vs software keys — hiệu quả và chi phí?

| Tiêu chí | HashiCorp Vault (project dùng) | HSM thật (AWS CloudHSM) |
|---|---|---|
| **Bảo mật** | Phần mềm — key trong RAM | Hardware — key không rời chip |
| **Chi phí** | Free / ~$0.03/10k ops | ~$1.5/giờ (~$1,000+/tháng) |
| **Latency** | ~1-5ms/call | ~1-10ms/call |
| **Phù hợp** | Prototype + production vừa | PCI-DSS Level 1, ngân hàng |
| **Trong project** | Vault Transit (envelope enc) + fallback AES-GCM local | Không dùng (chi phí cao) |

**Kết luận:** Vault đủ cho prototype. Production lớn → kết hợp HSM cho signing key (payment keys).

---

### Giả thuyết đề tài có được xác nhận?

**Xác nhận một phần.**

**Đạt được:**
- Token replay: **0% thành công** (JWT validation)
- HMAC forgery: **0% thành công** (HmacVerificationFilter)
- Price tampering: **0% thành công** (server-side recalculation)
- PII exposure: **N/A** (data mã hóa trong DB)

**Đánh đổi thực tế:**
- Latency +50-100ms do Vault calls (envelope encryption)
- 5 microservices → phức tạp hơn monolith
- Vault cần HA setup trong production → chi phí ops cao hơn

---

## 6. THIẾT KẾ SLIDE BÁO CÁO (16 Slides)

```
SLIDE 1 — TRANG BÌA
  Tên đề tài | Thành viên | HCMUTE
  [Hình: Security shield + microservices icons]

SLIDE 2 — ĐỘNG LỰC & VẤN ĐỀ
  Thống kê vi phạm TMĐT | Hệ thống xử lý PII + Thanh toán
  Câu hỏi: "Làm thế nào bảo vệ Đúng Người → Đúng SP → Đúng Giá → Giao dịch?"
  [Hình: Attack chain diagram]

SLIDE 3 — KIẾN TRÚC TỔNG QUAN
  5 Microservices | Vault KMS | MySQL Aiven Cloud
  Frontend → Gateway (8080) → [catalog|cart|order|payment]
  [Sơ đồ kiến trúc + luồng dữ liệu]

SLIDE 4 — BẢNG YÊU CẦU & THỰC HIỆN
  12 yêu cầu đề tài ↔ giải pháp triển khai tương ứng
  [Bảng 2 cột có icon checkmark]

SLIDE 5 — RỦI RO 1: SAI NGƯỜI
  Attack: Credential stuffing / JWT forgery / Token replay
  Defense: SHA3-512 + JWT HS256 + RBAC
  [Code snippet + Auth flow diagram]

SLIDE 6 — RỦI RO 2: SAI SẢN PHẨM
  Attack: Fake productId / price client-side
  Defense: Server-side validation từ catalog + product hash
  [Order creation flow diagram]

SLIDE 7 — RỦI RO 3: SAI GIÁ
  Attack: Price tampering trong request / MITM
  Defense: Server recalculates + HTTPS/TLS + HMAC service calls
  [Demo: Postman price=1 → rejected]

SLIDE 8 — RỦI RO 4: NGỤY TẠO GIAO DỊCH
  Attack: Replay payment / Fake HMAC / Bypass payment check
  Defense: HMAC timestamp 5p + PaymentSecurityGateway risk scoring
  [Payment Security Pipeline diagram]

SLIDE 9 — RỦI RO 5: LỘ DỮ LIỆU PII
  Attack: DB dump / Log injection
  Defense: AES-256-GCM field-level + Vault Transit + Blind Index
  [Demo: SELECT encrypted_address → Base64 gibberish]

SLIDE 10 — VAULT KMS & MÃ HÓA
  Vault Transit Engine | Envelope Encryption | Fallback local AES-GCM
  Blind Index: HMAC-SHA256(phone, secret) → tìm kiếm không giải mã
  [Vault integration + key hierarchy diagram]

SLIDE 11 — DEMO: LUỒNG THANH TOÁN THỰC TẾ
  Đăng nhập → JWT | Thêm giỏ → Checkout → QR VietQR
  Xác nhận → HMAC check → Vault encrypt → Order PAID
  [Screenshot: checkout + QR + admin orders]

SLIDE 12 — DEMO: TẤN CÔNG & PHÒNG THỦ
  Attack 1: Postman /payments/process không HMAC → 401
  Attack 2: Token replay từ IP khác → rejected
  Attack 3: price=1 → server recalculates
  [Terminal/Postman screenshots]

SLIDE 13 — KẾT QUẢ KIỂM THỬ
  Bảng:
    Token Replay        → 0% thành công
    HMAC Forgery        → 0% thành công
    Price Tampering     → 0% thành công
    PII Exposure        → N/A (encrypted)
  Performance: +50-100ms do Vault calls
  [Bar chart / Security KPIs]

SLIDE 14 — TRẢ LỜI CÂU HỎI NGHIÊN CỨU
  RQ1: JWT alg:none / plaintext pwd / PAN lưu trực tiếp → đã giải quyết
  RQ2: Tokenization hiệu quả + HMAC chống replay
  RQ3: Vault đủ cho prototype; HSM cho PCI-DSS Level 1
  [Bảng so sánh Vault vs HSM]

SLIDE 15 — KẾT LUẬN & HẠN CHẾ
  Đã làm: Đầy đủ crypto stack cho prototype
  Hạn chế: 3DS chưa hoàn chỉnh | ML fraud chưa train | mTLS chưa có
  Hướng phát triển: HSM thật | ML fraud scoring | Mobile attestation
  [Security maturity model]

SLIDE 16 — Q&A / CẢM ƠN
  "Xin cảm ơn thầy/cô và các bạn!"
  Repo: github.com/PhongTran0808/ShoppingWeb
  [QR code link repo]
```

---

## 7. TÓM TẮT (ELEVATOR PITCH — 1 phút)

> **"Chúng em xây dựng prototype TMĐT microservices, áp dụng mật mã để đảm bảo: Đúng Người (SHA3-512 + JWT HS256) → Đúng Sản Phẩm (server-side validation + product hash) → Đúng Giá (server recalculate + HMAC) → Giao Dịch Thành Công (PaymentSecurityGateway + AES-256-GCM + Vault KMS). Kết quả kiểm thử: 0% token replay, 0% HMAC forgery, dữ liệu PII hoàn toàn mã hóa trong DB."**

---

*Tổng hợp từ mã nguồn thực tế: `D:\HKII-2026\MatMaUngDung\project`*  
*Cập nhật: 2026-06-27*
