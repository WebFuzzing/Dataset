
repositories {
    mavenLocal()
    mavenCentral()
}


plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}


val EVOMASTER_VERSION = project.ext.get("EVOMASTER_VERSION")

dependencies{
    implementation("org.evomaster:evomaster-client-java-controller:$EVOMASTER_VERSION")
    implementation("org.evomaster:evomaster-client-java-instrumentation:$EVOMASTER_VERSION")

    // same version as the SUT, for the TCP server the SUT connects to
    implementation("com.h2database:h2:2.5.252")
}

// the SUT reads its Liquibase changelog and templates from the file system: shipped in the runner jar, no copy
sourceSets {
    main {
        resources {
            srcDir("../../../../cs/rest/traccar")
            include("openapi.yaml", "schema/**", "templates/**")
        }
    }
}


tasks.shadowJar {
    archiveBaseName.set("${project.name}-evomaster-runner")
    archiveClassifier.set("")
    archiveVersion.set("")
    isZip64 = true
    manifest {
        attributes["Implementation-Title"] = "EM"
        attributes["Implementation-Version"] = "1.0"
        attributes["Main-Class"] = "em.external.traccar.ExternalEvoMasterController"
        attributes["Premain-Class"] = "org.evomaster.client.java.instrumentation.InstrumentingAgent"
        attributes["Agent-Class"] = "org.evomaster.client.java.instrumentation.InstrumentingAgent"
        attributes["Can-Redefine-Classes"] = "true"
        attributes["Can-Retransform-Classes"] = "true"
    }
    // the runner jar is also the SUT's -javaagent: unrelocated slf4j/jackson shadow the SUT's own
    relocate("org.slf4j", "shaded.emb.org.slf4j")
    relocate("com.fasterxml.jackson", "shaded.emb.com.fasterxml.jackson")
    mergeServiceFiles()
}

tasks {
    "build" {
        dependsOn(shadowJar)
    }
}
