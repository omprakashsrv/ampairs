// Root build file for Ampairs multi-module project
// This coordinates builds between different components

plugins {
    base // Provides clean and other basic tasks
}

// Project information
group = "com.ampairs"
version = "1.0.0"

// Global tasks for the entire project
tasks.register("buildAll") {
    description = "Build all project components"
    group = "build"
    
    dependsOn(gradle.includedBuild("ampairs-backend").task(":ampairs_service:bootJar"))
    
    doLast {
        println("✅ All components built successfully!")
        println("📦 Backend JAR: ampairs-backend/ampairs_service/build/libs/")
    }
}

tasks.register("testAll") {
    description = "Run tests for all project components"
    group = "verification"
    
    dependsOn(gradle.includedBuild("ampairs-backend").task(":test"))
    
    doLast {
        println("✅ All tests completed!")
    }
}

tasks.register("cleanAll") {
    description = "Clean all project components"
    group = "build"
    
    dependsOn(gradle.includedBuild("ampairs-backend").task(":clean"))
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
        📁 ampairs-backend/     - Spring Boot backend services
        📁 ampairs-web/         - Angular web application  
        📁 ampairs-mp-app/      - Kotlin Multiplatform mobile app
        
        Available Commands:
        • ./gradlew buildAll    - Build all components
        • ./gradlew testAll     - Run all tests
        • ./gradlew cleanAll    - Clean all components
        • ./gradlew ciBuild     - CI/CD build with tests
        
        Backend Development:
        • cd ampairs-backend && ./gradlew bootRun
        
        For detailed setup instructions, see DEPLOYMENT.md
        """.trimIndent())
    }
}

// Default task information
defaultTasks("devSetup")