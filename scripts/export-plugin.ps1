<#
.SYNOPSIS
  Export plugins/<id> as a standalone, Plugin Hub-ready Gradle project in build/export/<id>.

.DESCRIPTION
  The Plugin Hub builds every plugin from the root of its own repository using the standard
  example-plugin build, so a plugin can't depend on this repo's core/ project there. This
  script produces that standalone layout:
    - the plugin's files plus template/standalone/ build files and the Gradle wrapper
    - only the core/ classes the plugin actually uses (transitively), copied in and relocated
      from <corePackage> to <pluginPackage>.core so each hub plugin owns its copy
  Nothing is pushed anywhere. Publish the folder yourself (see the runelite-plugin-hub skill).

.EXAMPLE
  ./scripts/export-plugin.ps1 -Id panel-popout -Build
#>
[CmdletBinding()]
param(
	[Parameter(Mandatory = $true)][string]$Id,
	[string]$OutDir = "",
	[switch]$Build
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root "plugins\$Id"
if (-not (Test-Path (Join-Path $src "build.gradle"))) { throw "plugins/$Id/build.gradle not found" }
if (-not $OutDir) { $OutDir = Join-Path $root "build\export\$Id" }

$props = @{}
foreach ($line in Get-Content (Join-Path $root "workspace.properties")) {
	if ($line -match '^\s*([^#=\s]+)\s*=\s*(.*)$') { $props[$Matches[1]] = $Matches[2].Trim() }
}
$corePackage = $props["corePackage"]
if (-not $corePackage) { throw "corePackage= missing from workspace.properties" }

# Plugin package and class prefix come from ext.pluginMainClass in the plugin's build.gradle.
$pluginGradle = Get-Content -Raw (Join-Path $src "build.gradle")
if ($pluginGradle -notmatch "pluginMainClass\s*=\s*'([\w.]+)\.(\w+)PluginTest'") { throw "Couldn't read ext.pluginMainClass from plugins/$Id/build.gradle" }
$pluginPackage = $Matches[1]
$class = $Matches[2]
$newCorePackage = "$pluginPackage.core"

# Plugin-specific dependencies must be merged by hand.
if ($pluginGradle -match '(?s)dependencies\s*\{(.*?)\n\}') {
	$deps = ($Matches[1] -split "`n" | Where-Object { $_.Trim() -and -not $_.Trim().StartsWith("//") })
	if ($deps) { Write-Warning "plugins/$Id/build.gradle declares dependencies; add them to $OutDir/build.gradle by hand:"; $deps | ForEach-Object { Write-Warning "  $($_.Trim())" } }
}

if (Test-Path $OutDir) { Remove-Item -Recurse -Force $OutDir }
New-Item -ItemType Directory -Force $OutDir | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding $false

# 1. Plugin files (minus build output and the monorepo build.gradle).
Get-ChildItem -Force $src | Where-Object { @("build", ".gradle", "build.gradle", "out") -notcontains $_.Name } | ForEach-Object {
	Copy-Item -Recurse $_.FullName (Join-Path $OutDir $_.Name)
}

# Dev-only test harnesses (*SelfTest*.java) never ship: reviewers should only see plugin code.
# The launcher is regenerated from the template so it can't reference them.
$tokens = @{ "__PACKAGE__" = $pluginPackage; "__Class__" = $class; "__ID__" = $Id }
Get-ChildItem -Recurse -File -Filter "*SelfTest*.java" (Join-Path $OutDir "src") -ErrorAction SilentlyContinue | ForEach-Object {
	Write-Host "  dropping dev-only $($_.Name)"
	Remove-Item $_.FullName
}
$launcherTemplate = [System.IO.File]::ReadAllText((Join-Path $root "template\plugin\src\test\java\__package__\__Class__PluginTest.java"))
foreach ($k in $tokens.Keys) { $launcherTemplate = $launcherTemplate.Replace($k, $tokens[$k]) }
$launcher = Join-Path $OutDir ("src\test\java\" + ($pluginPackage -replace '\.', '\') + "\${class}PluginTest.java")
[System.IO.File]::WriteAllText($launcher, $launcherTemplate, $utf8)

# 2. Standalone build files + wrapper.
foreach ($f in @("build.gradle", "settings.gradle", ".gitignore")) {
	$text = [System.IO.File]::ReadAllText((Join-Path $root "template\standalone\$f"))
	foreach ($k in $tokens.Keys) { $text = $text.Replace($k, $tokens[$k]) }
	[System.IO.File]::WriteAllText((Join-Path $OutDir $f), $text, $utf8)
}
Copy-Item (Join-Path $root "gradlew"), (Join-Path $root "gradlew.bat") $OutDir
Copy-Item -Recurse (Join-Path $root "gradle") (Join-Path $OutDir "gradle")
[System.IO.File]::WriteAllText((Join-Path $OutDir ".gitattributes"), "* text=auto`ngradlew text eol=lf`n*.bat text eol=crlf`n*.jar binary`n*.png binary`n", $utf8)

# 3. Work out which core classes the plugin needs (transitive closure).
$coreRoot = Join-Path $root ("core\src\main\java\" + ($corePackage -replace '\.', '\'))
$coreFiles = @{}
if (Test-Path $coreRoot) {
	Get-ChildItem -Recurse -File -Filter *.java $coreRoot | ForEach-Object {
		$rel = $_.FullName.Substring($coreRoot.Length + 1)
		$pkg = $corePackage + (($(Split-Path -Parent $rel) -replace '\\', '.') -replace '^(.+)$', '.$1')
		$coreFiles[$_.FullName] = @{ Rel = $rel; Pkg = $pkg; Name = $_.BaseName; Text = [System.IO.File]::ReadAllText($_.FullName) }
	}
}

function Get-Referenced([string]$text, [string]$filePkg) {
	foreach ($cf in $coreFiles.Values) {
		$fqcn = "$($cf.Pkg).$($cf.Name)"
		$visible = ($filePkg -eq $cf.Pkg) -or $text.Contains("import $($cf.Pkg).*;")
		if ($text.Contains($fqcn) -or ($visible -and $text -match "\b$([regex]::Escape($cf.Name))\b")) { $fqcn }
	}
}

$needed = New-Object System.Collections.Generic.HashSet[string]
$queue = New-Object System.Collections.Generic.Queue[object]
Get-ChildItem -Recurse -File -Filter *.java (Join-Path $OutDir "src\main\java") | ForEach-Object {
	$queue.Enqueue(@{ Text = [System.IO.File]::ReadAllText($_.FullName); Pkg = "" })
}
while ($queue.Count -gt 0) {
	$item = $queue.Dequeue()
	foreach ($fqcn in (Get-Referenced $item.Text $item.Pkg)) {
		if ($needed.Add($fqcn)) {
			$cf = $coreFiles.Values | Where-Object { "$($_.Pkg).$($_.Name)" -eq $fqcn } | Select-Object -First 1
			$queue.Enqueue(@{ Text = $cf.Text; Pkg = $cf.Pkg })
		}
	}
}

# 4. Copy needed core classes, relocated, and rewrite references everywhere.
$destCore = Join-Path $OutDir ("src\main\java\" + ($newCorePackage -replace '\.', '\'))
foreach ($cf in $coreFiles.Values) {
	if (-not $needed.Contains("$($cf.Pkg).$($cf.Name)")) { continue }
	$target = Join-Path $destCore $cf.Rel
	New-Item -ItemType Directory -Force (Split-Path -Parent $target) | Out-Null
	[System.IO.File]::WriteAllText($target, $cf.Text.Replace($corePackage, $newCorePackage), $utf8)
}
$coreRes = Join-Path $root ("core\src\main\resources\" + ($corePackage -replace '\.', '\'))
if ($needed.Count -gt 0 -and (Test-Path $coreRes)) {
	$destRes = Join-Path $OutDir ("src\main\resources\" + ($newCorePackage -replace '\.', '\'))
	New-Item -ItemType Directory -Force $destRes | Out-Null
	Copy-Item -Recurse (Join-Path $coreRes "*") $destRes
}
Get-ChildItem -Recurse -File -Include *.java (Join-Path $OutDir "src") | ForEach-Object {
	$text = [System.IO.File]::ReadAllText($_.FullName)
	if ($text.Contains("$corePackage.") -and -not $text.Contains("$newCorePackage.")) {
		[System.IO.File]::WriteAllText($_.FullName, $text.Replace("$corePackage.", "$newCorePackage."), $utf8)
	}
}

Write-Host "Exported plugins/$Id -> $OutDir"
Write-Host "  core classes included: $($needed.Count) (relocated to $newCorePackage)"
$needed | Sort-Object | ForEach-Object { Write-Host "    $_" }

if ($Build) {
	Push-Location $OutDir
	try {
		if ($IsWindows -or $env:OS -eq "Windows_NT") { & .\gradlew.bat build --console=plain } else { & ./gradlew build --console=plain }
		if ($LASTEXITCODE -ne 0) { throw "standalone build failed" }
		Write-Host "Standalone build OK"
	}
	finally { Pop-Location }
}
