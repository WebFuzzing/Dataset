# Script to run Neo4j migration in debug mode
# This script runs the application with the 'migration' profile and 
# allows you to attach a debugger on port 5005.

$ENABLED_STEPS = "user"
$DRY_RUN = "true"
$EXIT_ON_COMPLETE = "false"

if ($args.Count -ge 1) { $ENABLED_STEPS = $args[0] }
if ($args.Count -ge 2) { $DRY_RUN = $args[1] }
if ($args.Count -ge 3) { $EXIT_ON_COMPLETE = $args[2] }

Write-Host "Starting migration debug session..."
Write-Host "Enabled steps: $ENABLED_STEPS"
Write-Host "Dry run: $DRY_RUN"
Write-Host "Exit on complete: $EXIT_ON_COMPLETE"
Write-Host "Attach your debugger to localhost:5005"

.\mvnw.cmd spring-boot:run `
    "-Dspring-boot.run.profiles=migration" `
    "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005" `
    "-Dspring-boot.run.arguments=--migration.enabled-steps=$ENABLED_STEPS --migration.dry-run=$DRY_RUN --migration.exit-on-complete=$EXIT_ON_COMPLETE"
