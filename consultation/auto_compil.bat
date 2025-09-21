@echo off
REM =======================
REM Compilation du programme Python avec PyInstaller
REM =======================

py -m PyInstaller --onefile --add-data "Data/.env:Data" --noconsole --name "DRB Consultation" --distpath . Main.py

rmdir /s /q __pycache__
rmdir /s /q build
rmdir /s /q dist
del "DRB Connsultation.spec"