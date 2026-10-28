#!/usr/bin/env bash
# ==============================================================================
# MediCare AI - Linux / Cloud Deployment Helper Script
# ==============================================================================
set -e

echo "=============================================="
echo "   MediCare AI - Automated Cloud Deployment   "
echo "=============================================="

# 1. Environment check
if [ ! -f .env ]; then
    echo "[1/3] Initializing .env configuration from template..."
    cp .env.example .env
    echo "      Created .env file. Please review production credentials."
else
    echo "[1/3] Existing .env file detected."
fi

# 2. Build & Launch Containers
echo "[2/3] Building and starting containers via docker-compose..."
docker compose up -d --build

# 3. Status Check
echo "[3/3] Verifying service health..."
sleep 5
docker compose ps

echo "=============================================="
echo "Deployment successful!"
echo "  Web Portal:    http://localhost:8080"
echo "  Mailpit UI:    http://localhost:8025"
echo "  Health Check:  http://localhost:8080/actuator/health"
echo "=============================================="
