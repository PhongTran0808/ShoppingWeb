Write-Host "================================================================="
Write-Host "       KHOI DONG TOAN BO HE THONG (BACKEND + FRONTEND)"
Write-Host "================================================================="
Write-Host ""

# 1. Start Watcher
$watcherCode = @"
    param(`$ParentPid)
    Wait-Process -Id `$ParentPid -ErrorAction SilentlyContinue
    Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
    Stop-Process -Name "vault" -Force -ErrorAction SilentlyContinue
    Stop-Process -Name "httpd" -Force -ErrorAction SilentlyContinue
    Stop-Process -Name "node" -Force -ErrorAction SilentlyContinue
"@
$watcherFile = "$env:TEMP\ecommerce_watcher.ps1"
$watcherCode | Out-File -FilePath $watcherFile -Encoding UTF8
Start-Process powershell -ArgumentList "-WindowStyle Hidden -ExecutionPolicy Bypass -File `"$watcherFile`" -ParentPid $PID"

# 2. Setup Java
if (Test-Path "C:\Program Files\Java\jdk-17") {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}

# 3. Kill existing
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "vault" -Force -ErrorAction SilentlyContinue
Stop-Process -Name "httpd" -Force -ErrorAction SilentlyContinue

# 4. Start Vault
Write-Host "[1/4] Dang khoi dong HashiCorp Vault (KMS)..."
Start-Process -FilePath ".\vault.exe" -ArgumentList "server", "-dev" -WindowStyle Hidden -RedirectStandardOutput "vault_out.txt"
Start-Sleep -Seconds 3

# 5. Extract Token & Update
$tokenMatch = Select-String -Path "vault_out.txt" -Pattern "Root Token: (hvs\.[a-zA-Z0-9]+)"
if ($tokenMatch) {
    $token = $tokenMatch.Matches.Groups[1].Value
    Write-Host "-> Da lay duoc Vault Token tu dong: $token"
    $files = Get-ChildItem -Path "backend" -Filter "application.yml" -Recurse
    foreach ($file in $files) {
        $content = Get-Content $file.FullName
        $content = $content -replace 'token: .*', "token: `"$token`""
        Set-Content -Path $file.FullName -Value $content
    }
}

# 6. Start Backend
Write-Host "[2/4] Dang khoi dong 5 Microservices chay ngam..."
$javaExe = "java"
Start-Process $javaExe -ArgumentList "-jar backend\gateway-service\target\gateway-service-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden
Start-Process $javaExe -ArgumentList "-jar backend\catalog-service\target\catalog-service-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden
Start-Process $javaExe -ArgumentList "-jar backend\cart-service\target\cart-service-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden
Start-Process $javaExe -ArgumentList "-jar backend\order-service\target\order-service-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden
Start-Process $javaExe -ArgumentList "-jar backend\payment-service\target\payment-service-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden

# 7. Start Apache
Write-Host "[3/4] Dang khoi dong Apache HTTPS..."
$apacheExe = "$PSScriptRoot\web\Apache24\bin\httpd.exe"
if (Test-Path $apacheExe) {
    Start-Process $apacheExe -WindowStyle Hidden
} else {
    Write-Host "Khong tim thay Apache tai: $apacheExe"
}

# 8. Start Frontend
Write-Host "[4/4] Dang mo Frontend (Next.js)..."
Write-Host "================================================================="
Write-Host " HE THONG DANG CHAY. DUNG TAT CUA SO NAY!"
Write-Host " -> Khi ban bam 'X' de tat cua so nay, toan bo he thong se TU DONG DONG."
Write-Host " -> VUI LONG DOI 30 GIAY DE CAC DICH VU KHOI DONG TRUOC KHI VAO WEB."
Write-Host " -> Truy cap: https://tienthienvienman.site.je"
Write-Host "================================================================="
Set-Location "frontend"
npm run dev
