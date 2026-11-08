<#
.SYNOPSIS
  Scaffold a new RuneLite plugin under plugins/<id> from the workspace template.

.EXAMPLE
  ./scripts/new-plugin.ps1 -Name "Helmet Check" -Description "Warns when your head slot is empty"

.EXAMPLE
  ./scripts/new-plugin.ps1 -Name "Helmet Check" -Id helmet-check -Package com.someone.helmetcheck
#>
[CmdletBinding()]
param(
	[Parameter(Mandatory = $true)][string]$Name,
	[string]$Description = "",
	[string]$Id = "",
	[string]$Package = "",
	[string]$Author = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$template = Join-Path $root "template"

# Workspace defaults (author, basePackage) live in workspace.properties at the repo root.
$props = @{}
$propsFile = Join-Path $root "workspace.properties"
if (Test-Path $propsFile) {
	foreach ($line in Get-Content $propsFile) {
		if ($line -match '^\s*([^#=\s]+)\s*=\s*(.*)$') { $props[$Matches[1]] = $Matches[2].Trim() }
	}
}

$words = @([regex]::Matches($Name, '[A-Za-z0-9]+') | ForEach-Object { $_.Value })
if ($words.Count -eq 0) { throw "Name must contain letters or digits" }
$class = ($words | ForEach-Object { $_.Substring(0, 1).ToUpper() + $_.Substring(1) }) -join ""
if ($class -match '^[0-9]') { throw "Name must not start with a digit (it becomes a Java class name)" }
if (-not $Id) { $Id = ($words | ForEach-Object { $_.ToLower() }) -join "-" }
if ($Id -notmatch '^[a-z0-9]+(-[a-z0-9]+)*$') { throw "Id must be kebab-case, e.g. helmet-check" }
if (-not $Author) { $Author = $props["author"] }
if (-not $Author) { throw "Pass -Author or set author= in workspace.properties" }
if (-not $Package) {
	$base = $props["basePackage"]
	if (-not $base) { throw "Pass -Package or set basePackage= in workspace.properties" }
	$Package = "$base." + ($Id -replace '-', '')
}
if ($Package -notmatch '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$') { throw "Package '$Package' is not a valid lower-case Java package" }
if (-not $Description) { $Description = $Name }

$dest = Join-Path $root "plugins\$Id"
if (Test-Path $dest) { throw "$dest already exists" }

Copy-Item -Recurse $template $dest

# Move source folders into the real package path.
$pkgPath = $Package -replace '\.', '\'
foreach ($set in @("main", "test")) {
	$from = Join-Path $dest "src\$set\java\__package__"
	$to = Join-Path $dest "src\$set\java\$pkgPath"
	New-Item -ItemType Directory -Force (Split-Path -Parent $to) | Out-Null
	Move-Item $from $to
}

$tokens = [ordered]@{
	"__PACKAGE__"     = $Package
	"__Class__"       = $class
	"__NAME__"        = $Name
	"__ID__"          = $Id
	"__AUTHOR__"      = $Author
	"__DESCRIPTION__" = $Description
	"__YEAR__"        = (Get-Date).Year.ToString()
}
$utf8 = New-Object System.Text.UTF8Encoding $false
$textExt = @(".java", ".gradle", ".properties", ".xml", ".md", "")
Get-ChildItem -Recurse -File $dest | Where-Object { $textExt -contains $_.Extension -and $_.Name -notlike "gradlew*" -and $_.Name -ne ".gitignore" } | ForEach-Object {
	$text = [System.IO.File]::ReadAllText($_.FullName)
	$orig = $text
	foreach ($k in $tokens.Keys) { $text = $text.Replace($k, $tokens[$k]) }
	if ($text -ne $orig) { [System.IO.File]::WriteAllText($_.FullName, $text, $utf8) }
	if ($_.Name -like "*__Class__*") { Rename-Item $_.FullName ($_.Name.Replace("__Class__", $class)) }
}

$left = Get-ChildItem -Recurse -File $dest | Where-Object { $textExt -contains $_.Extension } | Select-String -Pattern '__[A-Za-z]+__'
if ($left) { Write-Warning "Unreplaced tokens remain:"; $left | ForEach-Object { Write-Warning "  $($_.Path):$($_.LineNumber)" } }

# Git on Windows can't see the executable bit; the Plugin Hub builds on Linux and needs it.
git -C $root rev-parse --is-inside-work-tree 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) { git -C $root add --chmod=+x "plugins/$Id/gradlew" }

Write-Host "Created plugins/$Id"
Write-Host "  package : $Package"
Write-Host "  classes : ${class}Plugin, ${class}Config, ${class}PluginTest"
Write-Host "  config  : group '$Id'"
Write-Host ""
Write-Host "Next: cd plugins/$Id; ./gradlew build; ./gradlew run"
