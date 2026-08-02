$ErrorActionPreference = "Stop"

$gatewayUrl = if ($env:GATEWAY_URL) {
    $env:GATEWAY_URL.TrimEnd("/")
} else {
    "http://localhost:8080"
}

function Wait-ForGateway {
    param(
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        try {
            $health = Invoke-RestMethod `
                -Method Get `
                -Uri "$gatewayUrl/actuator/health" `
                -TimeoutSec 3

            if ($health.status -eq "UP") {
                Write-Host "API Gateway is UP"
                return
            }
        } catch {
            # The gateway might still be starting.
        }

        Start-Sleep -Seconds 1
    }

    throw "API Gateway did not become healthy within $TimeoutSeconds seconds."
}

try {
    Write-Host "Waiting for API Gateway at $gatewayUrl..."
    Wait-ForGateway

    $initialStock = 10
    $orderQuantity = 2
    $productPrice = [decimal]25.00
    $expectedStock = $initialStock - $orderQuantity
    $expectedTotal = $productPrice * $orderQuantity

    Write-Host "Creating test product through API Gateway..."

    $productBody = @{
        name        = "Gateway Pipeline Product $([guid]::NewGuid())"
        description = "Successful API Gateway system test"
        price       = $productPrice
        stock       = $initialStock
    } | ConvertTo-Json

    $productResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$gatewayUrl/api/products" `
        -ContentType "application/json" `
        -Body $productBody

    if ($productResponse.status -ne 201) {
        throw "Expected product response status 201, received $($productResponse.status)."
    }

    $productId = $productResponse.data.id

    if (-not $productId) {
        throw "Product response did not contain an ID."
    }

    if ($productResponse.data.stock -ne $initialStock) {
        throw "Expected initial stock $initialStock, received $($productResponse.data.stock)."
    }

    Write-Host "Created product $productId with stock $initialStock."
    Write-Host "Creating order through API Gateway..."

    $orderBody = @{
        userId    = 1
        productId = $productId
        quantity  = $orderQuantity
    } | ConvertTo-Json

    $orderResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$gatewayUrl/api/orders" `
        -ContentType "application/json" `
        -Body $orderBody

    if ($orderResponse.status -ne 201) {
        throw "Expected order response status 201, received $($orderResponse.status)."
    }

    $orderId = $orderResponse.data.id

    if (-not $orderId) {
        throw "Order response did not contain an ID."
    }

    Write-Host "Created order $orderId. Waiting for Kafka processing..."

    $deadline = (Get-Date).AddSeconds(30)
    $finalOrder = $null

    while ((Get-Date) -lt $deadline) {
        $finalOrder = Invoke-RestMethod `
            -Method Get `
            -Uri "$gatewayUrl/api/orders/$orderId"

        if ($finalOrder.data.status -eq "STOCK_RESERVED") {
            break
        }

        if ($finalOrder.data.status -eq "REJECTED") {
            throw "Order $orderId was unexpectedly rejected."
        }

        Start-Sleep -Milliseconds 500
    }

    if ($null -eq $finalOrder) {
        throw "Order $orderId could not be retrieved."
    }

    if ($finalOrder.data.status -ne "STOCK_RESERVED") {
        throw "Expected STOCK_RESERVED, received $($finalOrder.data.status)."
    }

    if ([decimal]$finalOrder.data.totalAmount -ne [decimal]$expectedTotal) {
        throw "Expected total amount $expectedTotal, received $($finalOrder.data.totalAmount)."
    }

    Write-Host "Order reached STOCK_RESERVED."
    Write-Host "Checking remaining product stock through API Gateway..."

    $finalProduct = Invoke-RestMethod `
        -Method Get `
        -Uri "$gatewayUrl/api/products/$productId"

    if ($finalProduct.data.stock -ne $expectedStock) {
        throw "Expected stock $expectedStock, received $($finalProduct.data.stock)."
    }

    Write-Host "Stock changed from $initialStock to $expectedStock."
    Write-Host "Successful-order API Gateway test passed." -ForegroundColor Green
    exit 0
} catch {
    Write-Host "Successful-order API Gateway test failed." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
