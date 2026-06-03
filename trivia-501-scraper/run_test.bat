@echo off
REM Football 501 Integration Test Runner (Windows)

echo ======================================================================
echo   Football 501 - Integration Test Runner
echo ======================================================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo [1/4] Starting PostgreSQL with docker-compose...
docker-compose up -d postgres

echo.
echo [2/4] Waiting for PostgreSQL to be ready...
timeout /t 10 /nobreak >nul

echo.
echo [3/4] Checking if Python dependencies are installed...
python -c "import ScraperFC" 2>nul
if errorlevel 1 (
    echo Installing Python dependencies...
    pip install -r requirements.txt
)

echo.
echo [4/4] Running integration test...
echo.
python test_integration.py

echo.
echo ======================================================================
echo   Test Complete
echo ======================================================================
echo.

pause
