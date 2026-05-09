param(
    [switch]$SkipBuild,
    [switch]$KeepDockerServiceContainers
)

# Starts databases, Elasticsearch, Redis, and RabbitMQ in Docker and runs each Spring Boot service
# locally in its own PowerShell window.

$ErrorActionPreference = "Stop"

$backendDir = $PSScriptRoot
$runnerDir = Join-Path $backendDir ".local-runners"
$dotenvLoaderPath = Join-Path $backendDir "scripts\Load-DotEnv.ps1"

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

$infrastructureContainers = @(
    "connectsphere-auth-db",
    "connectsphere-post-db",
    "connectsphere-follow-db",
    "connectsphere-comment-db",
    "connectsphere-like-db",
    "connectsphere-notification-db",
    "connectsphere-media-db",
    "connectsphere-search-db",
    "connectsphere-elasticsearch",
    "connectsphere-redis",
    "connectsphere-rabbitmq"
)

function Invoke-ComposeUp {
    if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
        & docker-compose up -d
    } else {
        & docker compose up -d
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to start infrastructure with Docker Compose"
        exit 1
    }
}

function Test-PortAvailable {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    return $null -eq $connection
}

function Stop-ProcessOnPort {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        $processId = $connection.OwningProcess
        if (-not $processId -or $processId -eq $PID) {
            continue
        }

        try {
            $process = Get-Process -Id $processId -ErrorAction Stop
            Write-Host "Stopping process $processId ($($process.ProcessName)) using port $Port..."
            Stop-Process -Id $processId -Force -ErrorAction Stop
        } catch {
            Write-Warning "Could not stop process $processId on port ${Port}: $($_.Exception.Message)"
        }
    }
}

function ConvertTo-PowerShellLiteral {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    return "'" + ($Value -replace "'", "''") + "'"
}

function Wait-ForInfrastructure {
    param(
        [int]$TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastStatusLine = ""

    do {
        $notReady = @()

        foreach ($container in $infrastructureContainers) {
            $status = & docker inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}" $container 2>$null
            if ($LASTEXITCODE -ne 0) {
                $notReady += "${container}:not-found"
                continue
            }

            if ($status -ne "healthy" -and $status -ne "running") {
                $notReady += "${container}:$status"
            }
        }

        if ($notReady.Count -eq 0) {
            Write-Host "Infrastructure is ready."
            return
        }

        $statusLine = $notReady -join ", "
        if ($statusLine -ne $lastStatusLine) {
            Write-Host "Still waiting on: $statusLine"
            $lastStatusLine = $statusLine
        }

        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)

    Write-Warning "Infrastructure was not fully healthy after $TimeoutSeconds seconds. Starting services anyway."
}

Set-Location -LiteralPath $backendDir

if (Test-Path -LiteralPath $dotenvLoaderPath) {
    . $dotenvLoaderPath
}

Write-Host "Starting infrastructure containers..."
Invoke-ComposeUp

if (-not $KeepDockerServiceContainers) {
    foreach ($service in $services.Keys) {
        $containerName = "connectsphere-$service"
        $containerId = & docker ps -aq --filter "name=$containerName" | Select-Object -First 1
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to check for Docker app container $containerName"
            exit 1
        }

        if ($containerId) {
            Write-Host "Removing Docker app container $containerName so local port $($services[$service]) is free..."
            & docker rm -f $containerName | Out-Null
            if ($LASTEXITCODE -ne 0) {
                Write-Error "Failed to remove $containerName"
                exit 1
            }
        }
    }
}

Write-Host "Waiting for infrastructure to be ready..."
Wait-ForInfrastructure

Write-Host "Stopping existing local service processes before build..."
foreach ($service in $services.Keys) {
    Stop-ProcessOnPort -Port $services[$service]
}
Start-Sleep -Seconds 2

if (-not $SkipBuild) {
    Write-Host "Cleaning and building all services with Maven..."
    & mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed"
        exit 1
    }
}

foreach ($service in $services.Keys) {
    $port = $services[$service]
    if (-not (Test-PortAvailable -Port $port)) {
        Write-Warning "$service uses port $port, but that port is already in use. The service may fail to start."
    }
}

if (-not (Test-Path -LiteralPath $runnerDir)) {
    New-Item -ItemType Directory -Path $runnerDir | Out-Null
}

$backendLiteral = ConvertTo-PowerShellLiteral -Value $backendDir
$dotenvLoaderLiteral = ConvertTo-PowerShellLiteral -Value $dotenvLoaderPath

foreach ($service in $services.Keys) {
    $port = $services[$service]
    $serviceLiteral = ConvertTo-PowerShellLiteral -Value $service
    $runnerPath = Join-Path $runnerDir "$service.ps1"

    $runnerScript = @"
`$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $backendLiteral
`$Host.UI.RawUI.WindowTitle = 'connectsphere-$service'
if (Test-Path -LiteralPath $dotenvLoaderLiteral) {
    . $dotenvLoaderLiteral
}
`$env:EUREKA_ENABLED = 'true'
`$env:EUREKA_DEFAULT_ZONE = 'http://localhost:8761/eureka/'
Write-Host 'Starting $service on port $port...'
Write-Host 'Backend directory: $backendDir'
Write-Host ''
& mvn -pl $serviceLiteral spring-boot:run "-Dspring-boot.run.workingDirectory=$backendDir"
if (`$LASTEXITCODE -ne 0) {
    Write-Host ''
    Write-Host '$service stopped with exit code' `$LASTEXITCODE -ForegroundColor Red
}
Write-Host ''
Read-Host 'Press Enter to close this window'
"@

    Set-Content -Path $runnerPath -Value $runnerScript -Encoding ascii

    Write-Host "Opening terminal for $service on port $port..."
    Start-Process powershell.exe -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-File", $runnerPath
    )

    Start-Sleep -Seconds 2
}

Write-Host "All microservice terminals have been opened."
Write-Host "Use Ctrl+C inside a service terminal to stop that service."
