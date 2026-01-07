$baseUrl = "http://localhost:8080/fhir"
$pass = "password"

function Get-Creds {
    param($user)
    return [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${user}:$pass"))
}

function Test-Endpoint {
    param($path, $user, $expectedCode, $method = "GET", $body = $null)
    $url = "$baseUrl$path"
    $creds = Get-Creds $user
    $headers = @{
        "Authorization" = "Basic $creds"
        "Accept"        = "application/fhir+json"
    }
    if ($body) {
        $headers["Content-Type"] = "application/fhir+json"
    }
    
    try {
        if ($method -eq "POST") {
            $response = Invoke-WebRequest -Uri $url -Headers $headers -Method Post -Body $body -ErrorAction Stop -UseBasicParsing
        }
        else {
            $response = Invoke-WebRequest -Uri $url -Headers $headers -Method Get -ErrorAction Stop -UseBasicParsing
        }
        $code = $response.StatusCode
    }
    catch {
        if ($_.Exception.Response) {
            $code = $_.Exception.Response.StatusCode.value__
        }
        else {
            $code = 0
            # Write-Host "[ERROR] Could not connect to $url - $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }

    if ($code -eq $expectedCode) {
        Write-Host "[PASS] $user -> $method $path : Got $code (Expected $expectedCode)" -ForegroundColor Green
    }
    else {
        Write-Host "[FAIL] $user -> $method $path : Got $code (Expected $expectedCode)" -ForegroundColor Red
    }
}

Write-Host "--- Granular RBAC Verification ---" -ForegroundColor Cyan

# 1. Patient Resource (Registrar vs Scheduler)
Test-Endpoint "/Patient" "registrar" 200
Test-Endpoint "/Patient" "scheduler" 403

# 2. Observation Resource (Physician vs Registrar)
Test-Endpoint "/Observation" "physician" 200
Test-Endpoint "/Observation" "registrar" 403

# 3. Clinical Resources (Nurse vs Lab Tech)
Test-Endpoint "/Condition" "nurse" 200
Test-Endpoint "/Condition" "lab_tech" 403

# 4. Administrative Resources (Practice Mgr vs Physician)
$orgJson = '{"resourceType":"Organization","name":"Test"}'
Test-Endpoint "/Organization" "practice_mgr" 201 "POST" $orgJson
Test-Endpoint "/Organization" "physician" 403 "POST" $orgJson

# 5. Scheduling Resources (Scheduler vs Nurse)
Test-Endpoint "/Practitioner" "scheduler" 200
Test-Endpoint "/Practitioner" "nurse" 403

# 6. System Resources (Sys Admin vs Physician)
# Note: Using POST for Subscription as only Create/Delete are implemented
$subJson = '{"resourceType":"Subscription","status":"requested","reason":"Test","criteria":"Observation?","channel":{"type":"rest-hook","endpoint":"http://test.com"}}'
Test-Endpoint "/Subscription" "sys_admin" 201 "POST" $subJson
Test-Endpoint "/Subscription" "physician" 403 "POST" $subJson

Write-Host "--- Verification Complete ---" -ForegroundColor Cyan
