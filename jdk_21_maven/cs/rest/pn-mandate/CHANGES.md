# Changes to `pagopa/pn-mandate` for EMB Integration

Upstream repository: `https://github.com/pagopa/pn-mandate` (version `2.14.1-SNAPSHOT`).

Only the minimal changes required to build in the EMB multi-module Maven tree and run against a non-default LocalStack S3 endpoint were made:

## 1. `pom.xml`
- Added `<relativePath>` to `<parent>` (`it.pagopa.pn:pn-parent:2.1.3-SPRINGBOOT3`) pointing to `../../../local-repo/it/pagopa/pn/pn-parent/2.1.3-SPRINGBOOT3/pn-parent-2.1.3-SPRINGBOOT3.pom`, and declared `<repositories>` pointing to `file://${project.basedir}/../../../local-repo` so PagoPA parent and internal dependencies resolve offline from the EMB local repository.
- Updated `it.pagopa.pn:pn-model` dependency version from `2.11.0-SPRINGBOOT3` to `2.13.0-SPRINGBOOT3` (matching the version built and available in `local-repo`).
- Added `<jacoco.min.line.cover.ratio>0.00</jacoco.min.line.cover.ratio>` so Maven builds in the multi-module reactor without failing on coverage enforcement.
- Added `config/application.properties` as a Maven `<resource>` under `<targetPath>benchmark</targetPath>` so the packaged JAR and embedded dependency can load it via `classpath:/benchmark/application.properties`.
- Configured `spring-boot-maven-plugin` (`<finalName>pn-mandate</finalName>`, `<classifier>sut</classifier>`, and `repackage` execution) so Maven produces both `target/pn-mandate-sut.jar` (executable fat JAR for the External controller and `dist/`) and `target/pn-mandate-2.14.1-SNAPSHOT.jar` (plain library JAR for the Embedded controller).

## 2. `src/main/java/it/pagopa/pn/mandate/config/PnMandateConfig.java`
- Injected `@Value("${aws.endpoint-url:}") private String endpointUrl` and updated `configureBuilder(...)` to call `builder.endpointOverride(URI.create(this.endpointUrl))` and `s3Client()` to call `clientBuilder.forcePathStyle(true)` when `endpointUrl` is non-blank.
- **Reason**: Upstream `PnMandateConfig.configureBuilder` only configures credentials and region, omitting `endpointOverride`, which causes `S3Client` (used eagerly at startup by `CieCheckerAdapterImpl` via `cieChecker.init()` to download `new_IT_MasterListCSCA.zip`) to target real AWS S3 instead of LocalStack.

## 3. Excluded Upstream Files
Skipped non-runtime / VCS / CI / IDE files when importing into `jdk_21_maven/cs/rest/pn-mandate`:
- `.git/`, `.github/`, `.gitignore`, `.gitattributes`, `.mvn/`, `mvnw`, `mvnw.cmd`, `.spectral.yml`, `CODEOWNERS`, IDE metadata
