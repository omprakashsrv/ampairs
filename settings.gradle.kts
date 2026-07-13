rootProject.name = "ampairs"

include("core")
include("notification")
include("auth")
include("user")
include("workspace")
include("form")
include("event")
include("file")
include("product")
include("business")
include("customer")
include("supplier")
include("order")
include("invoice")
include("purchase")
include("payment")
include("pricing")
include("tax")
include("unit")
include("setting")
include("printing")
include("agent")
include("sequence")
include("subscription")
include("ecom")
include("sfa")
include("trade")
include("dms")
include("claim")
include("ampairs_service")


// Include web frontend (Angular) - if using Gradle for build coordination
// includeBuild("ampairs-web")

// Include mobile app (Kotlin Multiplatform) - if using Gradle for build coordination  
// includeBuild("ampairs-mp-app")