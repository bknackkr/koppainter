[CmdletBinding()]
param(
    [string]$WorldPainterDir = "",
    [string]$JideVersion = "",
    [switch]$Force,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = "Stop"

# 1. Locate WorldPainter directory
if (-not $WorldPainterDir) {
    if ($env:WORLDPAINTER_HOME -and (Test-Path $env:WORLDPAINTER_HOME)) {
        $WorldPainterDir = $env:WORLDPAINTER_HOME
    } elseif ($env:WP_HOME -and (Test-Path $env:WP_HOME)) {
        $WorldPainterDir = $env:WP_HOME
    } elseif ($env:WORLDPAINTER_DIR -and (Test-Path $env:WORLDPAINTER_DIR)) {
        $WorldPainterDir = $env:WORLDPAINTER_DIR
    } else {
        $candidates = @(
            "C:\Program Files\WorldPainter",
            "C:\Program Files (x86)\WorldPainter",
            "$env:LOCALAPPDATA\Programs\WorldPainter"
        )
        foreach ($candidate in $candidates) {
            if (Test-Path $candidate) {
                $WorldPainterDir = $candidate
                break
            }
        }
    }
}

if (-not $WorldPainterDir -or -not (Test-Path $WorldPainterDir)) {
    Write-Error "WorldPainter directory not found. Please specify -WorldPainterDir or set WORLDPAINTER_HOME."
    exit 1
}

# Locate lib directory
$wpLibDir = Join-Path $WorldPainterDir "lib"
if (-not (Test-Path $wpLibDir)) {
    $wpLibDir = $WorldPainterDir
}

$jideCommonJar = Join-Path $wpLibDir "jide-common.jar"
$jideDockJar = Join-Path $wpLibDir "jide-dock.jar"

if (-not (Test-Path $jideCommonJar) -or -not (Test-Path $jideDockJar)) {
    Write-Error "Could not find jide-common.jar and/or jide-dock.jar in '$wpLibDir'."
    exit 1
}

# 2. Detect JIDE version from JAR manifest if not specified
if (-not $JideVersion) {
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
        $zip = [System.IO.Compression.ZipFile]::OpenRead($jideCommonJar)
        $manifestEntry = $zip.GetEntry("META-INF/MANIFEST.MF")
        if ($manifestEntry) {
            $stream = $manifestEntry.Open()
            $reader = New-Object System.IO.StreamReader($stream)
            $manifestText = $reader.ReadToEnd()
            $reader.Close()
            $stream.Close()
            if ($manifestText -match '(?m)^Jide-Version:\s*([^\r\n]+)') {
                $JideVersion = $matches[1].Trim()
            }
        }
        $zip.Dispose()
    } catch {
        # Fallback if zip reading fails
    }
}

if (-not $JideVersion) {
    $JideVersion = "3.8.1"
}

Write-Host "Target JIDE version: $JideVersion" -ForegroundColor Cyan

# 3. Locate Maven Wrapper
$mvnw = Join-Path $PSScriptRoot "mvnw.cmd"
if (-not (Test-Path $mvnw)) {
    $mvnw = "mvn"
}

# 4. Configure JAVA_HOME if not already set
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $javaCandidates = @(
        "C:\Users\bknackkr\programs\graalvm-jdk-21.0.11+9.1",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Microsoft\jdk-21*",
        "$env:LOCALAPPDATA\Programs\IntelliJ IDEA\jbr"
    )
    foreach ($cand in $javaCandidates) {
        $resolved = Resolve-Path $cand -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Path -First 1
        if ($resolved -and (Test-Path "$resolved\bin\java.exe")) {
            $env:JAVA_HOME = $resolved
            Write-Host "Configured JAVA_HOME=$env:JAVA_HOME" -ForegroundColor DarkGray
            break
        }
    }
}

# 5. Check if JIDE jars are already installed in ~/.m2/repository
$m2Repo = if ($env:M2_REPO) { $env:M2_REPO } else { Join-Path $HOME ".m2\repository" }
$installedCommon = Join-Path $m2Repo "com\jidesoft\jide-common\$JideVersion\jide-common-$JideVersion.jar"
$installedDock = Join-Path $m2Repo "com\jidesoft\jide-dock\$JideVersion\jide-dock-$JideVersion.jar"

$needsInstall = $Force -or -not (Test-Path $installedCommon) -or -not (Test-Path $installedDock)

if ($needsInstall) {
    Write-Host "Installing JIDE $JideVersion jars into local Maven repository..." -ForegroundColor Yellow
    & $mvnw install:install-file "-Dfile=$jideCommonJar" "-DgroupId=com.jidesoft" "-DartifactId=jide-common" "-Dversion=$JideVersion" "-Dpackaging=jar"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to install jide-common.jar"
        exit $LASTEXITCODE
    }
    & $mvnw install:install-file "-Dfile=$jideDockJar" "-DgroupId=com.jidesoft" "-DartifactId=jide-dock" "-Dversion=$JideVersion" "-Dpackaging=jar"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to install jide-dock.jar"
        exit $LASTEXITCODE
    }
    Write-Host "JIDE $JideVersion installed successfully." -ForegroundColor Green
} else {
    Write-Host "JIDE $JideVersion is already installed in local Maven repository. Skipping installation." -ForegroundColor DarkGray
}

# 6. Execute mvnw test -P testWithWorldPainter
Write-Host "Running Maven test with WorldPainter profile..." -ForegroundColor Green
$cmdArgs = @("test", "-P", "testWithWorldPainter")
if ($MavenArgs) {
    $cmdArgs += $MavenArgs
}

& $mvnw @cmdArgs
exit $LASTEXITCODE
