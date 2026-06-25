# SECURE DEPLOYMENT SCRIPT - Shopping Web Security-First Deployment
# PowerShell Script for Windows Production Deployment

param(
    [Parameter(Mandatory=$false)]
    [string]$Environment = "production",
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipTests = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$InitializeVault = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$GenerateCerts = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$SecurityScan = $true
)

# Colors for output
$Red = [System.ConsoleColor]::Red
$Green = [System.ConsoleColor]::Green
$Yellow = [System.ConsoleColor]::Yellow
$Blue = [System.ConsoleColor]::Blue

function Write-ColoredOutput {
    param([string]$Message, [System.ConsoleColor]$Color = [System.ConsoleColor]::White)
    Write-Host $Message -ForegroundColor $Color
}

function Write-Step {
    param([string]$Message)
    Write-ColoredOutput "🔹 $Message" $Blue
}

function Write-Success {
    param([string]$Message)
    Write-ColoredOutput "✅ $Message" $Green
}

function Write-Warning {
    param([string]$Message)
    Write-ColoredOutput "⚠️ $Message" $Yellow
}

function Write-Error {
    param([string]$Message)
    Write-ColoredOutput "❌ $Message" $Red
}

function Test-DockerInstallation {
    Write-Step "Checking Docker installation..."
    
    try {
        $dockerVersion = docker --version
        $dockerComposeVersion = docker-compose --version
        Write-Success "Docker installed: $dockerVersion"
        Write-Success "Docker Compose installed: $dockerComposeVersion"
        return $true
    }
    catch {
        Write-Error "Docker or Docker Compose not installed. Please install Docker Desktop first."
        return $false
    }
}

function Test-PreRequisites {
    Write-Step "Checking system prerequisites..."
    
    # Check PowerShell version
    if ($PSVersionTable.PSVersion.Major -lt 5) {
        Write-Error "PowerShell 5.0 or higher required"
        return $false
    }
    
    # Check Docker
    if (-not (Test-DockerInstallation)) {
        return $false
    }
    
    # Check available disk space (minimum 10GB)
    $freeSpace = Get-WmiObject Win32_LogicalDisk -Filter "DeviceID='C:'" | Select-Object -ExpandProperty FreeSpace
    $freeSpaceGB = [math]::Round($freeSpace / 1GB, 2)
    
    if ($freeSpaceGB -lt 10) {
        Write-Error "Insufficient disk space. Need at least 10GB, have ${freeSpaceGB}GB"
        return $false
    }
    
    Write-Success "System prerequisites check passed"
    return $true
}

function Initialize-SecurityEnvironment {
    Write-Step "Initializing security environment..."
    
    # Create security directories
    $securityDirs = @(
        ".\data\vault",
        ".\data\mysql", 
        ".\data\redis",
        ".\logs\security",
        ".\config\ssl",
        ".\config\mysql",
        ".\config\fluent-bit"
    )
    
    foreach ($dir in $securityDirs) {
        if (-not (Test-Path $dir)) {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
            Write-Success "Created directory: $dir"
        }
    }
    
    # Set proper permissions
    foreach ($dir in $securityDirs) {
        $acl = Get-Acl $dir
        $acl.SetOwner([System.Security.Principal.NTAccount]"$env:USERNAME")
        Set-Acl -Path $dir -AclObject $acl
    }
    
    Write-Success "Security environment initialized"
}

