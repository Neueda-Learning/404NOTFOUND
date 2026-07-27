$baseUrl = "http://localhost:8080/api/transactions"
$headers = @{ "Content-Type" = "application/json" }
$now = [System.DateTimeOffset]::UtcNow

function Post-Transaction($body) {
    try {
        $json = $body | ConvertTo-Json
        $resp = Invoke-RestMethod -Method POST -Uri $baseUrl -Headers $headers -Body $json
        Write-Host "  OK: $($resp.transactionId) -> hasAlert=$($resp.hasAlert)" -ForegroundColor Green
    } catch {
        $msg = $_.Exception.Message
        Write-Host "  ERR: $msg" -ForegroundColor Red
    }
}

Write-Host "`n=== Inserting Mock Transactions ===" -ForegroundColor Cyan

# ── Account ACC-001: Large amounts → triggers Amount Threshold (>$10,000)
Write-Host "`n[ACC-001] Large transaction alerts..."
Post-Transaction @{
    transactionId   = "TXN-2026-0001"
    accountId       = "ACC-001"
    payeeId         = "PAYEE-A01"
    payeeName       = "Global Trade Ltd"
    type            = "DEBIT"
    amount          = 15000.00
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddMinutes(-120)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "WIRE"
    country         = "US"
    description     = "International wire transfer"
}

Post-Transaction @{
    transactionId   = "TXN-2026-0002"
    accountId       = "ACC-001"
    payeeId         = "PAYEE-A02"
    payeeName       = "Offshore Holdings Inc"
    type            = "TRANSFER"
    amount          = 25000.00
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddMinutes(-90)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "ONLINE"
    country         = "KY"
    description     = "Capital transfer to subsidiary"
}

Post-Transaction @{
    transactionId   = "TXN-2026-0003"
    accountId       = "ACC-001"
    payeeId         = "PAYEE-A01"
    payeeName       = "Global Trade Ltd"
    type            = "DEBIT"
    amount          = 9500.00
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddMinutes(-60)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "WIRE"
    country         = "US"
    description     = "Partial payment"
}

# ── Account ACC-002: Velocity rule → 6 rapid transactions
Write-Host "`n[ACC-002] Velocity alert (6 txns in 10 min)..."
for ($i = 1; $i -le 6; $i++) {
    Post-Transaction @{
        transactionId   = "TXN-2026-010$i"
        accountId       = "ACC-002"
        payeeId         = "PAYEE-B0$i"
        payeeName       = "Vendor $i"
        type            = "DEBIT"
        amount          = [math]::Round(500 + ($i * 123.45), 2)
        currency        = "USD"
        status          = "COMPLETED"
        transactionTime = ($now.AddMinutes(-9 + $i)).ToString("yyyy-MM-ddTHH:mm:ssZ")
        paymentChannel  = "POS"
        country         = "US"
        description     = "POS purchase $i"
    }
}

# ── Account ACC-003: New payee alerts
Write-Host "`n[ACC-003] New payee alerts..."
Post-Transaction @{
    transactionId   = "TXN-2026-0201"
    accountId       = "ACC-003"
    payeeId         = "PAYEE-NEW-001"
    payeeName       = "Unknown Merchant Alpha"
    type            = "DEBIT"
    amount          = 2500.00
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddMinutes(-45)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "ONLINE"
    country         = "US"
    description     = "First-time purchase"
}

Post-Transaction @{
    transactionId   = "TXN-2026-0202"
    accountId       = "ACC-003"
    payeeId         = "PAYEE-NEW-002"
    payeeName       = "Crypto Exchange Beta"
    type            = "DEBIT"
    amount          = 5000.00
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddMinutes(-30)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "ONLINE"
    country         = "MT"
    description     = "Crypto purchase"
}

