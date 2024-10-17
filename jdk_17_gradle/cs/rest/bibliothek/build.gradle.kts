plugins {
  id("java")

  alias(libs.plugins.indra)
  //alias(libs.plugins.indra.checkstyle)
  alias(libs.plugins.spotless)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.spring.boot)
}

group = "io.papermc"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

indra {
  javaVersions {
    target(17)
  }

  github("PaperMC", "bibliothek")
  mitLicense()
}

spotless {
  java {
    endWithNewline()
    importOrderFile(rootProject.file("cs/rest/bibliothek/.spotless/bibliothek.importorder"))
    indentWithSpaces(2)
    licenseHeaderFile(rootProject.file("cs/rest/bibliothek/license_header.txt"))
    trimTrailingWhitespace()
  }
}

dependencies {

  annotationProcessor("org.springframework.boot", "spring-boot-configuration-processor")
  //checkstyle(libs.stylecheck)
  implementation(libs.jetbrains.annotations)
  implementation(libs.springdoc.openapi.starter.webmvc.ui)
  implementation("org.springframework.boot", "spring-boot-starter-data-mongodb")
  implementation("org.springframework.boot", "spring-boot-starter-validation")
  implementation("org.springframework.boot", "spring-boot-starter-web")
  implementation("org.springframework.boot", "spring-boot-starter-security")
  implementation("io.jsonwebtoken:jjwt:0.12.6")
  implementation("io.jsonwebtoken:jjwt-api:0.12.6")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("org.springframework.boot", "spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  testImplementation("org.springframework.security", "spring-security-test")
}

tasks {
  bootJar {
    version = ""
    archiveClassifier.set("sut")
    launchScript()
  }

  jar{
    archiveClassifier.set("plain")
  }

  // From StackOverflow: https://stackoverflow.com/a/53087407
  // Licensed under: CC BY-SA 4.0
  // Adapted to Kotlin
  register<Copy>("buildForDocker") {
    from(bootJar)
    into("build/libs/docker")
    rename { fileName ->
      // a simple way is to remove the "-$version" from the jar filename
      // but you can customize the filename replacement rule as you wish.
      fileName.replace("-$version", "")
    }
  }
}
