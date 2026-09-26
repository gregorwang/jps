# v3 rewrite build helper — serialises Gradle builds across parallel agents (16 GB machine).
#   powershell -ExecutionPolicy Bypass -File <worktree>\android-app\design\v3build.ps1 -Mode compile
#   powershell -ExecutionPolicy Bypass -File <worktree>\android-app\design\v3build.ps1 -Mode full
# compile = compileDebugKotlin; full = testDebugUnitTest assembleDebug.
# Run it from anywhere; it builds the android-app folder that contains this script.
param(
    [ValidateSet('compile', 'full')][string]$Mode = 'compile',
    [int]$MaxWaitMinutes = 60
)
$ErrorActionPreference = 'Continue'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$app = Split-Path -Parent $PSScriptRoot
$lock = Join-Path $env:LOCALAPPDATA 'ajl-v3-build.lock'
$logDir = Join-Path $app 'app\build\v3logs'
New-Item -ItemType Directory -Force $logDir | Out-Null
$log = Join-Path $logDir ("$Mode-" + (Get-Date -Format 'HHmmss') + '.log')

# Worktrees lack the ignored local files; link them from the main checkout.
$main = 'C:\Users\汪家俊\jps\android-app'
if ($app -ne $main) {
    if (-not (Test-Path (Join-Path $app 'local.properties'))) {
        Copy-Item (Join-Path $main 'local.properties') (Join-Path $app 'local.properties')
    }
    # Character art (drawable-nodpi) and a few legacy vectors are gitignored too.
    $res = 'app\src\main\res'
    if (-not (Test-Path (Join-Path $app "$res\drawable-nodpi"))) {
        cmd /c mklink /J (Join-Path $app "$res\drawable-nodpi") (Join-Path $main "$res\drawable-nodpi") | Out-Null
    }
    Get-ChildItem (Join-Path $main "$res\drawable") -File | ForEach-Object {
        $dst = Join-Path $app "$res\drawable\$($_.Name)"
        if (-not (Test-Path $dst)) { Copy-Item $_.FullName $dst }
    }
}

# Acquire the global lock (mkdir is atomic). Stale locks (>25 min) are broken.
$deadline = (Get-Date).AddMinutes($MaxWaitMinutes)
while ($true) {
    try {
        New-Item -ItemType Directory -Path $lock -ErrorAction Stop | Out-Null
        Set-Content (Join-Path $lock 'owner.txt') "$app $Mode $(Get-Date -Format o)"
        break
    } catch {
        $age = (Get-Date) - (Get-Item $lock).CreationTime
        if ($age.TotalMinutes -gt 25) { Remove-Item -Recurse -Force $lock -ErrorAction SilentlyContinue; continue }
        if ((Get-Date) -gt $deadline) { Write-Output 'LOCK TIMEOUT'; exit 3 }
        Start-Sleep -Seconds 10
    }
}
try {
    Set-Location $app
    $tasks = @(if ($Mode -eq 'full') { 'testDebugUnitTest'; 'assembleDebug' } else { 'compileDebugKotlin' })
    & .\gradlew.bat @tasks --no-daemon --console=plain --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process' *> $log
    $code = $LASTEXITCODE
} finally {
    Remove-Item -Recurse -Force $lock -ErrorAction SilentlyContinue
}
Get-Content $log | Select-String -Pattern '^e: |BUILD SUCCESSFUL|BUILD FAILED|FAILED$|tests completed|What went wrong' | Select-Object -First 60 | ForEach-Object { $_.Line }
if ($Mode -eq 'full') {
    $files = Get-ChildItem (Join-Path $app 'app\build\test-results\testDebugUnitTest') -Filter *.xml -ErrorAction SilentlyContinue
    $t = 0; $f = 0
    foreach ($x in $files) { $xml = [xml](Get-Content $x.FullName -Raw); $t += [int]$xml.testsuite.tests; $f += [int]$xml.testsuite.failures + [int]$xml.testsuite.errors }
    Write-Output "TESTS: $t run, $f failed"
}
Write-Output "EXIT $code  (log: $log)"
exit $code
