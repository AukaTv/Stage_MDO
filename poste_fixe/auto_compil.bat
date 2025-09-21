@echo off
REM =======================
REM Compilation du programme Python avec PyInstaller
REM =======================

py -m PyInstaller --onefile --noconsole --name "Entree Reliquat" --distpath . --additional-hooks-dir=. Main.py

rmdir /s /q __pycache__
rmdir /s /q build
rmdir /s /q dist
del "Entree Reliquat.spec"