
repositories {
    mavenLocal()
    mavenCentral()
}


plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val EVOMASTER_VERSION = project.ext.get("EVOMASTER_VERSION")

dependencies{
    implementation("org.evomaster:evomaster-client-java-controller:$EVOMASTER_VERSION")
    implementation("org.evomaster:evomaster-client-java-dependencies:$EVOMASTER_VERSION"){
        exclude("com.github.tomakehurst")
    }

    api(project(":cs:rest:traccar"))

    //Gradle api() is not importing transitive dependencies???
    implementation("com.google.inject:guice:7.0.0")

    // the EM controller's Jersey (WADL) needs a javax JAXB implementation, otherwise it fails at startup with a NPE
    runtimeOnly("org.glassfish.jaxb:jaxb-runtime:2.3.9")
}

// the SUT reads its Liquibase changelog and templates from the file system: shipped as resources, no copy
sourceSets {
    main {
        resources {
            srcDir("../../../../cs/rest/traccar")
            include("schema/**", "templates/**")
        }
    }
}
