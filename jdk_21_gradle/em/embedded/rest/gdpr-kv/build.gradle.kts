
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

    api(project(":cs:rest:gdpr-kv"))

    //Gradle api() is not importing transitive dependencies???
    //the BOMs also keep slf4j/jackson on the versions the SUT expects
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.2"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(platform("software.amazon.awssdk:bom:2.25.67"))
    implementation("software.amazon.awssdk:dynamodb")
}
