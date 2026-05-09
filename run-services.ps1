# PowerShell script to build service Dockerfiles, build images, start infrastructure, and run all services

# Define services and their ports
$services = [ordered]@{
    "eureka-server" = 8761
    "admin-server" = 8090
    "api-gateway" = 8088
    "auth-service" = 8080
    "post-service" = 8081
    "follow-service" = 8082
    "comment-service" = 8083
    "like-service" = 8084
    "notification-service" = 8085
    "media-service" = 8086
    "search-service" = 8087
}

# Path to the backend directory
$backendDir = $PSScriptRoot
$dotenvFile = Join-Path $backendDir ".env"

# Template for Dockerfile. Jars are built locally first so Docker builds do not
# repeatedly download Maven dependencies for every service.
$dockerfileTemplate = @"
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY {SERVICE}/target/{SERVICE}-*.jar app.jar
EXPOSE {PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
"@

# Sync Dockerfiles for all services so stale files do not keep breaking builds
foreach ($service in $services.Keys) {
    $dockerfilePath = Join-Path $backendDir "$service\Dockerfile"
    $port = $services[$service]
    $dockerfileContent = $dockerfileTemplate -replace "{SERVICE}", $service -replace "{PORT}", $port
    Set-Content -Path $dockerfilePath -Value $dockerfileContent -Encoding ascii
    Write-Host "Synced Dockerfile for $service"
}

# Build executable Spring Boot jars once using the local Maven cache
Write-Host "Building service jars with Maven..."
& mvn package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to build service jars"
    exit 1
}

# Build Docker images for all services
foreach ($service in $services.Keys) {
    Write-Host "Building Docker image for $service..."
    & docker build -t "connectsphere/$service" -f "$service/Dockerfile" "$backendDir"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to build image for $service"
        exit 1
    }
}

# Start infrastructure (databases, Elasticsearch, Redis, and RabbitMQ)
Write-Host "Starting infrastructure with docker-compose..."
& docker-compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to start docker-compose"
    exit 1
}

# Wait a bit for databases to be ready
Start-Sleep -Seconds 30

# Run all service containers
foreach ($service in $services.Keys) {
    $port = $services[$service]
    $containerName = "connectsphere-$service"

    $existingContainerId = & docker container inspect --format "{{.Id}}" $containerName 2>$null
    if ($LASTEXITCODE -eq 0 -and $existingContainerId) {
        Write-Host "Removing existing container for $service..."
        & docker rm -f $containerName | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to remove existing container for $service"
            exit 1
        }
    }

    Write-Host "Running container for $service on port $port..."
    $dockerRunArgs = @("-d", "--name", $containerName, "--network", "host")
    if (Test-Path -LiteralPath $dotenvFile) {
        $dockerRunArgs += @("--env-file", $dotenvFile)
    }
    $dockerRunArgs += "connectsphere/$service"
    & docker run @dockerRunArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to run container for $service"
        exit 1
    }

    Start-Sleep -Seconds 3
    $isRunning = & docker container inspect --format "{{.State.Running}}" $containerName 2>$null
    if ($LASTEXITCODE -ne 0 -or $isRunning -ne "true") {
        Write-Error "$service exited immediately. Recent logs:"
        & docker logs --tail 80 $containerName
        exit 1
    }
}

Write-Host "All services are running!"
