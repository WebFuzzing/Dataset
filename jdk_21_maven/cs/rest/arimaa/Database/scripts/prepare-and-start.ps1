# =============================================
# prepare-and-start.ps1
# Restarts databases, performs migrations, and starts the program
# =============================================

$ErrorActionPreference = "Continue" # Don't stop on warnings from native commands

# 1. Restart the databases
Write-Host "Step 1: Restarting all databases..." -ForegroundColor Cyan
& ".\Database\scripts\restart-all-dbs.ps1"

# 2. Perform migrations
Write-Host "`nStep 2: Performing migrations..." -ForegroundColor Cyan
& ".\mvnw.cmd" spring-boot:run "-Dspring-boot.run.profiles=migration"

# 3. Start the program
Write-Host "`nStep 3: Starting the program..." -ForegroundColor Cyan
& ".\mvnw.cmd" spring-boot:run
