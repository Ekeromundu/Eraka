@echo off
REM ============================================================
REM  Eraka: start the Firebase EMULATORS on this laptop
REM  - Data is loaded from and saved back to .\firebase_emulator_data
REM  - The phone must be on the SAME Wi-Fi, and the app set to
REM    "Emulator" with this laptop's IPv4 (long-press the logo on
REM    the sign-in screen, or erakaEmulatorHost in gradle.properties)
REM  - Emulator UI in your browser: http://localhost:4000
REM ============================================================
cd /d "%~dp0"
echo.
echo This laptop's IPv4 address(es) - type one of these into the phone:
ipconfig | findstr /R /C:"IPv4"
echo.
if exist "firebase_emulator_data" (
    firebase emulators:start --project otta-project-1ee57 --import=firebase_emulator_data --export-on-exit=firebase_emulator_data
) else (
    echo No firebase_emulator_data folder yet - starting EMPTY emulators.
    echo Copy your old export folder "firebase_data_new_user" here and rename it
    echo to "firebase_emulator_data" to get the starter pictures back.
    echo.
    firebase emulators:start --project otta-project-1ee57 --export-on-exit=firebase_emulator_data
)
pause
