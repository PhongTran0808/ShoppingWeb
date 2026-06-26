# BÁO CÁO TIẾN ĐỘ ĐỒ ÁN
**Đề tài:** Application Scenarios: Online Shopping Service Platform
**Môn học:** Mật mã ứng dụng

---

## 1. Risks (Rủi ro) & Security Goals (Mục tiêu bảo mật)

Dựa trên đặc thù của hệ thống e-commerce, các tiêu chí rủi ro được đặt lên hàng đầu và phân loại như sau:

**Các rủi ro cốt lõi của nền tảng (Risks):**
1. **Giao dịch và Xác thực Thanh toán (Transaction & Payment) - *Tiêu chí rủi ro số 1*:**
   * Khi một giao dịch được đẩy từ luồng Đặt hàng (Order) sang Thanh toán (Payment), làm sao để xác minh được đó là **đúng Session, đúng người dùng**?
   * Chuỗi rủi ro cần giải quyết: Làm sao kiểm tra **đúng người** -> **đúng mặt hàng** người đó đã thao tác -> **đúng giá tiền** không bị thay đổi -> giao dịch thực sự **đã thành công hay chưa**?
   * *Rủi ro Man-in-the-Middle:* Kẻ gian can thiệp vào request trên đường truyền để sửa đổi tổng tiền thanh toán từ 10 triệu xuống còn 10 nghìn đồng.
2. **Tính toàn vẹn của Dữ liệu (Integrity):**
   * Các mặt hàng hiển thị trên trang Shopping (giá cả, tên sản phẩm) có bị sửa đổi trái phép từ Database hoặc trên đường truyền hay không?
3. **Xác thực và Kiểm soát Truy cập (Access Control):**
   * Rủi ro leo thang đặc quyền: Một khách hàng thông thường (User) cố tình truy cập trái phép vào các API hoặc giao diện của Quản trị viên (Admin).
4. **Lưu trữ dữ liệu (Storage):**
   * Nguy cơ lộ lọt thông tin cá nhân (PII), địa chỉ giao hàng, và thông tin thẻ (PAN) nếu Database bị tấn công.

**Mục tiêu bảo mật (Security Goals):**
*   **Xác thực giao dịch tuyệt đối:** Mọi transition (chuyển giao dịch) đều phải được ký điện tử để đảm bảo tính toàn vẹn (không thể sửa đổi) và tính xác thực (đúng người, đúng phiên).
*   **Access Control nghiêm ngặt:** Quản lý Identity tập trung bằng chuẩn OAuth2/OIDC, đảm bảo Zero Trust.
*   **Data at Rest & In Transit:** Mã hóa toàn bộ dữ liệu nhạy cảm bằng AES-256-GCM. Không lưu thẻ tín dụng.

---

## 2. Kiến trúc giải pháp (Solution Architectures) theo Mô hình 3 Trụ cột an toàn

Để giải quyết triệt để bài toán rủi ro giao dịch và Access Control, hệ thống áp dụng chiến lược bảo vệ toàn diện với kiến trúc Microservices (Spring Boot & Next.js), được chia làm 3 trụ cột chính:

### 2.1. Giao thức an toàn (Secure Protocols - Data in Transit)
*   **TLS/mTLS:** Toàn bộ giao tiếp giữa Client - API Gateway, cũng như giữa các Microservices nội bộ với nhau, đều bắt buộc sử dụng HTTPS/TLS 1.3. Ngăn chặn hoàn toàn các nỗ lực nghe lén (Sniffing/Man-in-the-Middle) trên đường truyền mạng.
*   **Xác thực OAuth 2.0 / OpenID Connect:** Cấu hình **Keycloak** làm Identity Provider trung tâm. Áp dụng chuẩn JWT (JSON Web Token) có vòng đời ngắn kết hợp Session Binding để xác minh định danh người dùng một cách an toàn mà không phải truyền mật khẩu thô giữa các service.

### 2.2. Xử lý an toàn (Secure Processing - Data in Use)
*   **Xác thực Toàn vẹn Giao dịch (Đúng mặt hàng, đúng giá tiền):**
    * Khi luồng Đặt hàng (Order) chuyển sang luồng Thanh toán (Payment), một chữ ký **HMAC-SHA256** được thuật toán tự động sinh ra (băm từ ID Khách hàng + Giỏ hàng + Tổng tiền + Timestamp).
    * Bất kỳ sự xáo trộn nào (như hacker cố tình sửa đổi giá tiền từ 10 triệu xuống 10 nghìn đồng) trong lúc xử lý sẽ làm sai lệch chữ ký. `HmacVerificationFilter` của hệ thống sẽ bắt lỗi và lập tức hủy giao dịch.
