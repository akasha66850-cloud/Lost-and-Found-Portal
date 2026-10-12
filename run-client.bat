@echo off
title Campus Lost & Found - JavaFX Desktop Client
echo ===============================================================
echo   Starting JavaFX Desktop Client...
echo   Target Backend: http://localhost:8088
echo ===============================================================
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
set "PATH=%JAVA_HOME%\bin;C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin;%PATH%"

cd /d "%~dp0\client"
call mvn javafx:run
pause
