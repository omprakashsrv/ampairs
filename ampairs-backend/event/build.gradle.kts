plugins {
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.2.0"
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

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

extra.apply {
    set("jakarta-servlet.version", "5.0.0")
}

repositories {
    mavenCentral()
}

allOpen {
    annotation("jakarta.persistence.Entity")
}

dependencies {
    api(project(mapOf("path" to ":core")))
    api(project(mapOf("path" to ":auth")))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    compileOnly("org.springframework.data:spring-data-rest-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // WebSocket support
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-messaging")

    // RabbitMQ for distributed WebSocket with STOMP relay
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("io.projectreactor.netty:reactor-netty")

    // Jackson for JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    runtimeOnly("com.mysql:mysql-connector-j")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}

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
