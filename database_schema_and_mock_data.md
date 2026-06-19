-- ==============================================================================
-- KIẾN TRÚC DATABASE MICROSERVICES CHO E-COMMERCE BẢO MẬT
-- Hệ quản trị: MySQL (Chuyển đổi từ PostgreSQL để tương thích với Aiven MySQL)
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS shopping;
USE shopping;

-- ==============================================================================
-- 1. CATALOG DATABASE (Quản lý Sản phẩm & Trải nghiệm mua sắm)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    price DECIMAL(12,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS product_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL, 
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ==============================================================================
-- 2. CART DATABASE (Quản lý Giỏ hàng)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE NOT NULL, 
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL, 
    quantity INT NOT NULL CHECK (quantity > 0),
    FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE
);

-- ==============================================================================
-- 3. ORDER DATABASE (Nghiệp vụ Đơn hàng & Bảo vệ PII)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_number VARCHAR(100) UNIQUE NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(50) NOT NULL, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL, 
    quantity INT NOT NULL CHECK (quantity > 0),
    price DECIMAL(12,2) NOT NULL,       
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- BẢNG BẢO MẬT: Field-Level Encryption & Blind Index
CREATE TABLE IF NOT EXISTS shipping_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    customer_name VARCHAR(255) NOT NULL,
    encrypted_address TEXT NOT NULL,      
    encrypted_phone VARCHAR(500) NOT NULL, 
    phone_blind_index VARCHAR(255) NOT NULL, 
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- ==============================================================================
-- 4. PAYMENT DATABASE (Thanh toán & Chống gian lận)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE, 
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'VND',
    status VARCHAR(50) NOT NULL, 
    payment_token VARCHAR(255) NOT NULL, 
    hmac_signature VARCHAR(500) NOT NULL, 
    encrypted_log TEXT, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL, 
    order_id BIGINT,
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    risk_score DECIMAL(5,2), 
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 5. IDENTITY DATABASE (Dành cho Keycloak hoặc Local Auth Demo)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY, -- MySQL sử dụng chuỗi 36 ký tự cho UUID
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL, 
    email VARCHAR(100) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- INSERT DỮ LIỆU MẪU (USERS & DANH MỤC & SẢN PHẨM)
-- ==============================================================================

