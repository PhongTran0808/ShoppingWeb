# 📦 Hướng dẫn Cài đặt Chi tiết

## 🔍 Kiểm tra trước khi cài đặt

### 1. Kiểm tra Node.js
```bash
node --version
# Cần: v18.0.0 trở lên
```

### 2. Kiểm tra Java
```bash
java --version
# Cần: Java 21
```

### 3. Kiểm tra Maven
```bash
mvn --version
# Cần: Maven 3.8+
```

### 4. Kiểm tra Docker
```bash
docker --version
docker-compose --version
```

---

## 📥 Cài đặt Frontend

### Bước 1: Mở Command Prompt/PowerShell
```cmd
cd d:\Nam3\MatMaUngDung\CuoiKi\ShoppingWeb\frontend
```

### Bước 2: Cài đặt Dependencies
```cmd
npm install
```

**Nếu gặp lỗi, thử:**
```cmd
# Xóa node_modules và package-lock.json (nếu có)
rmdir /s /q node_modules
del package-lock.json

# Cài lại
npm install --legacy-peer-deps
```

### Bước 3: Kiểm tra cài đặt
```cmd
npm run build
```

Nếu build thành công → Cài đặt OK!

### Bước 4: Chạy Development Server
```cmd
npm run dev
```

Mở browser: http://localhost:3000

---

## 🐳 Cài đặt Infrastructure

### Bước 1: Khởi động Core Services
```cmd
cd d:\Nam3\MatMaUngDung\CuoiKi\ShoppingWeb
docker-compose up -d
```

### Bước 2: Kiểm tra trạng thái
```cmd
docker ps
```

Bạn sẽ thấy 4 containers:
- ecom-mysql
- ecom-vault
- ecom-keycloak
- ecom-redis

### Bước 3: Khởi tạo Vault
```cmd
docker exec -it ecom-vault sh /init-vault.sh
```

### Bước 4: Kiểm tra Vault
```cmd
docker exec -it ecom-vault vault status
```

### Bước 5: Khởi động Monitoring (Tuỳ chọn)
```cmd
docker-compose -f docker-compose.monitoring.yml up -d
```

---

## ☕ Cài đặt Backend

### Option 1: Sử dụng IntelliJ IDEA (Khuyến nghị)

1. **Mở IntelliJ IDEA**
2. **File → Open → Chọn thư mục `backend/`**
3. **Đợi Maven import dependencies**
4. **Chạy từng service:**
   - Tìm `GatewayApplication.java` → Click chuột phải → Run
   - Tìm `CatalogApplication.java` → Click chuột phải → Run
   - Tìm `CartApplication.java` → Click chuột phải → Run
   - Tìm `OrderApplication.java` → Click chuột phải → Run
   - Tìm `PaymentApplication.java` → Click chuột phải → Run

### Option 2: Sử dụng Maven CLI

Mở 5 Command Prompt windows riêng biệt:

**Window 1: Gateway**
```cmd
cd d:\Nam3\MatMaUngDung\CuoiKi\ShoppingWeb\backend\gateway-service
mvn spring-boot:run
```

**Window 2: Catalog**
```cmd
cd d:\Nam3\MatMaUngDung\CuoiKi\ShoppingWeb\backend\catalog-service
mvn spring-boot:run
```

**Window 3: Cart**
```cmd
cd d:\Nam3\MatMaUngDung\CuoiKi\ShoppingWeb\backend\cart-service
mvn spring-boot:run
```

**Window 4: Order**
```cmd
cd d:\Nam3\MatMaUngDung\CuoiKi\ShoppingWeb\backend\order-service
mvn spring-boot:run
```

**Window 5: Payment**
```cmd
cd d:\Nam3\MatMaUngDung\CuoiKi\ShoppingWeb\backend\payment-service
mvn spring-boot:run
```

### Kiểm tra Backend Services

