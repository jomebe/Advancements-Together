param([Parameter(Mandatory)][string]$Version, [string]$Java = "$env:JAVA_HOME/bin/java", [switch]$AcceptEula)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (!$AcceptEula) { throw 'Read https://aka.ms/MinecraftEULA and pass -AcceptEula only with consent.' }
$entry = Get-Content docs/versions.json | ConvertFrom-Json | Where-Object minecraft -eq $Version
if (!$entry) { throw "Unknown Minecraft target: $Version" }
$directory = ".tools/smoke/$Version"
New-Item -ItemType Directory -Force "$directory/mods" | Out-Null
$jar = "versions/$Version/build/libs/SharedAdvancements-$Version-0.1.0-alpha.2.jar"
if (!(Test-Path $jar)) { throw "Build first: $jar" }
Copy-Item $jar "$directory/mods" -Force
Get-ChildItem "$directory/mods" -Filter 'SharedAdvancements-*.jar' | Where-Object Name -ne ([IO.Path]::GetFileName($jar)) | Remove-Item
if (!(Test-Path "$directory/server.jar")) {
    Invoke-WebRequest "https://meta.fabricmc.net/v2/versions/loader/$Version/$($entry.loader)/1.1.2/server/jar" -OutFile "$directory/server.jar"
}
$api = $entry.fabricApi
if (!(Test-Path "$directory/mods/fabric-api.jar")) {
    Invoke-WebRequest "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$api/fabric-api-$api.jar" -OutFile "$directory/mods/fabric-api.jar"
}
'eula=true' | Set-Content "$directory/eula.txt"
@'
server-ip=127.0.0.1
server-port=25576
online-mode=false
view-distance=2
simulation-distance=2
generate-structures=false
max-tick-time=60000
'@ | Set-Content "$directory/server.properties"
node scripts/smoke-server.cjs $Java $directory
if ($LASTEXITCODE -ne 0) { throw "Server smoke failed: $Version" }
