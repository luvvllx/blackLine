$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$asmcp = "$root\NEW_ASM\asm-9.10.1.jar;$root\NEW_ASM\asm-tree-9.10.1.jar;$root\NEW_ASM\asm-analysis-9.10.1.jar"
$out = "$PSScriptRoot\stage1-build"
$jar = "$root\blackLine-stage1.jar"

# compile the donor-derived obfuscator sources under src/ ...
New-Item -ItemType Directory $out -Force | Out-Null
Remove-Item "$out\*" -Recurse -Force -ErrorAction SilentlyContinue
$files = Get-ChildItem "$root\src" -Recurse -Filter *.java | ForEach-Object { '"' + $_.FullName + '"' }
$log = cmd /c "javac -nowarn -encoding UTF-8 -cp `"$asmcp`" -d `"$out`" $($files -join ' ') 2>&1" | Out-String
if ($log | Select-String "error:") { Write-Output $log; throw "stage1 sources failed to compile" }

# ... bundle the ASM runtime classes it links against, drop module-info, and add
# the old-ASM verifier payload it reads as a resource at run time
$tmp = "$env:TEMP\bl_asmx"
foreach ($j in @("asm-9.10.1.jar", "asm-tree-9.10.1.jar", "asm-analysis-9.10.1.jar")) {
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $z = [System.IO.Compression.ZipFile]::OpenRead("$root\NEW_ASM\$j")
  foreach ($e in $z.Entries) {
    if ($e.FullName -like "*.class" -and $e.FullName -ne "module-info.class") {
      $d = Join-Path $out $e.FullName
      New-Item -ItemType Directory (Split-Path $d) -Force | Out-Null
      $o = [System.IO.File]::Create($d); $s = $e.Open(); $s.CopyTo($o); $s.Close(); $o.Close()
    }
  }
  $z.Dispose()
}
Copy-Item "$root\src\asm503legacy.jar" "$out\asm503legacy.jar" -Force

$mf = "$env:TEMP\bl_manifest.txt"
Set-Content $mf "Manifest-Version: 1.0`r`nMain-Class: dev.luvvllx.bline.Main`r`n" -Encoding ASCII -NoNewline
Remove-Item $jar -ErrorAction SilentlyContinue
Push-Location $out
& jar cfm $jar $mf * 2>&1 | Out-Null
Pop-Location
if (-not (Test-Path $jar)) { throw "stage1 jar not produced" }
Write-Output ("stage1 jar -> " + [math]::Round((Get-Item $jar).Length / 1kb) + " KB")
