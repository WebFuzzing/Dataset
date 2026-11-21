# Changes to `wcmc-its/ReCiter` for EMB Integration

Upstream repository: `https://github.com/wcmc-its/ReCiter` (version `3.0.0`).

Only the minimal changes required to build in the EMB multi-module Maven tree and start cleanly without requiring external environment variables were made:

## 1. `pom.xml`
- Added `<relativePath/>` to `<parent>` (`org.springframework.boot:spring-boot-starter-parent:3.4.3`) so Maven resolves the parent POM from repositories rather than looking at the EMB parent `cs/rest/pom.xml`.
- Configured `spring-boot-maven-plugin` with `<finalName>reciter</finalName>` and `<classifier>sut</classifier>` so Maven builds both `target/reciter-sut.jar` (executable fat JAR for the External controller and `dist/`) and `target/reciter-3.0.0.jar` (plain JAR library dependency for the Embedded controller).
- Commented out `maven-antrun-plugin` (which unzipped/re-zipped `${project.build.finalName}.jar` to inject Elastic Beanstalk `.ebextensions/`) and `maven-dependency-plugin` (`copy` task extracting sqlite native libs into `native-libs/`).

## 2. `src/main/java/reciter/consumer/service/CognitoAuthService.java`
- Updated `AWS_REGION` initialization (`System.getenv("AWS_REGION")`) to fall back to `"us-east-1"` when `AWS_REGION` is null or blank.
- **Reason**: `CognitoAuthService` is a Spring `@Service` instantiated eagerly at startup, constructing `CognitoIdentityProviderClient.builder().region(Region.of(AWS_REGION)).build()`. Without this fallback, `Region.of(null)` throws `NullPointerException: region must not be null.` when the child SUT JVM runs without `AWS_REGION` set in the OS environment.

## 3. Excluded Upstream Files (not part of the buildable Java service)
Skipped non-runtime / oversized / VCS files when importing into `jdk_21_maven/cs/rest/reciter`:
- `.git/`, `.github/`, `.ebextensions/`, `.gitignore`, `.dockerignore`
- Root `files/` directory (>100 MB of demo `.mov`/`.gif` videos; runtime JSON resources under `src/main/resources/files/` were preserved intact)
- `docs/`, `kubernetes/`, `native-libs/`, `nginx/`, `scripts/`, `Dockerfile`, `docker-compose.yaml`, `buildspec.yml`, `k8-buildspec.yml`, `wikinotes.md`