# ── Account ACC-004: Daily limit exceeded
Write-Host "`n[ACC-004] Daily limit breach (total >$50,000)..."
Post-Transaction @{
    transactionId   = "TXN-2026-0301"
    accountId       = "ACC-004"
    payeeId         = "PAYEE-C01"
    payeeName       = "Real Estate Holdings"
    type            = "DEBIT"
    amount          = 20000.00
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddHours(-5)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "WIRE"
    country         = "US"
    description     = "Property down-payment"
}

Post-Transaction @{
    transactionId   = "TXN-2026-0302"
    accountId       = "ACC-004"
    payeeId         = "PAYEE-C02"
    payeeName       = "Legal Services LLC"
    type            = "DEBIT"
    amount          = 18000.00
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddHours(-3)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "WIRE"
    country         = "US"
    description     = "Legal retainer payment"
}

Post-Transaction @{
    transactionId   = "TXN-2026-0303"
    accountId       = "ACC-004"
    payeeId         = "PAYEE-C03"
    payeeName       = "Investment Fund XYZ"
    type            = "DEBIT"
    amount          = 15000.00
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddHours(-1)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "WIRE"
    country         = "US"
    description     = "Fund subscription"
}

# ── Account ACC-005: Mixed normal transactions (no alerts)
Write-Host "`n[ACC-005] Normal transactions (no alerts expected)..."
Post-Transaction @{
    transactionId   = "TXN-2026-0401"
    accountId       = "ACC-005"
    payeeId         = "PAYEE-D01"
    payeeName       = "Supermarket Chain"
    type            = "DEBIT"
    amount          = 127.50
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddHours(-2)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "POS"
    country         = "US"
    description     = "Grocery shopping"
}

Post-Transaction @{
    transactionId   = "TXN-2026-0402"
    accountId       = "ACC-005"
    payeeId         = "PAYEE-D02"
    payeeName       = "Utility Company"
    type            = "DEBIT"
    amount          = 245.00
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddMinutes(-15)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "ONLINE"
    country         = "US"
    description     = "Monthly utility bill"
}

Post-Transaction @{
    transactionId   = "TXN-2026-0403"
    accountId       = "ACC-005"
    payeeId         = "PAYEE-D03"
    payeeName       = "Salary Inc"
    type            = "CREDIT"
    amount          = 5200.00
    currency        = "USD"
    status          = "COMPLETED"
    transactionTime = ($now.AddMinutes(-5)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "ACH"
    country         = "US"
    description     = "Monthly salary credit"
}

# ── PENDING transactions (not evaluated)
Write-Host "`n[Mixed] Pending transactions..."
Post-Transaction @{
    transactionId   = "TXN-2026-0501"
    accountId       = "ACC-006"
    payeeId         = "PAYEE-E01"
    payeeName       = "Online Shop"
    type            = "DEBIT"
    amount          = 890.00
    currency        = "USD"
    status          = "PENDING"
    transactionTime = ($now.AddMinutes(-3)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "ONLINE"
    country         = "US"
    description     = "Electronics purchase - pending"
}

Post-Transaction @{
    transactionId   = "TXN-2026-0502"
    accountId       = "ACC-006"
    payeeId         = "PAYEE-E02"
    payeeName       = "Travel Agency"
    type            = "DEBIT"
    amount          = 3200.00
    currency        = "USD"
    status          = "PENDING"
    transactionTime = ($now.AddMinutes(-1)).ToString("yyyy-MM-ddTHH:mm:ssZ")
    paymentChannel  = "ONLINE"
    country         = "US"
    description     = "Flight booking - pending auth"
}

# ── Summary
Write-Host "`n=== Summary ===" -ForegroundColor Cyan
$alerts = Invoke-RestMethod -Uri "http://localhost:8080/api/alerts?size=100" -Method GET
Write-Host "Total Alerts Generated: $($alerts.totalElements)" -ForegroundColor Yellow

$txns = Invoke-RestMethod -Uri "http://localhost:8080/api/transactions?size=100" -Method GET
Write-Host "Total Transactions:     $($txns.totalElements)" -ForegroundColor Yellow

Write-Host "`nDone! Open http://localhost:3001 to view the dashboard." -ForegroundColor Green
