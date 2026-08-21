# Jasper Load Testing

This module contains comprehensive load testing for the Jasper application using Gatling.

## Overview

The load testing has been separated into its own module to isolate Gatling dependencies from the main application. This resolves Netty version conflicts that occur when upgrading Gatling.

## Test Scenarios

This module now includes four comprehensive test scenarios:

### 1. SimpleJasperSimulation
Basic smoke test that verifies core functionality:
- Basic CRUD operations on references
- Extension and plugin management
- User and system health checks
- Quick validation of all major components

### 2. ComprehensiveJasperSimulation  
Realistic knowledge management workflows:
- Knowledge workers creating and organizing content
- Content browsers searching and consuming information
- Administrators managing system configuration
- Heavy readers with intensive content consumption
- Power users with mixed operations including content enrichment

### 3. UserJourneySimulation
Real-world user journey patterns:
- **Research Sessions**: Users researching topics, saving references, taking notes
- **Daily Reviews**: Users checking recent updates and organizing content  
- **Content Curation**: Users creating and managing templates and plugins
- **Collaborative Work**: Multiple users working on shared content
- Includes realistic data feeders for topics and sources

### 4. StressTestSimulation
System limits and edge case testing:
- High-volume concurrent operations
- Large payloads and complex queries
- Error conditions and recovery scenarios
- Replication and backup operations
- System administration under load
- **Retry Logic**: All requests automatically retry up to 3 times on 429 (Too Many Requests) and 503 (Service Unavailable) errors

### 5. InfernoSimulation
Extreme load testing to push system to absolute limits:
- 15 second warmup trickle to let the system initialize
- One cycle of massive load (50,000 users ramped in 10 seconds, then 5 users/sec for 50 seconds)
- One cycle of massive load (5,000 users ramped in 10 seconds, then 5 users/sec for 50 seconds)
- 2 minute 15 second total duration
- Tests extreme concurrent operations and system recovery
- **Very Low Success Threshold**: Only >15% success rate required (85% failure tolerance)

## Running Load Tests

To run all scenarios:

```bash
cd gatling
mvn gatling:test
```

To run from the root directory:

```bash
mvn -f gatling/pom.xml gatling:test
```

To run a specific simulation:

```bash
mvn gatling:test -Dgatling.simulationClass=simulations.jasper.SimpleJasperSimulation
```

To run the Inferno extreme load test:

```bash
mvn gatling:test -Dgatling.simulationClass=simulations.jasper.InfernoSimulation
```

## Docker

The load tests can also be run via Docker using the `gatling` stage:

```bash
docker build --target gatling -t jasper-gatling .
docker run jasper-gatling
```

Or run a specific test with environment variable:

```bash
docker build --target test -t jasper-gatling .
docker run -e GATLING_TEST=Inferno jasper-gatling
```

Or using the docker-compose file:

```bash
docker compose --profile lt -f gatling/docker-compose.yaml up --build --exit-code-from gatling
```

## Configuration

- **Gatling Version**: 3.14.5
- **Target URL**: http://localhost:8081 (configurable in simulation files)
- **Simulations**: Located in `src/test/java/simulations/jasper/`

## Performance Expectations

Each simulation includes assertions and performance expectations:

- **SimpleJasperSimulation**: Quick smoke test (45 seconds), >85% success rate
- **ComprehensiveJasperSimulation**: Comprehensive test (3 minutes), >80% success rate
- **UserJourneySimulation**: Realistic workflows (5 minutes), >75% success rate  
- **StressTestSimulation**: System limits test (4 minutes), >70% success rate
- **InfernoSimulation**: Extreme load test (2 minutes 15 seconds), >15% success rate

## Adding New Tests

Create new simulation classes in `src/test/java/simulations/jasper/` that extend `Simulation` from Gatling.

## Real-World Scenarios Included

The enhanced test suite includes:
- Knowledge workers adding references with metadata
- Search and filtering operations with complex queries
- Plugin and template management workflows
- Graph relationship queries
- Content enrichment via proxy and scraping
- User management and authentication flows
- Backup and replication operations
- Error handling and edge cases
- Concurrent operations and race conditions

## Dependencies

This module uses its own dependencies isolated from the main application:
- Gatling Highcharts 3.14.5
- Netty 4.2.6.Final (via Gatling)

This isolation prevents dependency conflicts with the main Spring Boot application.
