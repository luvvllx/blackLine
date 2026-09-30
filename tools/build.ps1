$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$asm = "$root\NEW_ASM\asm-9.10.1.jar;$root\NEW_ASM\asm-tree-9.10.1.jar;$root\NEW_ASM\asm-commons-9.10.1.jar"
$out = "$PSScriptRoot\build"
$srcs = @(
  "$PSScriptRoot\dev\luvvllx\bline\BlackLine.java",
  "$PSScriptRoot\dev\luvvllx\bline\Shred.java",
  "$PSScriptRoot\dev\luvvllx\bline\KeyGen.java",
  "$PSScriptRoot\dev\luvvllx\runtime\Boot.java",
  "$PSScriptRoot\dev\luvvllx\runtime\Cx.java",
  "$PSScriptRoot\dev\luvvllx\runtime\Forge.java",
  "$PSScriptRoot\dev\luvvllx\runtime\Vm.java",
  "$PSScriptRoot\dev\luvvllx\runtime\Rt.java"
)
New-Item -ItemType Directory -Path $out -Force | Out-Null
$log = cmd /c "javac -nowarn -encoding UTF-8 -cp `"$asm`" -d `"$out`" `"$($srcs -join '" "')`" 2>&1" | Out-String
if ($log | Select-String "error:") {
  Write-Output $log
  exit 1
}
Write-Output "compiled -> $out"
