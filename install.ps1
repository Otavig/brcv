# brcv installer for Windows.
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\install.ps1
#   powershell -ExecutionPolicy Bypass -File .\install.ps1 -Uninstall

param([switch]$Uninstall)

$ErrorActionPreference = "Stop"

$AppDir   = Join-Path $env:LOCALAPPDATA "brcv"
$BinDir   = Join-Path $AppDir "bin"
$JarDest  = Join-Path $AppDir "brcv.jar"
$Launcher = Join-Path $BinDir "brcv.cmd"

$JarSrc = Join-Path $PSScriptRoot "brcv.jar"
if (-not (Test-Path $JarSrc -PathType Leaf)) {
    $JarSrc = Join-Path $PSScriptRoot "target\brcv.jar"
}

function Say($msg) {
    Write-Host "==> $msg" -ForegroundColor Green
}

function Warn($msg) {
    Write-Host "Warning: $msg" -ForegroundColor Yellow
}

function Remove-UserPathEntry {
    param([string]$PathToRemove)

    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $entries = @(
        $userPath -split ";" |
        Where-Object {
            $_ -and
            $_.TrimEnd("\") -ine $PathToRemove.TrimEnd("\")
        }
    )

    [Environment]::SetEnvironmentVariable(
        "Path",
        ($entries -join ";"),
        "User"
    )
}

if ($Uninstall) {
    Remove-Item -Recurse -Force $AppDir -ErrorAction SilentlyContinue
    Remove-UserPathEntry -PathToRemove $BinDir

    Say "brcv removed."
    exit 0
}

$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCommand) {
    throw "Java not found. Install JDK 21+ (e.g. 'choco install temurin21 -y') and try again."
}

$versionOutput = (& java -version 2>&1 | Out-String)

if ($versionOutput -notmatch 'version "(\d+)') {
    throw "Could not read the Java version."
}

$javaMajor = [int]$Matches[1]

if ($javaMajor -lt 21) {
    throw "Java $javaMajor found; brcv needs Java 21 or newer."
}

Say "Java $javaMajor found."

if (-not (Test-Path $JarSrc -PathType Leaf)) {
    $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue

    if (-not $mavenCommand) {
        throw "brcv.jar was not found. This distribution needs brcv.jar in its root or target\brcv.jar. Maven is required only to build from source."
    }

    Say "Building the JAR with Maven..."

    Push-Location $PSScriptRoot
    try {
        & mvn -q clean package

        if ($LASTEXITCODE -ne 0) {
            throw "mvn clean package failed."
        }
    }
    finally {
        Pop-Location
    }

    $JarSrc = Join-Path $PSScriptRoot "target\brcv.jar"
}

if (-not (Test-Path $JarSrc -PathType Leaf)) {
    throw "Could not find the compiled JAR: $JarSrc"
}

Say "Installing in $AppDir"

New-Item -ItemType Directory -Force -Path $BinDir | Out-Null
Copy-Item $JarSrc $JarDest -Force

$launcherContent = @"
@echo off
java -jar "$JarDest" %*
exit /b %ERRORLEVEL%
"@

Set-Content -Path $Launcher -Encoding ASCII -Value $launcherContent

$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
$pathEntries = @($userPath -split ";" | Where-Object { $_ })

$alreadyPresent = $false
foreach ($entry in $pathEntries) {
    if ($entry.TrimEnd("\") -ieq $BinDir.TrimEnd("\")) {
        $alreadyPresent = $true
        break
    }
}

if (-not $alreadyPresent) {
    $newPath = if ([string]::IsNullOrWhiteSpace($userPath)) {
        $BinDir
    }
    else {
        "$userPath;$BinDir"
    }

    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
    Warn "PATH updated. Open a new terminal to use brcv."
}
else {
    Say "Command directory is already in the user PATH."
}

Say "Installed successfully."
Say "Test with: brcv --version"
