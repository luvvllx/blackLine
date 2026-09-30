$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$outt = Join-Path $root "outt"
New-Item -ItemType Directory $outt -Force | Out-Null

# stage 1, temp name, shredded after stage 2
$in = (Get-ChildItem (Join-Path $root "input") -Filter *.jar | Select-Object -First 1).FullName
if (-not $in) { throw "no jar in input/" }
$stage1 = Join-Path $env:TEMP ("ms-" + [guid]::NewGuid().ToString("N").Substring(0,8) + ".tmp")
try {
$log = java -jar (Join-Path $root "blackLine-stage1.jar") `
  -inJar $in `
  -outJar $stage1 `
  -inClass "(.*)" `
  -numberObf -numberObf -numberObf `
  -junkSwitchCaseObf `
  -localVarObf true `
  -fieldMethodResorter `
  -removeMethodThrows 2>&1 | Out-String
if (-not (Test-Path $stage1)) { Write-Output $log; throw "pre-pass failed" }

# stage 2
& "$PSScriptRoot\build.ps1"
if ($LASTEXITCODE -ne 0) { throw "build failed" }
$out = Join-Path $outt "blackLine.jar"
Remove-Item $out -ErrorAction SilentlyContinue
$res = cmd /c "java -cp `"$PSScriptRoot\build;$root\NEW_ASM\asm-9.10.1.jar;$root\NEW_ASM\asm-tree-9.10.1.jar;$root\NEW_ASM\asm-commons-9.10.1.jar`" dev.luvvllx.bline.BlackLine `"$stage1`" `"$out`" blackLine luvvllx 0 2>&1"
$res
} finally {
  Remove-Item $stage1 -Force -ErrorAction SilentlyContinue
  Remove-Item (Join-Path $outt "stage1.jar") -Force -ErrorAction SilentlyContinue
}
