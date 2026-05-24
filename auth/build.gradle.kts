plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
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



repositories {
    mavenCentral()
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    api(project(mapOf("path" to ":core")))
    api(project(mapOf("path" to ":notification")))
    api(project(mapOf("path" to ":file")))

    val springCloudAwsVersion = "4.0.2"
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:${springCloudAwsVersion}"))


    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.data:spring-data-rest-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("no.digipost.jaxb:jaxb2-jackson-helper:1.0.1")

    implementation("io.awspring.cloud:spring-cloud-aws-starter-sns")

    // Firebase Admin SDK for authentication verification
    implementation("com.google.firebase:firebase-admin:9.9.0")

    // OpenAPI/Swagger Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")

    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    runtimeOnly("com.mysql:mysql-connector-j")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(project(":ampairs_service"))
    testImplementation("com.mysql:mysql-connector-j")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("com.h2database:h2")
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
