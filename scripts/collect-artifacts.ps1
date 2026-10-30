$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$destination = 'releases/alpha'
New-Item -ItemType Directory -Force $destination | Out-Null
$checksums = foreach ($entry in (Get-Content docs/versions.json | ConvertFrom-Json)) {
    if ($entry.build -ne 'passed') { continue }
    $name = "SharedAdvancements-$($entry.minecraft)-0.1.0-alpha.1.jar"
    $source = "versions/$($entry.minecraft)/build/libs/$name"
    if (!(Test-Path $source)) { throw "Missing built artifact: $source" }
    Copy-Item $source "$destination/$name" -Force
    "$((Get-FileHash $source -Algorithm SHA256).Hash.ToLower())  $name"
}
$checksums | Set-Content "$destination/SHA256SUMS.txt"
Copy-Item docs/versions.json "$destination/versions.json" -Force
Copy-Item docs/VERIFICATION.md "$destination/VERIFICATION.md" -Force
'Development alpha candidates. Build/server-smoke success is not full client compatibility. See docs/VERIFICATION.md in the repository.' | Set-Content "$destination/README.txt"
