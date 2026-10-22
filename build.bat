@echo off
setlocal
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven no esta instalado o no esta en PATH.
  echo Instala Maven 3.9+ y verifica que java -version muestre JDK 17 o superior.
  pause
  exit /b 1
)
mvn clean package
if errorlevel 1 (
  echo Error compilando UnboundProtection. Revisa el error anterior.
  pause
  exit /b 1
)
echo.
echo JAR generado en target\UnboundProtection-1.20.1.jar
pause
