@echo off
chcp 65001 >nul
title Зупинка CustomsDocGen
color 0C

echo ╔══════════════════════════════════════════════════════════════╗
echo ║         ЗУПИНКА ДОДАТКУ                                      ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

echo Зупинка процесів Java...

REM Спробувати зупинити javaw.exe (якщо запущено у фоні)
taskkill /F /IM javaw.exe >nul 2>&1

REM Спробувати зупинити java.exe (якщо запущено з консоллю)
taskkill /F /IM java.exe >nul 2>&1

if %ERRORLEVEL% EQU 0 (
    color 0A
    echo.
    echo ✓ Додаток успішно зупинено!
    echo.
) else (
    echo.
    echo ⚠️  Процес Java не знайдено (можливо вже зупинено)
    echo.
)

timeout /t 2 >nul

