@echo off
title Campus Lost & Found - Backend Server (Port 8088)
echo ===============================================================
echo   Starting Spring Boot Backend Server on Port 8088...
echo   API Base: http://localhost:8088
echo ===============================================================
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
set "PATH=%JAVA_HOME%\bin;C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin;%PATH%"

cd /d "%~dp0\backend"
java -jar target\backend-1.0.0-SNAPSHOT.jar
pause
