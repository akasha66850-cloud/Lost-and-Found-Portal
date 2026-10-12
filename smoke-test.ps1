# ==============================================================================
# Campus Lost & Found System - PowerShell End-to-End Smoke Test Script (Windows)
# ==============================================================================

$BaseUrl = "http://localhost:8088"
Write-Host "=== 1. Starting Smoke Test against $BaseUrl ===" -ForegroundColor Cyan

$RandomId = Get-Random -Minimum 1000 -Maximum 9999
$StudentEmail = "student_$RandomId@campus.edu"
$StudentRoll = "ROLL$RandomId"

# 1. Register student
Write-Host "--> Registering student: $StudentEmail..." -ForegroundColor Yellow
$regBody = @{
    name = "Test Student $RandomId"
    email = $StudentEmail
    rollNumber = $StudentRoll
    password = "Password@123"
} | ConvertTo-Json

$regResp = Invoke-RestMethod -Uri "$BaseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json"
Write-Host "Registered: $($regResp.name) ($($regResp.email))" -ForegroundColor Green

# 2. Login as student
Write-Host "--> Logging in as student..." -ForegroundColor Yellow
$loginBody = @{
    identifier = $StudentEmail
    password = "Password@123"
} | ConvertTo-Json
$studentAuth = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$studentToken = $studentAuth.token
Write-Host "Student JWT Acquired: $($studentToken.Substring(0, 20))..." -ForegroundColor Green

# 3. Login as staff
Write-Host "--> Logging in as default seeded staff..." -ForegroundColor Yellow
$staffLoginBody = @{
    identifier = "staff@campus.edu"
    password = "Staff@123"
} | ConvertTo-Json
$staffAuth = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $staffLoginBody -ContentType "application/json"
$staffToken = $staffAuth.token
Write-Host "Staff JWT Acquired: $($staffToken.Substring(0, 20))..." -ForegroundColor Green

# 4. Student reports lost item
Write-Host "--> Student reports lost item..." -ForegroundColor Yellow
$lostBody = @{
    type = "LOST"
    category = "WALLET"
    title = "Brown Leather Tommy Hilfiger Wallet"
    description = "Lost near library coffee counter. Contains student ID card."
    color = "Brown"
    brand = "Tommy Hilfiger"
    location = "Library"
    eventDate = (Get-Date).ToString("yyyy-MM-dd")
} | ConvertTo-Json
$lostHeaders = @{ Authorization = "Bearer $studentToken" }
$lostItem = Invoke-RestMethod -Uri "$BaseUrl/api/items" -Method Post -Body $lostBody -ContentType "application/json" -Headers $lostHeaders
Write-Host "Lost Item Created: #$($lostItem.id) - $($lostItem.title)" -ForegroundColor Green

# 5. Staff logs found item
Write-Host "--> Staff logs matching found item..." -ForegroundColor Yellow
$foundBody = @{
    type = "FOUND"
    category = "WALLET"
    title = "Tommy Hilfiger Leather Wallet Brown"
    description = "Found near library counter by janitor. Safely stored."
    color = "Brown"
    brand = "Tommy Hilfiger"
    location = "Library"
    eventDate = (Get-Date).ToString("yyyy-MM-dd")
    storedAt = "Main Gate Security Locker 4B"
    hiddenDetail = "Contains driving license and two vintage stamps"
} | ConvertTo-Json
$staffHeaders = @{ Authorization = "Bearer $staffToken" }
$foundItem = Invoke-RestMethod -Uri "$BaseUrl/api/items" -Method Post -Body $foundBody -ContentType "application/json" -Headers $staffHeaders
Write-Host "Found Item Created: #$($foundItem.id) - $($foundItem.title)" -ForegroundColor Green

# 6. Check matches
Write-Host "--> Fetching smart matches for student..." -ForegroundColor Yellow
$matches = Invoke-RestMethod -Uri "$BaseUrl/api/matches" -Method Get -Headers $lostHeaders
Write-Host "Found $($matches.Count) match candidate(s). Top score: $($matches[0].score)%" -ForegroundColor Green

# 7. Student submits claim
Write-Host "--> Student submits claim for found item #$($foundItem.id)..." -ForegroundColor Yellow
$claimBody = @{
    itemId = $foundItem.id
    lostItemId = $lostItem.id
    answer = "Contains driving license and two vintage stamps"
} | ConvertTo-Json
$claim = Invoke-RestMethod -Uri "$BaseUrl/api/claims" -Method Post -Body $claimBody -ContentType "application/json" -Headers $lostHeaders
Write-Host "Claim Created: #$($claim.id) (Status: $($claim.status))" -ForegroundColor Green

# 8. Staff reviews and approves claim
Write-Host "--> Staff reviews and approves claim #$($claim.id)..." -ForegroundColor Yellow
$reviewBody = @{
    decision = "APPROVE"
    note = "Secret stamps verified with claimant."
} | ConvertTo-Json
$reviewResult = Invoke-RestMethod -Uri "$BaseUrl/api/claims/$($claim.id)/review" -Method Post -Body $reviewBody -ContentType "application/json" -Headers $staffHeaders
Write-Host "Claim Reviewed: Status is now $($reviewResult.status)" -ForegroundColor Green

# 9. Verify notifications
Write-Host "--> Checking student notifications..." -ForegroundColor Yellow
$notifs = Invoke-RestMethod -Uri "$BaseUrl/api/notifications" -Method Get -Headers $lostHeaders
Write-Host "Notifications received by student: $($notifs.content.Count)" -ForegroundColor Green
foreach ($n in $notifs.content) {
    Write-Host "  [$($n.type)] $($n.message)" -ForegroundColor DarkGray
}

Write-Host "`n=== All Smoke Tests Passed Successfully! ===" -ForegroundColor Cyan
