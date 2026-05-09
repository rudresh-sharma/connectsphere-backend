$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath 'C:\Users\rudre\Desktop\SocialMedia\connectsphere-backend'
$Host.UI.RawUI.WindowTitle = 'connectsphere-api-gateway'
if (Test-Path -LiteralPath 'C:\Users\rudre\Desktop\SocialMedia\connectsphere-backend\scripts\Load-DotEnv.ps1') {
    . 'C:\Users\rudre\Desktop\SocialMedia\connectsphere-backend\scripts\Load-DotEnv.ps1'
}
$env:EUREKA_ENABLED = 'true'
$env:EUREKA_DEFAULT_ZONE = 'http://localhost:8761/eureka/'
Write-Host 'Starting api-gateway on port 8088...'
Write-Host 'Backend directory: C:\Users\rudre\Desktop\SocialMedia\connectsphere-backend'
Write-Host ''
& mvn -pl 'api-gateway' spring-boot:run "-Dspring-boot.run.workingDirectory=C:\Users\rudre\Desktop\SocialMedia\connectsphere-backend"
if ($LASTEXITCODE -ne 0) {
    Write-Host ''
    Write-Host 'api-gateway stopped with exit code' $LASTEXITCODE -ForegroundColor Red
}
Write-Host ''
Read-Host 'Press Enter to close this window'
