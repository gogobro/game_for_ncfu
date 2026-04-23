@echo off
chcp 65001 >nul
where java >nul 2>nul
if errorlevel 1 (
  echo Java не найдена. Установи JDK/JRE 17+ и попробуй снова.
  pause
  exit /b 1
)
cd /d "%~dp0dist"
java -jar horror-rooms.jar
