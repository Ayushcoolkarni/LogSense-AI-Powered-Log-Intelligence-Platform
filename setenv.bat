@echo off
REM ================================================================
REM  setenv.bat — Set AI provider env vars in current CMD session
REM
REM  Usage:
REM    setenv.bat                   (prompts for key)
REM    setenv.bat AIzaXXXXXXXXXX   (pass key as argument)
REM
REM  After running, start docker-compose in the same window:
REM    docker-compose --env-file .env up -d
REM ================================================================

REM Accept API key as argument or prompt
if not "%~1"=="" (
    set GEMINI_API_KEY=%~1
    set GEMINI_ENABLED=true
) else (
    set /p GEMINI_API_KEY="Paste Gemini API key (https://aistudio.google.com/app/apikey): "
    if not "!GEMINI_API_KEY!"=="" (
        set GEMINI_ENABLED=true
    ) else (
        set GEMINI_ENABLED=false
    )
)

REM Core AI provider vars
set GEMINI_MODEL=gemini-2.0-flash
set OLLAMA_ENABLED=false
set OLLAMA_BASE_URL=http://localhost:11434
set OLLAMA_MODEL=llama3.2

REM Disable Anthropic completely
set ANTHROPIC_AI_ENABLED=false
set ANTHROPIC_API_KEY=

REM Infra defaults
set DB_HOST=postgres
set DB_PORT=5432
set DB_NAME=logsense_rca
set DB_USER=postgres
set DB_PASS=postgres
set KAFKA_BROKERS=kafka:9092

echo.
echo [ENV SET] Current session environment:
echo   GEMINI_ENABLED       = %GEMINI_ENABLED%
echo   GEMINI_API_KEY       = %GEMINI_API_KEY:~0,8%...
echo   GEMINI_MODEL         = %GEMINI_MODEL%
echo   OLLAMA_ENABLED       = %OLLAMA_ENABLED%
echo   ANTHROPIC_AI_ENABLED = %ANTHROPIC_AI_ENABLED%
echo.
echo Now run: docker-compose --env-file .env up -d rca-engine-service
echo.
