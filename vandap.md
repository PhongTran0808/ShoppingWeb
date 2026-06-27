# BỘ TÀI LIỆU ÔN TẬP VẤN ĐÁP (Q&A) ĐỒ ÁN MẬT MÃ ỨNG DỤNG

Tài liệu này được biên soạn để nhóm ôn tập, học thuộc các khái niệm cốt lõi và tự tin trả lời mọi câu hỏi chất vấn từ Hội đồng bảo vệ đồ án.

---

## PHẦN 1: 5 CƠ CHẾ KIẾN TRÚC BẢO MẬT (Để trả lời "Hệ thống của em bảo mật như thế nào?")

Nếu bị hỏi: *"Em hãy trình bày tổng quan cách hệ thống của em chống lại hacker?"*, hãy dùng 5 cơ chế này để trả lời:

### 1. Giao tiếp an toàn (Secure Communication)
*   **API Gateway Hardening (Cửa khẩu thép):** API Gateway (cổng 8080) chặn mọi request từ bên ngoài, giải mã và xác thực Token JWT. Không có request nặc danh nào lọt được vào các service phía sau.
*   **Inter-service HMAC Authentication (Giao tiếp nội bộ có chữ ký):** Khi các service (như Order gọi Payment) nói chuyện với nhau, chúng dùng chữ ký thuật toán **HMAC-SHA256** kèm theo *Timestamp* (thời gian). Điều này chống lại các cuộc tấn công phát lại (Replay Attacks) giữa các service.

### 2. Xử lý logic an toàn (Transaction-Centric Security)
Hệ thống đặt giao dịch làm trung tâm rủi ro. Mọi giao dịch phải qua cổng `PaymentSecurityGateway` với 6 ải kiểm tra:
1.  **Đúng người:** So sánh IP, dấu chân thiết bị (Device Fingerprinting).
2.  **Đúng Session:** Xác thực JWT, ràng buộc token với thiết bị của user.
3.  **Đúng Mặt hàng:** Mã hash sản phẩm được đối chiếu chéo trực tiếp với database của Catalog Service (chống sửa ID sản phẩm).
4.  **Đúng Giá tiền:** Backend tự động lấy giá gốc nhân số lượng. Lệch 1 VNĐ giữa client gửi lên và server tự tính → Hủy đơn ngay lập tức (Chống Price Tampering).
5.  **Giao dịch thành công:** Đơn hàng phải đang ở trạng thái PENDING mới cho thanh toán (Chống Double-spend - thanh toán 2 lần).
6.  **Đánh giá gian lận:** Chấm điểm rủi ro (Risk Score) theo hành vi (ví dụ: đơn hàng giá trị cao bất thường).

### 3. Lưu trữ an toàn (Secure Storage)
*   **PII Encryption (Mã hóa dữ liệu cá nhân):** Địa chỉ, SĐT trong bảng `shipping_info` được tự động mã hóa bằng thuật toán **AES-256 GCM** thông qua HashiCorp Vault. DB chỉ lưu chuỗi Base64 vô nghĩa.
*   **Phone Blind Index (Chỉ mục mù):** Để tìm kiếm SĐT đã bị mã hóa mà không cần giải mã toàn bộ DB, hệ thống băm SĐT bằng `HMAC-SHA256` tạo ra một "chỉ mục mù" lưu ở cột riêng.

### 4. Cách ly Host vật lý (No Shared Host)
*   **Database tách biệt hoàn toàn:** Cơ sở dữ liệu MySQL chạy trên đám mây Aiven Cloud, hoàn toàn tách biệt với máy chủ chạy code. Nếu máy chủ web bị chiếm quyền, hacker vẫn không dễ dàng truy cập thẳng vào DB.
*   **Microservices Isolation:** Các service chạy trên các cổng độc lập (8080-8084), giới hạn rủi ro lây lan chéo.

