[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    docker compose -p perfumestore-phase13-acceptance -f compose.phase13-acceptance.yaml --env-file phase13-acceptance.env.example stop
    if ($LASTEXITCODE -ne 0) { throw 'A parada da homologação falhou.' }
}
finally {
    Pop-Location
}