```cmd
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

Tất cả nên trả về `{"status":"UP"}`

---

## 🐍 Cài đặt Python cho Security Experiments

### Bước 1: Cài Python 3.11+
Download từ: https://www.python.org/downloads/

### Bước 2: Cài đặt Dependencies
```cmd
pip install requests pytest faker
```

### Bước 3: Test chạy experiment
```cmd
cd d:\Nam3\MatMaUngDung\CuoiKi\ShoppingWeb\experiments\token-replay-test
python test_token_replay.py
```

---

## ✅ Kiểm tra Toàn bộ Hệ thống

### Checklist

- [ ] **Infrastructure**
  - [ ] MySQL running (port 3306)
  - [ ] Vault running (port 8200)
  - [ ] Keycloak running (port 9090)
  - [ ] Redis running (port 6379)

- [ ] **Backend Services**
  - [ ] Gateway (port 8080)
  - [ ] Catalog (port 8081)
  - [ ] Cart (port 8082)
  - [ ] Order (port 8083)
  - [ ] Payment (port 8084)

- [ ] **Frontend**
  - [ ] Next.js dev server (port 3000)
  - [ ] Can access http://localhost:3000

- [ ] **Monitoring (Optional)**
  - [ ] Prometheus (port 9091)
  - [ ] Grafana (port 3001)
  - [ ] Kibana (port 5601)
  - [ ] Kafka UI (port 8090)

### Quick Test Script

Tạo file `test_all_services.ps1`:

```powershell
Write-Host "Testing all services..." -ForegroundColor Green

# Test Infrastructure
Write-Host "`nTesting Infrastructure..." -ForegroundColor Yellow
$urls = @(
    "http://localhost:3306",  # MySQL (will fail but shows port is open)
    "http://localhost:8200/v1/sys/health",  # Vault
    "http://localhost:9090",  # Keycloak
    "http://localhost:6379"   # Redis (will fail but shows port is open)
)

# Test Backend
Write-Host "`nTesting Backend Services..." -ForegroundColor Yellow
$backend = @(
    "http://localhost:8080/actuator/health",
    "http://localhost:8081/actuator/health",
    "http://localhost:8082/actuator/health",
    "http://localhost:8083/actuator/health",
    "http://localhost:8084/actuator/health"
)

foreach ($url in $backend) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2
        Write-Host "✓ $url - OK" -ForegroundColor Green
    } catch {
        Write-Host "✗ $url - FAILED" -ForegroundColor Red
    }
}

# Test Frontend
Write-Host "`nTesting Frontend..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:3000" -UseBasicParsing -TimeoutSec 2
    Write-Host "✓ Frontend - OK" -ForegroundColor Green
} catch {
    Write-Host "✗ Frontend - FAILED" -ForegroundColor Red
}

Write-Host "`nTest completed!" -ForegroundColor Green
```

Chạy:
```powershell
.\test_all_services.ps1
```

---

## 🔧 Troubleshooting

### Lỗi: Port đã bị sử dụng

```cmd
# Tìm process đang dùng port
netstat -ano | findstr "8080"

# Kill process (thay PID bằng số từ lệnh trên)
taskkill /PID [PID] /F
```

### Lỗi: Docker container không start

```cmd
# Xem logs
docker logs ecom-mysql
docker logs ecom-vault

# Restart container
docker-compose restart mysql
docker-compose restart vault

# Xóa và tạo lại
docker-compose down -v
docker-compose up -d
```

### Lỗi: Maven build failed

```cmd
# Clear Maven cache
mvn clean install -U

# Skip tests
mvn clean install -DskipTests
```

### Lỗi: npm install failed

```cmd
# Clear npm cache
npm cache clean --force

# Use different registry
npm install --registry=https://registry.npmmirror.com

# Install with legacy peer deps
npm install --legacy-peer-deps
```

### Lỗi: Cannot connect to database

1. Kiểm tra MySQL container:
```cmd
docker logs ecom-mysql
docker exec -it ecom-mysql mysql -uroot -prootpassword
```

2. Test connection:
```sql
SHOW DATABASES;
USE shopping;
SHOW TABLES;
```

### Lỗi: Vault not initialized

```cmd
# Re-run init script
docker exec -it ecom-vault sh /init-vault.sh

# Check status
docker exec -it ecom-vault vault status

# Enable transit engine manually
docker exec -it ecom-vault vault secrets enable transit
docker exec -it ecom-vault vault write -f transit/keys/ecommerce-key
```

---

## 📞 Cần Hỗ trợ?

1. **Xem documentation:**
   - README.md
   - QUICKSTART.md
   - SUMMARY.md

2. **Check logs:**
   - Docker: `docker logs [container-name]`
   - Backend: Check IntelliJ console
   - Frontend: Check terminal/browser console

3. **Common issues:**
   - Port conflicts → Change ports hoặc kill processes
   - Permission denied → Chạy với admin rights
   - Dependencies issue → Clear cache và reinstall

---

*Installation guide complete! 🎉*
