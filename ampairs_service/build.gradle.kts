plugins {
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.spring") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.1.10"
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
    implementation(project(mapOf("path" to ":core")))
    implementation(project(mapOf("path" to ":auth")))
    implementation(project(mapOf("path" to ":company")))
    implementation(project(mapOf("path" to ":customer")))
    implementation(project(mapOf("path" to ":product")))
    implementation(project(mapOf("path" to ":order")))
    implementation(project(mapOf("path" to ":invoice")))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")


    val caffeine = "3.2.0"
    implementation("javax.cache:cache-api:1.1.1")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeine")
    implementation("com.github.ben-manes.caffeine:jcache:$caffeine")


    val jwt = "0.11.5"
    implementation("io.jsonwebtoken:jjwt-api:$jwt")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwt")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwt")
    runtimeOnly("com.mysql:mysql-connector-j")

    val bucket4j = "0.9.0"
    implementation("com.giffing.bucket4j.spring.boot.starter:bucket4j-spring-boot-starter:$bucket4j")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}