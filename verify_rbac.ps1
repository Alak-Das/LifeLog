$baseUrl = "http://localhost:8080"
$adminCreds = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:password"))
$clinicianCreds = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("clinician:password"))

function Test-Endpoint {
    param($url, $creds, $user, $expectedCode)
    $headers = @{}
    if ($creds) { $headers["Authorization"] = "Basic $creds" }
    
    try {
        $response = Invoke-WebRequest -Uri $url -Headers $headers -Method Get -ErrorAction Stop
        $code = $response.StatusCode
    }
    catch {
        $code = $_.Exception.Response.StatusCode.value__
    }

    if ($code -eq $expectedCode) {
        Write-Host "[PASS] $user -> $url : Got $code (Expected $expectedCode)" -ForegroundColor Green
    }
    else {
        Write-Host "[FAIL] $user -> $url : Got $code (Expected $expectedCode)" -ForegroundColor Red
    }
}

Write-Host "Verifying RBAC..."

# 1. Public Access
Test-Endpoint "$baseUrl/fhir/metadata" $null "Anonymous" 200

# 2. Admin Resource (Patient)
Test-Endpoint "$baseUrl/fhir/Patient" $adminCreds "Admin" 200
Test-Endpoint "$baseUrl/fhir/Patient" $clinicianCreds "Clinician" 403

# 3. Clinical Resource (Observation)
Test-Endpoint "$baseUrl/fhir/Observation" $clinicianCreds "Clinician" 200
Test-Endpoint "$baseUrl/fhir/Observation" $adminCreds "Admin" 403

# 4. Subscription (Admin Only)
Test-Endpoint "$baseUrl/fhir/Subscription" $adminCreds "Admin" 200
Test-Endpoint "$baseUrl/fhir/Subscription" $clinicianCreds "Clinician" 403
