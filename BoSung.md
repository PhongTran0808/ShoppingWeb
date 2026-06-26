1. Giao tiếp an toàn (Secure Communication)
API Gateway Hardening: gateway-service đóng vai trò là cửa ngõ chặn lọc tất cả các yêu cầu từ bên ngoài, giải mã và xác thực Token JWT để đảm bảo không có request nặc danh nào xâm nhập vào các service phía sau.
Inter-service HMAC Authentication: Các microservices (Catalog, Cart, Order, Payment) giao tiếp nội bộ thông qua các Feign Client được ký mã hóa bằng thuật toán HMAC-SHA256 liên dịch vụ để chống lại các cuộc tấn công phát lại (Replay Attacks) và giả mạo gói tin giữa các service.
2. Xử lý logic an toàn & Kiểm soát rủi ro Giao dịch (Transaction-Centric Security)
Hệ thống đặt giao dịch làm trung tâm rủi ro hàng đầu, được kiểm soát chặt chẽ qua cổng bảo mật PaymentSecurityGateway trước khi chuyển sang payment-service để thanh toán. Quy trình xác thực giao dịch đi qua 6 cửa ải logic an toàn trong TransactionSecurityService.java:

Đúng người (User Identity Verification): Kiểm tra danh tính người dùng, so sánh IP Address, dấu chân thiết bị (Device Fingerprinting), lịch sử hoạt động để chấm điểm rủi ro người dùng.
Đúng Session (Session Binding Validation): Xác thực tính hợp lệ của JWT Token, kiểm tra thời hạn (expiration) và ràng buộc session (session binding) giữa token với IP/User-Agent của client hiện tại.
Đúng Mặt hàng (Product Integrity Check): Tính toán mã hash các mặt hàng trong giỏ hàng và đối chiếu trực tiếp chéo với Database của catalog-service. Tránh trường hợp hacker chèn mã ID sản phẩm giả mạo hoặc thay đổi thông tin sản phẩm.
Đúng Giá tiền (Price Accuracy Verification): Lấy giá trị gốc của từng sản phẩm từ Database rồi nhân với số lượng để tính lại tổng tiền. Hệ thống từ chối ngay lập tức nếu tổng tiền người dùng gửi lên lệch dù chỉ 1 VNĐ so với kết quả tính toán lại của Backend (chống lỗi Price Tampering).
Giao dịch thành công (Transaction Status Check): Đảm bảo đơn hàng đang ở trạng thái hợp lệ (PENDING), chống lỗi double-spend (thanh toán hai lần cho cùng một đơn hàng) hoặc thanh toán các đơn hàng đã bị hủy.
Đánh giá gian lận hành vi (Fraud Analysis): Chấm điểm rủi ro (Risk Score) dựa trên giá trị đơn hàng bất thường (High-value order) hoặc tần suất đặt hàng quá nhanh (Velocity check).
3. Lưu trữ an toàn (Secure Storage)
PII Encryption: Thông tin cá nhân nhạy cảm của khách hàng trong bảng shipping_info (như địa chỉ, số điện thoại) được tự động mã hóa bằng thuật toán mạnh AES-256 GCM thông qua HashiCorp Vault.
Phone Blind Index: Số điện thoại sau khi mã hóa được tạo thêm một chỉ mục mù (Blind Index) để hệ thống có thể tìm kiếm dữ liệu an toàn mà không cần giải mã cơ sở dữ liệu, tránh rò rỉ khóa.
4. Cách ly Host vật lý (No Shared Host)
Database tách biệt hoàn toàn: Cơ sở dữ liệu MySQL của dự án được lưu trữ và vận hành trên nền tảng đám mây độc lập Aiven Cloud (Host: mysql-1e26ae34-tuitentu131-e142.g.aivencloud.com), hoàn toàn không chạy trên máy chủ chứa mã nguồn ứng dụng.
Microservices Isolation: Các service của bạn chạy độc lập trên các cổng mạng khác nhau (8080, 8081, 8082, 8083, 8084, 3000), được thiết kế để dễ dàng phân tán ra các máy chủ vật lý hoặc các cụm mạng ảo (VPC) khác nhau trong thực tế.
5. Triển khai không dùng Docker (Native Hosting)
Đúng theo yêu cầu không sử dụng Docker để host, prototype hiện tại của bạn đang được triển khai chạy natively (chạy trực tiếp trên hệ điều hành vật lý) thông qua công cụ build Maven (mvn spring-boot:run) cho Backend Java và Node.js (npx next dev) cho Frontend Next.js.
Các dịch vụ giao tiếp qua các cổng port cục bộ, đảm bảo tính trực quan và dễ kiểm soát tài nguyên hệ thống khi chấm điểm đồ án.