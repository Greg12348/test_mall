[CmdletBinding()]
param(
    [string]$Namespace = 'mall',
    [string]$ClusterName = 'mall-test',
    [string]$ExpectedImageTag = 'aws-test-1',
    [long]$MigratedProductId = 10001,
    [int]$LocalGatewayPort = 18080
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$gatewayUrl = "http://127.0.0.1:$LocalGatewayPort"
$portForwardProcess = $null
$previousGatewayUrl = $env:GATEWAY_URL

function Invoke-Kubectl {
    param([Parameter(Mandatory)][string[]]$KubectlArguments)

    $previousErrorPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & kubectl @KubectlArguments 2>&1
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorPreference
    }
    if ($exitCode -ne 0) {
        throw "kubectl failed: $($output -join [Environment]::NewLine)"
    }
    return $output
}

function Assert-Equal {
    param($Actual, $Expected, [string]$Message)

    if ([string]$Actual -ne [string]$Expected) {
        throw "$Message Expected '$Expected', received '$Actual'."
    }
}

try {
    if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
        throw 'kubectl is not installed or is not in PATH.'
    }

    Write-Host '[1/7] Checking EKS context...'
    $context = (Invoke-Kubectl -KubectlArguments @('config', 'current-context') | Out-String).Trim()
    if ($context -notlike "*$ClusterName*") {
        throw "Current kubectl context '$context' is not the expected EKS cluster '$ClusterName'."
    }

    Write-Host '[2/7] Checking application and stateful workload rollouts...'
    foreach ($deployment in @('product-service', 'order-service', 'api-gateway')) {
        Invoke-Kubectl -KubectlArguments @('rollout', 'status', "deployment/$deployment", '-n', $Namespace, '--timeout=180s') | Out-Host
    }
    foreach ($statefulSet in @('product-mysql', 'order-mysql', 'kafka')) {
        Invoke-Kubectl -KubectlArguments @('rollout', 'status', "statefulset/$statefulSet", '-n', $Namespace, '--timeout=180s') | Out-Host
    }

    Write-Host '[3/7] Checking persistent EBS claims...'
    $pvcDocument = (Invoke-Kubectl -KubectlArguments @('get', 'pvc', '-n', $Namespace, '-o', 'json') | Out-String) | ConvertFrom-Json
    $requiredClaims = @(
        'product-mysql-data-product-mysql-0',
        'order-mysql-data-order-mysql-0',
        'kafka-data-kafka-0'
    )
    foreach ($claimName in $requiredClaims) {
        $claim = $pvcDocument.items | Where-Object { $_.metadata.name -eq $claimName }
        if (-not $claim) {
            throw "Required PVC '$claimName' does not exist."
        }
        Assert-Equal $claim.status.phase 'Bound' "PVC '$claimName' is not bound."
        Assert-Equal $claim.spec.storageClassName 'gp3' "PVC '$claimName' uses the wrong StorageClass."
    }

    Write-Host '[4/7] Checking application images in ECR...'
    $deployments = (Invoke-Kubectl -KubectlArguments @('get', 'deployments', '-n', $Namespace, '-o', 'json') | Out-String) | ConvertFrom-Json
    foreach ($deployment in $deployments.items) {
        $image = [string]$deployment.spec.template.spec.containers[0].image
        if ($image -notmatch '\.dkr\.ecr\.us-east-1\.amazonaws\.com/.+:' + [regex]::Escape($ExpectedImageTag) + '$') {
            throw "Deployment '$($deployment.metadata.name)' is not using expected ECR tag '$ExpectedImageTag': $image"
        }
    }

    Write-Host '[5/7] Checking the migrated sample product in AWS MySQL...'
    $sql = "SELECT CONCAT(id,'|',name,'|',price,'|',stock) FROM products WHERE id=$MigratedProductId"
    $databaseOutput = Invoke-Kubectl -KubectlArguments @(
        'exec', '-n', $Namespace, 'product-mysql-0', '--',
        'mysql', '-umall', '-pmall', '-N', '-B', '-D', 'mall_product', '-e', $sql
    )
    $productRow = ($databaseOutput | Where-Object { $_ -match '^\d+\|' } | Select-Object -Last 1).Trim()
    Assert-Equal $productRow "$MigratedProductId|Migrated AWS Demo Product|149.99|25" 'Migrated product verification failed.'

    Write-Host '[6/7] Starting a temporary API Gateway port-forward...'
    $kubectlPath = (Get-Command kubectl).Source
    $stdoutPath = Join-Path $env:TEMP 'mall-eks-port-forward.out.log'
    $stderrPath = Join-Path $env:TEMP 'mall-eks-port-forward.err.log'
    $portForwardProcess = Start-Process -FilePath $kubectlPath `
        -ArgumentList @('port-forward', '-n', $Namespace, 'service/api-gateway', "${LocalGatewayPort}:8080") `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -PassThru

    $deadline = (Get-Date).AddSeconds(60)
    do {
        if ($portForwardProcess.HasExited) {
            $details = if (Test-Path $stderrPath) { Get-Content $stderrPath -Raw } else { '' }
            throw "Port-forward exited early. $details"
        }
        try {
            $health = Invoke-RestMethod -Method Get -Uri "$gatewayUrl/actuator/health" -TimeoutSec 3
        }
        catch {
            $health = $null
            Start-Sleep -Seconds 1
        }
    } while (($null -eq $health -or $health.status -ne 'UP') -and (Get-Date) -lt $deadline)

    if ($null -eq $health -or $health.status -ne 'UP') {
        throw 'API Gateway did not become healthy through the EKS port-forward.'
    }

    $apiProduct = Invoke-RestMethod -Method Get -Uri "$gatewayUrl/api/products/$MigratedProductId" -TimeoutSec 10
    Assert-Equal $apiProduct.data.name 'Migrated AWS Demo Product' 'Gateway did not return the migrated product.'

    Write-Host '[7/7] Running the successful-order Gateway system test...'
    $env:GATEWAY_URL = $gatewayUrl
    $testScript = Join-Path $PSScriptRoot '..\system-tests\successful-order-gateway.ps1'
    $powerShellPath = (Get-Process -Id $PID).Path
    & $powerShellPath -NoProfile -ExecutionPolicy Bypass -File $testScript
    if ($LASTEXITCODE -ne 0) {
        throw "Successful-order Gateway system test failed with exit code $LASTEXITCODE."
    }

    Write-Host 'AWS MIGRATION VALIDATION: SUCCESS' -ForegroundColor Green
    exit 0
}
catch {
    Write-Host 'AWS MIGRATION VALIDATION: FAILURE' -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
finally {
    $env:GATEWAY_URL = $previousGatewayUrl
    if ($null -ne $portForwardProcess -and -not $portForwardProcess.HasExited) {
        Stop-Process -Id $portForwardProcess.Id -Force -ErrorAction SilentlyContinue
        $portForwardProcess.WaitForExit()
    }
}
