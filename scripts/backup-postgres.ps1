# Script opcional. Exige pg_dump no PATH e variáveis PGHOST, PGPORT, PGDATABASE,
# PGUSER, PGPASSWORD no ambiente. Não armazene senhas no repositório.
$ErrorActionPreference = 'Stop'
if (-not $env:PGHOST -or -not $env:PGDATABASE -or -not $env:PGUSER) {
    throw 'Defina PGHOST, PGDATABASE e PGUSER antes do backup.'
}
$folder = Join-Path $PSScriptRoot '..\backups'
New-Item -Path $folder -ItemType Directory -Force | Out-Null
$destino = Join-Path $folder ('iba-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump')
& pg_dump --format=custom --no-owner --no-acl --file=$destino
if ($LASTEXITCODE -ne 0) { throw ('Backup falhou. Código: ' + $LASTEXITCODE) }
Write-Output ('Backup criado: ' + $destino)
# O arquivo contém DADOS SENSÍVEIS. Guarde fora do computador/repositório, criptografado.