### 5. Triển khai Native Hosting (Không dùng Docker)
*   Hệ thống chạy trực tiếp trên hệ điều hành vật lý (Java Spring Boot dùng `mvn`, Next.js dùng `npx`). Cách này đòi hỏi việc quản lý cổng mạng và kết nối bảo mật khắt khe hơn so với khi được Docker ảo hóa giùm.

---

## PHẦN 2: TRẢ LỜI CÁC CÂU HỎI TRONG FILE YEUCAU.MD

Phần này phân tích sâu các câu hỏi nghiên cứu (RQ - Research Questions) do đề tài đặt ra, kèm giải nghĩa thuật ngữ để hội đồng thấy nhóm thực sự hiểu bản chất.

### ❓ RQ1: Những điểm yếu mật mã nào thường dẫn đến compromise (xâm nhập) trong hệ thống TMĐT?
**Trả lời:** Có 3 điểm yếu chí mạng thường gặp:
1.  **Cấu hình sai JWT (JWT Misconfiguration):** Dùng thuật toán yếu hoặc lỗi `alg:none` (cho phép JWT không cần chữ ký). Mật khẩu của thuật toán (Secret Key) quá ngắn và dễ đoán.
2.  **Lưu trữ PAN (Số thẻ ngân hàng) trực tiếp:** Lưu thẳng số thẻ vào database mà không mã hóa, khi bị hack dump DB sẽ lộ toàn bộ tài khoản ngân hàng của khách.
3.  **Không bảo vệ Giao tiếp nội bộ:** Các microservices gọi nhau không cần xác thực (Trust all internal traffic). Nếu hacker vào được 1 service, chúng có thể gọi các service khác thoải mái.

> 📚 **GIẢI THÍCH THUẬT NGỮ CHO RQ1:**
> *   **alg:none attack:** Lỗ hổng trong đó hacker sửa phần Header của JWT, đổi thuật toán thành `none` (không dùng mã hóa), xóa chữ ký và mạo danh bất kỳ ai. *(Hệ thống của ta đã chặn lỗi này bằng thư viện nimbus-jose-jwt bắt buộc dùng HS256).*
> *   **PAN (Primary Account Number):** Chuỗi 16 số trên mặt thẻ tín dụng. Theo chuẩn quốc tế PCI-DSS, cấm lưu trữ PAN nếu không có chứng chỉ bảo mật phần cứng nghiêm ngặt.

---

### ❓ RQ2: Chiến lược Tokenization có giảm đáng kể rủi ro gian lận thanh toán không trong khi vẫn chấp nhận được về độ trễ (latency)?
**Trả lời:** **CÓ, chiến lược này cực kỳ hiệu quả.**
*   **Về mặt rủi ro:** Tokenization giúp hệ thống của ta đạt trạng thái "Zero PAN" — số thẻ của khách không bao giờ chạm đến máy chủ của chúng ta. Thay vào đó, ta sử dụng payment token (thông qua mã VietQR). Kết hợp với chữ ký **HMAC Timestamp**, ta loại bỏ hoàn toàn nguy cơ kẻ gian đánh cắp thông tin thẻ và chặn luôn cả các cuộc tấn công phát lại (Replay Attacks).
*   **Về mặt độ trễ (Latency):** Tuy quá trình gọi qua cổng bảo mật `PaymentSecurityGateway` và tính toán chữ ký có thêm một chút tính toán mã hóa, nhưng thuật toán Hash (SHA, HMAC) cực kỳ nhanh (vài mili-giây). Do đó, UX (trải nghiệm người dùng) hoàn toàn mượt mà, không bị khựng.

> 📚 **GIẢI THÍCH THUẬT NGỮ CHO RQ2:**
> *   **Tokenization (Token hóa):** Quá trình thay thế dữ liệu nhạy cảm (như số thẻ Visa) bằng một chuỗi ký tự vô nghĩa (Token) dùng 1 lần hoặc có thời hạn.
> *   **Replay Attack (Tấn công phát lại):** Hacker nghe lén bắt được gói tin "Thanh toán đơn hàng A", sau đó gửi lại chính gói tin đó nhiều lần để ép hệ thống xử lý lại. *(Hệ thống của ta dùng Timestamp trong HMAC: nếu gói tin quá 5 phút so với thời gian hiện tại thì từ chối ngay lập tức).*