-- 3 Admin & 10 User 
-- Mật khẩu đã được BĂM BẰNG SHA3-512 (Định dạng Plaintext ghi bên cạnh để test)
INSERT INTO users (id, username, password_hash, email, role) VALUES
(UUID(), 'admin1', '0dd62dbc18490c6b865efa971d73af0c59ce45627a1c8da65aec51289e2c9bab6fa77533c3cbc6151ae49767c94f843c7a0f533d849e162af493133723c4a5ef', 'admin1@shopping.com', 'ROLE_ADMIN'), -- Pass: AdminPass@1
(UUID(), 'admin2', '7ed4420b0f73d269567dd02448aeaf98e6f37d78e40f094962ffbdbfc90c8f890f673d134ca0e95ef8e1b3090e30e3ec5bca90b5a3fa2920b540ac5f9066d683', 'admin2@shopping.com', 'ROLE_ADMIN'), -- Pass: AdminPass@2
(UUID(), 'admin3', 'dc87d7d94c058478c6c3e22c2511487bdffd1e64fdc40703126814731ff187385a060e0a46c7e098c82684ad8b9f265b041331b4048bca8c6c05359f81ffb1ee', 'admin3@shopping.com', 'ROLE_ADMIN'), -- Pass: AdminPass@3
(UUID(), 'user1', '8ff8ef3d62115854978812349a72122df63c89ce2fbad21405de7f38f66b2c9027800a09fd7d9ecd5b8596ae24a963915a978366d86a7836e02ed0445543a3f8', 'user1@gmail.com', 'ROLE_USER'), -- Pass: UserPass@1
(UUID(), 'user2', 'ebe24e86881a319dbac9e056ec47662c63067b1ccf006a7cf867df0886c06535a9d288cdd79833d211731abea29b4bf300f887db9dfe4008708f2fe8a301f2a2', 'user2@gmail.com', 'ROLE_USER'), -- Pass: UserPass@2
(UUID(), 'user3', 'ad2d441ca78edaa43549ee8ce151a103785ec20af3dc31d822021a0861e03a2ab7360f6d078c0c84a063818081e59f6dc3c680b52135fe4e876d4fd1a9875c81', 'user3@gmail.com', 'ROLE_USER'), -- Pass: UserPass@3
(UUID(), 'user4', '75028b70a89a8c27783b19d3bd17d8d1a6486e23c6e32749651e4b7d88fbe28a75ddb97d6a1a26fc51a252d2b8e10483484f571ea03ecad4e0c065b62564172e', 'user4@gmail.com', 'ROLE_USER'), -- Pass: UserPass@4
(UUID(), 'user5', '21ab4e661235e2c3a0b0df8fb5e62ed9630c4fdbe6a24dc24b6f88909d07c1503a88284892e4ecf9851b0928db2f7b9a595b4cea57e3e57212fb323306347790', 'user5@gmail.com', 'ROLE_USER'), -- Pass: UserPass@5
(UUID(), 'user6', '5f69a8e60bc6450886e997ffbdab89bdf89aaedad407ba79db7ab8b2a688d9bc52140652cdfae1a45fa18c86e2d3832109de1552c2a25304520fb8aaf38732d8', 'user6@gmail.com', 'ROLE_USER'), -- Pass: UserPass@6
(UUID(), 'user7', '9646f675af509d557557eb0c94eec1e1a83a2ad7bb14a784801d5b2e2b6d66daf305e2a40f6e2702c4dcd3650b57eeaefaf4ebd98fa456fa41634c38bd8d9499', 'user7@gmail.com', 'ROLE_USER'), -- Pass: UserPass@7
(UUID(), 'user8', 'cc39444a751b16d573652bbd34c06e2bc28d5b95d340fd9230df6f568d1ad21816e8c415a0c29e371d4be244a79abe49d7fe9ac22f14c7b321a5a704e4eee56c', 'user8@gmail.com', 'ROLE_USER'), -- Pass: UserPass@8
(UUID(), 'user9', 'a4742dec4cb2d035b06a4eade312531857328f9171c3dbc9e02432f5f5b8c4df9d8056ccc6cc53436f72f10ed657c2761559df8377af5ebc35b0e0bd957d25b0', 'user9@gmail.com', 'ROLE_USER'), -- Pass: UserPass@9
(UUID(), 'user10', '9a12f8e2e5e89b32c118b3e7f2c8e61cc5d185e492aff1a4b4784b709f86941807f64d1fcb808267d07c6ee0dd90f4b81a30575cc71c4ad6006d366e6b09b43b', 'user10@gmail.com', 'ROLE_USER');

-- 5 Danh mục sản phẩm
INSERT INTO categories (id, name, slug, description) VALUES
(1, 'Smartphone', 'smartphone', 'Điện thoại thông minh'),
(2, 'Laptop', 'laptop', 'Máy tính xách tay'),
(3, 'Tablet', 'tablet', 'Máy tính bảng'),
(4, 'Audio', 'audio', 'Thiết bị âm thanh'),
(5, 'Accessories', 'accessories', 'Phụ kiện công nghệ');

-- 50 Sản phẩm mẫu
INSERT INTO products (category_id, name, slug, description, price, stock, image_url) VALUES
(1, 'iPhone 15 Pro Max', 'iphone-15-pro-max', 'Apple Smartphone 256GB Titanium', 29000000.00, 50, 'https://example.com/iphone15.jpg'),
(1, 'Samsung Galaxy S24 Ultra', 'samsung-s24-ultra', 'Samsung Flagship AI Phone 512GB', 31000000.00, 30, 'https://example.com/s24ultra.jpg'),
(1, 'Google Pixel 8 Pro', 'pixel-8-pro', 'Google Pure Android Camera Phone', 25000000.00, 25, 'https://example.com/pixel8pro.jpg'),
(1, 'Xiaomi 14 Pro', 'xiaomi-14-pro', 'Xiaomi Leica Camera Flagship', 22000000.00, 40, 'https://example.com/xiaomi14.jpg'),
(1, 'Oppo Find X7 Ultra', 'oppo-x7-ultra', 'Oppo Hasselblad Camera Phone', 26000000.00, 15, 'https://example.com/oppox7.jpg'),
(1, 'Sony Xperia 1 V', 'xperia-1-v', 'Sony Professional Camera Phone', 32000000.00, 10, 'https://example.com/xperia1.jpg'),
(1, 'Asus ROG Phone 8', 'rog-phone-8', 'Asus Ultimate Gaming Phone', 28000000.00, 20, 'https://example.com/rog8.jpg'),
(1, 'iPhone 15', 'iphone-15', 'Apple Base Model 128GB', 20000000.00, 100, 'https://example.com/iphone15_base.jpg'),
(1, 'Samsung Galaxy Z Fold 5', 'z-fold-5', 'Samsung Premium Foldable Phone', 40000000.00, 15, 'https://example.com/fold5.jpg'),
(1, 'Samsung Galaxy Z Flip 5', 'z-flip-5', 'Samsung Compact Foldable Phone', 22000000.00, 35, 'https://example.com/flip5.jpg'),