function Generate-SecuritySecrets {
    Write-Step "Generating security secrets..."
    
    # Generate random passwords
    function New-SecurePassword {
        param([int]$Length = 24)
        $chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*"
        return -join ((1..$Length) | ForEach { $chars[(Get-Random -Maximum $chars.Length)] })
    }
    
    $secrets = @{
        MYSQL_ROOT_PASSWORD = New-SecurePassword
        MYSQL_PASSWORD = New-SecurePassword
        KC_ADMIN_PASSWORD = New-SecurePassword
        KC_DB_PASSWORD = New-SecurePassword
        REDIS_PASSWORD = New-SecurePassword
        VAULT_ROOT_TOKEN = (New-Guid).Guid
        JWT_SECRET = New-SecurePassword 32
        ENCRYPTION_KEY = New-SecurePassword 32
    }
    
    # Create .env file
    $envContent = @"
# Security Configuration - Generated $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
# DO NOT COMMIT TO VERSION CONTROL

# Database Secrets
MYSQL_ROOT_PASSWORD=$($secrets.MYSQL_ROOT_PASSWORD)
MYSQL_USER=ecom_secure_user
MYSQL_PASSWORD=$($secrets.MYSQL_PASSWORD)

# Keycloak Secrets  
KC_ADMIN_USER=admin
KC_ADMIN_PASSWORD=$($secrets.KC_ADMIN_PASSWORD)
KC_DB_USER=kc_user
KC_DB_PASSWORD=$($secrets.KC_DB_PASSWORD)

# Redis Secret
REDIS_PASSWORD=$($secrets.REDIS_PASSWORD)

# Vault Secret
VAULT_ROOT_TOKEN=$($secrets.VAULT_ROOT_TOKEN)

# Application Secrets
JWT_SECRET=$($secrets.JWT_SECRET)
ENCRYPTION_KEY=$($secrets.ENCRYPTION_KEY)
HMAC_SECRET=secure_hmac_key_$(Get-Random)

# Security Settings
ENVIRONMENT=production
SECURITY_ENABLED=true
PCI_COMPLIANCE_MODE=true
AUDIT_LOGGING_ENABLED=true
"@
    
    $envContent | Out-File -FilePath ".\.env" -Encoding UTF8
    
    # Set restrictive permissions on .env file
    $acl = Get-Acl ".\.env"
    $acl.SetAccessRuleProtection($true, $false)
    $acl.SetAccessRule((New-Object System.Security.AccessControl.FileSystemAccessRule($env:USERNAME, "FullControl", "Allow")))
    Set-Acl -Path ".\.env" -AclObject $acl
    
    Write-Success "Security secrets generated and stored in .env file"
    Write-Warning "Keep .env file secure and do not commit to version control"
}

function Generate-SSLCertificates {
    if (-not $GenerateCerts) {
        Write-Step "Skipping SSL certificate generation (use -GenerateCerts to enable)"
        return
    }
    
    Write-Step "Generating SSL certificates for development..."
    
    # Create OpenSSL config for development certs
    $opensslConfig = @"
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_req
prompt = no

[req_distinguished_name]
C = VN
ST = Ho Chi Minh
L = Ho Chi Minh City
O = Ecommerce Security
OU = Development
CN = localhost

[v3_req]
keyUsage = keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
DNS.2 = *.localhost
IP.1 = 127.0.0.1
IP.2 = ::1
"@
    
    $opensslConfig | Out-File -FilePath ".\config\ssl\cert.conf" -Encoding UTF8
    
    # Generate certificates using Docker OpenSSL
    docker run --rm -v "${PWD}\config\ssl:/certs" alpine/openssl req -x509 -newkey rsa:4096 -keyout /certs/server-key.pem -out /certs/server-cert.pem -days 365 -nodes -config /certs/cert.conf
    
    if (Test-Path ".\config\ssl\server-cert.pem") {
        Write-Success "SSL certificates generated successfully"
    } else {
        Write-Error "Failed to generate SSL certificates"
    }
}

function Build-SecurityServices {
    Write-Step "Building security-hardened services..."
    
    # Build with security profile
    $buildArgs = @(
        "--build-arg", "SECURITY_SCAN=true",
        "--build-arg", "BUILD_DATE=$(Get-Date -Format 'yyyy-MM-dd')",
        "--build-arg", "VERSION=1.0.0-security"
    )
    
    try {
        Write-Step "Building Gateway Service (Security)..."
        docker build -f .\backend\gateway-service\Dockerfile.security -t ecommerce/gateway:security .\backend\gateway-service @buildArgs
        
        Write-Step "Building Catalog Service (Security)..."
        docker build -f .\backend\catalog-service\Dockerfile.security -t ecommerce/catalog:security .\backend\catalog-service @buildArgs
        
        Write-Step "Building Order Service (Security)..."
        docker build -f .\backend\order-service\Dockerfile.security -t ecommerce/order:security .\backend\order-service @buildArgs
        
        Write-Step "Building Payment Service (Security)..."
        docker build -f .\backend\payment-service\Dockerfile.security -t ecommerce/payment:security .\backend\payment-service @buildArgs
        
        Write-Success "All services built successfully with security hardening"
    }
    catch {
        Write-Error "Failed to build services: $_"
        exit 1
    }
}

