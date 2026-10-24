[CmdletBinding()]
param(
    [ValidateSet('dev', 'acceptance')]
    [string]$Environment = 'dev'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    if ($Environment -eq 'dev') {
        docker compose ps
        if ($LASTEXITCODE -ne 0) { throw 'Não foi possível consultar o Compose local.' }
        $healthUrl = 'http://localhost:18081/actuator/health/readiness'
    }
    else {
        $compose = @('-p', 'perfumestore-phase13-acceptance', '-f', 'compose.phase13-acceptance.yaml', '--env-file', 'phase13-acceptance.env.example')
        docker compose @compose ps
        if ($LASTEXITCODE -ne 0) { throw 'Não foi possível consultar o Compose de homologação.' }
        $healthUrl = 'https://localhost:19680/actuator/health/readiness'
    }

    $status = curl.exe -k -sS -o NUL -w '%{http_code}' $healthUrl
    if ($LASTEXITCODE -ne 0 -or $status -ne '200') {
        throw "Health check falhou: HTTP $status"
    }
    Write-Output "health=$status url=$healthUrl"
}
finally {
    Pop-Location
}
