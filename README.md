# Hệ thống E-commerce Bảo mật Cao cấp (Secure E-commerce Platform)

Đây là dự án nguyên mẫu nền tảng E-commerce với kiến trúc Microservices, tập trung chuyên sâu vào các khía cạnh bảo mật và ứng dụng Mật mã học để chống lại các cuộc tấn công hiện đại (SQL Injection, Data Breach, Replay Attack).

Hệ thống bao gồm 2 phần chính:
- **Backend:** Spring Boot Microservices (Gateway, Catalog, Cart, Order, Payment).
- **Frontend:** Next.js (React) với giao diện Dark Mode / Glassmorphism tối tân.

---

## 🛠 Yêu cầu Môi trường

Để chạy dự án trên máy tính cá nhân, bạn cần cài đặt sẵn:
1. **Docker & Docker Compose:** Dùng để chạy Cơ sở hạ tầng (MySQL, Vault, Keycloak, Redis).
2. **Node.js (v18 trở lên):** Để chạy ứng dụng Frontend (Next.js).
3. **Java 21 & Maven:** Để chạy các dịch vụ Backend (Spring Boot).
4. **IDE (Khuyên dùng):** IntelliJ IDEA (cho Backend) và VS Code (cho Frontend).

---

## 🚀 Hướng dẫn Cài đặt & Khởi chạy

### Bước 1: Khởi động Cơ sở Hạ tầng (Infrastructure)
Dự án sử dụng Docker để giả lập môi trường hạ tầng nhanh chóng.
Mở Terminal tại thư mục gốc của dự án và chạy:
```bash
docker-compose up -d
```
*Hệ thống sẽ khởi tạo:*
- MySQL (Port 3306) với 5 database được cấu hình sẵn.
- HashiCorp Vault (Port 8200) - Quản lý khóa KMS.
- Keycloak (Port 9090) - Quản lý Identity & Access.
- Redis (Port 6379) - Dùng cho Rate Limiting.

**Lưu ý:** Sau khi Vault khởi động xong, bạn cần cấu hình Transit Engine (Sinh khóa mã hóa) bằng lệnh sau:
```bash
docker exec -it ecom-vault sh /init-vault.sh
```

### Bước 2: Khởi động Backend (Spring Boot)
Toàn bộ mã nguồn Backend nằm trong thư mục `backend/`.
Mở IntelliJ IDEA, chọn Open thư mục `backend/`. IDEA sẽ nhận diện đây là dự án Maven.
Hãy chạy lần lượt 5 module ứng dụng sau (Chạy file `*Application.java`):
1. `GatewayApplication` (Port 8080)
2. `CatalogApplication` (Port 8081)
3. `CartApplication` (Port 8082)
4. `OrderApplication` (Port 8083)
5. `PaymentApplication` (Port 8084)

*(Hoặc chạy qua terminal bằng lệnh `mvn spring-boot:run` tại từng thư mục).*

### Bước 3: Khởi động Frontend (Next.js)
Mở một cửa sổ Terminal mới, di chuyển vào thư mục `frontend/`:
```bash
cd frontend
npm install
npm run dev
```
Sau khi cài đặt xong dependencies và biên dịch thành công, mở trình duyệt web và truy cập vào:
👉 **http://localhost:3000**

---

## 🛡 Tính năng Mật mã & Bảo mật Nổi bật

Hệ thống được thiết kế với các cơ chế bảo mật ngầm (Zero Trust):
- **Field-Level Encryption (AES-256 GCM):** Khi bạn nhập tên, địa chỉ, hay số điện thoại, dữ liệu sẽ được tự động băm (Encrypt) trước khi đưa vào Database. Dữ liệu trong Database hoàn toàn vô nghĩa đối với hacker.
- **HashiCorp Vault Transit Engine:** Token và dữ liệu doanh thu của Admin được chuyển qua Transit Engine để giải mã (Decrypt).
- **HMAC (Hash-based Message Authentication Code):** Giữa các Microservices (VD: Order gọi sang Payment), hệ thống sử dụng HMAC-SHA256 kết hợp Timestamp để ký xác thực. Nếu request bị đánh cắp và gửi lại (Replay Attack), hệ thống sẽ từ chối.
- **Secure Fallback:** Trong trường hợp Vault/API gặp sự cố, Frontend được trang bị tính năng Web Crypto API để mô phỏng và fallback an toàn trên trình duyệt.

---

## 💻 Trải nghiệm Ứng dụng

1. **Khách vãng lai (Guest):** Truy cập `/catalog` để tìm kiếm sản phẩm, lọc theo danh mục/giá, và xem chi tiết sản phẩm. Có thể thêm hàng vào giỏ.
2. **Đăng nhập & Đặt hàng:** Bạn có thể tự tạo tài khoản mới tại trang Đăng ký. Mật khẩu của bạn sẽ được băm bằng thuật toán SHA-256 kết hợp Web Crypto. Sau đó, tiến hành đặt hàng để xem danh sách "Đơn hàng của tôi".
3. **Trang Quản Trị (Admin Panel):**
   - Truy cập trang Dashboard bằng cách đăng nhập bằng tài khoản Admin (Nếu chưa có, đăng ký 1 user và sửa `role` trong DB/LocalStorage thành `ROLE_ADMIN`).
   - Admin có thể Quản lý Sản phẩm, Khóa/Mở khóa Người dùng (Soft Delete), và Quan sát danh sách Đơn hàng theo thời gian thực.
   - Thử tính năng Giải mã dữ liệu doanh thu (Transit Engine) hoặc xem cấu hình KMS.

---
*Chúc bạn có trải nghiệm tuyệt vời với nền tảng Secure E-commerce Platform!*
