param(
    [Parameter(Mandatory = $true)][string]$BackupPath,
    [string]$ComposeProject = "perfumestore-phase12-acceptance",
    [string]$ComposeFile = "compose.phase12-acceptance.yaml",
    [string]$EnvFile = "phase12-acceptance.env.example",
    [string]$RestoreDatabase = "perfumestore_phase12_restore"
)

$ErrorActionPreference = "Stop"
$resolvedBackup = [System.IO.Path]::GetFullPath($BackupPath)
if (-not (Test-Path -LiteralPath $resolvedBackup -PathType Leaf)) { throw "Backup não encontrado: $resolvedBackup" }
if ($RestoreDatabase -notmatch '^perfumestore_phase12_restore_[a-z0-9_]+$') {
    throw "O banco de restauração deve ser novo e seguir o prefixo perfumestore_phase12_restore_."
}

docker compose -p $ComposeProject -f $ComposeFile --env-file $EnvFile up -d postgres | Out-Host
$containerId = (docker compose -p $ComposeProject -f $ComposeFile --env-file $EnvFile ps -q postgres).Trim()
if ([string]::IsNullOrWhiteSpace($containerId)) { throw "O PostgreSQL isolado não está disponível." }

$envValues = @{}
Get-Content $EnvFile | Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    $envValues[$parts[0]] = $parts[1]
}
$user = $envValues["POSTGRES_USER"]
docker exec $containerId createdb --username=$user --template=template0 $RestoreDatabase
if ($LASTEXITCODE -ne 0) { throw "Não foi possível criar o banco novo de restauração." }

$escapedBackup = $resolvedBackup.Replace('"', '""')
cmd.exe /d /c "docker exec -i $containerId pg_restore --no-owner --exit-on-error --username=$user --dbname=$RestoreDatabase < `"$escapedBackup`""
if ($LASTEXITCODE -ne 0) { throw "pg_restore falhou com código $LASTEXITCODE." }
Write-Output "Restauração concluída somente no banco novo $RestoreDatabase."
