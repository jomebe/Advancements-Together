param([string[]]$Versions = @())
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to JDK 25 before building.' }
New-Item -ItemType Directory -Force build/matrix | Out-Null
$matrix = Get-Content docs/versions.json | ConvertFrom-Json
if ($Versions.Count -eq 0) { $Versions = $matrix.minecraft }
$failed = @()
foreach ($version in $Versions) {
    if ($version -notin $matrix.minecraft) { throw "Unknown target: $version" }
    & ./gradlew.bat "-Pmc=$version" build --console plain *> "build/matrix/$version.log"
    $passed = $LASTEXITCODE -eq 0
    $matrix | Where-Object minecraft -eq $version | ForEach-Object { $_.build = if ($passed) { 'passed' } else { 'failed' } }
    $matrix | ConvertTo-Json -Depth 5 | Set-Content docs/versions.json
    Write-Output "$version build: $passed"
    if (!$passed) { $failed += $version; Get-Content "build/matrix/$version.log" -Tail 45 }
}
if ($failed.Count) { throw "Build failures: $($failed -join ', ')" }
