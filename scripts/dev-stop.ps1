[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    docker compose stop
    if ($LASTEXITCODE -ne 0) { throw 'A parada local falhou.' }
}
finally {
    Pop-Location
}
