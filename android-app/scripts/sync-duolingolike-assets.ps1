param(
    [string]$SourceRes = "C:\Users\汪家俊\proui\duolingo-like-assets\_apktool_out\res"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$androidRoot = Resolve-Path (Join-Path $scriptDir "..")
$targetRoot = Join-Path $androidRoot "local-duolingolike-assets"
$resolvedTargetRoot = [System.IO.Path]::GetFullPath($targetRoot)
$expectedPrefix = [System.IO.Path]::GetFullPath((Join-Path $androidRoot "local-duolingolike-assets"))

if (-not (Test-Path -LiteralPath $SourceRes)) {
    throw "Source res directory not found: $SourceRes"
}

if (-not $resolvedTargetRoot.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to write outside local-duolingolike-assets: $resolvedTargetRoot"
}

if (Test-Path -LiteralPath $resolvedTargetRoot) {
    Remove-Item -LiteralPath $resolvedTargetRoot -Recurse -Force
}

$compiledResRoot = Join-Path $resolvedTargetRoot "res"
$referenceAssetsRoot = Join-Path $resolvedTargetRoot "assets\duolingolike\reference\res"
New-Item -ItemType Directory -Force -Path $compiledResRoot, $referenceAssetsRoot | Out-Null

function Copy-DirectoryContents {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Destination
    )
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    Get-ChildItem -LiteralPath $Source -Force | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $Destination -Recurse -Force
    }
}

$compiledDirs = @("raw", "font")
foreach ($dir in $compiledDirs) {
    $sourceDir = Join-Path $SourceRes $dir
    if (Test-Path -LiteralPath $sourceDir) {
        Copy-DirectoryContents -Source $sourceDir -Destination (Join-Path $compiledResRoot $dir)
    }
}

Get-ChildItem -LiteralPath $SourceRes -Directory | Where-Object {
    $_.Name -notin $compiledDirs
} | ForEach-Object {
    Copy-DirectoryContents -Source $_.FullName -Destination (Join-Path $referenceAssetsRoot $_.Name)
}

$summary = Get-ChildItem -LiteralPath $resolvedTargetRoot -File -Recurse |
    Measure-Object -Property Length -Sum

Write-Host "Synced Duolingo-like local assets to $resolvedTargetRoot"
Write-Host ("Files: {0}, Size: {1:N2} MB" -f $summary.Count, ($summary.Sum / 1MB))
