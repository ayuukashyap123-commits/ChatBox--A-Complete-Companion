@echo off
setlocal
title Chatbox - A Complete Companion

set "APP_EXE=%~dp0dist\Chatbox\Chatbox.exe"

if not exist "%APP_EXE%" goto NOT_FOUND

echo Starting Chatbox - A Complete Companion...
start "" "%APP_EXE%"
if %ERRORLEVEL% neq 0 goto LAUNCH_ERROR

endlocal
exit /b 0

:LAUNCH_ERROR
echo.
echo =======================================================================
echo   [ERROR] Failed to launch Chatbox.exe (Exit code: %ERRORLEVEL%).
echo   Please verify that all files in 'dist\Chatbox' are intact.
echo =======================================================================
echo.
pause
exit /b %ERRORLEVEL%

:NOT_FOUND
echo =======================================================================
echo   Chatbox - Native Windows Application Not Found
echo =======================================================================
echo.
echo   The packaged native executable was not found at:
echo   "%APP_EXE%"
echo.
echo   To build and package the native application:
echo     Step 1: Ensure JDK 21 and Maven 3.9+ are installed and on your PATH.
echo     Step 2: Run: mvn package -DskipTests
echo     Step 3: Run jpackage to generate the native runtime image:
echo.
echo       if exist target\package-input rmdir /s /q target\package-input
echo       mkdir target\package-input
echo       copy target\chatbox-companion-1.0.0.jar target\package-input\
echo       jpackage --type app-image --name Chatbox --input target/package-input --main-jar chatbox-companion-1.0.0.jar --main-class com.chatbox.app.Main --dest dist --vendor "Chatbox" --app-version 1.0.0 --description "Chatbox - A Complete Companion"
echo.
echo   Note: Silent fallback to 'java -jar' is disabled to prevent
echo   crashes on systems lacking external JavaFX runtimes.
echo =======================================================================
echo.
pause
exit /b 1
