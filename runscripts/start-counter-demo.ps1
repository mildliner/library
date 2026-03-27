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
Write-Host "Local counter demo folders are ready:"
foreach ($id in 0..($ReplicaCount - 1)) {
    Write-Host ("  {0}" -f (Join-Path $localRoot ("rep{0}" -f $id)))
}
Write-Host ("  {0}" -f (Join-Path $localRoot "cli0"))

Write-Host ""
Write-Host "Start the replicas in four separate terminals:"
foreach ($id in 0..($ReplicaCount - 1)) {
    Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.counter.CounterServer {1}" -f (Join-Path $localRoot ("rep{0}" -f $id)), $id)
}

Write-Host ""
Write-Host "After all replicas print 'Ready to process operations', run the client:"
Write-Host ("  Set-Location `"{0}`"; cmd /c smartrun.cmd bftsmart.demo.counter.CounterClient 1001 1 1" -f (Join-Path $localRoot "cli0"))

Write-Host ""
Write-Host "To stop replicas that are still listening on the demo ports:"
Write-Host ("  powershell -ExecutionPolicy Bypass -File `"{0}`"" -f (Join-Path $PSScriptRoot "stop-counter-demo.ps1"))
