<#
.SYNOPSIS
  Pre-submission check of plugins/<id> against the RuneLite Plugin Hub rules.

.DESCRIPTION
  Mirrors what we know the hub enforces (docs/plugin-hub-rules.md): the packager's hard
  checks (Java 11, icon size, net.runelite package, manifest) and the wiki's forbidden APIs and
  rejected feature categories, as a source scan of the plugin plus the core/ classes it would
  ship with (scanned from the export). It's a review aid, not a guarantee: human/AI review decides. Matches inside
  comments still count; reword or justify them.

  Exit code 1 if any ERROR. With -Build it also runs the standalone export build (Java 11 bytecode
  and the hub's standard build.gradle).

.EXAMPLE
  ./scripts/hub-check.ps1 -Id panel-popout
  ./scripts/hub-check.ps1 -Id panel-popout -Build
#>
[CmdletBinding()]
param(
	[Parameter(Mandatory = $true)][string]$Id,
	[switch]$Build
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root "plugins\$Id"
if (-not (Test-Path $dir)) { throw "plugins/$Id not found" }

$script:errors = 0
$script:warnings = 0
function Report([string]$level, [string]$msg) {
	if ($level -eq "ERROR") { $script:errors++ } elseif ($level -eq "WARN") { $script:warnings++ }
	Write-Host ("{0,-5} {1}" -f $level, $msg)
}

# ---- manifest ---------------------------------------------------------------------------------
$propsFile = Join-Path $dir "runelite-plugin.properties"
$props = @{}
if (Test-Path $propsFile) {
	foreach ($line in Get-Content $propsFile) { if ($line -match '^\s*([^#=\s]+)\s*=\s*(.*)$') { $props[$Matches[1]] = $Matches[2].Trim() } }
}
else { Report "ERROR" "runelite-plugin.properties missing" }
foreach ($k in @("displayName", "author", "description", "plugins")) {
	if (-not $props[$k]) { Report "ERROR" "runelite-plugin.properties: '$k' is empty" }
}
if (-not $props["tags"]) { Report "WARN" "runelite-plugin.properties: no tags (they drive hub search)" }
if ($props["build"] -and @("standard", "gradle") -notcontains $props["build"]) { Report "ERROR" "build= must be 'standard' or 'gradle'" }
if ($props["build"] -eq "gradle") { Report "WARN" "build=gradle needs third-party dependency verification and slows review" }
foreach ($cls in ($props["plugins"] -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ })) {
	$path = Join-Path $dir ("src\main\java\" + ($cls -replace '\.', '\') + ".java")
	if (-not (Test-Path $path)) { Report "ERROR" "plugins= lists $cls but $path doesn't exist" }
	if ($cls -like "net.runelite*") { Report "ERROR" "plugin package must not be under net.runelite" }
	if ($cls -like "com.example*") { Report "ERROR" "template package com.example not renamed" }
}

# ---- files ------------------------------------------------------------------------------------
if (-not (Test-Path (Join-Path $dir "LICENSE"))) { Report "ERROR" "LICENSE missing (BSD-2 recommended)" }
$readme = Join-Path $dir "README.md"
if (-not (Test-Path $readme)) { Report "ERROR" "README.md missing (it's the hub page)" }
elseif ((Get-Content -Raw $readme).Length -lt 300) { Report "WARN" "README.md is very short; explain what the plugin does (screenshots help)" }
$icon = Join-Path $dir "icon.png"
if (Test-Path $icon) {
	$bytes = [System.IO.File]::ReadAllBytes($icon)
	$isPng = $bytes.Length -gt 24 -and $bytes[0] -eq 0x89 -and $bytes[1] -eq 0x50 -and $bytes[2] -eq 0x4E -and $bytes[3] -eq 0x47
	if (-not $isPng) { Report "ERROR" "icon.png isn't a real PNG" }
	else {
		$w = ($bytes[16] -shl 24) -bor ($bytes[17] -shl 16) -bor ($bytes[18] -shl 8) -bor $bytes[19]
		$h = ($bytes[20] -shl 24) -bor ($bytes[21] -shl 16) -bor ($bytes[22] -shl 8) -bor $bytes[23]
		if ($w * $h -gt 5000) { Report "ERROR" "icon.png is ${w}x${h}; the packager rejects more than 5000 px (use 48x72 or smaller)" }
		elseif ($w -gt 48 -or $h -gt 72) { Report "WARN" "icon.png is ${w}x${h}; recommended maximum is 48x72" }
	}
}
else { Report "INFO" "no icon.png (optional, at most 48x72)" }
if (Get-ChildItem -Recurse -File -Include *.kt, *.kts, *.scala, *.groovy (Join-Path $dir "src") -ErrorAction SilentlyContinue) { Report "ERROR" "non-Java sources; hub plugins must be Java" }
if (Test-Path (Join-Path $dir "src\main\resources\META-INF\services")) { Report "ERROR" "remove META-INF/services (not allowed)" }

# ---- source scan --------------------------------------------------------------------------------
# Scan exactly what would ship: the export (plugin sources + the core classes it uses, relocated,
# without dev-only *SelfTest* files). -Build also compiles it with the hub's standard build.
$exportDir = Join-Path $root "build\hub-check\$Id"
$exportArgs = @("-NoProfile", "-File", (Join-Path $PSScriptRoot "export-plugin.ps1"), "-Id", $Id, "-OutDir", $exportDir)
if ($Build) { $exportArgs += "-Build" }
$exportOutput = & pwsh @exportArgs 2>&1
if ($LASTEXITCODE -ne 0) {
	$exportOutput | Select-Object -Last 15 | ForEach-Object { Write-Host "      $_" }
	Report "ERROR" ($(if ($Build) { "standalone export build failed (Java 11 / hub build.gradle)" } else { "export failed" }))
}
elseif ($Build) { Report "OK" "standalone export builds (Java 11, hub build.gradle)" }
$sources = @(Get-ChildItem -Recurse -File -Filter *.java (Join-Path $exportDir "src\main\java") -ErrorAction SilentlyContinue)
$shippedTests = @(Get-ChildItem -Recurse -File -Filter *.java (Join-Path $exportDir "src\test\java") -ErrorAction SilentlyContinue)
$sources += $shippedTests

$rules = @(
	# level, regex, message  (sources: docs/plugin-hub-rules.md)
	@("ERROR", 'java\.lang\.reflect|getDeclared(Field|Method|Constructor)s?\(|setAccessible\(|Class\.forName\(|\.getMethod\(', "reflection is forbidden"),
	@("ERROR", '\bnative\s+\w+.*\(|com\.sun\.jna|System\.load(Library)?\(', "JNI/JNA is forbidden"),
	@("ERROR", 'ProcessBuilder|Runtime\.getRuntime|java\.lang\.Runtime\b', "executing processes / java.lang.Runtime is forbidden"),
	@("ERROR", 'ClassLoader|defineClass\(', "dynamic class loading is forbidden"),
	@("ERROR", 'ObjectInputStream|ObjectOutputStream', "Java serialization is forbidden"),
	@("ERROR", 'Desktop\.(open|browse)|Desktop\.getDesktop|LinkBrowser\.open\(', "use LinkBrowser.browse (Desktop and LinkBrowser.open are forbidden)"),
	@("ERROR", 'Thread\.sleep\(|\.interrupt\(\)', "Thread.sleep / Thread.interrupt are forbidden"),
	@("ERROR", 'KeyboardFocusManager', "java.awt.KeyboardFocusManager is forbidden"),
	@("ERROR", 'import java\.io\.(File|FileInputStream|FileOutputStream|FileReader|FileWriter|RandomAccessFile)\b|new File\(', "java.io file APIs are forbidden; use Filepath"),
	@("ERROR", 'HttpURLConnection|java\.net\.http|org\.apache\.http', "only the injected OkHttpClient may do network IO"),
	@("ERROR", 'new\s+Gson\(|new\s+GsonBuilder\(', "inject Gson (use .newBuilder() to customise)"),
	@("ERROR", 'new\s+OkHttpClient\(|new\s+OkHttpClient\.Builder\(', "inject OkHttpClient"),
	@("ERROR", '\bWidgetInfo\b|\bWidgetID\b', "WidgetInfo/WidgetID are terminally deprecated; use InterfaceID/gameval"),
	@("ERROR", 'ChatMessageManager\b.*\.update\(', "ChatMessageManager.update is a disallowed no-op"),
	@("WARN", 'menuAction\(|runScript\(', "sensitive API: needs extra review (see rules doc)"),
	@("WARN", 'hopToWorld\(|Filepath\.Unchecked|setCanSendPackets', "restricted API: forces manual review"),
	@("ERROR", 'ClientSidebar|getTabPlacement\(|TABBED_PANE_|Frame\.getFrames\(|Window\.getWindows\(|ClientUI\.(?!ICON_)\w+|\bClientUI\s+[a-z]\w*', "touches core RuneLite UI (sidebar/root frame): a rejected feature category"),
	@("WARN", 'getWindowAncestor\(|setExtendedState\(|toFront\(|requestFocus', "window/focus handling: never act on the client window (rejected category)"),
	@("WARN", 'System\.(out|err)\.|printStackTrace\(', "use the logger, not System.out/printStackTrace"),
	@("WARN", '(set|unset)Configuration\(', "writes config: only ever your own config group (modifying other plugins' configs is a rejected category)")
)

foreach ($file in $sources) {
	$rel = $file.FullName.Substring($exportDir.Length + 1)
	$lines = Get-Content $file.FullName
	for ($i = 0; $i -lt $lines.Count; $i++) {
		foreach ($r in $rules) {
			if ($lines[$i] -match $r[1]) { Report $r[0] ("{0}:{1}: {2}  |  {3}" -f $rel, ($i + 1), $r[2], $lines[$i].Trim()) }
		}
	}
	if ((Get-Content -Raw $file.FullName) -match 'log\.info\(' ) { Report "INFO" "${rel}: uses log.info; fine for rare events, not per-tick/frame" }
}

Write-Host ""
Write-Host "hub-check ${Id}: $($script:errors) error(s), $($script:warnings) warning(s). Also read docs/plugin-hub-rules.md for judgement calls (rejected features, game rules)."
if ($script:errors -gt 0) { exit 1 }
