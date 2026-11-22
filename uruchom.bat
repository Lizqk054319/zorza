@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Sprawdzam biblioteki...
python -m pip install --disable-pip-version-check -q -r requirements.txt
if errorlevel 1 (
  echo Nie udalo sie zainstalowac bibliotek. Sprawdz internet i czy Python jest w PATH.
  pause
  exit /b
)
python launcher.py
if errorlevel 1 pause