(2, 'MacBook Pro 16 M3 Max', 'macbook-pro-16-m3', 'Apple Laptop for Professionals 36GB RAM', 85000000.00, 10, 'https://example.com/macm3max.jpg'),
(2, 'MacBook Air 15 M3', 'macbook-air-15-m3', 'Apple Thin & Light Laptop 16GB', 35000000.00, 40, 'https://example.com/macairm3.jpg'),
(2, 'Dell XPS 15 9530', 'dell-xps-15', 'Dell Premium Windows Laptop i9', 55000000.00, 20, 'https://example.com/dellxps.jpg'),
(2, 'Lenovo ThinkPad X1 Carbon Gen 11', 'thinkpad-x1', 'Lenovo Business Laptop', 45000000.00, 30, 'https://example.com/thinkpad.jpg'),
(2, 'Asus Zephyrus G14', 'zephyrus-g14', 'Asus Compact Gaming Laptop', 40000000.00, 25, 'https://example.com/g14.jpg'),
(2, 'Razer Blade 16', 'razer-blade-16', 'Razer Premium Gaming Laptop RTX 4080', 90000000.00, 5, 'https://example.com/razer.jpg'),
(2, 'HP Spectre x360', 'hp-spectre-x360', 'HP 2-in-1 Premium Laptop', 38000000.00, 20, 'https://example.com/spectre.jpg'),
(2, 'LG Gram 17', 'lg-gram-17', 'LG Ultra Lightweight Laptop', 36000000.00, 15, 'https://example.com/lggram.jpg'),
(2, 'Alienware m18', 'alienware-m18', 'Alienware Desktop Replacement Laptop', 85000000.00, 8, 'https://example.com/alienware.jpg'),
(2, 'Surface Laptop Studio 2', 'surface-laptop-studio-2', 'Microsoft Professional Hybrid Laptop', 65000000.00, 12, 'https://example.com/surface.jpg'),

(3, 'iPad Pro 12.9 M2', 'ipad-pro-12-9-m2', 'Apple Ultimate Tablet', 30000000.00, 40, 'https://example.com/ipadpro.jpg'),
(3, 'iPad Air 5', 'ipad-air-5', 'Apple Mid-range Tablet', 15000000.00, 80, 'https://example.com/ipadair.jpg'),
(3, 'Samsung Galaxy Tab S9 Ultra', 'tab-s9-ultra', 'Samsung Ultimate Android Tablet', 28000000.00, 25, 'https://example.com/tabs9.jpg'),
(3, 'Xiaomi Pad 6 Max', 'xiaomi-pad-6-max', 'Xiaomi Large Entertainment Tablet', 12000000.00, 50, 'https://example.com/mipad6.jpg'),
(3, 'Amazon Kindle Paperwhite', 'kindle-paperwhite', 'Amazon e-Reader', 3500000.00, 100, 'https://example.com/kindle.jpg'),
(3, 'Boox Note Air 3', 'boox-note-air-3', 'E-ink Tablet for Note Taking', 11000000.00, 20, 'https://example.com/boox.jpg'),
(3, 'Surface Pro 9', 'surface-pro-9', 'Microsoft Windows 2-in-1 Tablet', 25000000.00, 30, 'https://example.com/surfacepro.jpg'),
(3, 'Lenovo Tab P12 Pro', 'lenovo-tab-p12', 'Lenovo Premium Android Tablet', 18000000.00, 35, 'https://example.com/lenovotab.jpg'),
(3, 'iPad Mini 6', 'ipad-mini-6', 'Apple Compact Tablet', 13000000.00, 60, 'https://example.com/ipadmini.jpg'),
(3, 'Remarkable 2', 'remarkable-2', 'Digital Paper Tablet', 12000000.00, 15, 'https://example.com/remarkable.jpg'),

