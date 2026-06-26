import java.net.URLClassLoader

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "com.ampairs"
// Inherit the version from the root build (subprojects { version = "1.0.0" }). Do NOT pin
// it to "" here — bootBuildInfo publishes project.version to /api/actuator/info as
// build.version, which the deploy pipeline reads to confirm what is live.
version = rootProject.version
java.sourceCompatibility = JavaVersion.VERSION_21
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

// ── Deployed-version visibility ────────────────────────────────────────────────
// `bootBuildInfo` writes META-INF/build-info.properties into the jar; Spring Boot's
// InfoContributor then surfaces it at GET /api/actuator/info as `build.*`. The
// deploy pipeline (and the server-side pull agent) reads `build.commit` from that
// endpoint to confirm which commit is actually running in production — no SSH needed.
springBoot {
    buildInfo {
        properties {
            // `additional` entries appear under build.* in the /actuator/info payload.
            additional.put("commit", resolveGitCommit())
        }
    }
}

// Resolve the git commit being built: CI exports GITHUB_SHA; locally fall back to
// `git rev-parse`. Returns "unknown" if neither is available (e.g. a source tarball).
fun resolveGitCommit(): String =
    System.getenv("GITHUB_SHA")?.takeIf { it.isNotBlank() }
        ?: runCatching {
            ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(rootDir)
                .redirectErrorStream(true)
                .start()
                .inputStream.bufferedReader().use { it.readText() }
                .trim()
        }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: "unknown"

