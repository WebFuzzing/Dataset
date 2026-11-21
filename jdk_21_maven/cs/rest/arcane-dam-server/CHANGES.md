# Changes to `IroshanRathnayake/arcane-dam-server` for EMB Integration

Upstream repository: `https://github.com/IroshanRathnayake/arcane-dam-server` (version `0.0.1-SNAPSHOT`, Spring Boot `3.3.5`, Java `21`).

Because EvoMaster's DynamoDB white-box instrumentation (`DynamoDbClassReplacement`, `DynamoDbEnhancedClientClassReplacement`) and `ReflectionBasedDynamoDbClient` only support AWS SDK v2 (`software.amazon.awssdk`), the DynamoDB persistence layer was migrated from AWS SDK v1 (`DynamoDBMapper`) to AWS SDK v2 (`DynamoDbEnhancedClient`), and minimal local endpoint / security configuration hooks were added.

## 1. `pom.xml`
- Replaced the legacy full `com.amazonaws:aws-java-sdk:1.12.777` mega-bundle with targeted `com.amazonaws:aws-java-sdk-cognitoidp:1.12.777` (for `CognitoConfig` / `CognitoService`).
- Replaced `com.amazonaws:aws-java-sdk-dynamodb` and `io.github.boostchicken:spring-data-dynamodb` (unused `UserRepo` interface was removed) with `software.amazon.awssdk:dynamodb:2.29.17` and `software.amazon.awssdk:dynamodb-enhanced:2.29.17` (matching the existing `s3:2.29.17` version).
- Removed the duplicate `springdoc-openapi-starter-webmvc-ui` dependency declaration.
- Added `config` as a Maven `<resource>` under `<targetPath>benchmark</targetPath>` so the packaged JAR and embedded dependency can load `classpath:/benchmark/application.properties`.
- Configured `spring-boot-maven-plugin` (`<finalName>arcane-dam-server</finalName>`, `<classifier>sut</classifier>`, and `repackage` execution) so Maven produces both `target/arcane-dam-server-sut.jar` (executable fat JAR for the External controller and `dist/`) and `target/arcane-dam-server-0.0.1-SNAPSHOT.jar` (plain library JAR for the Embedded controller).

## 2. `config/application.properties` (added; upstream had no `application.properties`)
- Upstream `application.properties` was gitignored and absent from the repository. Added default benchmark properties (`aws.*`, `aws.s3.bucket-name=arcane-bucket`, Cognito/JWK placeholders, `OAuth2ClientAutoConfiguration` exclusion, `app.security.enabled=false`, `springdoc.api-docs.version=OPENAPI_3_0`).

## 3. AWS & Security Configuration
- `src/main/java/com/arcane/dam/config/aws/AWSConfig.java`: Migrated `buildAmazonDynamoDB()` bean from SDK v1 `AmazonDynamoDB` to SDK v2 `DynamoDbClient`.
- `src/main/java/com/arcane/dam/config/aws/DynamoDBConfig.java`: Migrated `DynamoDBMapper` bean to `DynamoDbEnhancedClient`.
- `src/main/java/com/arcane/dam/config/security/CognitoConfig.java`: Added optional `@Value("${aws.endpoint:}")` override for `AWSCognitoIdentityProvider` so local runs never attempt external AWS calls.
- `src/main/java/com/arcane/dam/config/security/SecurityConfig.java`: Added `@Value("${app.security.enabled:true}")` flag to permit all requests when `app.security.enabled=false`, and permitted `/v3/api-docs/**` and `/swagger-ui/**` when enabled.
- `src/main/java/com/arcane/dam/service/impl/AssetServiceImpl.java`: Added optional `@Value("${aws.endpoint:}")` and `@Value("${aws.region:us-east-1}")` properties with `.endpointOverride(...)` and `.forcePathStyle(true)` on `S3Client` for LocalStack compatibility.

## 4. Entities, Repositories & Cleanup (SDK v1 -> SDK v2)
- `src/main/java/com/arcane/dam/entity/AssetMetaData.java`, `Team.java`, `Users.java`, `Space.java`: Migrated annotations from `com.amazonaws.services.dynamodbv2.datamodeling.*` (`@DynamoDBTable`, `@DynamoDBHashKey`, `@DynamoDBAttribute`, `@DynamoDBDocument`, `@DynamoDBTypeConverted`) to `software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*` (`@DynamoDbBean`, `@DynamoDbPartitionKey`, `@DynamoDbAttribute` on explicit getters).
- `src/main/java/com/arcane/dam/repository/AssetRepository.java`, `AuthRepository.java`, `TeamRepository.java`, `UserRepository.java`: Migrated from `DynamoDBMapper` to `DynamoDbEnhancedClient` / `DynamoDbTable` (`putItem`, `getItem`, `deleteItem`, `scan`, and conditional `attribute_exists(...)` on updates; UUID generation on `save()` when `id` is null/blank; fixed `UserRepository.getUserByUserName` scan filter from `userName` to `username` to match the `@DynamoDbAttribute("username")` schema).
- `src/main/java/com/arcane/dam/util/InstantConverter.java` and `src/main/java/com/arcane/dam/dto/TeamDTO.java`: Removed unused AWS SDK v1 `com.amazonaws.services.dynamodbv2.*` imports (`Instant` is natively supported by AWS SDK v2 Enhanced Client).
- Removed unused `src/main/java/com/arcane/dam/repository/UserRepo.java` (referenced removed `org.socialsignin.spring.data.dynamodb` annotations and was not injected anywhere).

## 5. Excluded Upstream Files
Skipped non-runtime / VCS / local wrapper files when importing into `jdk_21_maven/cs/rest/arcane-dam-server`:
- `.git/`, `.gitignore`, `.gitattributes`, `.mvn/`, `mvnw`, `mvnw.cmd`, `docker-compose.yml`
