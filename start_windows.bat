@echo off
cd /d "%~dp0"
py satanachia.py --gui
if errorlevel 1 python satanachia.py --gui
pause
