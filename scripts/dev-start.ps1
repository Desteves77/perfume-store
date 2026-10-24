[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'A configuração local do Compose é inválida.' }
    docker compose up -d --build
    if ($LASTEXITCODE -ne 0) { throw 'A inicialização local falhou.' }
    docker compose ps
}
finally {
    Pop-Location
}
