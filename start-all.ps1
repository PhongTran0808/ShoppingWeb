Write-Host "=============================================="
Write-Host " KÍCH HOẠT HỆ THỐNG E-COMMERCE TỰ ĐỘNG "
Write-Host "=============================================="
Write-Host ""

# Ensure we use JDK 17 to avoid Lombok compatibility issues with JDK 25
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Write-Host "-> Đã thiết lập JAVA_HOME = $env:JAVA_HOME"
Write-Host ""
Write-Host "[1/3] Đang khởi động HashiCorp Vault (KMS)..."
# Tắt vault cũ nếu đang chạy
Stop-Process -Name "vault" -ErrorAction SilentlyContinue

# Khởi chạy Vault ngầm
$vaultProcess = Start-Process -FilePath ".\vault.exe" -ArgumentList "server", "-dev" -WindowStyle Hidden -PassThru -RedirectStandardOutput "vault_out.txt"
Start-Sleep -Seconds 3

# Lấy Token tự động
$tokenMatch = Select-String -Path "vault_out.txt" -Pattern "Root Token: (hvs\.[a-zA-Z0-9]+)"
if ($tokenMatch) {
    $token = $tokenMatch.Matches.Groups[1].Value
    Write-Host "-> Đã lấy được Vault Token tự động: $token"
    
    Write-Host "[2/3] Đang nhúng Token vào cấu hình Spring Boot..."
    $files = Get-ChildItem -Path "backend" -Filter "application.yml" -Recurse
    foreach ($file in $files) {
        $content = Get-Content $file.FullName
        $content = $content -replace 'token: .*', "token: `"$token`""
        Set-Content -Path $file.FullName -Value $content
    }
    Write-Host "-> Đã cập nhật xong cấu hình!"
} else {
    Write-Host "-> [LỖI] Không lấy được Token. Hãy chắc chắn file vault.exe nằm ở thư mục này!"
    exit
}

Write-Host "[3/3] Đang khởi động 5 Microservices Spring Boot (Chế độ siêu nhẹ giảm lag)..."
Start-Process cmd -ArgumentList "/c title Gateway Service (8080) && java -jar backend\gateway-service\target\gateway-service-1.0.0-SNAPSHOT.jar"
Start-Process cmd -ArgumentList "/c title Catalog Service (8081) && java -jar backend\catalog-service\target\catalog-service-1.0.0-SNAPSHOT.jar"
Start-Process cmd -ArgumentList "/c title Cart Service (8082) && java -jar backend\cart-service\target\cart-service-1.0.0-SNAPSHOT.jar"
Start-Process cmd -ArgumentList "/c title Order Service (8083) && java -jar backend\order-service\target\order-service-1.0.0-SNAPSHOT.jar"
Start-Process cmd -ArgumentList "/c title Payment Service (8084) && java -jar backend\payment-service\target\payment-service-1.0.0-SNAPSHOT.jar"

Write-Host "=============================================="
Write-Host "HOÀN TẤT! 5 cửa sổ Terminal đang được mở để chạy Code (bằng file .jar nguyên bản)."
Write-Host "Vui lòng đợi khoảng 30-60 giây để chúng khởi động xong."
Write-Host "Hãy vào bằng link: http://localhost:3000 (KHÔNG vào tên miền tienthienvienman.site.je)"
Write-Host "=============================================="