*   **Kiểm soát Truy cập (Access Control) & Chống Replay Attack:** 
    * API Gateway phân tích claim `Role` trong JWT Token. Nếu một khách hàng (User) cố tình gọi API trích xuất doanh thu của Admin, request sẽ bị chặn ngay ở Gateway (tránh lỗi BOLA - Broken Object Level Authorization).
    * Áp dụng xác minh Timestamp đính kèm trong các giao thức gọi API để từ chối các request đã quá hạn, chặn đứng tấn công phát lại (Replay Attack).

### 2.3. Lưu trữ an toàn (Secure Storage - Data at Rest)
*   **Mã hóa dữ liệu tại chỗ (Field-Level Encryption):** 
    * Tích hợp **HashiCorp Vault**. Bất kỳ thông tin định danh cá nhân nào (PII như địa chỉ, số điện thoại) đều bị mã hóa bằng thuật toán đối xứng mạnh **AES-256-GCM** trước khi ghi vào Database (MySQL).
    * Đảm bảo nguyên tắc: Kẻ tấn công có đánh cắp toàn bộ Database thì cũng chỉ thu được ciphertext vô nghĩa.
*   **Mã hóa Phong bì (Envelope Encryption) & Tách biệt Khóa:** Doanh thu hệ thống và các con số nhạy cảm được đẩy qua *Vault Transit Engine*. Cơ sở dữ liệu và nơi lưu trữ Khóa (Keys) nằm ở 2 container hoàn toàn tách biệt.
*   **Bảo mật Thanh toán (PCI-DSS Compliance):** Hệ thống sử dụng quy trình *Tokenization*. Tuyệt đối không lưu trữ số thẻ gốc (PAN) hay mã CVV của khách hàng xuống bộ nhớ máy chủ, thay vào đó chỉ lưu trữ Token thanh toán vô hại (`tok_visa...`).

---

## 3. Demonstration Architectures (Kiến trúc mô phỏng/trình diễn)

Hệ thống được thiết kế dưới dạng Prototype chạy trong môi trường Lab cô lập.
*   **Vị trí triển khai (Hosting/Deployment):** 
    *   Hệ thống hiện tại được triển khai trên **Môi trường Lab nội bộ (Localhost / Sandbox)** thông qua Docker Desktop.
    *   **Cơ sở hạ tầng (Infrastructure):** Chạy trong các Docker Container độc lập (MySQL: 3306, Keycloak: 9090, HashiCorp Vault: 8200, Redis: 6379).
    *   **Microservices Backend:** Triển khai trên máy chủ Local (chạy qua Maven/Java 21) chiếm các port từ 8080 đến 8084.
    *   **Frontend Web App:** Triển khai Node.js server (Next.js) tại port 3000.
*   **Workflow Mô phỏng Giao dịch:**
    *   *User (Frontend)* -> *API Gateway* -> *Order Service (Ký HMAC)* -> *Payment Service (Verify HMAC)* -> *Trả kết quả*.

---

## 4. Demonstration Results (Kết quả trình diễn tính đến hiện tại)

Nhóm đã hoàn thiện phần lớn Prototype và code thực tế trình diễn thành công các yêu cầu rủi ro:
*   **Trình diễn tính toàn vẹn và Xác thực Giao dịch:** 
    * Xây dựng thành công Frontend E-commerce. Ở luồng Đặt hàng (Checkout), dữ liệu được đưa qua quy trình Tokenization (chỉ dùng Payment Token, không lưu thẻ thật).
    * Bất kỳ nỗ lực nào chỉnh sửa số lượng, giá trị đơn hàng sau khi ấn nút thanh toán đều sẽ làm sai lệch chữ ký HMAC tại Backend, khiến giao dịch bị Payment Service từ chối. Điều này chứng minh bài toán "đúng mặt hàng, đúng giá tiền".
*   **Trình diễn Access Control (Kiểm soát truy cập):** 
    * Frontend đã phân quyền rạch ròi: User chỉ có thể thao tác ở trang mua sắm (`/catalog`), xem đơn hàng cá nhân (`/orders`). 
    * Tự động chặn và chuyển hướng nếu một User/Guest cố tình gõ URL `/admin/orders` trên trình duyệt.
*   **Trình diễn Decryption thời gian thực (Vault Transit Engine):** 
    * Tại trang Quản trị (Admin Panel), các dữ liệu báo cáo/doanh thu bị khóa chặt dưới dạng ciphertext. 
    * Quản trị viên phải click biểu tượng "Unlock" kèm theo Token bảo mật, hệ thống mới kết nối với HashiCorp Vault để giải mã thành plaintext (số tiền thực tế). Trình diễn rõ ràng sự khác biệt giữa Data gốc và Data mã hóa trong kho lưu trữ.
