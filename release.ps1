# Builds the Windows release: dist\brcv-<version>-windows.zip
# Contents: brcv-<version>-setup.exe (own runtime, adds brcv to PATH), brcv.jar, install.ps1, install.bat, LEIA-ME.md
# Usage: powershell -ExecutionPolicy Bypass -File .\release.ps1 [-Yes]
param([switch]$Yes)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$Version = ([regex]::Match((Get-Content pom.xml -Raw), '<version>(.*?)</version>')).Groups[1].Value
$Name = "brcv-$Version-windows"
$Dist = Join-Path $PSScriptRoot "dist"
$Pkg = Join-Path $Dist $Name

function Refresh-Path {
    $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
}

function Install-Tool($wingetId, $chocoPkg, $name) {
    $winget = Get-Command winget -ErrorAction SilentlyContinue
    $choco = Get-Command choco -ErrorAction SilentlyContinue
    if (-not $winget -and -not $choco) {
        throw "$name not found, and neither winget nor Chocolatey is available. Install $name manually (see INSTALL-WINDOWS.md)."
    }
    if (-not $Yes) {
        $answer = Read-Host "$name not found. Install it now? (y/n)"
        if ($answer -ne "y") { throw "Cancelled. Install $name and run .\release.ps1 again." }
    }
    if ($winget) {
        winget install --id $wingetId -e --silent --accept-package-agreements --accept-source-agreements
    } else {
        choco install $chocoPkg -y
    }
    if ($LASTEXITCODE -ne 0) { throw "Install of $name failed. Try again as Administrator." }
    Refresh-Path
}

function Install-Maven {
    $mvnVersion = "3.9.9"
    $tools = Join-Path $PSScriptRoot ".tools"
    $mvnHome = Join-Path $tools "apache-maven-$mvnVersion"
    if (-not (Test-Path "$mvnHome\bin\mvn.cmd")) {
        if (-not $Yes) {
            $answer = Read-Host "Maven not found. Download it into .tools (no install needed)? (y/n)"
            if ($answer -ne "y") { throw "Cancelled. Install Maven and run .\release.ps1 again." }
        }
        New-Item -ItemType Directory -Force -Path $tools | Out-Null
        $zip = Join-Path $tools "maven.zip"
        $ProgressPreference = "SilentlyContinue"
        Invoke-WebRequest "https://archive.apache.org/dist/maven/maven-3/$mvnVersion/binaries/apache-maven-$mvnVersion-bin.zip" -OutFile $zip
        Expand-Archive $zip -DestinationPath $tools -Force
        Remove-Item $zip
    }
    $env:Path = "$mvnHome\bin;$env:Path"
}

function Get-JavaOut($args2) {
    $old = $ErrorActionPreference; $ErrorActionPreference = "Continue"
    $out = (& java @args2 2>&1 | Out-String)
    $ErrorActionPreference = $old
    return $out
}

function Find-Jpackage {
    $c = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    $homes = @($env:JAVA_HOME)
    if ((Get-JavaOut @("-XshowSettings:properties", "-version")) -match 'java\.home = (.+)') { $homes += $Matches[1].Trim() }
    $homes += @(Get-ChildItem "$env:ProgramFiles\Eclipse Adoptium\jdk-21*", "$env:ProgramFiles\Java\jdk-21*", "$env:ProgramFiles\Microsoft\jdk-21*" -ErrorAction SilentlyContinue | ForEach-Object FullName)
    foreach ($h in $homes) {
        if ($h -and (Test-Path "$h\bin\jpackage.exe")) { return "$h\bin\jpackage.exe" }
    }
    return $null
}

function Find-Iscc {
    $c = Get-Command iscc -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    foreach ($p in @("${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe", "$env:ProgramFiles\Inno Setup 6\ISCC.exe", "$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe")) {
        if (Test-Path $p) { return $p }
    }
    return $null
}

Write-Host "==> Checking requirements..." -ForegroundColor Cyan
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Install-Tool "EclipseAdoptium.Temurin.21.JDK" "temurin21" "Java 21 JDK" }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { Install-Maven }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw "Maven could not be set up." }
$Jpackage = Find-Jpackage
if (-not $Jpackage) { Install-Tool "EclipseAdoptium.Temurin.21.JDK" "temurin21" "JDK 21 (with jpackage)"; $Jpackage = Find-Jpackage }
if (-not $Jpackage) { throw "jpackage not found. Install a full JDK 21+ (choco install temurin21 -y), open a new terminal and try again." }
$Iscc = Find-Iscc
if (-not $Iscc) { Install-Tool "JRSoftware.InnoSetup" "innosetup" "Inno Setup"; $Iscc = Find-Iscc }
if (-not $Iscc) { throw "Inno Setup (ISCC.exe) not found." }

