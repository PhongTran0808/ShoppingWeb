MỤC LỤC BÁO CÁO ĐỒ ÁN
Đề tài: Thiết kế và Triển khai Hệ thống Giao dịch TMĐT Bảo mật bằng Mật mã học Ứng dụng
Môn học: Mật mã Ứng dụng


PHẦN MỞ ĐẦU
1.1. Tính cấp thiết của đề tài
1.2. Mục đích của đề tài
1.3. Cách tiếp cận và phương pháp nghiên cứu
    1.3.1. Đối tượng nghiên cứu
    1.3.2. Phạm vi nghiên cứu
1.4. Phân tích các công trình có liên quan


PHẦN NỘI DUNG

CHƯƠNG 1: TỔNG QUAN ĐỀ TÀI VÀ KIẾN TRÚC HỆ THỐNG
1.1. Kiến trúc Microservices tổng thể của hệ thống E-Commerce
1.2. Phân rã cấu trúc ứng dụng và Giao diện Web
    1.2.1. Lớp Frontend Next.js
    1.2.2. Lớp API Gateway Spring Cloud Gateway
    1.2.3. Lớp Backend Microservices
1.3. Mô hình triển khai hạ tầng thực tế và Liên kết dữ liệu
    1.3.1. Apache Reverse Proxy TLS 1.3
    1.3.2. Lưu trữ phân tán MySQL Cloud và Key Management Service


CHƯƠNG 2: CƠ CHẾ CHỮ KÝ SỐ XÁC THỰC VÀ CHỐNG CHỐI BỎ GIAO DỊCH
2.1. Nguyên lý Chữ ký số bất đối xứng ECDSA
    2.1.1. So sánh Chữ ký số bất đối xứng và Chữ ký đối xứng HMAC
    2.1.2. Tại sao chỉ Chữ ký số bất đối xứng mới đạt được tính Chống chối bỏ
2.2. Thiết kế quy trình Ký số xác thực đơn hàng tại thời điểm Checkout
    2.2.1. Quy trình phía Client - Sinh khóa, băm dữ liệu và ký số bằng Private Key
    2.2.2. Quy trình phía Server - Xác thực chữ ký bằng Public Key và phê duyệt giao dịch
2.3. Tính Không thể thay đổi và Không thể giả mạo của giao dịch sau khi ký
    2.3.1. Cơ chế phát hiện và từ chối đơn hàng khi bị thay đổi thông tin
    2.3.2. Ghi nhận chứng cứ số vào hệ thống Audit Log làm cơ sở đối soát pháp lý


CHƯƠNG 3: CƠ CHẾ MÃ HÓA VÀ LƯU TRỮ DỮ LIỆU AN TOÀN
3.1. Bảo vệ phiên làm việc với JWT Signature HS256
3.2. Mã hóa dữ liệu tĩnh nhạy cảm tại cơ sở dữ liệu
    3.2.1. Thuật toán mã hóa đối xứng AES-256-GCM bảo vệ dữ liệu cá nhân
    3.2.2. Quy trình mã hóa phong bì kết hợp HashiCorp Vault Transit Engine
3.3. Giải pháp tìm kiếm dữ liệu đã mã hóa bằng Chỉ mục mù
    3.3.1. Nguyên lý tạo Chỉ mục mù định danh bằng HmacSHA256 có muối bảo mật
    3.3.2. Quy trình tìm kiếm số điện thoại đã mã hóa mà không cần giải mã cơ sở dữ liệu
3.4. Bảo mật đường truyền giao tiếp nội bộ Microservices
    3.4.1. HTTPS/TLS 1.3 bảo mật ngoài Internet qua Apache Reverse Proxy
    3.4.2. Chữ ký HMAC kèm Timestamp bảo vệ kết nối giữa các Microservice nội bộ


CHƯƠNG 4: THỰC NGHIỆM KỸ THUẬT VÀ ĐÁNH GIÁ KẾT QUẢ
4.1. Môi trường triển khai thực tế và Cấu hình hệ thống
4.2. Thực nghiệm 1 - Xác thực quy trình Ký số thanh toán và Chống chối bỏ
    4.2.1. Quy trình sinh cặp khóa ECDSA và ký số thông tin đơn hàng tại Client
    4.2.2. Kiểm tra bằng chứng chống chối bỏ lưu trong bảng audit_log
4.3. Thực nghiệm 2 - Kiểm chứng tính toàn vẹn Đơn hàng khi bị thao túng giá
    4.3.1. Thử nghiệm sửa đổi giá trị đơn hàng thành 1 đồng
    4.3.2. Kết quả Server tự động đối chiếu cơ sở dữ liệu gốc và bác bỏ giao dịch sai lệch
4.4. Thực nghiệm 3 - Kiểm chứng cơ chế Mã hóa tĩnh PII và Chỉ mục mù
    4.4.1. Kết xuất trực tiếp dữ liệu thô trong MySQL Cloud sau mã hóa
    4.4.2. Thực nghiệm tìm kiếm số điện thoại thông qua cột Blind Index
4.5. Đánh giá hiệu năng mật mã và Hướng phát triển


PHẦN KẾT LUẬN
1. Các kết quả kỹ thuật đạt được của đồ án
2. Những hạn chế kỹ thuật hiện tại và Hướng khắc phục
3. Hướng nghiên cứu mở rộng trong tương lai


TÀI LIỆU THAM KHẢO
