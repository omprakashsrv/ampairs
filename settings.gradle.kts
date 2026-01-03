rootProject.name = "ampairs"

include("core")
include("notification")
include("auth")
include("workspace")
include("form")
include("event")
include("file")
include("product")
include("business")
include("customer")
include("order")
include("invoice")
include("tax")
include("unit")
include("subscription")
include("ampairs_service")


// Include web frontend (Angular) - if using Gradle for build coordination
// includeBuild("ampairs-web")

// Include mobile app (Kotlin Multiplatform) - if using Gradle for build coordination  
// includeBuild("ampairs-mp-app")