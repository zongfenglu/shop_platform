# Print (or write) local domain hosts entries.
#   powershell -ExecutionPolicy Bypass -File docker\scripts\setup-local-domains.ps1
#   powershell -ExecutionPolicy Bypass -File docker\scripts\setup-local-domains.ps1 -Apply   (admin)

param(
    [switch]$Apply
)

$ErrorActionPreference = "Stop"
$envFile = Join-Path $PSScriptRoot "..\.env"
$domain = "shop.test"
if (Test-Path $envFile) {
    $line = Get-Content $envFile | Where-Object { $_ -match '^\s*PLATFORM_BASE_DOMAIN\s*=' } | Select-Object -Last 1
    if ($line) {
        $domain = ($line -replace '^\s*PLATFORM_BASE_DOMAIN\s*=\s*', '').Trim().Trim('"').Trim("'")
    }
}

$ip = $null
$addrs = @(Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object {
        $_.IPAddress -notlike "127.*" -and
        $_.IPAddress -notlike "169.254.*" -and
        $_.IPAddress -notlike "172.*"
    } | Select-Object -ExpandProperty IPAddress)
$ip = $addrs | Where-Object { $_ -like "192.168.*" } | Select-Object -First 1
if (-not $ip) { $ip = $addrs | Where-Object { $_ -like "10.*" } | Select-Object -First 1 }
if (-not $ip) { $ip = $addrs | Select-Object -First 1 }
if (-not $ip) { $ip = "192.168.x.x" }

$hosts = @(
    "$ip  admin.$domain",
    "$ip  store.$domain",
    "$ip  demo.$domain",
    "$ip  www.$domain",
    "$ip  $domain"
)

Write-Host "PLATFORM_BASE_DOMAIN = $domain"
Write-Host "Add these lines to C:\Windows\System32\drivers\etc\hosts :"
Write-Host ""
$hosts | ForEach-Object { Write-Host $_ }
Write-Host ""
Write-Host "URLs:"
Write-Host "  admin  http://admin.$domain"
Write-Host "  store  http://store.$domain"
Write-Host "  H5     http://demo.$domain"
Write-Host ""

if (-not $Apply) {
    Write-Host "Re-run with -Apply (Administrator) to write hosts."
    Write-Host "Skip this if public DNS already points to this machine."
    exit 0
}

$hostsPath = "$env:SystemRoot\System32\drivers\etc\hosts"
$existing = Get-Content $hostsPath -ErrorAction Stop
$toAdd = @()
foreach ($row in $hosts) {
    $name = ($row -split '\s+', 2)[1]
    if ($existing | Where-Object { $_ -match [regex]::Escape($name) }) { continue }
    $toAdd += $row
}
if ($toAdd.Count -eq 0) {
    Write-Host "hosts already contains these names."
    exit 0
}
Add-Content -Path $hostsPath -Value ""
Add-Content -Path $hostsPath -Value "# shop-platform local domains"
Add-Content -Path $hostsPath -Value $toAdd
Write-Host ("Wrote {0} line(s) to hosts." -f $toAdd.Count)
