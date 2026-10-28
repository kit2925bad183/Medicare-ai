# ==============================================================================
# MediCare AI - Windows 1-Click Deployment Helper Script
# ==============================================================================

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   MediCare AI - Automated Deployment Script   " -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

# 1. Check for .env file
if (-not (Test-Path ".env")) {
    Write-Host "[1/4] Copying default environment configuration from .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "      Created .env. Please update secrets before going live!" -ForegroundColor Green
} else {
    Write-Host "[1/4] Existing .env file found." -ForegroundColor Green
}

# 2. Check Docker availability
Write-Host "[2/4] Checking Docker engine..." -ForegroundColor Yellow
$dockerInstalled = Get-Command "docker" -ErrorAction SilentlyContinue

if ($dockerInstalled) {
    Write-Host "      Docker is installed. Launching containers via docker-compose..." -ForegroundColor Green
    docker compose up -d --build
    Write-Host "==============================================" -ForegroundColor Green
    Write-Host "MediCare AI is running in Docker:" -ForegroundColor Green
    Write-Host "  Web App:       http://localhost:8080" -ForegroundColor White
    Write-Host "  Mailpit UI:    http://localhost:8025" -ForegroundColor White
    Write-Host "  MySQL DB:      localhost:3306" -ForegroundColor White
    Write-Host "==============================================" -ForegroundColor Green
} else {
    Write-Host "      Docker not found. Falling back to local Maven standalone package build..." -ForegroundColor Yellow
    Write-Host "[3/4] Packaging executable JAR with Maven..." -ForegroundColor Yellow
    & "C:\Users\vishv\.maven\maven-3.9.15\bin\mvn.cmd" clean package -DskipTests
    Write-Host "[4/4] Executable JAR built successfully at target\medicare-ai-0.0.1-SNAPSHOT.jar" -ForegroundColor Green
    Write-Host "      To run locally: java -jar target\medicare-ai-0.0.1-SNAPSHOT.jar" -ForegroundColor White
}
