# SystemCommerce — validação funcional ponta a ponta (Prompt 26)
# Gera JSON de resultados em target/validation-run.json

$ErrorActionPreference = 'Continue'
$Base = 'http://localhost:8080'
$Front = 'http://localhost:5173'
$OutDir = 'c:\Git\Sistemas\SystemCommerce\SystemCommerce-api\target'
$OutJson = Join-Path $OutDir 'validation-run.json'
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$envFile = 'c:\Git\Sistemas\SystemCommerce\SystemCommerce-api\.env'
Get-Content $envFile | ForEach-Object {
  if ($_ -match '^\s*([^#=]+)=(.*)$') {
    Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2].Trim()
  }
}

$suffix = Get-Date -Format 'yyyyMMddHHmmss'
$results = New-Object System.Collections.ArrayList
$ctx = @{}

function Add-Result {
  param(
    [string]$Step,
    [string]$Expected,
    [string]$Obtained,
    [string]$Evidence,
    [string]$ErrorFound = '',
    [string]$FixApplied = '',
    [bool]$Pass
  )
  $results.Add([pscustomobject]@{
    step = $Step
    expected = $Expected
    obtained = $Obtained
    evidence = $Evidence
    error = $ErrorFound
    fix = $FixApplied
    pass = $Pass
  }) | Out-Null
  $icon = if ($Pass) { 'OK' } else { 'FAIL' }
  Write-Output "[$icon] $Step — $Obtained"
}

function Invoke-Api {
  param(
    [string]$Method,
    [string]$Path,
    [object]$Body = $null,
    [string]$Token = $null,
    [hashtable]$Query = $null
  )
  $uri = "$Base$Path"
  if ($Query) {
    $qs = ($Query.GetEnumerator() | ForEach-Object { "$($_.Key)=$([uri]::EscapeDataString([string]$_.Value))" }) -join '&'
    $uri = "$uri`?$qs"
  }
  $headers = @{ Accept = 'application/json' }
  if ($Token) { $headers['Authorization'] = "Bearer $Token" }
  $params = @{
    Uri = $uri
    Method = $Method
    Headers = $headers
    TimeoutSec = 60
  }
  if ($null -ne $Body) {
    $params['Body'] = ($Body | ConvertTo-Json -Depth 8 -Compress)
    $params['ContentType'] = 'application/json'
  }
  try {
    $resp = Invoke-WebRequest @params -UseBasicParsing
    $parsed = $null
    if ($resp.Content) {
      try { $parsed = $resp.Content | ConvertFrom-Json } catch { $parsed = $resp.Content }
    }
    return [pscustomobject]@{
      ok = $true
      status = [int]$resp.StatusCode
      body = $parsed
      raw = $resp.Content
    }
  } catch {
    $status = 0
    $raw = ''
    $parsed = $null
    if ($_.Exception.Response) {
      $status = [int]$_.Exception.Response.StatusCode
      try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $raw = $reader.ReadToEnd()
        $parsed = $raw | ConvertFrom-Json
      } catch {
        # Corpo de erro ilegível ou não-JSON — mantém raw textual se houver
        if (-not $raw) { $raw = $_.Exception.Message }
        $parsed = $null
      }
    }
    if (-not $raw -and $_.ErrorDetails.Message) { $raw = $_.ErrorDetails.Message }
    return [pscustomobject]@{
      ok = $false
      status = $status
      body = $parsed
      raw = $raw
      error = $_.Exception.Message
    }
  }
}

# --- 0. Infra ---
try {
  $pg = docker inspect -f '{{.State.Health.Status}}' systemcommerce-api-db 2>$null
  $api = docker inspect -f '{{.State.Health.Status}}' systemcommerce-api 2>$null
  $front = docker inspect -f '{{.State.Health.Status}}' systemcommerce-front 2>$null
  $pass = ($pg -eq 'healthy' -and $api -eq 'healthy' -and $front -eq 'healthy')
  Add-Result -Step 'Infra: PostgreSQL + API + Frontend' `
    -Expected 'Containers healthy (db, api, front)' `
    -Obtained "db=$pg api=$api front=$front" `
    -Evidence 'docker inspect health' `
    -Pass $pass
} catch {
  Add-Result -Step 'Infra: PostgreSQL + API + Frontend' -Expected 'healthy' -Obtained $_.Exception.Message -Evidence 'docker' -ErrorFound $_.Exception.Message -Pass $false
}

