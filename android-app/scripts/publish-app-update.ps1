[CmdletBinding()]
param(
    [string]$ReleaseNotes = "",
    [switch]$SkipBuild,
    [switch]$AllowRepublish
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$repositoryRoot = Split-Path -Parent $projectRoot
$bucketName = 'anime-japanese-lab-android-updates'
$updateBaseUrl = 'https://anime-japanese-lab-android-updates.ishallnotwant123.workers.dev'
$javaHome = 'C:\Program Files\Android\Android Studio\jbr'
$wrangler = Join-Path $repositoryRoot 'node_modules\.bin\wrangler.cmd'
$metadataPath = Join-Path $projectRoot 'app\build\outputs\apk\localSlim\output-metadata.json'

if (-not (Test-Path -LiteralPath $wrangler -PathType Leaf)) {
    throw "Wrangler was not found at $wrangler. Run pnpm install in $repositoryRoot first."
}

if (-not $SkipBuild) {
    if (-not (Test-Path -LiteralPath $javaHome -PathType Container)) {
        throw "Android Studio JBR was not found at $javaHome."
    }
    $env:JAVA_HOME = $javaHome
    & (Join-Path $projectRoot 'gradlew.bat') assembleLocalSlim --no-daemon --console=plain --max-workers=2
    if ($LASTEXITCODE -ne 0) {
        throw "assembleLocalSlim failed with exit code $LASTEXITCODE."
    }
}

if (-not (Test-Path -LiteralPath $metadataPath -PathType Leaf)) {
    throw "APK metadata was not found at $metadataPath. Build localSlim first."
}

$metadata = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
$element = @($metadata.elements)[0]
if ($null -eq $element) {
    throw 'No APK output was recorded in output-metadata.json.'
}

$versionCode = [long]$element.versionCode
$versionName = [string]$element.versionName
$apkPath = Join-Path (Split-Path -Parent $metadataPath) ([string]$element.outputFile)
if (-not (Test-Path -LiteralPath $apkPath -PathType Leaf)) {
    throw "APK was not found at $apkPath."
}

if (-not $AllowRepublish) {
    try {
        $latest = Invoke-RestMethod -Method Get -Uri "$updateBaseUrl/v1/latest" -Headers @{ 'Cache-Control' = 'no-cache' }
        if ([long]$latest.versionCode -ge $versionCode) {
            throw "Cloud versionCode $($latest.versionCode) is not lower than local versionCode $versionCode. Increase versionCode before publishing."
        }
    } catch {
        if ($_.Exception.Message -like 'Cloud versionCode*') {
            throw
        }
        $response = $_.Exception.Response
        $statusCode = if ($null -ne $response -and $null -ne $response.StatusCode) {
            [int]$response.StatusCode
        } else {
            0
        }
        if ($statusCode -ne 404) {
            throw "Unable to verify the current cloud version; publishing was stopped. $($_.Exception.Message)"
        }
        # A new bucket legitimately returns 404 until its first latest manifest is uploaded.
    }
}

$apkFile = Get-Item -LiteralPath $apkPath
$sha256 = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash.ToLowerInvariant()
$notes = if ([string]::IsNullOrWhiteSpace($ReleaseNotes)) {
    "Nihongo Lab $versionName"
} else {
    $ReleaseNotes.Trim()
}
$objectPrefix = "releases/$versionCode"
$manifest = [ordered]@{
    schemaVersion = 1
    versionCode = $versionCode
    versionName = $versionName
    apkObjectKey = "$objectPrefix/app-localSlim.apk"
    sha256 = $sha256
    sizeBytes = [long]$apkFile.Length
    releaseNotes = $notes
    publishedAt = (Get-Date).ToUniversalTime().ToString('o')
}
$manifestPath = Join-Path (Split-Path -Parent $metadataPath) "release-manifest-$versionCode.json"
$manifestJson = $manifest | ConvertTo-Json -Depth 4
[System.IO.File]::WriteAllText($manifestPath, $manifestJson, [System.Text.UTF8Encoding]::new($false))

& $wrangler r2 object put "$bucketName/$objectPrefix/app-localSlim.apk" --remote --file $apkPath --content-type 'application/vnd.android.package-archive' --cache-control 'public, max-age=31536000, immutable' --force
if ($LASTEXITCODE -ne 0) { throw 'APK upload failed.' }

& $wrangler r2 object put "$bucketName/$objectPrefix/manifest.json" --remote --file $manifestPath --content-type 'application/json; charset=utf-8' --cache-control 'public, max-age=31536000, immutable' --force
if ($LASTEXITCODE -ne 0) { throw 'Version manifest upload failed.' }

# Publish latest.json last so clients never see a release whose APK is not ready yet.
& $wrangler r2 object put "$bucketName/releases/latest.json" --remote --file $manifestPath --content-type 'application/json; charset=utf-8' --cache-control 'public, max-age=60, must-revalidate' --force
if ($LASTEXITCODE -ne 0) { throw 'Latest manifest upload failed.' }

Write-Host "Published Nihongo Lab $versionName (versionCode $versionCode)."
Write-Host "APK: $($apkFile.Length) bytes"
Write-Host "SHA-256: $sha256"
Write-Host "Manifest: $updateBaseUrl/v1/latest"
