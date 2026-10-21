# build.ps1
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

if (Test-Path ".\mvnw.cmd") {
    $MavenCmd = ".\mvnw.cmd"
} else {
    $MavenCmd = "mvn"
}

Write-Host "Compilando o projeto com $MavenCmd..." -ForegroundColor Cyan

& $MavenCmd -q clean package

java -jar target/brcv.jar $args