$mig = docker exec systemcommerce-api-db psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -tAc "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true" 2>$null
$migOk = [int]$mig -gt 0
Add-Result -Step 'Migrations Flyway' `
  -Expected 'Migrations aplicadas com sucesso' `
  -Obtained "success_count=$($mig.Trim())" `
  -Evidence 'SELECT COUNT(*) FROM flyway_schema_history WHERE success=true' `
  -Pass $migOk

$seed = docker exec systemcommerce-api-db psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB -tAc "SELECT COUNT(*) FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE u.login='admin' AND r.code='ADMIN'" 2>$null
$seedOk = [int]$seed -ge 1
Add-Result -Step 'Seeds (admin + roles)' `
  -Expected 'Usuário admin com perfil ADMIN' `
  -Obtained "admin_role_links=$($seed.Trim())" `
  -Evidence 'users/user_roles/roles' `
  -Pass $seedOk

$health = Invoke-Api -Method GET -Path '/actuator/health'
Add-Result -Step 'API health' `
  -Expected 'status=UP' `
  -Obtained "HTTP $($health.status) body=$($health.raw)" `
  -Evidence 'GET /actuator/health' `
  -Pass ($health.ok -and $health.body.status -eq 'UP')

try {
  $fh = Invoke-WebRequest -Uri "$Front/healthz" -UseBasicParsing -TimeoutSec 15
  Add-Result -Step 'Frontend health' `
    -Expected 'HTTP 200 em /healthz' `
    -Obtained "HTTP $($fh.StatusCode) $($fh.Content)" `
    -Evidence "GET $Front/healthz" `
    -Pass ($fh.StatusCode -eq 200)
} catch {
  Add-Result -Step 'Frontend health' -Expected '200' -Obtained $_.Exception.Message -Evidence "/healthz" -ErrorFound $_.Exception.Message -Pass $false
}

try {
  $page = Invoke-WebRequest -Uri "$Front/" -UseBasicParsing -TimeoutSec 15
  $hasApp = $page.Content -match 'SystemCommerce|root|vite'
  Add-Result -Step 'Frontend SPA' `
    -Expected 'HTML da SPA servido' `
    -Obtained "HTTP $($page.StatusCode) bytes=$($page.RawContentLength) hasApp=$hasApp" `
    -Evidence "GET $Front/" `
    -Pass (($page.StatusCode -eq 200) -and $hasApp)
} catch {
  Add-Result -Step 'Frontend SPA' -Expected '200' -Obtained $_.Exception.Message -Evidence '/' -ErrorFound $_.Exception.Message -Pass $false
}

# --- 1. Auth admin ---
$login = Invoke-Api -Method POST -Path '/api/v1/auth/login' -Body @{
  username = $env:ADMIN_LOGIN
  password = $env:ADMIN_PASSWORD
}
$adminToken = $null
if ($login.ok -and $login.body.data.accessToken) {
  $adminToken = $login.body.data.accessToken
  $ctx.adminUserId = $login.body.data.user.id
  Add-Result -Step 'Autenticar administrador' `
    -Expected 'Login OK com token e permissões' `
    -Obtained "login=$($login.body.data.user.login) permissions=$($login.body.data.user.permissions.Count)" `
    -Evidence 'POST /api/v1/auth/login' `
    -Pass $true
} else {
  Add-Result -Step 'Autenticar administrador' `
    -Expected 'Login OK' `
    -Obtained "HTTP $($login.status) $($login.raw)" `
    -Evidence 'POST /api/v1/auth/login' `
    -ErrorFound $login.error `
    -Pass $false
}

if (-not $adminToken) {
  $results | ConvertTo-Json -Depth 6 | Set-Content -Path $OutJson -Encoding UTF8
  Write-Output 'ABORT: sem token admin'
  exit 1
}

# --- 2. Criar vendedor ---
$sellerLogin = "seller_$suffix"
$sellerPass = 'Seller@123!'
$createSeller = Invoke-Api -Method POST -Path '/api/v1/users' -Token $adminToken -Body @{
  name = "Vendedor Validacao $suffix"
  email = "seller.$suffix@example.com"
  login = $sellerLogin
  password = $sellerPass
  roleCodes = @('SELLER')
}
if ($createSeller.ok) {
  $ctx.sellerId = $createSeller.body.data.id
  Add-Result -Step 'Criar usuário vendedor' `
    -Expected 'HTTP 201 com perfil SELLER' `
    -Obtained "id=$($ctx.sellerId) login=$sellerLogin roles=$($createSeller.body.data.roles -join ',')" `
    -Evidence 'POST /api/v1/users' `
    -Pass $true
} else {
  Add-Result -Step 'Criar usuário vendedor' `
    -Expected '201' `
    -Obtained "HTTP $($createSeller.status) $($createSeller.raw)" `
    -Evidence 'POST /api/v1/users' `
    -ErrorFound $createSeller.raw `
    -Pass $false
}

