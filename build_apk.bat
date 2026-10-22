@echo off
setlocal enabledelayedexpansion
title Video Browser - APK Builder

:: Ensure we are running in the project directory
cd /d "%~dp0"

echo =====================================================================
echo               Video Browser - Android APK Builder
echo =====================================================================
echo.

:: 1. Check for Java / JDK
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java is not detected in your system PATH.
    echo Please install Java JDK (version 17 or higher recommended) and try again.
    echo Download: https://adoptium.net/ or install Android Studio.
    echo.
    pause
    exit /b 1
)

:: 2. Display Java version
echo [*] Java detected:
java -version 2>&1 | findstr /i "version"
echo.

:: 3. Choose Gradle command
set GRADLE_CMD=
if exist "%~dp0gradlew.bat" (
    set GRADLE_CMD="%~dp0gradlew.bat"
) else (
    where gradle >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        set GRADLE_CMD=gradle
    )
)

if "%GRADLE_CMD%"=="" (
    echo [ERROR] Neither gradlew.bat nor a system gradle installation was found.
    echo Please ensure the project files are intact.
    echo.
    pause
    exit /b 1
)

:: 4. Build APK
echo [*] Starting APK compilation via Gradle (assembleDebug)...
echo [*] This may take a minute or two on first run...
echo.

call %GRADLE_CMD% assembleDebug

if %ERRORLEVEL% neq 0 (
    echo.
    echo =====================================================================
    echo [ERROR] APK compilation failed with error code %ERRORLEVEL%.
    echo Check the error logs above for missing SDK components or dependencies.
    echo =====================================================================
    echo.
    pause
    exit /b %ERRORLEVEL%
)

:: 5. Locate APK
set "APK_SOURCE=app\build\outputs\apk\debug\app-debug.apk"
set "APK_DEST=VideoBrowser-debug.apk"

echo.
echo =====================================================================
echo [SUCCESS] APK compiled successfully!
echo =====================================================================

if exist "%APK_SOURCE%" (
    :: Copy to root directory for convenient immediate access
    copy /Y "%APK_SOURCE%" "%APK_DEST%" >nul
    echo.
    echo [*] APK copied to root folder for easy access:
    echo     File: %~dp0%APK_DEST%
    echo.
    for %%F in ("%APK_DEST%") do (
        echo [*] Size: %%~zF bytes
    )
    echo.
    :: Highlight generated APK in Windows File Explorer
    if exist "%SystemRoot%\explorer.exe" (
        explorer.exe /select,"%~dp0%APK_DEST%"
    )
) else (
    echo [*] Warning: Expected APK at %APK_SOURCE% was not found.
    echo Check app\build\outputs\apk\ folder manually.
)

echo.
echo Press any key to close this window...
pause >nul
