<#
.SYNOPSIS
    AccessZero PowerShell Administration Script
.DESCRIPTION
    Identity Breach Containment & Access Revocation Tool
.EXAMPLE
    .\accesszero.ps1 list
    .\accesszero.ps1 analyze -User rahul.sharma
    .\accesszero.ps1 simulate -User rahul.sharma
    .\accesszero.ps1 contain -User rahul.sharma -Admin anil.admin
    .\accesszero.ps1 approve -Operation 1 -Admin priya.security
    .\accesszero.ps1 verify -User rahul.sharma
    .\accesszero.ps1 rollback -Operation 1
    .\accesszero.ps1 audit
#>

param(
    [Parameter(Position=0, Mandatory=$true)]
    [ValidateSet("list","analyze","simulate","contain","approve","verify","rollback","audit")]
    [string]$Command,

    [Parameter(Mandatory=$false)]
    [string]$User,

    [Parameter(Mandatory=$false)]
    [int]$Operation,

    [Parameter(Mandatory=$false)]
    [string]$Admin = "anil.admin",

    [Parameter(Mandatory=$false)]
    [string]$Reason = "Suspected credential compromise",

    [Parameter(Mandatory=$false)]
    [switch]$Emergency
)

$BaseUrl = "http://localhost:8080/api/v1"

function Show-Banner {
    Write-Host "=================================================================" -ForegroundColor Cyan
    Write-Host "     AccessZero — Identity Breach Containment Platform (PS)     " -ForegroundColor Cyan
    Write-Host "=================================================================" -ForegroundColor Cyan
}

switch ($Command) {
    "list" {
        Show-Banner
        $users = Invoke-RestMethod -Uri "$BaseUrl/identities" -Method Get
        $users | Format-Table id, username, department, status, riskLevel, riskScore, activeSessionsCount, accessPathCount
    }
    "analyze" {
        Show-Banner
        if (-not $User) { Write-Error "-User parameter is required for analyze"; exit 1 }
        Write-Host "[*] Calculating Access Blast Radius for $User..." -ForegroundColor Yellow
        $data = Invoke-RestMethod -Uri "$BaseUrl/identities/username/$User/blast-radius" -Method Get
        Write-Host "Identity:           $($data.username) (Risk: $($data.riskLevel) - Score: $($data.riskScore)/100)" -ForegroundColor Red
        Write-Host "Active Sessions:    $($data.activeSessionsCount)"
        Write-Host "OAuth Tokens:       $($data.activeTokensCount)"
        Write-Host "LDAP Groups:        $($data.groupsCount) ($($data.sensitiveGroupsCount) privileged)"
        Write-Host "Applications:       $($data.applicationsAffectedCount) ($($data.privilegedApplicationsCount) critical/high)"
        Write-Host "Access Paths:       $($data.totalAccessPathsCount)" -ForegroundColor Red
        Write-Host "`nAffected Applications:" -ForegroundColor Cyan
        $data.affectedApplications | Format-Table applicationName, type, sensitivityLevel, privileged, accessPathCount
    }
    "simulate" {
        Show-Banner
        if (-not $User) { Write-Error "-User parameter is required for simulate"; exit 1 }
        Write-Host "[*] Running Containment Simulation for $User..." -ForegroundColor Yellow
        $data = Invoke-RestMethod -Uri "$BaseUrl/identities/username/$User/simulate" -Method Post
        Write-Host "Target:             $($data.username)"
        Write-Host "Disruption Score:   $($data.disruptionScore)/100 ($($data.disruptionLevel))" -ForegroundColor Red
        Write-Host "2-Person Approval:  $($data.requiresApproval)" -ForegroundColor Magenta
        Write-Host "`nSimulation Summary:" -ForegroundColor Cyan
        $data.actionSummary | ForEach-Object { Write-Host "  ➔ $_" -ForegroundColor White }
    }
    "contain" {
        Show-Banner
        if (-not $User) { Write-Error "-User parameter is required for contain"; exit 1 }
        Write-Host "🚨 DISPATCHING CONTAINMENT FOR $User..." -ForegroundColor Red
        $body = @{
            username = $User
            requestedBy = $Admin
            reason = $Reason
            emergencyOverride = $Emergency.IsPresent
        } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri "$BaseUrl/containment/request" -Method Post -Body $body -ContentType "application/json"
        if ($res.requiresApproval) {
            Write-Host "⚠️  CONTAINMENT PENDING APPROVAL (Operation #$($res.operationId))" -ForegroundColor Yellow
            Write-Host "Secondary admin must approve: .\accesszero.ps1 approve -Operation $($res.operationId) -Admin priya.security"
        } else {
            Write-Host "✓ Containment Complete! Status: $($res.status)" -ForegroundColor Green
            Write-Host "Paths Revoked: $($res.accessPathsRevoked) / $($res.accessPathsFound)"
            $res.actionsExecuted | ForEach-Object { Write-Host "  ✓ $_" -ForegroundColor Green }
        }
    }
    "approve" {
        Show-Banner
        if (-not $Operation) { Write-Error "-Operation parameter is required for approve"; exit 1 }
        Write-Host "[*] Approving Operation #$Operation as $Admin..." -ForegroundColor Cyan
        $body = @{ approvedBy = $Admin; notes = "Approved via PowerShell CLI" } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri "$BaseUrl/containment/$Operation/approve" -Method Post -Body $body -ContentType "application/json"
        Write-Host "✓ Operation #$Operation APPROVED & EXECUTED! Status: $($res.status)" -ForegroundColor Green
        $res.actionsExecuted | ForEach-Object { Write-Host "  ✓ $_" -ForegroundColor Green }
    }
    "verify" {
        Show-Banner
        if (-not $User) { Write-Error "-User parameter is required for verify"; exit 1 }
        Write-Host "[*] Verifying Zero Access for $User..." -ForegroundColor Yellow
        $data = Invoke-RestMethod -Uri "$BaseUrl/verification/username/$User?verifiedBy=$Admin" -Method Get
        Write-Host "Overall Status:     $($data.overallStatus)" -ForegroundColor Yellow
        Write-Host "Paths Revoked:      $($data.accessPathsRevoked) / $($data.accessPathsFound)"
        Write-Host "Manual Action Items:$($data.requiresManualActionCount)"
        if ($data.remainingRisks) {
            Write-Host "`nRemaining Risks / Actions Required:" -ForegroundColor Red
            $data.remainingRisks | ForEach-Object { Write-Host "  ⚠ $_" -ForegroundColor Red }
        }
    }
    "rollback" {
        Show-Banner
        if (-not $Operation) { Write-Error "-Operation parameter is required for rollback"; exit 1 }
        Write-Host "[*] Rolling back Operation #$Operation..." -ForegroundColor Yellow
        $body = @{ rolledBackBy = $Admin } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri "$BaseUrl/containment/$Operation/rollback" -Method Post -Body $body -ContentType "application/json"
        Write-Host "✓ User $($res.username) restored to ACTIVE state!" -ForegroundColor Green
    }
    "audit" {
        Show-Banner
        $events = Invoke-RestMethod -Uri "$BaseUrl/audit/events" -Method Get
        $events | Select-Object -First 20 | Format-Table timestamp, actor, action, target, result, checksum
    }
}
