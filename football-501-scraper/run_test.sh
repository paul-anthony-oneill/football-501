#!/bin/bash
# Football 501 Integration Test Runner (Mac/Linux)

set -e

echo "======================================================================"
echo "  Football 501 - Integration Test Runner"
echo "======================================================================"
echo ""

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo "ERROR: Docker is not running!"
    echo "Please start Docker and try again."
    exit 1
fi

echo "[1/4] Starting PostgreSQL with docker-compose..."
docker-compose up -d postgres

echo ""
echo "[2/4] Waiting for PostgreSQL to be ready..."
sleep 10

echo ""
echo "[3/4] Checking if Python dependencies are installed..."
if ! python3 -c "import ScraperFC" 2>/dev/null; then
    echo "Installing Python dependencies..."
    pip3 install -r requirements.txt
fi

echo ""
echo "[4/4] Running integration test..."
echo ""
python3 test_integration.py

echo ""
echo "======================================================================"
echo "  Test Complete"
echo "======================================================================"
echo ""
