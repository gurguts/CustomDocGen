@echo off
chcp 65001 >nul
title Генератор Митних Документів
color 0B

echo ╔══════════════════════════════════════════════════════════════╗
echo ║         ГЕНЕРАТОР МИТНИХ ДОКУМЕНТІВ                          ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

cd /d "%~dp0"

REM Перевірка наявності Java
echo [1/3] Перевірка Java...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    color 0C
    echo.
    echo ❌ ПОМИЛКА: Java не встановлена!
    echo.
    echo Для роботи додатку потрібна Java 17 або вище.
    echo.
    echo Завантажте та встановіть Java:
    echo   https://adoptium.net/
    echo.
    echo Виберіть: Eclipse Temurin JRE 17 або вище
    echo.
    pause
    exit /b 1
)
echo    ✓ Java знайдена

REM Пошук JAR файлу
echo [2/3] Пошук JAR файлу...

set "JAR_FILE="
if exist "customs-doc-gen-0.0.1-SNAPSHOT.jar" (
    set "JAR_FILE=customs-doc-gen-0.0.1-SNAPSHOT.jar"
) else if exist "target\customs-doc-gen-0.0.1-SNAPSHOT.jar" (
    set "JAR_FILE=target\customs-doc-gen-0.0.1-SNAPSHOT.jar"
)

if "%JAR_FILE%"=="" (
    color 0C
    echo.
    echo ❌ ПОМИЛКА: JAR файл не знайдено!
    echo.
    echo Виконайте збірку проєкту:
    echo   mvnw clean package
    echo.
    echo Або скопіюйте customs-doc-gen-0.0.1-SNAPSHOT.jar в цю папку
    echo.
    pause
    exit /b 1
)
echo    ✓ JAR файл знайдено: %JAR_FILE%

REM Запуск додатку
echo [3/3] Запуск додатку...
echo.

REM Запускаємо додаток в окремій консолі
start "CustomsDocGen Server" cmd /k "java -jar "%JAR_FILE%""

REM Чекаємо запуску сервера та відкриваємо браузер
timeout /t 8 >nul
start http://localhost:8080

REM Закриваємо це вікно
exit

