# brcv installer for Windows.
# Usage: powershell -ExecutionPolicy Bypass -File .\install.ps1 [-Uninstall]
param([switch]$Uninstall)

$ErrorActionPreference = "Stop"

$AppDir  = Join-Path $env:LOCALAPPDATA "brcv"
$BinDir  = Join-Path $AppDir "bin"
$JarDest = Join-Path $AppDir "brcv.jar"
$Launcher = Join-Path $BinDir "brcv.cmd"
$JarSrc  = Join-Path $PSScriptRoot "target\brcv.jar"

function Say($msg)  { Write-Host "==> $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "Warning: $msg" -ForegroundColor Yellow }

if ($Uninstall) {
    Remove-Item -Recurse -Force $AppDir -ErrorAction SilentlyContinue
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $newPath = ($userPath -split ";" | Where-Object { $_ -and $_ -ne $BinDir }) -join ";"
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
    Say "brcv removed."
    exit 0
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java not found. Install JDK 21+ (e.g. 'choco install temurin21 -y') and try again."
}
$versionLine = (cmd /c "java -version 2>&1" | Select-Object -First 1)
if ($versionLine -notmatch 'version "(\d+)') { throw "Could not read the Java version." }
$javaMajor = [int]$Matches[1]
if ($javaMajor -lt 21) { throw "Java $javaMajor found; brcv needs Java 21 or newer." }
Say "Java $javaMajor found."

if (-not (Test-Path $JarSrc)) {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw "target\brcv.jar does not exist and Maven is not installed. Run .\build.ps1 first (it offers to install Maven with Chocolatey)."
    }
    Say "Building the JAR with Maven..."
    Push-Location $PSScriptRoot
    try { & mvn -q clean package; if ($LASTEXITCODE -ne 0) { throw "mvn package failed." } }
    finally { Pop-Location }
}

Say "Installing in $AppDir"
New-Item -ItemType Directory -Force -Path $BinDir | Out-Null
Copy-Item $JarSrc $JarDest -Force

Set-Content -Path $Launcher -Encoding ASCII -Value "@echo off`r`njava -jar `"$JarDest`" %*"

$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
if (($userPath -split ";") -notcontains $BinDir) {
    $newPath = if ($userPath) { "$userPath;$BinDir" } else { $BinDir }
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
    Warn "PATH updated. Open a new terminal to use brcv."
}

Say "Installed. Test with: brcv --version"
