import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.spring") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.1.10"
}

group = "com.ampairs"
version = ""
java.sourceCompatibility = JavaVersion.VERSION_17

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

dependencies {
    api(project(mapOf("path" to ":core")))
//    implementation(project(mapOf("path" to ":core_user")))
    api(project(mapOf("path" to ":invoice")))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("io.hypersistence:hypersistence-utils-hibernate-62:3.5.2")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
    runtimeOnly("com.mysql:mysql-connector-j")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("prepareKotlinBuildScriptModel") {}

tasks.withType<BootJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
