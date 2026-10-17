@echo off
setlocal DisableDelayedExpansion
set "ktc_version=0.3.0"
if defined KTC_PLUGINS_BINARY goto run
if not defined KTC_PLUGINS_BINARY_CACHE set "KTC_PLUGINS_BINARY_CACHE=%LOCALAPPDATA%\ktc-plugins\binaries"
set "KTC_PLUGINS_BINARY=%KTC_PLUGINS_BINARY_CACHE%\%ktc_version%\windows-x64\ktc-plugins-%ktc_version%-windows-x64.exe"
set "KTC_PLUGINS_WRAPPER=%~f0"
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command "$t=[IO.File]::ReadAllText($env:KTC_PLUGINS_WRAPPER); & ([scriptblock]::Create($t.Substring($t.LastIndexOf('# POWERSHELL')+12)))"
if errorlevel 1 exit /b 1
:run
"%KTC_PLUGINS_BINARY%" %*
exit /b %errorlevel%
# POWERSHELL
$ErrorActionPreference = 'Stop'
$sha = '9716207715a544c84daa603f9e387a7862756d8358bc93f69b0efcad0e19647c' # SHA_WINDOWS_X64
try {
    $arch = if ($env:PROCESSOR_ARCHITEW6432) { $env:PROCESSOR_ARCHITEW6432 } else { $env:PROCESSOR_ARCHITECTURE }
    if ($arch -ne 'AMD64') { throw "Unsupported Windows architecture: $arch" }
    if ($sha -eq 'UNRELEASED') { throw 'This checkout has no release pins. Build locally and set KTC_PLUGINS_BINARY; release bundles contain pinned wrappers.' }
    $binary = $env:KTC_PLUGINS_BINARY
    $folder = [IO.Path]::GetDirectoryName($binary)
    [IO.Directory]::CreateDirectory($folder) | Out-Null
    if (Test-Path -LiteralPath $binary) {
        if ((Get-Item -LiteralPath $binary).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Refusing a symlink in the binary cache' }
    } else {
        $temp = Join-Path $folder ('.download-' + [guid]::NewGuid().ToString('N'))
        try {
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
            $artifact = [IO.Path]::GetFileName($binary)
            Invoke-WebRequest -UseBasicParsing -Uri "https://github.com/Heapy/ktc-plugins/releases/download/v$env:ktc_version/$artifact" -OutFile $temp -TimeoutSec 300
            if ((Get-FileHash -LiteralPath $temp -Algorithm SHA256).Hash.ToLowerInvariant() -ne $sha) { throw 'Downloaded executable checksum mismatch' }
            try { [IO.File]::Move($temp, $binary) }
            catch { if (!(Test-Path -LiteralPath $binary)) { throw } }
        } finally { if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp -Force } }
    }
    if ((Get-FileHash -LiteralPath $binary -Algorithm SHA256).Hash.ToLowerInvariant() -ne $sha) { throw "Cached executable checksum mismatch; remove $binary and retry" }
} catch { [Console]::Error.WriteLine('ktc-plugins: ' + $_.Exception.Message); exit 1 }
