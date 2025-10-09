plugins {
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.2.20"
}

group = "com.ampairs"
version = ""
java.sourceCompatibility = JavaVersion.VERSION_25
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
kotlin {
    jvmToolchain(25)
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}



allOpen {
    annotation("jakarta.persistence.Entity")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(mapOf("path" to ":core")))
    api(project(mapOf("path" to ":event")))
    compileOnly("org.springframework.boot:spring-boot-starter-data-rest")
    compileOnly("org.springframework.data:spring-data-rest-webmvc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.hypersistence:hypersistence-utils-hibernate-62:3.5.2")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("no.digipost.jaxb:jaxb2-jackson-helper:1.0.1")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    runtimeOnly("com.mysql:mysql-connector-j")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    testImplementation(project(":ampairs_service"))
    testImplementation("com.h2database:h2")}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("prepareKotlinBuildScriptModel") {}

// Disable bootJar since this module doesn't have a main class
tasks.named("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("")
}
