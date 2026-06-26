$ErrorActionPreference = "Stop"

Write-Host "============================================"
Write-Host "   KIEM TRA QUYEN ADMIN DOC DANH SACH USER"
Write-Host "============================================"
Write-Host ""

try {
    # 1. Dang nhap voi tu cach admin1 de lay JWT Token
    Write-Host "[1] Dang dang nhap vao he thong voi tai khoan admin1..."
    $loginResponse = Invoke-RestMethod -Uri "https://tienthienvienman.site.je/api/users/login" -Method Post -Body '{"username":"admin1","password":"AdminPass@1"}' -ContentType "application/json"
    
    $jwt = $loginResponse.token
    Write-Host " -> Dang nhap thanh cong! Da lay duoc JWT Token."
    Write-Host ""

    # 2. Dung JWT Token do de lay danh sach Users
    Write-Host "[2] Dang dung Token de lay danh sach Nguoi dung (Users)..."
    $usersResponse = Invoke-RestMethod -Uri "https://tienthienvienman.site.je/api/users" -Method Get -Headers @{Authorization="Bearer $jwt"}
    
    Write-Host " -> Lay danh sach thanh cong! Co $($usersResponse.Count) nguoi dung trong he thong."
    Write-Host " -> Hien thi 3 nguoi dung dau tien:"
    $usersResponse | Select-Object -First 3 | Format-Table id, username, email, role, isActive

    Write-Host ""
    Write-Host "============================================"
    Write-Host "          TEST THANH CONG! TAI HAO!"
    Write-Host "============================================"
} catch {
    Write-Host ""
    Write-Host "❌ CO LOI XAY RA KHI KIEM TRA:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "❌ Vui long dam bao ban da chay 'chay-toan-bo.bat' roi nhe!" -ForegroundColor Yellow
}

Write-Host ""
Read-Host -Prompt "Bam Enter de thoat..."
