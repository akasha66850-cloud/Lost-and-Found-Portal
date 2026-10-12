@echo off
title Campus Lost & Found - System Launcher
echo ===============================================================
echo      CAMPUS LOST & FOUND SYSTEM - ONE-CLICK LAUNCHER
echo ===============================================================
echo.
echo [1/2] Starting Backend Server (Port 8088)...
start "Campus Lost & Found - Backend Server" "%~dp0run-backend.bat"

echo.
echo Waiting 6 seconds for Spring Boot server to start...
timeout /t 6 /nobreak >nul

echo.
echo [2/2] Starting JavaFX Desktop Client...
start "Campus Lost & Found - Desktop Client" "%~dp0run-client.bat"

echo.
echo ===============================================================
echo Both Backend Server and Desktop Client have been started!
echo Leave the Backend Server window running while using the app.
echo ===============================================================
