# build.ps1 - builds brcv with Maven and runs it. Usage: .\build.ps1 [brcv arguments]
$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $MyInvocation.MyCommand.Path)

function Install-WithChoco($package, $name) {
    if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
        Write-Host "$name not found and Chocolatey is not installed." -ForegroundColor Red
        Write-Host "Install Chocolatey (https://chocolatey.org/install) or see INSTALL-WINDOWS.md for the manual download."
        exit 1
    }

    $answer = Read-Host "$name not found. Install it now with Chocolatey (choco install $package)? (y/n)"
    if ($answer -ne "y") {
        Write-Host "Cancelled. Install $name and run .\build.ps1 again."
        exit 1
    }

    choco install $package -y
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Install failed. Open PowerShell as Administrator and try again." -ForegroundColor Red
        exit 1
    }

    # reload PATH in this terminal
    $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Install-WithChoco "temurin21" "Java 21"
}

if (Test-Path ".\mvnw.cmd") {
    $Maven = ".\mvnw.cmd"
} else {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        Install-WithChoco "maven" "Maven"
    }
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        Write-Host "Maven was installed but is not in the PATH yet. Open a new terminal and run .\build.ps1 again." -ForegroundColor Yellow
        exit 1
    }
    $Maven = "mvn"
}

Write-Host "Building with $Maven..." -ForegroundColor Cyan
& $Maven -q clean package
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed." -ForegroundColor Red
    exit 1
}

java -jar target/brcv.jar $args
