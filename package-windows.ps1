# Gera um instalador .exe do brcv com o Java embutido.
# Requisitos (rodar no Windows): JDK 21+ e WiX Toolset 3.14 (para o tipo exe).
# Uso: powershell -ExecutionPolicy Bypass -File .\package-windows.ps1

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$Version = "1.0.0"

Write-Host "==> Compilando com Maven..."
mvn -q clean package
if ($LASTEXITCODE -ne 0) { throw "Falha no mvn package." }

Write-Host "==> Gerando o instalador .exe..."
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "dist\input" | Out-Null
Copy-Item "target\brcv.jar" "dist\input\"

jpackage `
  --type exe `
  --name brcv `
  --app-version $Version `
  --input "dist\input" `
  --main-jar brcv.jar `
  --main-class br.com.brcodevalidator.Main `
  --dest dist `
  --win-console `
  --win-menu `
  --win-shortcut `
  --win-dir-chooser

if ($LASTEXITCODE -ne 0) { throw "Falha no jpackage." }
Write-Host "Pronto: dist\brcv-$Version.exe"
