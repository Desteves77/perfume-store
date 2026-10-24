[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $compose = @('-p', 'perfumestore-phase13-acceptance', '-f', 'compose.phase13-acceptance.yaml', '--env-file', 'phase13-acceptance.env.example')
    docker compose @compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'A configuração de homologação é inválida.' }
    docker compose @compose up -d --build --force-recreate
    if ($LASTEXITCODE -ne 0) { throw 'A inicialização da homologação falhou.' }
    docker compose @compose ps
}
finally {
    Pop-Location
}
