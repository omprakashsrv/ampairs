// Root build file for Ampairs multi-module project
// This coordinates builds between different components

plugins {
    base
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.flywaydb.flyway") version "11.14.1" apply false
    kotlin("jvm") version "2.3.20" apply false
    kotlin("plugin.spring") version "2.3.20" apply false
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