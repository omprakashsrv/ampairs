plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "com.ampairs"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
kotlin {
    jvmToolchain(21)
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}



repositories {
    mavenCentral()
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
}

dependencies {
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:4.0.2"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    // AWS S3 for presigned URLs (app update downloads)
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")

    // Jackson Kotlin support for data class deserialization.
    // Spring Boot 4 runs Jackson 3 (tools.jackson) — the module MUST use the tools.jackson
    // coordinates or it never registers, Kotlin constructor defaults are ignored for missing
    // JSON fields, and requests fail with "Cannot map null into type int/long"
    // (Jackson 3 enables FAIL_ON_NULL_FOR_PRIMITIVES by default).
    implementation("tools.jackson.module:jackson-module-kotlin")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // Database
    runtimeOnly("com.mysql:mysql-connector-j")

    // XML Processing
    implementation("no.digipost.jaxb:jaxb2-jackson-helper:1.0.1")

    // Caching
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("javax.cache:cache-api:1.1.1")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")
    implementation("com.github.ben-manes.caffeine:jcache:3.2.4")

    // Rate limiting - using custom comprehensive rate limiting service (no external dependencies needed)

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("com.h2database:h2")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Disable bootJar since this is a library module
tasks.named("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("")
}

tasks.register("prepareKotlinBuildScriptModel") {}
