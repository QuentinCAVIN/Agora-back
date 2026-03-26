$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $root

if (-not (Test-Path ".env")) {
  Write-Error "Fichier .env introuvable à la racine du repo. Copie d'abord: cp .env.example .env"
}

docker compose up -d postgres | Out-Null

Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*#') { return }
  if ($_ -match '^\s*$') { return }
  $kv = $_.Split('=', 2)
  if ($kv.Count -ne 2) { return }
  [System.Environment]::SetEnvironmentVariable($kv[0], $kv[1])
}

.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

