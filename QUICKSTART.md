# 🚀 Quick Start Guide - Secure E-commerce Platform

## Khởi động nhanh trong 5 phút

### 1️⃣ Khởi động Infrastructure (2 phút)

```bash
# Terminal 1: Core infrastructure
docker-compose up -d

# Đợi 30 giây, sau đó init Vault
docker exec -it ecom-vault sh /init-vault.sh

# (Tuỳ chọn) Terminal 2: Monitoring stack
docker-compose -f docker-compose.monitoring.yml up -d
```

### 2️⃣ Khởi động Backend (2 phút)

```bash
# Mở IntelliJ IDEA
# File -> Open -> chọn thư mục backend/
# Chạy các Application theo thứ tự:
# 1. GatewayApplication (8080)
# 2. CatalogApplication (8081)
# 3. CartApplication (8082)
# 4. OrderApplication (8083)
# 5. PaymentApplication (8084)
```

**Hoặc dùng Maven CLI:**
```bash
cd backend/gateway-service && mvn spring-boot:run &
cd backend/catalog-service && mvn spring-boot:run &
cd backend/cart-service && mvn spring-boot:run &
cd backend/order-service && mvn spring-boot:run &
cd backend/payment-service && mvn spring-boot:run &
```

### 3️⃣ Khởi động Frontend (1 phút)

```bash
cd frontend
npm install
npm run dev
```

Truy cập: **http://localhost:3000** ✨

---

## 🧪 Test nhanh các tính năng

### Test 1: Xem sản phẩm
1. Mở http://localhost:3000/catalog
2. Tìm kiếm và lọc sản phẩm
3. Thêm vào giỏ hàng

### Test 2: Đăng nhập
1. Click "Sign In"
2. Username: `user1`, Password: `UserPass@1`
3. Xem giỏ hàng và checkout

### Test 3: Admin Panel
1. Đăng nhập: `admin1` / `AdminPass@1`
2. Truy cập /admin
3. Quản lý sản phẩm và users

### Test 4: Security Experiment
```bash
cd experiments/token-replay-test
python test_token_replay.py
```

---

## 📊 Monitoring URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | - |
| API Gateway | http://localhost:8080 | - |
| Keycloak | http://localhost:9090 | admin/admin |
| Vault UI | http://localhost:8200 | Token: root |
| Grafana | http://localhost:3001 | admin/admin |
| Kibana | http://localhost:5601 | - |
| Kafka UI | http://localhost:8090 | - |
| Jaeger | http://localhost:16686 | - |
| Prometheus | http://localhost:9091 | - |

---

## 🔧 Troubleshooting

### Docker containers không start
```bash
# Kiểm tra ports đã bị chiếm
netstat -ano | findstr "3306 8200 9090 6379"

# Xóa containers cũ
docker-compose down -v
docker system prune -a
```

### Backend service không kết nối DB
```bash
# Check MySQL status
docker logs ecom-mysql

# Restart MySQL
docker-compose restart mysql
```

### Frontend không gọi được API
```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Check CORS configuration
# Xem file: backend/gateway-service/src/main/resources/application.yml
```

### Vault Transit Engine chưa được init
```bash
# Re-run init script
docker exec -it ecom-vault sh /init-vault.sh

# Verify
docker exec -it ecom-vault vault secrets list
```

---

## 🎯 Demo Scenario

### Kịch bản 1: Normal User Shopping Flow
1. Browse catalog → Add to cart → Login → Checkout
2. Xem encryption trong DB: `SELECT * FROM shipping_info;`
3. Data đã được mã hóa bằng AES-256 GCM

### Kịch bản 2: Security Attack Simulation
1. Run token replay test
2. Observe attack blocked in logs
3. View audit trail in Kibana

### Kịch bản 3: Admin Operations
1. Login as admin
2. Create new product
3. View metrics in Grafana
4. Check logs in Kibana

---

## 📝 Next Steps

1. ✅ Explore các tính năng security trong code
2. ✅ Chạy tất cả security experiments
3. ✅ Xem metrics và logs trong monitoring stack
4. ✅ Đọc YEUCAU.md để hiểu requirements chi tiết
5. ✅ Customize và extend các features

---

## 🆘 Need Help?

- **Documentation**: Xem README.md chi tiết
- **Experiments**: Xem experiments/README.md
- **Frontend**: Xem frontend/README.md
- **API Docs**: http://localhost:8080/swagger-ui.html (TODO)

---

*Happy Coding! 🚀*
