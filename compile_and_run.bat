@echo off
echo Compiling Java solution...
javac -cp "gson-2.10.1.jar" ShamirSecret.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Running Sample 1:
java -cp ".;gson-2.10.1.jar" ShamirSecret samples/sample1.json

echo.
echo Running Sample 2:
java -cp ".;gson-2.10.1.jar" ShamirSecret samples/sample2.json

pause
