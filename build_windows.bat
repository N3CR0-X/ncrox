@echo off
setlocal
cd /d "%~dp0"
where py >nul 2>nul
if errorlevel 1 (echo Instale Python 3 para Windows primeiro.& exit /b 1)
py -m venv .venv
call .venv\Scripts\activate.bat
python -m pip install --upgrade pip pyinstaller
pyinstaller --noconfirm --clean --onefile --windowed --name "Satanachia-ncrox" --icon icon.ico --add-data "icon.png;." --add-data "banners;banners" satanachia.py
if errorlevel 1 exit /b 1
echo EXE pronto em dist\Satanachia-ncrox.exe
endlocal
