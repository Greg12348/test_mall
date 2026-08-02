$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $projectRoot "compose.yaml"
$gatewayTest = Join-Path $PSScriptRoot "successful-order-gateway.ps1"

function Wait-ForHealthEndpoint {
    param(
        [Parameter(Mandatory)]
        [string]$Name,

        [Parameter(Mandatory)]
        [string]$Url,

        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        try {
            $health = Invoke-RestMethod `
                -Method Get `
                -Uri $Url `
                -TimeoutSec 3

            if ($health.status -eq "UP") {
                Write-Host "$Name is UP"
                return
            }
        } catch {
            # The container might still be starting.
        }

        Start-Sleep -Seconds 1
    }

    throw "$Name did not become healthy within $TimeoutSeconds seconds."
}

try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker CLI was not found. Install or start Docker Desktop first."
    }

    Write-Host "Validating Docker Compose configuration..."
    & docker compose -f $composeFile config --quiet

    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose configuration validation failed."
    }

    Write-Host "Checking required Docker containers..."
    $runningServices = @(
        & docker compose -f $composeFile ps --services --status running
    )

    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose could not inspect the running containers."
    }

    $requiredServices = @(
        "api-gateway",
        "order-service",
        "product-service",
        "kafka",
        "order-mysql",
        "product-mysql"
    )

    $missingServices = @(
        $requiredServices | Where-Object { $_ -notin $runningServices }
    )

    if ($missingServices.Count -gt 0) {
        throw "Required containers are not running: $($missingServices -join ', '). Start them with 'docker compose up -d'."
    }

    Write-Host "All required Docker containers are running."

    Write-Host "Waiting for application containers..."
    Wait-ForHealthEndpoint `
        -Name "Product Service" `
        -Url "http://localhost:8082/actuator/health"

    Wait-ForHealthEndpoint `
        -Name "Order Service" `
        -Url "http://localhost:8084/actuator/health"

    Wait-ForHealthEndpoint `
        -Name "API Gateway" `
        -Url "http://localhost:8080/actuator/health"

    Write-Host "Running the successful-order flow through API Gateway..."

    $powerShellExecutable = Join-Path $PSHOME "powershell.exe"

    if (-not (Test-Path $powerShellExecutable)) {
        $powerShellExecutable = (Get-Process -Id $PID).Path
    }

    & $powerShellExecutable `
        -NoProfile `
        -ExecutionPolicy Bypass `
        -File $gatewayTest

    if ($LASTEXITCODE -ne 0) {
        throw "The successful-order API Gateway test failed."
    }

    Write-Host "Successful-order Docker test passed." -ForegroundColor Green
    Write-Host "The existing containers were left running."
    exit 0
} catch {
    Write-Host "Successful-order Docker test failed." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "Inspect the containers with: docker compose ps"
    Write-Host "Inspect application logs with: docker compose logs api-gateway order-service product-service"
    exit 1
}