dependencies {
    // Project modules
    implementation(project(mapOf("path" to ":core")))
    implementation(project(mapOf("path" to ":notification")))
    implementation(project(mapOf("path" to ":auth")))
    implementation(project(mapOf("path" to ":user")))
    implementation(project(mapOf("path" to ":workspace")))
    implementation(project(mapOf("path" to ":business")))
    implementation(project(mapOf("path" to ":form")))
    implementation(project(mapOf("path" to ":event")))
    implementation(project(mapOf("path" to ":customer")))
    implementation(project(mapOf("path" to ":file")))
    implementation(project(mapOf("path" to ":unit")))
    implementation(project(mapOf("path" to ":setting")))
    implementation(project(mapOf("path" to ":sequence")))
    implementation(project(mapOf("path" to ":product")))
    implementation(project(mapOf("path" to ":order")))
    implementation(project(mapOf("path" to ":invoice")))
    implementation(project(mapOf("path" to ":payment")))
    implementation(project(mapOf("path" to ":tax")))
    implementation(project(mapOf("path" to ":subscription")))
    implementation(project(mapOf("path" to ":ecom")))
    implementation(project(mapOf("path" to ":printing")))
    implementation(project(mapOf("path" to ":agent")))

    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Spring Retry
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")

    // JWT
    val jwt = "0.13.0"
    implementation("io.jsonwebtoken:jjwt-api:$jwt")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwt")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwt")

    // Database & Migrations
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("org.postgresql:postgresql")
    // Spring Boot 4.x requires the starter for Flyway auto-configuration (flyway-core alone is not enough)
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    // Flyway 10+ split MySQL support into its own module — without it a MySQL DB_URL fails at startup
    implementation("org.flywaydb:flyway-mysql")

    // Spring Cloud AWS
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:4.0.2"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sns")

    // Jackson
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // OpenAPI/Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("com.h2database:h2")
    implementation(kotlin("stdlib"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val migrationModules = listOf(
    "auth", "business", "core", "customer", "ecom", "event", "file", "form",
    "invoice", "notification", "order", "payment", "printing", "product", "sequence", "setting", "subscription",
    "tax", "unit", "user", "workspace"
)

// ── Local-dev migration tasks ──────────────────────────────────────────────────
// The Flyway Gradle plugin has a classloader bug in Gradle 9.x (flyway/flyway#4165).
// These tasks bypass the plugin entirely: a URLClassLoader we fully control
// ensures ServiceLoader finds flyway-database-postgresql every time.
// In production, Spring Boot runs Flyway on startup via spring-boot-starter-flyway.

val flywayRuntime by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    flywayRuntime("org.flywaydb:flyway-core:12.9.0")
    flywayRuntime("org.flywaydb:flyway-database-postgresql:12.9.0")
    flywayRuntime("org.flywaydb:flyway-mysql:12.9.0")
    flywayRuntime("org.postgresql:postgresql:42.7.11")
    flywayRuntime("com.mysql:mysql-connector-j:9.7.0")
    flywayRuntime("org.slf4j:slf4j-simple:2.0.18")
}

fun runFlyway(command: String) {
    val dbUrl      = System.getenv("DB_URL")      ?: "jdbc:postgresql://localhost:5432/springdb"
    val dbUser     = System.getenv("DB_USERNAME") ?: "springuser"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "springpass"
    // Pick the vendor migration directory from the JDBC URL (mirrors Spring's {vendor} placeholder).
    val vendor = if (dbUrl.startsWith("jdbc:mysql:")) "mysql" else "postgresql"
    val locations: Array<String> = migrationModules
        .map { "filesystem:${rootDir}/$it/src/main/resources/db/migration/$vendor" }
        .toTypedArray()

    val urls = flywayRuntime.map { it.toURI().toURL() }.toTypedArray()
    val cl = URLClassLoader(urls, ClassLoader.getPlatformClassLoader())
    val saved = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = cl
    try {
        val flywayClass: Class<*> = cl.loadClass("org.flywaydb.core.Flyway")
        var cfg: Any = flywayClass.getMethod("configure", ClassLoader::class.java).invoke(null, cl)!!
        cfg = cfg.javaClass.getMethod("dataSource", String::class.java, String::class.java, String::class.java)
            .invoke(cfg, dbUrl, dbUser, dbPassword)!!
        cfg = cfg.javaClass.getMethod("locations", Array<String>::class.java)
            .invoke(cfg, locations as Any)!!
        cfg = cfg.javaClass.getMethod("baselineOnMigrate", Boolean::class.javaPrimitiveType!!)
            .invoke(cfg, true)!!
        cfg = cfg.javaClass.getMethod("baselineVersion", String::class.java)
            .invoke(cfg, "1")!!
        val flyway: Any = cfg.javaClass.getMethod("load").invoke(cfg)!!
        try {
            when (command) {
                "migrate"  -> flyway.javaClass.getMethod("migrate").invoke(flyway)
                "validate" -> flyway.javaClass.getMethod("validate").invoke(flyway)
                "repair"   -> flyway.javaClass.getMethod("repair").invoke(flyway)
                "info"     -> {
                    val info: Any = flyway.javaClass.getMethod("info").invoke(flyway)!!
                    val all = info.javaClass.getMethod("all").invoke(info) as Array<*>
                    all.forEach { m ->
                        if (m != null) {
                            val version = runCatching { m.javaClass.getMethod("getVersion").invoke(m) }.getOrNull()
                            val state   = m.javaClass.getMethod("getState").invoke(m)
                            val desc    = m.javaClass.getMethod("getDescription").invoke(m)
                            println("  [$state] ${version ?: "?"} $desc")
                        }
                    }
                }
            }
        } catch (e: java.lang.reflect.InvocationTargetException) {
            val cause = e.cause ?: e
            System.err.println("Flyway $command failed: ${cause.message}")
            cause.printStackTrace(System.err)
            throw cause
        }
    } finally {
        Thread.currentThread().contextClassLoader = saved
        cl.close()
    }
}

tasks.register("dbMigrate")   { group = "flyway"; description = "Apply pending migrations";    doLast { runFlyway("migrate")  } }
tasks.register("dbInfo")      { group = "flyway"; description = "Show migration status";       doLast { runFlyway("info")     } }
tasks.register("dbValidate")  { group = "flyway"; description = "Validate applied migrations"; doLast { runFlyway("validate") } }
tasks.register("dbRepair")    { group = "flyway"; description = "Repair schema history";       doLast { runFlyway("repair")   } }
