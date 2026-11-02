$ErrorActionPreference = "Stop"

$ModJarSource = "C:\Users\Niklas\Programming\TimedEndUnlock\build\libs\timed-end-unlock-1.0.0.jar"
$ServerDir    = "D:\Minecraft\Test-Environment\server-base"
$ModsDir      = "D:\Minecraft\Test-Environment\server-base\mods"
$ServerJar    = "D:\Minecraft\Test-Environment\server-base\server.jar"
$ModJarDest   = Join-Path $ModsDir "timed-end-unlock-1.0.0.jar"

$script:ServerProcess = $null

function Stop-MinecraftServer {
    if ($null -ne $script:ServerProcess -and -not $script:ServerProcess.HasExited) {
        Write-Host "[Watcher] Stopping Minecraft server..." -ForegroundColor Yellow
        try {
            $script:ServerProcess.StandardInput.WriteLine("stop")
            $script:ServerProcess.StandardInput.Flush()
        } catch {
            # Ignore if stdin is already closed
        }

        if (-not $script:ServerProcess.WaitForExit(20000)) {
            Write-Host "[Watcher] Server did not exit within 20s, forcing termination..." -ForegroundColor Red
            $script:ServerProcess.Kill()
            $script:ServerProcess.WaitForExit()
        }
        Write-Host "[Watcher] Server stopped." -ForegroundColor Yellow
    }
    $script:ServerProcess = $null
}

function Copy-ModJar {
    if (-not (Test-Path -LiteralPath $ModJarSource)) {
        Write-Host "[Watcher] Source JAR not found at: $ModJarSource" -ForegroundColor Red
        return
    }

    # Wait briefly to ensure Gradle has finished writing and released the file lock
    for ($i = 0; $i -lt 10; $i++) {
        try {
            $stream = [System.IO.File]::Open($ModJarSource, 'Open', 'Read', 'Read')
            $stream.Close()
            break
        } catch {
            Start-Sleep -Milliseconds 300
        }
    }

    if (-not (Test-Path -LiteralPath $ModsDir)) {
        New-Item -ItemType Directory -Path $ModsDir -Force | Out-Null
    }

    Copy-Item -LiteralPath $ModJarSource -Destination $ModJarDest -Force
    Write-Host "[Watcher] Copied $(Split-Path $ModJarSource -Leaf) -> $ModsDir" -ForegroundColor Cyan
}

function Start-MinecraftServer {
    Write-Host "[Watcher] Starting Minecraft server..." -ForegroundColor Green
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "java"
    $psi.Arguments = "-Xms1000M -Xmx1000M -jar `"$ServerJar`" nogui"
    $psi.WorkingDirectory = $ServerDir
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true

    $script:ServerProcess = [System.Diagnostics.Process]::Start($psi)
}

function Get-FileTimestamp {
    if (Test-Path -LiteralPath $ModJarSource) {
        return (Get-Item -LiteralPath $ModJarSource).LastWriteTimeUtc.Ticks
    }
    return $null
}

try {
    Copy-ModJar
    $lastTimestamp = Get-FileTimestamp
    Start-MinecraftServer

    Write-Host "[Watcher] Watching for changes to: $ModJarSource (Press Ctrl+C to stop)" -ForegroundColor Gray

    while ($true) {
        Start-Sleep -Seconds 1
        $currentTimestamp = Get-FileTimestamp

        if ($null -ne $currentTimestamp -and $currentTimestamp -ne $lastTimestamp) {
            Write-Host "[Watcher] Detected rebuilt mod JAR." -ForegroundColor Magenta
            $lastTimestamp = $currentTimestamp

            Stop-MinecraftServer
            Copy-ModJar
            $lastTimestamp = Get-FileTimestamp
            Start-MinecraftServer
        }
    }
} finally {
    Stop-MinecraftServer
}

