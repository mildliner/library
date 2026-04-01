param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [switch]$KeepLocalCopies
)

$ErrorActionPreference = "Stop"
$localRoot = Join-Path $RepoRoot "build\local"

function Get-ReplicaDirectories {
    if (-not (Test-Path $localRoot)) {
        return @()
    }

    return Get-ChildItem -Path $localRoot -Directory |
        Where-Object { $_.Name -match '^rep\d+$' } |
        Sort-Object { [int]$_.Name.Substring(3) }
}

function Get-ReplicaPort {
    param(
        [string]$ReplicaDirectory,
        [int]$ReplicaId
    )

    $hostsConfig = Join-Path $ReplicaDirectory "config\hosts.config"
    if (Test-Path $hostsConfig) {
        foreach ($line in Get-Content -Path $hostsConfig) {
            if ($line -match "^\s*$ReplicaId\s+\S+\s+(\d+)\s+\d+\s*$") {
                return [int]$Matches[1]
            }
        }
    }

    return 11000 + ($ReplicaId * 10)
}

function Get-ListeningPid {
    param([int]$Port)

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

function Stop-ManagedProcess {
    param(
        [string]$PidValue,
        [string]$Label
    )

    if (-not $PidValue) {
        return $false
    }

    $process = Get-Process -Id $PidValue -ErrorAction SilentlyContinue
    if (-not $process) {
        return $false
    }

    Stop-Process -Id $PidValue -Force
    Wait-Process -Id $PidValue -Timeout 10 -ErrorAction SilentlyContinue
    Write-Host $Label
    return $true
}

function Remove-DirectoryWithRetry {
    param(
        [string]$Path,
        [int]$Attempts = 6,
        [int]$DelayMs = 500
    )

    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        if (-not (Test-Path $Path)) {
            return $true
        }

        try {
            Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
            return $true
        } catch {
            if ($attempt -lt $Attempts) {
                Start-Sleep -Milliseconds $DelayMs
            } else {
                Write-Warning ("Could not remove {0}. Another process may still be using it or your current shell may be inside that directory." -f $Path)
                return $false
            }
        }
    }

    return $false
}

foreach ($replicaDir in (Get-ReplicaDirectories)) {
    $replicaId = [int]$replicaDir.Name.Substring(3)
    $pidFile = Join-Path $replicaDir.FullName "replica.pid"
    $pidValue = $null
    $stoppedByPid = $false
    $port = Get-ReplicaPort -ReplicaDirectory $replicaDir.FullName -ReplicaId $replicaId

    if (Test-Path $pidFile) {
        $pidValue = (Get-Content $pidFile | Select-Object -First 1).Trim()
    }

    $stoppedByPid = Stop-ManagedProcess -PidValue $pidValue -Label ("replica {0}: stopped launcher PID {1}" -f $replicaId, $pidValue)

    $listeningPid = Get-ListeningPid -Port $port
    if (Stop-ManagedProcess -PidValue $listeningPid -Label ("replica {0}: stopped listening PID {1} on port {2}" -f $replicaId, $listeningPid, $port)) {
    } elseif (-not $stoppedByPid) {
        Write-Host ("replica {0}: no running process found" -f $replicaId)
    } else {
        Write-Host ("replica {0}: launcher stopped and no listening process remained" -f $replicaId)
    }

    if (Test-Path $pidFile) {
        Remove-Item -LiteralPath $pidFile -Force
    }
}

if (-not $KeepLocalCopies -and (Test-Path $localRoot)) {
    $ownedDirectories = Get-ChildItem -Path $localRoot -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^(rep|cli)\d+$' }

    foreach ($directory in $ownedDirectories) {
        if (Remove-DirectoryWithRetry -Path $directory.FullName) {
            Write-Host ("removed local copy: {0}" -f $directory.Name)
        }
    }
}
