@echo off
REM ============================================================
REM  Eraka: upload the security rules to the LIVE Firebase project
REM  (database.rules.json; Storage is not used on the free plan). Run once after any
REM  change to those files. First time only: "firebase login".
REM ============================================================
cd /d "%~dp0"
firebase deploy --only database --project otta-project-1ee57
pause
