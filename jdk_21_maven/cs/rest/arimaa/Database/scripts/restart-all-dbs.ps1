# =============================================
# restart-all-dbs.ps1
# Starts ALL three databases reliably
# =============================================

Write-Host "Restarting all 3 databases..." -ForegroundColor Cyan
Write-Host "=====================================`n"

# Stop everything first (clean state)
Write-Host "Stopping all containers..." -ForegroundColor Yellow
docker compose --env-file .env -f .\Database\docker-compose.mysql.yml down -v --remove-orphans
docker compose --env-file .env -f .\Database\neo4j\docker-compose.neo4j.yml down -v --remove-orphans
docker compose --env-file .env -f .\Database\docker-compose.mongodb.yml down -v --remove-orphans

Write-Host "`nStarting databases..." -ForegroundColor Green

# Start them in a good order
docker compose --env-file .env -f .\Database\docker-compose.mysql.yml up -d
Start-Sleep -Seconds 3
docker compose --env-file .env -f .\Database\docker-compose.mongodb.yml up -d
Start-Sleep -Seconds 2
docker compose --env-file .env -f .\Database\neo4j\docker-compose.neo4j.yml up -d

Write-Host "`nAll databases started." -ForegroundColor Green
Write-Host "Waiting 10 seconds for full initialization..." -ForegroundColor Gray
Start-Sleep -Seconds 10

# Run health check
.\Database\scripts\check-databases.ps1

