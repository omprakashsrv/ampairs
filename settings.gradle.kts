rootProject.name = "ampairs"

// Include backend Spring Boot modules
includeBuild("ampairs-backend")

// Include web frontend (Angular) - if using Gradle for build coordination
// includeBuild("ampairs-web")

// Include mobile app (Kotlin Multiplatform) - if using Gradle for build coordination  
// includeBuild("ampairs-mp-app")