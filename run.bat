@echo off
setlocal
cd /d "%~dp0"

if not exist "bin" mkdir bin
javac -cp "lib\mysql-connector-j-9.4.0.jar" -d bin src\*.java
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

echo Starting HealthFirst PIMS...
java -cp "bin;lib\mysql-connector-j-9.4.0.jar" Main
