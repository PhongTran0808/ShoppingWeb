Write-Host "=============================================="
Write-Host " KICH HOAT HE THONG E-COMMERCE TU DONG "
Write-Host "=============================================="
Write-Host ""

# Auto-detect Java 17 or fallback to system Java
if (Test-Path "C:\Program Files\Java\jdk-17") {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    Write-Host "-> Da thiet lap JAVA_HOME = $env:JAVA_HOME"
} else {
    Write-Host "-> Khong tim thay JDK 17 o o C, su dung Java mac dinh cua he thong."
}
Write-Host ""
Write-Host "[1/3] Dang khoi dong HashiCorp Vault (KMS)..."
# Tat vault cu neu dang chay
Stop-Process -Name "vault" -ErrorAction SilentlyContinue

# Khoi chay Vault ngam
$vaultProcess = Start-Process -FilePath ".\vault.exe" -ArgumentList "server", "-dev" -WindowStyle Hidden -PassThru -RedirectStandardOutput "vault_out.txt"
Start-Sleep -Seconds 3

# Lay Token tu dong
$tokenMatch = Select-String -Path "vault_out.txt" -Pattern "Root Token: (hvs\.[a-zA-Z0-9]+)"
if ($tokenMatch) {
    $token = $tokenMatch.Matches.Groups[1].Value
    Write-Host "-> Da lay duoc Vault Token tu dong: $token"
    
    Write-Host "[2/3] Dang nhung Token vao cau hinh Spring Boot..."
    $files = Get-ChildItem -Path "backend" -Filter "application.yml" -Recurse
    foreach ($file in $files) {
        $content = Get-Content $file.FullName
        $content = $content -replace 'token: .*', "token: `"$token`""
        Set-Content -Path $file.FullName -Value $content
    }
    Write-Host "-> Da cap nhat xong cau hinh!"
} else {
    Write-Host "-> [LOI] Khong lay duoc Token. Hay chac chan file vault.exe nam o thu muc nay!"
    exit
}

Write-Host "[3/3] Dang don dep cac cong cu (neu co)..."
foreach ($port in 8080, 8081, 8082, 8083, 8084) {
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conn) {
        Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "[4/4] Dang khoi dong 5 Microservices chay NGAM (Che do sieu nhe)..."
$p1 = Start-Process java -ArgumentList "-jar backend\gateway-service\target\gateway-service-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden -PassThru
$p2 = Start-Process java -ArgumentList "-jar backend\catalog-service\target\catalog-service-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden -PassThru
$p3 = Start-Process java -ArgumentList "-jar backend\cart-service\target\cart-service-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden -PassThru
$p4 = Start-Process java -ArgumentList "-jar backend\order-service\target\order-service-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden -PassThru
$p5 = Start-Process java -ArgumentList "-jar backend\payment-service\target\payment-service-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden -PassThru

Write-Host "=============================================="
Write-Host " HOAN TAT! Tat ca dich vu dang chay ngam cuc ky nhe may."
Write-Host " Vui long doi khoang 30-60 giay de he thong san sang."
Write-Host " Hay vao trinh duyet: http://localhost:3000"
Write-Host "=============================================="
Write-Host ""
Write-Host " [DE TAT TOAN BO HE THONG]: Nhan phim ENTER tai day..." -ForegroundColor Red
Read-Host

Write-Host "Dang tat he thong..."
Stop-Process -Id $p1.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $p2.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $p3.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $p4.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $p5.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Name vault -Force -ErrorAction SilentlyContinue
Write-Host "Da tat toan bo!"
