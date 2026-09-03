
# check-databases.ps1
# Checks status of MySQL, MongoDB, and Neo4j


Write-Host "Database Health Check"
Write-Host "=====================================`n"

$services = @(
    @{
        Name = "MySQL"
        Container = "arimaadockermysqldb"
    },
    @{
        Name = "MongoDB"
        Container = "arimaadockermongodb"
    },
    @{
        Name = "Neo4j"
        Container = "arimaadockerneo4jdb"
    }
)

foreach ($svc in $services) {
    Write-Host "Checking $($svc.Name)..." -ForegroundColor Yellow

    Start-Sleep -Milliseconds 800

    $containerStatus = docker ps --filter "name=$($svc.Container)" --format "{{.Status}}" 2>$null

    if (-not $containerStatus) {
        Write-Host "   Container not found or not started yet" -ForegroundColor Red
    }
    elseif ($containerStatus -like "Up*") {
        Write-Host "   Container is running" -ForegroundColor Green

        switch ($svc.Name) {
            "MySQL" {
                $result = docker exec $svc.Container mysqladmin ping -h localhost -u root -p123456 --silent 2>&1 | Out-Null
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "   MySQL is responsive" -ForegroundColor Green
                } else {
                    Write-Host "   MySQL running but still starting up" -ForegroundColor Yellow
                }
            }
            "MongoDB" {
                $result = docker exec $svc.Container mongosh --eval "db.runCommand({ ping: 1 })" --quiet 2>&1
                if ($LASTEXITCODE -eq 0 -and $result) {
                    Write-Host "   MongoDB is responsive" -ForegroundColor Green
                } else {
                    Write-Host "   MongoDB running but still starting up" -ForegroundColor Yellow
                }
            }
            "Neo4j" {
                $result = docker exec $svc.Container cypher-shell -u neo4j -p arimaa123 "RETURN 'ready' AS status" 2>&1
                if ($LASTEXITCODE -eq 0 -and $result -like "*ready*") {
                    Write-Host "   Neo4j is responsive" -ForegroundColor Green
                } else {
                    Write-Host "   Neo4j running but still starting up" -ForegroundColor Yellow
                }
            }
        }
    }
    else {
        Write-Host "   Container exists but not running (Status: $containerStatus)" -ForegroundColor Red
    }

    Write-Host ""
}

Write-Host "Health check completed." -ForegroundColor Cyan