@echo off
title PinPulse Master Admin Console (Hardware-Secured)
echo ========================================================================
echo   Launching PinPulse Master Admin Console...
echo   Hardware Security Check: Authorized PC [0A:E0:AF:A6:0E:2F]
echo ========================================================================

node --version >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Node.js is required but was not found in PATH.
  echo Please install Node.js from https://nodejs.org
  pause
  exit /b 1
)

start "" http://localhost:8080
node server.js
pause
