@echo off
setlocal

set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not exist "%JAVA_EXE%" (
    if exist "C:\Users\bknackkr\programs\graalvm-jdk-21.0.11+9.1\bin\java.exe" (
        set "JAVA_EXE=C:\Users\bknackkr\programs\graalvm-jdk-21.0.11+9.1\bin\java.exe"
    )
)
if not exist "%JAVA_EXE%" (
    if exist "C:\Users\bknackkr\AppData\Local\Programs\IntelliJ IDEA\jbr\bin\java.exe" (
        set "JAVA_EXE=C:\Users\bknackkr\AppData\Local\Programs\IntelliJ IDEA\jbr\bin\java.exe"
    )
)
if not exist "%JAVA_EXE%" (
    set "JAVA_EXE=java"
)

set "WP_LIB=C:\Program Files\WorldPainter\lib"
set "WP_I4J=C:\Program Files\WorldPainter\.install4j\i4jruntime.jar"
set "PLUGIN_JAR=%~dp0target\koppainter-1.0.0-SNAPSHOT.jar"

set "MEMORY=%WP_MEMORY%"
if "%MEMORY%"=="" set "MEMORY=%~1"
if "%MEMORY%"=="" (
    for /f "tokens=*" %%A in ('findstr /b /c:"-Xmx" "C:\Program Files\WorldPainter\worldpainter.vmoptions" 2^>nul') do (
        set "LINE=%%A"
        call set "MEMORY=%%LINE:~4%%"
    )
)
if "%MEMORY%"=="" set "MEMORY=16G"

if not exist "%PLUGIN_JAR%" (
    echo Building Köppainter JAR...
    call "%~dp0mvnw.cmd" package -DskipTests
)

echo Starting WorldPainter with Köppainter plugin (-Xmx%MEMORY%)...
"%JAVA_EXE%" ^
    -Xmx%MEMORY% ^
    --add-exports java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED ^
    --add-exports java.desktop/javax.swing.plaf.synth=ALL-UNNAMED ^
    --add-exports java.desktop/sun.awt.shell=ALL-UNNAMED ^
    --add-exports java.desktop/sun.swing=ALL-UNNAMED ^
    --add-exports java.desktop/sun.awt=ALL-UNNAMED ^
    --add-exports java.desktop/sun.awt.windows=ALL-UNNAMED ^
    --add-exports java.desktop/sun.awt.image=ALL-UNNAMED ^
    -cp "%WP_LIB%\WPDynmapPreviewer.jar;%WP_LIB%\*;%WP_I4J%;%PLUGIN_JAR%" ^
    org.pepsoft.worldpainter.Main %*
