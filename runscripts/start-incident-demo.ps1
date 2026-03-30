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
    $stdoutLog = Join-Path $replicaDir "replica.log"
    $stderrLog = Join-Path $replicaDir "replica.err.log"
    $pidFile = Join-Path $replicaDir "replica.pid"

    if (Test-Path $currentView) {
        Remove-Item -LiteralPath $currentView -Force
    }
    foreach ($path in @($stdoutLog, $stderrLog, $pidFile)) {
        if (Test-Path $path) {
            Remove-Item -LiteralPath $path -Force
        }
    }
}

Write-Host ""
Write-Host "Local incident demo folders are ready:"
foreach ($id in 0..($ReplicaCount - 1)) {
    Write-Host ("  {0}" -f (Join-Path $localRoot ("rep{0}" -f $id)))
}
Write-Host ("  {0}" -f (Join-Path $localRoot "cli0"))

Write-Host ""
Write-Host "Start the replicas in four separate terminals:"
foreach ($id in 0..($ReplicaCount - 1)) {
    Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incident.IncidentServer {1}" -f (Join-Path $localRoot ("rep{0}" -f $id)), $id)
}

Write-Host ""
Write-Host "Submit a sample incident after all replicas print 'Ready to process operations':"
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incident.IncidentClient 1001 submit-pbft incident-001 ship-A 35.1000 129.0400 1 hash-ship-a Collision reported near Busan North Harbor" -f (Join-Path $localRoot "cli0"))

Write-Host ""
Write-Host "Confirm the incident from other ships:"
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incident.IncidentClient 1002 confirm incident-001 ship-B hash-ship-b" -f (Join-Path $localRoot "cli0"))
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incident.IncidentClient 1003 confirm incident-001 ship-C hash-ship-c" -f (Join-Path $localRoot "cli0"))

Write-Host ""
Write-Host "Inspect the replicated incident ledger:"
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incident.IncidentClient 1004 get incident-001" -f (Join-Path $localRoot "cli0"))
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incident.IncidentClient 1005 list" -f (Join-Path $localRoot "cli0"))
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.incident.IncidentClient 1006 head" -f (Join-Path $localRoot "cli0"))

Write-Host ""
Write-Host "To stop replicas that are still listening on the demo ports:"
Write-Host ("  powershell -ExecutionPolicy Bypass -File `"{0}`"" -f (Join-Path $PSScriptRoot "stop-counter-demo.ps1"))
