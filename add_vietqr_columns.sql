USE shopping;

-- Đánh dấu phương thức thanh toán và mã tham chiếu giao dịch (nếu có)
ALTER TABLE transactions 
ADD COLUMN payment_method VARCHAR(50) DEFAULT 'VIETQR',
ADD COLUMN bank_reference_id VARCHAR(255) NULL;

-- Thêm trạng thái đối soát của Admin
ALTER TABLE transactions 
ADD COLUMN admin_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN verified_at TIMESTAMP NULL;