$javaOut = Get-JavaOut @("-version")
if ($javaOut -notmatch 'version "(\d+)' -or [int]$Matches[1] -lt 21) { throw "Java 21 or newer is required." }

Write-Host "==> Building jar..." -ForegroundColor Cyan
& mvn -q clean package
if ($LASTEXITCODE -ne 0) { throw "mvn clean package failed." }
if (-not (Test-Path "target\brcv.jar")) { throw "target\brcv.jar not generated." }

Write-Host "==> Bundling runtime (jpackage)..." -ForegroundColor Cyan
if (Test-Path $Dist) { Remove-Item -Recurse -Force $Dist }
New-Item -ItemType Directory -Force -Path "$Dist\in", "$Dist\tmp", $Pkg | Out-Null
Copy-Item "target\brcv.jar" "$Dist\in\"
& $Jpackage --type app-image --name brcv --app-version $Version `
    --input "$Dist\in" --main-jar brcv.jar --main-class br.com.brcodevalidator.Main `
    --dest "$Dist\tmp" --win-console --add-modules java.base `
    --jlink-options "--strip-debug --no-man-pages --no-header-files"
if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }

Write-Host "==> Smoke test..." -ForegroundColor Cyan
& "$Dist\tmp\brcv\brcv.exe" --version
if ($LASTEXITCODE -ne 0) { throw "brcv.exe does not run." }

Write-Host "==> Creating setup .exe (Inno Setup)..." -ForegroundColor Cyan
$iss = @"
[Setup]
AppName=brcv
AppVersion=$Version
DefaultDirName={localappdata}\brcv
PrivilegesRequired=lowest
DisableProgramGroupPage=yes
ChangesEnvironment=yes
OutputDir=$Pkg
OutputBaseFilename=brcv-$Version-setup
Compression=lzma2
SolidCompression=yes

[Files]
Source: "$Dist\tmp\brcv\*"; DestDir: "{app}"; Flags: recursesubdirs ignoreversion

[Registry]
Root: HKCU; Subkey: "Environment"; ValueType: expandsz; ValueName: "Path"; ValueData: "{olddata};{app}"; Check: NeedsAddPath(ExpandConstant('{app}'))

[Code]
function NeedsAddPath(Param: string): Boolean;
var P: string;
begin
  if not RegQueryStringValue(HKCU, 'Environment', 'Path', P) then begin Result := True; exit; end;
  Result := Pos(';' + Uppercase(Param) + ';', ';' + Uppercase(P) + ';') = 0;
end;

procedure CurUninstallStepChanged(Step: TUninstallStep);
var P, A: string; I: Integer;
begin
  if Step = usPostUninstall then
    if RegQueryStringValue(HKCU, 'Environment', 'Path', P) then begin
      A := ';' + ExpandConstant('{app}');
      I := Pos(Uppercase(A), Uppercase(P));
      if I > 0 then begin Delete(P, I, Length(A)); RegWriteExpandStringValue(HKCU, 'Environment', 'Path', P); end;
    end;
end;
"@
Set-Content -Path "$Dist\brcv.iss" -Value $iss -Encoding ASCII
& $Iscc /Q "$Dist\brcv.iss"
if ($LASTEXITCODE -ne 0) { throw "Inno Setup failed." }
if (-not (Test-Path "$Pkg\brcv-$Version-setup.exe")) { throw "Setup .exe not generated." }

Write-Host "==> Packing..." -ForegroundColor Cyan
Copy-Item "target\brcv.jar", "install.ps1" $Pkg
Copy-Item "INSTALL-WINDOWS.md" "$Pkg\LEIA-ME.md"
Set-Content -Path "$Pkg\install.bat" -Encoding ASCII -Value "@echo off`r`npowershell -ExecutionPolicy Bypass -File `"%~dp0install.ps1`"`r`npause"
Compress-Archive -Path $Pkg -DestinationPath "$Dist\$Name.zip" -Force
Remove-Item -Recurse -Force "$Dist\in", "$Dist\tmp", "$Dist\brcv.iss"

Write-Host "==> Done: dist\$Name.zip" -ForegroundColor Green