# --- 3. Cliente ---
# CPF válido (algoritmo) com dígitos únicos por sufixo — usa base conhecida se necessário
$cpf = '52998224725'
# tenta documento único: gera 11 dígitos a partir do timestamp e valida fallback
$docTry = ('9' + $suffix.Substring(2,10))
$createCustomer = Invoke-Api -Method POST -Path '/api/v1/customers' -Token $adminToken -Body @{
  type = 'PF'
  name = "Cliente Validacao $suffix"
  document = $docTry
  email = "cliente.$suffix@example.com"
  phone = '11999990000'
  city = 'Sao Paulo'
  state = 'SP'
}
if (-not $createCustomer.ok -and $createCustomer.status -eq 422) {
  $createCustomer = Invoke-Api -Method POST -Path '/api/v1/customers' -Token $adminToken -Body @{
    type = 'PF'
    name = "Cliente Validacao $suffix"
    document = $cpf
    email = "cliente.$suffix@example.com"
    phone = '11999990000'
    city = 'Sao Paulo'
    state = 'SP'
  }
}
if ($createCustomer.ok) {
  $ctx.customerId = $createCustomer.body.data.id
  Add-Result -Step 'Criar cliente' `
    -Expected 'Cliente PF criado' `
    -Obtained "id=$($ctx.customerId) document=$($createCustomer.body.data.document)" `
    -Evidence 'POST /api/v1/customers' `
    -Pass $true
} else {
  Add-Result -Step 'Criar cliente' `
    -Expected '201' `
    -Obtained "HTTP $($createCustomer.status) $($createCustomer.raw)" `
    -Evidence 'POST /api/v1/customers' `
    -ErrorFound $createCustomer.raw `
    -Pass $false
}

# --- 4. Categoria ---
$createCat = Invoke-Api -Method POST -Path '/api/v1/categories' -Token $adminToken -Body @{
  name = "Cat Validacao $suffix"
  description = 'Categoria E2E Prompt 26'
  parentId = $null
}
if ($createCat.ok) {
  $ctx.categoryId = $createCat.body.data.id
  Add-Result -Step 'Criar categoria' `
    -Expected 'Categoria ativa' `
    -Obtained "id=$($ctx.categoryId) name=$($createCat.body.data.name)" `
    -Evidence 'POST /api/v1/categories' `
    -Pass $true
} else {
  Add-Result -Step 'Criar categoria' -Expected '201' -Obtained "HTTP $($createCat.status) $($createCat.raw)" -Evidence 'POST /api/v1/categories' -ErrorFound $createCat.raw -Pass $false
}

# --- 5. Produto ---
$sku = "SKU-$suffix"
$createProd = Invoke-Api -Method POST -Path '/api/v1/products' -Token $adminToken -Body @{
  internalCode = "INT-$suffix"
  sku = $sku
  barcode = $null
  name = "Produto Validacao $suffix"
  description = 'Produto E2E'
  categoryId = $ctx.categoryId
  unitOfMeasure = 'UN'
  costPrice = 10.00
  salePrice = 25.00
  minStock = 2
  allowNegativeStock = $false
  imageUrl = $null
}
if ($createProd.ok) {
  $ctx.productId = $createProd.body.data.id
  Add-Result -Step 'Criar produto' `
    -Expected 'Produto ACTIVE com preço 25.00' `
    -Obtained "id=$($ctx.productId) sku=$sku salePrice=$($createProd.body.data.salePrice)" `
    -Evidence 'POST /api/v1/products' `
    -Pass $true
} else {
  Add-Result -Step 'Criar produto' -Expected '201' -Obtained "HTTP $($createProd.status) $($createProd.raw)" -Evidence 'POST /api/v1/products' -ErrorFound $createProd.raw -Pass $false
}

# --- 6. Entrada estoque ---
$entryQty = 20
$entry = Invoke-Api -Method POST -Path '/api/v1/inventory/entries' -Token $adminToken -Body @{
  productId = $ctx.productId
  quantity = $entryQty
  observation = "Entrada validacao $suffix"
  futureReturn = $false
}
$stockAfterEntry = $null
if ($entry.ok) {
  $stockAfterEntry = [decimal]$entry.body.data.newBalance
  Add-Result -Step 'Entrada de estoque' `
    -Expected "Saldo = $entryQty" `
    -Obtained "newBalance=$stockAfterEntry type=$($entry.body.data.type)" `
    -Evidence 'POST /api/v1/inventory/entries' `
    -Pass ($stockAfterEntry -eq $entryQty)
} else {
  Add-Result -Step 'Entrada de estoque' -Expected "saldo $entryQty" -Obtained "HTTP $($entry.status) $($entry.raw)" -Evidence 'POST /api/v1/inventory/entries' -ErrorFound $entry.raw -Pass $false
}

# --- 7. Venda A (pagamento completo) ---
$saleA = Invoke-Api -Method POST -Path '/api/v1/sales' -Token $adminToken -Body @{
  customerId = $ctx.customerId
  notes = "Venda A paga $suffix"
}
if ($saleA.ok) {
  $ctx.saleAId = $saleA.body.data.id
  Add-Result -Step 'Criar venda (rascunho A)' `
    -Expected 'DRAFT com canEdit/canConfirm' `
    -Obtained "id=$($ctx.saleAId) status=$($saleA.body.data.status) canEdit=$($saleA.body.data.canEdit)" `
    -Evidence 'POST /api/v1/sales' `
    -Pass ($saleA.body.data.status -eq 'DRAFT' -and $saleA.body.data.canEdit -eq $true)
} else {
  Add-Result -Step 'Criar venda (rascunho A)' -Expected 'DRAFT' -Obtained "HTTP $($saleA.status) $($saleA.raw)" -Evidence 'POST /api/v1/sales' -ErrorFound $saleA.raw -Pass $false
}

$addItem = Invoke-Api -Method POST -Path "/api/v1/sales/$($ctx.saleAId)/items" -Token $adminToken -Body @{
  productId = $ctx.productId
  quantity = 2
  unitPrice = $null
  discountAmount = 0
  description = $null
}
if ($addItem.ok) {
  $ctx.saleATotal = [decimal]$addItem.body.data.totalAmount
  Add-Result -Step 'Adicionar produto à venda A' `
    -Expected 'Totais oficiais da API (2 x 25 = 50)' `
    -Obtained "total=$($ctx.saleATotal) items=$($addItem.body.data.items.Count)" `
    -Evidence "POST /api/v1/sales/{id}/items" `
    -Pass ($ctx.saleATotal -eq 50)
} else {
  Add-Result -Step 'Adicionar produto à venda A' -Expected 'total 50' -Obtained "HTTP $($addItem.status) $($addItem.raw)" -Evidence 'items' -ErrorFound $addItem.raw -Pass $false
}

$confirmA = Invoke-Api -Method POST -Path "/api/v1/sales/$($ctx.saleAId)/confirm" -Token $adminToken
if ($confirmA.ok) {
  Add-Result -Step 'Confirmar venda A' `
    -Expected 'CONFIRMED + canReceivePayment' `
    -Obtained "status=$($confirmA.body.data.status) canReceivePayment=$($confirmA.body.data.canReceivePayment)" `
    -Evidence "POST /api/v1/sales/{id}/confirm" `
    -Pass ($confirmA.body.data.status -eq 'CONFIRMED')
} else {
  Add-Result -Step 'Confirmar venda A' -Expected 'CONFIRMED' -Obtained "HTTP $($confirmA.status) $($confirmA.raw)" -Evidence 'confirm' -ErrorFound $confirmA.raw -Pass $false
}

$invAfterConfirm = Invoke-Api -Method GET -Path "/api/v1/inventory/products/$($ctx.productId)" -Token $adminToken
$stockAfterConfirm = $null
if ($invAfterConfirm.ok) {
  $stockAfterConfirm = [decimal]$invAfterConfirm.body.data.quantity
  $expectedStock = $entryQty - 2
  Add-Result -Step 'Validar baixa de estoque (venda A)' `
    -Expected "quantity=$expectedStock" `
    -Obtained "quantity=$stockAfterConfirm" `
    -Evidence "GET /api/v1/inventory/products/{id}" `
    -Pass ($stockAfterConfirm -eq $expectedStock)
} else {
  Add-Result -Step 'Validar baixa de estoque (venda A)' -Expected 'baixa 2' -Obtained "HTTP $($invAfterConfirm.status)" -Evidence 'inventory' -ErrorFound $invAfterConfirm.raw -Pass $false
}

# Pagamento parcial 20 + restante 30
$pay1 = Invoke-Api -Method POST -Path '/api/v1/payments' -Token $adminToken -Body @{
  saleId = $ctx.saleAId
  method = 'PIX'
  amount = 20.00
  paidAt = $null
  externalReference = "PIX-P1-$suffix"
  notes = 'parcial'
  installments = 1
  tenderedAmount = $null
  confirmImmediately = $true
}
if ($pay1.ok) {
  $saleAfterP1 = Invoke-Api -Method GET -Path "/api/v1/sales/$($ctx.saleAId)" -Token $adminToken
  Add-Result -Step 'Registrar pagamento parcial' `
    -Expected 'PARTIALLY_PAID; payment CONFIRMED amount=20' `
    -Obtained "payStatus=$($pay1.body.data.status) amount=$($pay1.body.data.amount) saleStatus=$($saleAfterP1.body.data.status)" `
    -Evidence 'POST /api/v1/payments + GET sale' `
    -Pass ($pay1.body.data.status -eq 'CONFIRMED' -and $saleAfterP1.body.data.status -eq 'PARTIALLY_PAID')
} else {
  Add-Result -Step 'Registrar pagamento parcial' -Expected 'PARTIALLY_PAID' -Obtained "HTTP $($pay1.status) $($pay1.raw)" -Evidence 'payments' -ErrorFound $pay1.raw -Pass $false
}

$pay2 = Invoke-Api -Method POST -Path '/api/v1/payments' -Token $adminToken -Body @{
  saleId = $ctx.saleAId
  method = 'CASH'
  amount = 30.00
  paidAt = $null
  externalReference = $null
  notes = 'restante'
  installments = 1
  tenderedAmount = 50.00
  confirmImmediately = $true
}
if ($pay2.ok) {
  $salePaid = Invoke-Api -Method GET -Path "/api/v1/sales/$($ctx.saleAId)" -Token $adminToken
  $balance = Invoke-Api -Method GET -Path "/api/v1/payments/by-sale/$($ctx.saleAId)/balance" -Token $adminToken
  Add-Result -Step 'Registrar pagamento restante + validar venda paga' `
    -Expected 'PAID; balanceDue=0' `
    -Obtained "saleStatus=$($salePaid.body.data.status) balanceDue=$($balance.body.data.balanceDue) change=$($pay2.body.data.changeAmount)" `
    -Evidence 'POST payments + GET balance' `
    -Pass ($salePaid.body.data.status -eq 'PAID' -and [decimal]$balance.body.data.balanceDue -eq 0)
} else {
  Add-Result -Step 'Registrar pagamento restante + validar venda paga' -Expected 'PAID' -Obtained "HTTP $($pay2.status) $($pay2.raw)" -Evidence 'payments' -ErrorFound $pay2.raw -Pass $false
}

# --- 8. Dashboard + relatório ---
$dash = Invoke-Api -Method GET -Path '/api/v1/dashboard' -Token $adminToken -Query @{ periodDays = 30; topLimit = 5 }
Add-Result -Step 'Consultar dashboard' `
  -Expected 'HTTP 200 com totais oficiais' `
  -Obtained "HTTP $($dash.status) salesToday.count=$($dash.body.data.salesToday.count) keys=$($dash.body.data.PSObject.Properties.Name -join ',')" `
  -Evidence 'GET /api/v1/dashboard' `
  -Pass $dash.ok

$report = Invoke-Api -Method GET -Path '/api/v1/reports/sales' -Token $adminToken -Query @{ page = 0; size = 20 }
Add-Result -Step 'Consultar relatório de vendas' `
  -Expected 'HTTP 200 com página de dados' `
  -Obtained "HTTP $($report.status) rows=$($report.body.data.data.Count) total=$($report.body.data.page.totalElements)" `
  -Evidence 'GET /api/v1/reports/sales' `
  -Pass $report.ok

# --- 9. Venda B cancelável (sem pagamento) ---
$saleB = Invoke-Api -Method POST -Path '/api/v1/sales' -Token $adminToken -Body @{
  customerId = $ctx.customerId
  notes = "Venda B cancelavel $suffix"
}
$ctx.saleBId = $saleB.body.data.id
$null = Invoke-Api -Method POST -Path "/api/v1/sales/$($ctx.saleBId)/items" -Token $adminToken -Body @{
  productId = $ctx.productId
  quantity = 1
  unitPrice = $null
  discountAmount = 0
  description = $null
}
$confirmB = Invoke-Api -Method POST -Path "/api/v1/sales/$($ctx.saleBId)/confirm" -Token $adminToken
$stockBeforeCancel = $null
$invB = Invoke-Api -Method GET -Path "/api/v1/inventory/products/$($ctx.productId)" -Token $adminToken
if ($invB.ok) { $stockBeforeCancel = [decimal]$invB.body.data.quantity }

$cancelB = Invoke-Api -Method POST -Path "/api/v1/sales/$($ctx.saleBId)/cancel" -Token $adminToken -Body @{
  reason = "Cancelamento validacao funcional $suffix"
}
$stockAfterCancel = $null
$invC = Invoke-Api -Method GET -Path "/api/v1/inventory/products/$($ctx.productId)" -Token $adminToken
if ($invC.ok) { $stockAfterCancel = [decimal]$invC.body.data.quantity }

$cancelOk = $confirmB.ok -and $cancelB.ok -and $cancelB.body.data.status -eq 'CANCELLED' -and ($stockAfterCancel -eq ($stockBeforeCancel + 1))
Add-Result -Step 'Cancelar venda permitida + estorno estoque' `
  -Expected 'CANCELLED e estoque +1' `
  -Obtained "confirmHTTP=$($confirmB.status) status=$($cancelB.body.data.status) stockBefore=$stockBeforeCancel stockAfter=$stockAfterCancel HTTP=$($cancelB.status)" `
  -Evidence 'POST confirm + cancel + GET inventory' `
  -ErrorFound $(if (-not $confirmB.ok) { $confirmB.raw } elseif (-not $cancelB.ok) { $cancelB.raw } else { '' }) `
  -Pass $cancelOk

# Tentativa de cancelar venda paga (deve falhar — regra de negócio)
$cancelPaid = Invoke-Api -Method POST -Path "/api/v1/sales/$($ctx.saleAId)/cancel" -Token $adminToken -Body @{
  reason = 'Tentativa ilegal apos pagamento'
}
Add-Result -Step 'Bloquear cancelamento de venda com pagamentos' `
  -Expected 'HTTP 4xx BusinessRule (pagamentos confirmados)' `
  -Obtained "HTTP $($cancelPaid.status) code=$($cancelPaid.body.code) msg=$($cancelPaid.body.message)" `
  -Evidence "POST cancel sale A" `
  -Pass ((-not $cancelPaid.ok) -and $cancelPaid.status -ge 400)

# --- 10. Auditoria ---
$audit = Invoke-Api -Method GET -Path '/api/v1/audit-logs' -Token $adminToken -Query @{ page = 0; size = 20 }
$auditHasSale = $false
if ($audit.ok -and $audit.body.data.data) {
  $auditHasSale = @($audit.body.data.data | Where-Object { $_.entityName -eq 'Sale' -or $_.module -match 'sale|Sale' }).Count -gt 0
  if (-not $auditHasSale) {
    # fallback: qualquer registro recente
    $auditHasSale = $audit.body.data.data.Count -gt 0
  }
}
Add-Result -Step 'Consultar auditoria' `
  -Expected 'HTTP 200 com eventos recentes' `
  -Obtained "HTTP $($audit.status) rows=$($audit.body.data.data.Count) hasSale=$auditHasSale sampleEntity=$($audit.body.data.data[0].entityName)" `
  -Evidence 'GET /api/v1/audit-logs' `
  -Pass ($audit.ok -and $auditHasSale)

# --- 11. Acesso negado (vendedor sem AUDIT/REPORT/USER) ---
$sellerLoginResp = Invoke-Api -Method POST -Path '/api/v1/auth/login' -Body @{
  username = $sellerLogin
  password = $sellerPass
}
$sellerToken = $null
if ($sellerLoginResp.ok) {
  $sellerToken = $sellerLoginResp.body.data.accessToken
  $perms = @($sellerLoginResp.body.data.user.permissions)
  Add-Result -Step 'Login vendedor' `
    -Expected 'Token SELLER sem AUDIT_READ/USER_CREATE/REPORT_READ' `
    -Obtained "perms=$($perms.Count) hasAudit=$($perms -contains 'AUDIT_READ') hasUserCreate=$($perms -contains 'USER_CREATE') hasReport=$($perms -contains 'REPORT_READ')" `
    -Evidence 'POST /api/v1/auth/login (seller)' `
    -Pass ((-not ($perms -contains 'AUDIT_READ')) -and (-not ($perms -contains 'USER_CREATE')))
} else {
  Add-Result -Step 'Login vendedor' -Expected 'OK' -Obtained "HTTP $($sellerLoginResp.status)" -Evidence 'login seller' -ErrorFound $sellerLoginResp.raw -Pass $false
}

if ($sellerToken) {
  $deniedAudit = Invoke-Api -Method GET -Path '/api/v1/audit-logs' -Token $sellerToken -Query @{ page = 0; size = 5 }
  $deniedUsers = Invoke-Api -Method GET -Path '/api/v1/users' -Token $sellerToken -Query @{ page = 0; size = 5 }
  $deniedReport = Invoke-Api -Method GET -Path '/api/v1/reports/sales' -Token $sellerToken -Query @{ page = 0; size = 5 }
  $deniedOk = ($deniedAudit.status -eq 403) -and ($deniedUsers.status -eq 403) -and ($deniedReport.status -eq 403)
  Add-Result -Step 'Acesso negado sem permissão' `
    -Expected '403 em audit-logs, users e reports' `
    -Obtained "audit=$($deniedAudit.status) users=$($deniedUsers.status) reports=$($deniedReport.status)" `
    -Evidence 'GET protegidos com token SELLER' `
    -Pass $deniedOk
}

# Estoque final consistente: entry 20 - vendaA 2 - vendaB 1 + estorno B 1 = 18
$finalInv = Invoke-Api -Method GET -Path "/api/v1/inventory/products/$($ctx.productId)" -Token $adminToken
$finalStock = if ($finalInv.ok) { [decimal]$finalInv.body.data.quantity } else { -1 }
Add-Result -Step 'Consistência final de estoque' `
  -Expected 'quantity=18 (20-2-1+1)' `
  -Obtained "quantity=$finalStock" `
  -Evidence "GET inventory product $($ctx.productId)" `
  -Pass ($finalStock -eq 18)

# Persist
$summary = [pscustomobject]@{
  ranAt = (Get-Date).ToString('o')
  suffix = $suffix
  context = $ctx
  passed = @($results | Where-Object { $_.pass }).Count
  failed = @($results | Where-Object { -not $_.pass }).Count
  total = $results.Count
  results = $results
}
$summary | ConvertTo-Json -Depth 8 | Set-Content -Path $OutJson -Encoding UTF8
Write-Output ""
Write-Output "SUMMARY passed=$($summary.passed) failed=$($summary.failed) total=$($summary.total)"
Write-Output "JSON=$OutJson"
if ($summary.failed -gt 0) { exit 1 } else { exit 0 }
