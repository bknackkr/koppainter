param(
    [string]$Memory = "",
    [string]$WorldPainterDir = "C:\Program Files\WorldPainter"
)

$vmoptionsFile = Join-Path $WorldPainterDir "worldpainter.vmoptions"
if (-not $Memory -and (Test-Path $vmoptionsFile)) {
    $xmxLine = Get-Content $vmoptionsFile | Where-Object { $_ -match '^-Xmx' } | Select-Object -First 1
    if ($xmxLine) {
        $Memory = $xmxLine -replace '^-Xmx', ''
    }
}
if (-not $Memory) {
    $Memory = "16G"
}

$javaExe = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    "$env:JAVA_HOME\bin\java.exe"
} elseif (Test-Path "C:\Users\bknackkr\programs\graalvm-jdk-21.0.11+9.1\bin\java.exe") {
    "C:\Users\bknackkr\programs\graalvm-jdk-21.0.11+9.1\bin\java.exe"
} elseif (Test-Path "C:\Users\bknackkr\AppData\Local\Programs\IntelliJ IDEA\jbr\bin\java.exe") {
    "C:\Users\bknackkr\AppData\Local\Programs\IntelliJ IDEA\jbr\bin\java.exe"
} else {
    "java"
}

$pluginJar = Join-Path $PSScriptRoot "target\koppainter-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path $pluginJar)) {
    Write-Host "Building Köppainter JAR..." -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot "mvnw.cmd") package -DskipTests
}

$wpDynmap = Join-Path $WorldPainterDir "lib\WPDynmapPreviewer.jar"
$wpLib = Join-Path $WorldPainterDir "lib\*"
$i4j = Join-Path $WorldPainterDir ".install4j\i4jruntime.jar"
$cp = "$wpDynmap;$wpLib;$i4j;$pluginJar"

Write-Host "Launching WorldPainter with Köppainter on classpath (-Xmx$Memory)..." -ForegroundColor Green
& $javaExe `
    "-Xmx$Memory" `
    --add-exports java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED `
    --add-exports java.desktop/javax.swing.plaf.synth=ALL-UNNAMED `
    --add-exports java.desktop/sun.awt.shell=ALL-UNNAMED `
    --add-exports java.desktop/sun.swing=ALL-UNNAMED `
    --add-exports java.desktop/sun.awt=ALL-UNNAMED `
    --add-exports java.desktop/sun.awt.windows=ALL-UNNAMED `
    --add-exports java.desktop/sun.awt.image=ALL-UNNAMED `
    -cp $cp `
    org.pepsoft.worldpainter.Main @args
