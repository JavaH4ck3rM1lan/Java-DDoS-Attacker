@echo off

:build
javac WebPingApp.java

if %errorlevel% neq 0 (
    echo.Build failed!
    goto finish
) else (
    echo.Build succsessful. Starting CLASS file.
    java WebPingApp
    goto finish
)

:finish
echo.Process finished with exit code %errorlevel%.
pause
goto EoF

:EoF