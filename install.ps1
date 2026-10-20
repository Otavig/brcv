# Instalador do brcv para Windows.
# Uso: powershell -ExecutionPolicy Bypass -File .\install.ps1 [-Uninstall]
param([switch]$Uninstall)

$ErrorActionPreference = "Stop"

$AppDir  = Join-Path $env:LOCALAPPDATA "brcv"
$BinDir  = Join-Path $AppDir "bin"
$JarDest = Join-Path $AppDir "brcv.jar"
$Launcher = Join-Path $BinDir "brcv.cmd"
$JarSrc  = Join-Path $PSScriptRoot "target\brcv.jar"

function Say($msg)  { Write-Host "==> $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "Aviso: $msg" -ForegroundColor Yellow }

if ($Uninstall) {
    Remove-Item -Recurse -Force $AppDir -ErrorAction SilentlyContinue
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $newPath = ($userPath -split ";" | Where-Object { $_ -and $_ -ne $BinDir }) -join ";"
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
    Say "brcv removido."
    exit 0
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java não encontrado. Instale o JDK 21+ e tente de novo."
}
$versionLine = (cmd /c "java -version 2>&1" | Select-Object -First 1)
if ($versionLine -notmatch 'version "(\d+)') { throw "Não consegui ler a versão do Java." }
$javaMajor = [int]$Matches[1]
if ($javaMajor -lt 21) { throw "Java $javaMajor detectado; o brcv precisa do Java 21 ou superior." }
Say "Java $javaMajor encontrado."

if (-not (Test-Path $JarSrc)) {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw "target\brcv.jar não existe e o Maven não está instalado. Rode 'mvn package' primeiro."
    }
    Say "Gerando o JAR com Maven..."
    Push-Location $PSScriptRoot
    try { & mvn -q clean package; if ($LASTEXITCODE -ne 0) { throw "Falha no mvn package." } }
    finally { Pop-Location }
}

Say "Instalando em $AppDir"
New-Item -ItemType Directory -Force -Path $BinDir | Out-Null
Copy-Item $JarSrc $JarDest -Force

Set-Content -Path $Launcher -Encoding ASCII -Value "@echo off`r`njava -jar `"$JarDest`" %*"

$userPath = [Environment]::GetEnvironmentVariable("Path", "User")
if (($userPath -split ";") -notcontains $BinDir) {
    $newPath = if ($userPath) { "$userPath;$BinDir" } else { $BinDir }
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
    Warn "PATH atualizado. Abra um novo terminal para usar o brcv."
}

Say "Instalado. Teste com: brcv --version"