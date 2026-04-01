param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [int]$ReplicaCount = 4
)

$ErrorActionPreference = "Stop"

$gradleUserHome = Join-Path $RepoRoot ".gradle-home"
$gradlew = Join-Path $RepoRoot "gradlew.bat"
$localRoot = Join-Path $RepoRoot "build\local"

Write-Host "Building distribution into build/install/library ..."
$env:GRADLE_USER_HOME = $gradleUserHome
& $gradlew installDist localDeploy | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build/localDeploy failed with exit code $LASTEXITCODE."
}

foreach ($id in 0..($ReplicaCount - 1)) {
    $replicaDir = Join-Path $localRoot ("rep{0}" -f $id)
    $currentView = Join-Path $replicaDir "config\currentView"
    if (Test-Path $currentView) {
        Remove-Item -LiteralPath $currentView -Force
    }
}

Write-Host ""
Write-Host "Start the simple PBFT incident replicas in four separate terminals:"
foreach ($id in 0..($ReplicaCount - 1)) {
    Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incidentsimple.SimpleIncidentServer {1}" -f (Join-Path $localRoot ("rep{0}" -f $id)), $id)
}

Write-Host ""
Write-Host "Demo scenario:"
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incidentsimple.SimpleIncidentClient 1001 submit-incident incident-001 ship-A 2 Collision reported near waypoint 17" -f (Join-Path $localRoot "cli0"))
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incidentsimple.SimpleIncidentClient 1002 confirm-incident incident-001 ship-B" -f (Join-Path $localRoot "cli0"))
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incidentsimple.SimpleIncidentClient 1003 confirm-incident incident-001 ship-C" -f (Join-Path $localRoot "cli0"))
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incidentsimple.SimpleIncidentClient 1004 get-incident incident-001" -f (Join-Path $localRoot "cli0"))

Write-Host ""
Write-Host "To stop replicas that are still listening on the demo ports:"
Write-Host ("  powershell -ExecutionPolicy Bypass -File `"{0}`"" -f (Join-Path $PSScriptRoot "stop-counter-demo.ps1"))
