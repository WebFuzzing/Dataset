
repositories {
    mavenLocal()
    mavenCentral()
}


plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val EVOMASTER_VERSION = project.ext.get("EVOMASTER_VERSION")

dependencies{
    implementation("org.evomaster:evomaster-client-java-controller:$EVOMASTER_VERSION")
    implementation("org.evomaster:evomaster-client-java-dependencies:$EVOMASTER_VERSION"){
        exclude("com.github.tomakehurst")
    }

    api(project(":cs:rest:komga"))
}
