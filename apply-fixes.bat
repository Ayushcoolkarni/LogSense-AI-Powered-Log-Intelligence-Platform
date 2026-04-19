@echo off
REM Run this from your repo root: LogSense-AI-Powered-Log-Intelligence-Platform\
REM Usage: apply-fixes.bat

echo Applying LogSense fixes...

REM ── docker-compose ──────────────────────────────────────────────────────────
copy /Y "logsense-fixes\docker-compose.yml" "infra\docker-compose.yml"

REM ── Frontend ─────────────────────────────────────────────────────────────────
copy /Y "logsense-fixes\frontend\Dockerfile" "frontend\Dockerfile"
copy /Y "logsense-fixes\frontend\nginx.conf" "frontend\nginx.conf"

REM ── log-ingestion-service ────────────────────────────────────────────────────
copy /Y "logsense-fixes\services\log-ingestion-service\Dockerfile" "services\log-ingestion-service\Dockerfile"
copy /Y "logsense-fixes\services\log-ingestion-service\src\main\resources\application.yml" "services\log-ingestion-service\src\main\resources\application.yml"

REM ── anomaly-detector-service ─────────────────────────────────────────────────
copy /Y "logsense-fixes\services\anomaly-detector-service\Dockerfile" "services\anomaly-detector-service\Dockerfile"
copy /Y "logsense-fixes\services\anomaly-detector-service\src\main\resources\application.yml" "services\anomaly-detector-service\src\main\resources\application.yml"

REM ── rca-engine-service ───────────────────────────────────────────────────────
copy /Y "logsense-fixes\services\rca-engine-service\Dockerfile" "services\rca-engine-service\Dockerfile"
copy /Y "logsense-fixes\services\rca-engine-service\src\main\resources\application.yml" "services\rca-engine-service\src\main\resources\application.yml"

REM ── alert-service ────────────────────────────────────────────────────────────
copy /Y "logsense-fixes\services\alert-service\Dockerfile" "services\alert-service\Dockerfile"
copy /Y "logsense-fixes\services\alert-service\src\main\resources\application.yml" "services\alert-service\src\main\resources\application.yml"

REM ── incident-query-service ───────────────────────────────────────────────────
copy /Y "logsense-fixes\services\incident-query-service\Dockerfile" "services\incident-query-service\Dockerfile"
copy /Y "logsense-fixes\services\incident-query-service\src\main\resources\application.yml" "services\incident-query-service\src\main\resources\application.yml"

echo.
echo All files applied. Now run:
echo   cd infra
echo   docker compose up --build
