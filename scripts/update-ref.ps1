<#
.SYNOPSIS
  Clone or refresh the read-only reference checkouts in _ref/ (git-ignored):
  runelite/runelite (client + API source) and runelite/example-plugin (plugin template).
#>
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$ref = Join-Path $root "_ref"
New-Item -ItemType Directory -Force $ref | Out-Null

foreach ($repo in @("runelite", "example-plugin")) {
	$dir = Join-Path $ref $repo
	if (Test-Path (Join-Path $dir ".git")) {
		Write-Host "Updating $repo"
		git -C $dir fetch --depth 1 origin
		if ($LASTEXITCODE -ne 0) { throw "fetch failed for $repo" }
		git -C $dir reset --hard FETCH_HEAD
	}
	else {
		Write-Host "Cloning $repo"
		git clone --depth 1 "https://github.com/runelite/$repo.git" $dir
		if ($LASTEXITCODE -ne 0) { throw "clone failed for $repo" }
	}
	git -C $dir log -1 --format="  %h %cd %s" --date=short
}