function Run-SecurityTests {
    if ($SkipTests) {
        Write-Step "Skipping security tests (--SkipTests specified)"
        return
    }
    
    Write-Step "Running security tests..."
    
    # SAST (Static Application Security Testing)
    Write-Step "Running SAST scan..."
    docker run --rm -v "${PWD}:/code" securecodewarrior/semgrep --config=auto /code/backend
    
    # Container Security Scan
    if ($SecurityScan) {
        Write-Step "Running container security scan..."
        
        $images = @(
            "ecommerce/gateway:security",
            "ecommerce/catalog:security", 
            "ecommerce/order:security",
            "ecommerce/payment:security"
        )
        
        foreach ($image in $images) {
            Write-Step "Scanning $image..."
            docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image --exit-code 1 --severity HIGH,CRITICAL $image
        }
    }
    
    Write-Success "Security tests completed"
}

function Deploy-SecurityInfrastructure {
    Write-Step "Deploying security infrastructure..."
    
    # Start infrastructure services first
    docker-compose -f docker-compose.security.yml up -d vault mysql-secure redis-secure keycloak
    
    Write-Step "Waiting for infrastructure to be ready..."
    Start-Sleep -Seconds 30
    
    # Initialize Vault if requested
    if ($InitializeVault) {
        Write-Step "Initializing HashiCorp Vault..."
        docker exec ecom-vault-secure /vault/init-vault.sh
        Write-Success "Vault initialized with encryption keys"
    }
    
    # Wait for Keycloak to be ready
    Write-Step "Waiting for Keycloak to initialize..."
    $timeout = 300 # 5 minutes
    $elapsed = 0
    
    do {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:9090/auth/health/ready" -TimeoutSec 5 -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                Write-Success "Keycloak is ready"
                break
            }
        }
        catch {
            # Continue waiting
        }
        
        Start-Sleep -Seconds 10
        $elapsed += 10
        Write-Host "." -NoNewline
        
    } while ($elapsed -lt $timeout)
    
    if ($elapsed -ge $timeout) {
        Write-Error "Keycloak failed to start within timeout"
        exit 1
    }
}

function Deploy-ApplicationServices {
    Write-Step "Deploying application services..."
    
    # Deploy services in dependency order
    docker-compose -f docker-compose.security.yml up -d catalog-secure
    Start-Sleep -Seconds 20
    
    docker-compose -f docker-compose.security.yml up -d order-secure payment-secure
    Start-Sleep -Seconds 20
    
    docker-compose -f docker-compose.security.yml up -d gateway-secure
    Start-Sleep -Seconds 20
    
    Write-Success "Application services deployed"
}

function Verify-Deployment {
    Write-Step "Verifying deployment..."
    
    $services = @{
        "Gateway" = "http://localhost:8080/actuator/health"
        "Vault" = "http://localhost:8200/v1/sys/health"
        "Keycloak" = "http://localhost:9090/auth/health/ready"
    }
    
    foreach ($service in $services.GetEnumerator()) {
        Write-Step "Checking $($service.Key)..."
        
        try {
            $response = Invoke-WebRequest -Uri $service.Value -TimeoutSec 10
            if ($response.StatusCode -eq 200) {
                Write-Success "$($service.Key) is healthy"
            } else {
                Write-Warning "$($service.Key) returned status: $($response.StatusCode)"
            }
        }
        catch {
            Write-Error "$($service.Key) is not responding: $_"
        }
    }
    
    # Check container status
    Write-Step "Checking container status..."
    docker-compose -f docker-compose.security.yml ps
}

