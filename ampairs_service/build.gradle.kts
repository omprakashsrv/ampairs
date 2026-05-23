buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:11.14.1")
        classpath("org.postgresql:postgresql:42.7.10")
    }
}

plugins {
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.flywaydb.flyway") version "11.14.1"
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.spring") version "2.3.20"
}

group = "com.ampairs"
version = ""
java.sourceCompatibility = JavaVersion.VERSION_21
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
kotlin {
    jvmToolchain(21)
}



repositories {
    mavenCentral()
}

allOpen {
    annotation("jakarta.persistence.Entity")
}

dependencies {
    // Project modules
    implementation(project(mapOf("path" to ":core")))
    implementation(project(mapOf("path" to ":notification")))
    implementation(project(mapOf("path" to ":auth")))
    implementation(project(mapOf("path" to ":workspace")))
    implementation(project(mapOf("path" to ":business")))
    implementation(project(mapOf("path" to ":form")))
    implementation(project(mapOf("path" to ":event")))
    implementation(project(mapOf("path" to ":customer")))
    implementation(project(mapOf("path" to ":file")))
    implementation(project(mapOf("path" to ":unit")))
    implementation(project(mapOf("path" to ":product")))
    implementation(project(mapOf("path" to ":order")))
    implementation(project(mapOf("path" to ":invoice")))
    implementation(project(mapOf("path" to ":tax")))
    implementation(project(mapOf("path" to ":subscription")))

    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Spring Retry
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")

    // Caching
    val caffeine = "3.2.4"
    implementation("javax.cache:cache-api:1.1.1")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeine")
    implementation("com.github.ben-manes.caffeine:jcache:$caffeine")

    // JWT
    val jwt = "0.13.0"
    implementation("io.jsonwebtoken:jjwt-api:$jwt")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwt")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwt")

    // Rate limiting - using custom comprehensive rate limiting service from core module

    // Database & Migrations
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-mysql")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Spring Cloud AWS - Auto-configuration for AWS services
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:4.0.2"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sns")

    // Jackson for JSON processing
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")

    // OpenAPI/Swagger Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.21.0"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("com.h2database:h2")
    implementation(kotlin("stdlib"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val migrationModules = listOf(
    "auth", "business", "core", "customer", "event", "form",
    "invoice", "notification", "order", "product", "subscription",
    "tax", "unit", "workspace"
)

flyway {
    url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/springdb"
    user = System.getenv("DB_USERNAME") ?: "springuser"
    password = System.getenv("DB_PASSWORD") ?: "springpass"
    locations = migrationModules
        .map { "filesystem:${rootDir}/$it/src/main/resources/db/migration/postgresql" }
        .toTypedArray()
    baselineOnMigrate = true
    baselineVersion = "1"
    outOfOrder = false
}
