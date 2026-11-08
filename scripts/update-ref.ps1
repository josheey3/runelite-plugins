<#
.SYNOPSIS
  Clone or refresh the read-only reference checkouts in _ref/ (git-ignored):
  runelite/runelite (client + API source), runelite/example-plugin (plugin template),
  runelite/runelite.wiki (hub rules: Rejected-or-Rolled-Back-Features.md, Plugin-Hub-Review.md),
  runelite/plugin-hub (manifests of every hub plugin) and runelite/plugin-hub-tooling (the packager's checks).
#>
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$ref = Join-Path $root "_ref"
New-Item -ItemType Directory -Force $ref | Out-Null

$repos = [ordered]@{
	"runelite" = "runelite"
	"example-plugin" = "example-plugin"
	"wiki" = "runelite.wiki"
	"plugin-hub" = "plugin-hub"
	"plugin-hub-tooling" = "plugin-hub-tooling"
}
foreach ($name in $repos.Keys) {
	$repo = $repos[$name]
	$dir = Join-Path $ref $name
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