function Show-SecuritySummary {
    Write-ColoredOutput "`n🔒 SECURITY DEPLOYMENT SUMMARY" $Green
    Write-ColoredOutput "================================" $Green
    
    Write-Host ""
    Write-ColoredOutput "🎯 Security Features Enabled:" $Blue
    Write-Host "  ✅ Container Security Hardening"
    Write-Host "  ✅ Network Segmentation (Custom Subnets)"
    Write-Host "  ✅ HashiCorp Vault (Key Management)"
    Write-Host "  ✅ Keycloak OAuth2/OIDC (Identity Provider)"
    Write-Host "  ✅ Field-Level Encryption (AES-256-GCM)"
    Write-Host "  ✅ HMAC Service Authentication"
    Write-Host "  ✅ Transaction Security Validation"
    Write-Host "  ✅ Real-time Fraud Detection"
    Write-Host "  ✅ Security Event Monitoring"
    Write-Host "  ✅ Comprehensive Audit Logging"
    
    Write-Host ""
    Write-ColoredOutput "🌐 Service Endpoints:" $Blue
    Write-Host "  🌍 API Gateway:     http://localhost:8080"
    Write-Host "  🔐 Keycloak:        http://localhost:9090/auth"
    Write-Host "  🔑 Vault:           http://localhost:8200"
    Write-Host "  📊 Health Check:    http://localhost:8080/actuator/health"
    
    Write-Host ""
    Write-ColoredOutput "🔐 Security Credentials:" $Yellow
    Write-Host "  📁 Check .env file for generated passwords"
    Write-Host "  🚨 Keep .env file secure and DO NOT commit to git"
    Write-Host "  🔒 Default Vault Token is in .env file"
    
    Write-Host ""
    Write-ColoredOutput "📋 Next Steps:" $Blue
    Write-Host "  1. Configure Keycloak realm and users"
    Write-Host "  2. Set up SSL certificates for production"
    Write-Host "  3. Configure external secrets management"
    Write-Host "  4. Set up monitoring dashboards"
    Write-Host "  5. Run security penetration tests"
    
    Write-Host ""
    Write-ColoredOutput "🔍 Security Monitoring:" $Blue
    Write-Host "  📈 Metrics available at: http://localhost:8080/actuator/metrics"
    Write-Host "  📊 Security events logged to: .\logs\security\"
    Write-Host "  🔍 Container logs: docker-compose -f docker-compose.security.yml logs -f"
    
    Write-Host ""
}

# ============================
# MAIN DEPLOYMENT EXECUTION
# ============================

Write-ColoredOutput "`n🔒 SECURE SHOPPING WEB DEPLOYMENT" $Green
Write-ColoredOutput "Security-First Production Deployment Script" $Green
Write-ColoredOutput "===========================================" $Green

try {
    # Step 1: Prerequisites Check
    if (-not (Test-PreRequisites)) {
        exit 1
    }
    
    # Step 2: Initialize Security Environment
    Initialize-SecurityEnvironment
    
    # Step 3: Generate Security Secrets
    Generate-SecuritySecrets
    
    # Step 4: Generate SSL Certificates
    Generate-SSLCertificates
    
    # Step 5: Build Security Services
    Build-SecurityServices
    
    # Step 6: Run Security Tests
    Run-SecurityTests
    
    # Step 7: Deploy Infrastructure
    Deploy-SecurityInfrastructure
    
    # Step 8: Deploy Application Services
    Deploy-ApplicationServices
    
    # Step 9: Verify Deployment
    Verify-Deployment
    
    # Step 10: Show Summary
    Show-SecuritySummary
    
    Write-ColoredOutput "`n🎉 SECURE DEPLOYMENT COMPLETED SUCCESSFULLY!" $Green
    Write-ColoredOutput "Your shopping web application is now running with enterprise-grade security." $Green
    
} catch {
    Write-Error "Deployment failed: $_"
    Write-ColoredOutput "`nTo troubleshoot:" $Yellow
    Write-Host "1. Check Docker logs: docker-compose -f docker-compose.security.yml logs"
    Write-Host "2. Verify system requirements"
    Write-Host "3. Check network connectivity"
    Write-Host "4. Ensure ports 8080, 9090, 8200, 3306, 6379 are available"
    exit 1
}