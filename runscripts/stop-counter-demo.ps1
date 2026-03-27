param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [int]$ReplicaCount = 4
)

$ErrorActionPreference = "Stop"
$localRoot = Join-Path $RepoRoot "build\local"

function Get-ReplicaPid {
    param(
        [int]$Port
    )

    $pattern = "LISTENING\s+(\d+)$"
    $line = netstat -ano | Select-String (":{0}\s" -f $Port) | Select-Object -First 1
    if (-not $line) {
        return $null
    }
    if ($line.Line -match $pattern) {
        return $Matches[1]
    }
    return $null
}

foreach ($id in 0..($ReplicaCount - 1)) {
    $replicaDir = Join-Path $localRoot ("rep{0}" -f $id)
    $pidFile = Join-Path $replicaDir "replica.pid"
    $pidValue = $null
    if (Test-Path $pidFile) {
        $pidValue = (Get-Content $pidFile | Select-Object -First 1).Trim()
    }
    if (-not $pidValue) {
        $pidValue = Get-ReplicaPid -Port (11000 + ($id * 10))
    }

    if ($pidValue -and (Get-Process -Id $pidValue -ErrorAction SilentlyContinue)) {
        Stop-Process -Id $pidValue -Force
        Write-Host ("replica {0}: stopped PID {1}" -f $id, $pidValue)
    } else {
        Write-Host ("replica {0}: no running process found" -f $id)
    }

    if (Test-Path $pidFile) {
        Remove-Item -LiteralPath $pidFile -Force
    }
}
