param(
    [string]$ComposeProject = "perfumestore-phase12-acceptance",
    [string]$ComposeFile = "compose.phase12-acceptance.yaml",
    [string]$EnvFile = "phase12-acceptance.env.example",
    [string]$OutputPath = "phase12-backups/perfumestore-phase12-$(Get-Date -Format yyyyMMdd-HHmmss)-utc.dump"
)

$ErrorActionPreference = "Stop"
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
$outputDirectory = Split-Path -Parent $resolvedOutput
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$containerId = (docker compose -p $ComposeProject -f $ComposeFile --env-file $EnvFile ps -q postgres).Trim()
if ([string]::IsNullOrWhiteSpace($containerId)) { throw "O PostgreSQL isolado não está em execução." }

$envValues = @{}
Get-Content $EnvFile | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    $envValues[$parts[0]] = $parts[1]
}
$user = $envValues["POSTGRES_USER"]
$database = $envValues["POSTGRES_DB"]
if ([string]::IsNullOrWhiteSpace($user) -or [string]::IsNullOrWhiteSpace($database)) { throw "POSTGRES_USER/POSTGRES_DB não configurados." }

$escapedOutput = $resolvedOutput.Replace('"', '""')
cmd.exe /d /c "docker exec $containerId pg_dump -Fc --no-owner --username=$user --dbname=$database > `"$escapedOutput`""
if ($LASTEXITCODE -ne 0) { throw "pg_dump falhou com código $LASTEXITCODE." }
Write-Output "Backup lógico criado em $resolvedOutput"
