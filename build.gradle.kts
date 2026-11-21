// Root build file for Ampairs multi-module project
// This coordinates builds between different components

plugins {
    base
    jacoco   // root needs it too, so the aggregate JacocoReport task gets its jacocoClasspath
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.flywaydb.flyway") version "12.11.0" apply false
    kotlin("jvm") version "2.4.10" apply false
    kotlin("plugin.spring") version "2.4.0" apply false
}

// Project information
group = "com.ampairs"
version = "1.0.0"

// Configure all subprojects
subprojects {
    group = "com.ampairs"
    version = "1.0.0"

    // Opt into Kotlin 2.x annotation default target (param-property): applies annotations on
    // constructor parameters to both the parameter and the generated backing field/property,
    // which is correct for Spring/Jackson annotations and silences KT-73255 warnings.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    // Override Spring Boot 4.0's managed testcontainers version (2.0.5 sub-modules don't
    // all exist on Maven Central yet). Pin to the last stable 1.x release instead.
    extra["testcontainers.version"] = "1.21.0"

    // ── Code coverage (JaCoCo) ───────────────────────────────────────────────
    // Every JVM module gets coverage. The jacoco plugin only materialises its
    // report task once the java plugin is present (each module applies kotlin("jvm")),
    // so the lazy withType {} configuration below attaches whenever that happens.
    apply(plugin = "jacoco")
    the<JacocoPluginExtension>().toolVersion = "0.8.13"
    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)   // machine-readable — consumed by CI publishing
            html.required.set(true)  // human-readable — uploaded as a CI artifact
        }
    }
    // `./gradlew test` also refreshes that module's coverage report.
    tasks.withType<Test>().configureEach {
        finalizedBy(tasks.withType<JacocoReport>())
    }
}

// ── Project-wide aggregated coverage ─────────────────────────────────────────
// ./gradlew codeCoverageReport
//   → build/reports/jacoco/aggregate/coverage.xml  (consumed by CI publishing)
//   → build/reports/jacoco/aggregate/html/         (uploaded as a CI artifact)
//
// Deliberately file-based (exec / classes / sources) rather than the
// `jacoco-report-aggregation` plugin: that plugin resolves each module's runtime
// classpath from the root, where Spring-BOM-managed deps (spring-kafka, etc.) have
// no version and fail to resolve. Reading files sidesteps classpath resolution.
tasks.register<JacocoReport>("codeCoverageReport") {
    group = "verification"
    description = "Aggregated JaCoCo coverage across all modules."

    // Run every module's tests (writes build/jacoco/test.exec) and its per-module
    // report. Depending on both keeps Gradle's implicit-dependency validation happy,
    // since this task reads files those tasks produce.
    dependsOn(subprojects.map { "${it.path}:test" })
    dependsOn(subprojects.map { "${it.path}:jacocoTestReport" })

    executionData.setFrom(
        fileTree(rootDir) { include("**/build/jacoco/test.exec") }
    )
    classDirectories.setFrom(
        files(subprojects.map { proj ->
            // Kotlin/JVM main output; exclude generated/boilerplate from the denominator.
            fileTree(proj.layout.buildDirectory.dir("classes/kotlin/main")) {
                exclude("**/config/**", "**/*Application*", "**/generated/**")
            }
        })
    )
    sourceDirectories.setFrom(
        files(subprojects.map { it.layout.projectDirectory.dir("src/main/kotlin") })
    )

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/aggregate/coverage.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregate/html"))
    }
}

// Global tasks for the entire project
tasks.register("buildAll") {
    description = "Build all project components"
    group = "build"

    dependsOn(":ampairs_service:bootJar")

    doLast {
        println("✅ All components built successfully!")
        println("📦 Backend JAR: ampairs_service/build/libs/")
    }
}

tasks.register("testAll") {
    description = "Run tests for all project components"
    group = "verification"

    // Test all subprojects
    subprojects.forEach { project ->
        dependsOn("${project.path}:test")
    }

    doLast {
        println("✅ All tests completed!")
    }
}

tasks.register("cleanAll") {
    description = "Clean all project components"
    group = "build"

    // Clean all subprojects
    subprojects.forEach { project ->
        dependsOn("${project.path}:clean")
    }
    dependsOn(tasks.clean)

    doLast {
        println("🧹 All components cleaned!")
    }
}

// Task for CI/CD pipeline
tasks.register("ciBuild") {
    description = "Build for CI/CD pipeline"
    group = "build"

    dependsOn("testAll")
    dependsOn("buildAll")

    // Ensure tests run before build
    tasks.findByName("buildAll")?.mustRunAfter("testAll")
}

// Development helper tasks
tasks.register("devSetup") {
    description = "Setup development environment"
    group = "help"

    doLast {
        println("""
        🚀 Ampairs Development Setup
        ============================

        Project Structure:
        📁 Root project modules - Spring Boot backend services
          ├─ core/              - Core utilities and multi-tenancy
          ├─ auth/              - Authentication & JWT
          ├─ workspace/         - Workspace & permissions
          ├─ business/          - Business management
          ├─ customer/          - Customer management
          ├─ product/           - Product & inventory
          ├─ order/             - Order processing
          ├─ invoice/           - Invoice generation
          ├─ unit/              - Unit conversions
          ├─ tax/               - Tax calculations
          ├─ form/              - Dynamic forms
          ├─ event/             - Event system
          ├─ file/              - File storage
          ├─ notification/      - Notifications
          └─ ampairs_service/   - Main application

        Available Commands:
        • ./gradlew buildAll              - Build all components
        • ./gradlew testAll               - Run all tests
        • ./gradlew cleanAll              - Clean all components
        • ./gradlew ciBuild               - CI/CD build with tests
        • ./gradlew :ampairs_service:bootRun - Run the application

        For detailed setup instructions, see DEPLOYMENT.md
        """.trimIndent())
    }
}

// Default task information
defaultTasks("devSetup")
repositories {
    mavenCentral()
}