(4, 'AirPods Pro 2', 'airpods-pro-2', 'Apple Noise Cancelling Earbuds', 6000000.00, 150, 'https://example.com/airpodspro.jpg'),
(4, 'AirPods Max', 'airpods-max', 'Apple Premium Over-ear Headphones', 12000000.00, 30, 'https://example.com/airpodsmax.jpg'),
(4, 'Sony WH-1000XM5', 'sony-wh-1000xm5', 'Sony Industry Leading ANC Headphones', 8500000.00, 60, 'https://example.com/sonyxm5.jpg'),
(4, 'Sony WF-1000XM5', 'sony-wf-1000xm5', 'Sony Premium Wireless Earbuds', 6500000.00, 70, 'https://example.com/sonywfxm5.jpg'),
(4, 'Bose QuietComfort Ultra', 'bose-qc-ultra', 'Bose Ultimate Comfort ANC Headphones', 9000000.00, 40, 'https://example.com/boseqc.jpg'),
(4, 'Sennheiser Momentum 4', 'sennheiser-momentum-4', 'Sennheiser Audiophile Wireless Headphones', 8000000.00, 25, 'https://example.com/sennheiser.jpg'),
(4, 'Marshall Motif II ANC', 'marshall-motif-ii', 'Marshall Classic Design Earbuds', 4500000.00, 50, 'https://example.com/marshall.jpg'),
(4, 'Jabra Elite 8 Active', 'jabra-elite-8', 'Jabra Sports Wireless Earbuds', 5000000.00, 45, 'https://example.com/jabra.jpg'),
(4, 'Samsung Galaxy Buds 2 Pro', 'galaxy-buds-2-pro', 'Samsung High-Res Earbuds', 4000000.00, 80, 'https://example.com/galaxybuds.jpg'),
(4, 'Devialet Gemini II', 'devialet-gemini-ii', 'Devialet Luxury Wireless Earbuds', 11000000.00, 10, 'https://example.com/devialet.jpg'),

(5, 'Apple Watch Ultra 2', 'apple-watch-ultra-2', 'Apple Rugged Smartwatch', 20000000.00, 40, 'https://example.com/watchultra.jpg'),
(5, 'Apple Watch Series 9', 'apple-watch-9', 'Apple Daily Smartwatch', 10000000.00, 100, 'https://example.com/watch9.jpg'),
(5, 'Samsung Galaxy Watch 6 Classic', 'galaxy-watch-6-classic', 'Samsung Smartwatch with Bezel', 8000000.00, 60, 'https://example.com/galaxywatch.jpg'),
(5, 'Garmin Epix Pro', 'garmin-epix-pro', 'Garmin Ultimate Sports Watch', 25000000.00, 20, 'https://example.com/garmin.jpg'),
(5, 'Garmin Fenix 7X', 'garmin-fenix-7x', 'Garmin Adventure Watch', 22000000.00, 25, 'https://example.com/fenix7.jpg'),
(5, 'Anker 737 Power Bank', 'anker-737', 'Anker 24000mAh 140W Charger', 3500000.00, 80, 'https://example.com/anker737.jpg'),
(5, 'Apple MagSafe Charger', 'magsafe-charger', 'Apple Official Wireless Charger', 1200000.00, 200, 'https://example.com/magsafe.jpg'),
(5, 'Logitech MX Master 3S', 'logitech-mx-master-3s', 'Logitech Premium Productivity Mouse', 2500000.00, 120, 'https://example.com/mxmaster.jpg'),
(5, 'Keychron Q1 Pro', 'keychron-q1-pro', 'Keychron Custom Mechanical Keyboard', 4500000.00, 50, 'https://example.com/keychron.jpg'),
(5, 'Sony DualSense Edge', 'sony-dualsense-edge', 'Sony Premium PS5 Controller', 5500000.00, 40, 'https://example.com/dualsense.jpg');