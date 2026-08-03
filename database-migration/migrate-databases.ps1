[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory = $false)]
    [string]$ConfigPath = "$PSScriptRoot/migration-config.json",

    [Parameter(Mandatory = $false)]
    [string]$BackupDirectory = "$PSScriptRoot/backups",

    [switch]$KeepDumps
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Invoke-NativeCommand {
    param(
        [Parameter(Mandatory)]
        [string]$Command,

        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $Command"
    }
}

function Assert-SafeName {
    param(
        [Parameter(Mandatory)]
        [string]$Value,

        [Parameter(Mandatory)]
        [string]$Label
    )

    if ($Value -notmatch '^[A-Za-z0-9_-]+$') {
        throw "$Label contains unsupported characters: $Value"
    }
}

if (-not (Test-Path -LiteralPath $ConfigPath -PathType Leaf)) {
    throw "Configuration file not found: $ConfigPath"
}

foreach ($requiredCommand in @('mysqldump', 'mysql', 'kubectl')) {
    if (-not (Get-Command $requiredCommand -ErrorAction SilentlyContinue)) {
        throw "Required command is not installed or not in PATH: $requiredCommand"
    }
}

$config = Get-Content -LiteralPath $ConfigPath -Raw | ConvertFrom-Json
if (-not $config.databases -or $config.databases.Count -eq 0) {
    throw 'The configuration must contain at least one database mapping.'
}

New-Item -ItemType Directory -Path $BackupDirectory -Force | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$originalMySqlPassword = [Environment]::GetEnvironmentVariable('MYSQL_PWD', 'Process')

try {
    foreach ($mapping in $config.databases) {
        Assert-SafeName -Value $mapping.source.database -Label 'Source database name'
        Assert-SafeName -Value $mapping.target.database -Label 'Target database name'
        Assert-SafeName -Value $mapping.target.namespace -Label 'Kubernetes namespace'
        Assert-SafeName -Value $mapping.target.pod -Label 'Kubernetes pod name'

        $sourcePassword = [Environment]::GetEnvironmentVariable(
            $mapping.source.passwordEnvironmentVariable,
            'Process'
        )
        if ([string]::IsNullOrWhiteSpace($sourcePassword)) {
            throw "Set environment variable $($mapping.source.passwordEnvironmentVariable) before running."
        }

        $dumpName = "$($mapping.source.database)-$timestamp.sql"
        $dumpPath = Join-Path $BackupDirectory $dumpName
        $remoteDump = "/tmp/$dumpName"
        $description = "$($mapping.source.database) -> $($mapping.target.namespace)/$($mapping.target.pod):$($mapping.target.database)"

        if (-not $PSCmdlet.ShouldProcess($description, 'Export and import MySQL database')) {
            continue
        }

        Write-Host "Exporting $($mapping.source.database)..."
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $sourcePassword, 'Process')
        Invoke-NativeCommand -Command 'mysqldump' -Arguments @(
            '--host', [string]$mapping.source.host,
            '--port', [string]$mapping.source.port,
            '--user', [string]$mapping.source.user,
            '--single-transaction',
            '--quick',
            '--routines',
            '--triggers',
            '--events',
            '--no-tablespaces',
            '--set-gtid-purged=OFF',
            '--databases', [string]$mapping.source.database,
            "--result-file=$dumpPath"
        )
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $null, 'Process')

        Write-Host "Copying dump to $($mapping.target.pod)..."
        Invoke-NativeCommand -Command 'kubectl' -Arguments @(
            'cp',
            $dumpPath,
            "$($mapping.target.namespace)/$($mapping.target.pod):$remoteDump"
        )

        Write-Host "Importing into $($mapping.target.database)..."
        $importCommand = 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < ' + $remoteDump
        Invoke-NativeCommand -Command 'kubectl' -Arguments @(
            'exec', '-n', [string]$mapping.target.namespace,
            [string]$mapping.target.pod,
            '--', 'sh', '-c', $importCommand
        )

        Write-Host 'Checking target tables...'
        $verifySql = "SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema='$($mapping.target.database)' ORDER BY table_name;"
        $verifyCommand = 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --batch --database=' + $mapping.target.database + ' --execute=' + "'$verifySql'"
        Invoke-NativeCommand -Command 'kubectl' -Arguments @(
            'exec', '-n', [string]$mapping.target.namespace,
            [string]$mapping.target.pod,
            '--', 'sh', '-c', $verifyCommand
        )

        Invoke-NativeCommand -Command 'kubectl' -Arguments @(
            'exec', '-n', [string]$mapping.target.namespace,
            [string]$mapping.target.pod,
            '--', 'rm', '-f', $remoteDump
        )

        if (-not $KeepDumps) {
            Remove-Item -LiteralPath $dumpPath -Force
        }

        Write-Host "Completed: $description"
    }
}
finally {
    [Environment]::SetEnvironmentVariable('MYSQL_PWD', $originalMySqlPassword, 'Process')
}

Write-Host 'All configured database migrations completed.'