---

### ❓ RQ3: Độ hiệu quả của việc dùng HSM/KMS cho payment keys so với software keys về mặt an ninh & chi phí?
**Trả lời:**
*   **Software keys (Lưu key trong code/file):** Rẻ, dễ làm, nhanh. NHƯNG bảo mật cực kém. Nếu hacker đọc được mã nguồn hoặc file `.env`, toàn bộ chìa khóa bị lộ, hệ thống sụp đổ.
*   **KMS / Vault (Dùng trong project):** Quản lý khóa bằng phần mềm chuyên dụng (HashiCorp Vault). Vault sử dụng cơ chế **Envelope Encryption** để mã hóa dữ liệu. Hiệu quả an ninh rất cao, đáp ứng tốt cho Prototype và doanh nghiệp vừa. Chi phí rẻ hoặc miễn phí.
*   **HSM (Hardware Security Module):** Đây là thiết bị phần cứng chuyên dụng giống như két sắt. Chìa khóa được sinh ra bên trong con chip và *không bao giờ có thể sao chép ra ngoài*. Hiệu quả an ninh tuyệt đối (Chuẩn ngân hàng). NHƯNG chi phí cực kỳ đắt đỏ (hàng ngàn đô mỗi tháng trên Cloud).

**Kết luận dự án:** Trong khuôn khổ Prototype, nhóm sử dụng **Vault KMS** làm phương án tối ưu nhất để cân bằng giữa bảo mật cao (hơn hẳn software keys) và chi phí thực tiễn.

> 📚 **GIẢI THÍCH THUẬT NGỮ CHO RQ3:**
> *   **KMS (Key Management Service):** Dịch vụ chuyên biệt chỉ để quản lý, lưu trữ, xoay vòng chìa khóa mã hóa (Ví dụ: HashiCorp Vault, AWS KMS).
> *   **Envelope Encryption (Mã hóa phong bì):** Khái niệm cực quan trọng. Thay vì gửi một file 1GB cho KMS mã hóa (gây nghẽn mạng). Hệ thống sẽ tự tạo 1 chìa khóa gọi là `Data Key` để mã hóa file 1GB đó. Sau đó, hệ thống chỉ gửi cái `Data Key` lên KMS để KMS mã hóa nó bằng `Master Key`. *(Giống như bạn bỏ tài liệu vào két sắt, rồi đem chìa khóa két sắt đi gửi ngân hàng).*

---

### ❓ TỔNG KẾT: Giả thuyết của đề tài có được xác nhận không?
**Trích giả thuyết gốc:** *"Kết hợp tokenization, KMS/HSM cho wrapping, và mTLS/HMAC giúp giảm đáng kể rủi ro; tuy nhiên cần đánh đổi về độ trễ, phức tạp vận hành và chi phí."*

**Trả lời:** **Giả thuyết này ĐÃ ĐƯỢC XÁC NHẬN qua quá trình làm đồ án.**
*   **Chống rủi ro:** Thông qua các bài test (trong folder `/experiments`), các rủi ro như token replay, giả mạo giá trị đơn hàng (price tampering) và lộ dữ liệu PII đã bị chặn đứng hoàn toàn (tỉ lệ thành công của hacker là 0%).
*   **Sự đánh đổi (Trade-off):** 
    1.  **Về độ trễ:** Có tăng nhẹ (khoảng 50-100ms) do các cuộc gọi chéo tới Vault KMS để mã hóa PII.
    2.  **Về độ phức tạp:** Tăng lên rất nhiều. Thay vì 1 cục Backend (Monolith), nhóm phải quản lý 5 Microservices, 1 API Gateway, cài đặt Vault và duy trì cấu trúc định tuyến (Routing).
    3.  **Về chi phí vận hành:** Cần hệ thống lưu trữ phân hiện, tốn thêm tài nguyên RAM/CPU để duy trì Vault và chạy thuật toán mã hóa liên tục.
