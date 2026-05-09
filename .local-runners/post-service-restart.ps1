$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath 'C:\Users\rudre\Desktop\SocialMedia\connectsphere-backend'
& mvn -pl 'post-service' spring-boot:run "-Dspring-boot.run.workingDirectory=C:\Users\rudre\Desktop\SocialMedia\connectsphere-backend"
