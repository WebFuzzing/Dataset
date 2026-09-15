
repositories {
    mavenLocal()
    mavenCentral()
}


plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


val EVOMASTER_VERSION = project.ext.get("EVOMASTER_VERSION")

dependencies{
    implementation("org.evomaster:evomaster-client-java-controller:$EVOMASTER_VERSION")
    implementation("org.evomaster:evomaster-client-java-instrumentation:$EVOMASTER_VERSION")
    runtimeOnly("org.xerial:sqlite-jdbc:${libs.versions.sqliteJdbc.get()}")
}


tasks.shadowJar {
    archiveBaseName.set("${project.name}-evomaster-runner")
    archiveClassifier.set("")
    archiveVersion.set("")
    isZip64 = true
    manifest {
        attributes["Implementation-Title"] = "EM"
        attributes["Implementation-Version"] = "1.0"
        attributes["Main-Class"] = "em.external.komga.ExternalEvoMasterController"
        attributes["Premain-Class"] = "org.evomaster.client.java.instrumentation.InstrumentingAgent"
        attributes["Agent-Class"] = "org.evomaster.client.java.instrumentation.InstrumentingAgent"
        attributes["Can-Redefine-Classes"] = "true"
        attributes["Can-Retransform-Classes"] = "true"
    }
    relocate("org.slf4j", "shaded.emb.org.slf4j")
    relocate("com.fasterxml.jackson", "shaded.emb.com.fasterxml.jackson")
    mergeServiceFiles()
}

tasks {
    "build" {
        dependsOn(shadowJar)
    }
}
