param(
  [string]$Branch = ""
)

$ErrorActionPreference = "Stop"

if (-not $Branch) {
  $Branch = git branch --show-current
}

if (-not $Branch) {
  throw "Cannot determine current Git branch."
}

pnpm build
git push origin $Branch
pnpm wrangler deploy
