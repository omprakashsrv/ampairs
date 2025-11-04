// Root build file for Ampairs multi-module project
// This coordinates builds between different components

plugins {
    base // Provides clean and other basic tasks
}

// Project information
group = "com.ampairs"
version = "1.0.0"

// Configure all subprojects
subprojects {
    group = "com.ampairs"
    version = "1.0.0"
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