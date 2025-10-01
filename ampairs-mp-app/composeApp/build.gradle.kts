import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

configurations.all {
    exclude(group = "androidx.compose.ui", module = "ui-test-android")
}


kotlin {
    androidTarget()

    jvm("desktop")

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // macOS targets with executable configuration
    listOf(
        macosX64(),
        macosArm64()
    ).forEach { macosTarget ->
        macosTarget.binaries {
            executable {
                baseName = "Ampairs"
                entryPoint = "main"
            }
        }
    }

    // Linux target with executable configuration
    linuxX64 {
        binaries {
            executable {
                baseName = "ampairs"
                entryPoint = "main"
            }
        }
    }

    // Windows target with executable configuration
    mingwX64 {
        binaries {
            executable {
                baseName = "Ampairs"
                entryPoint = "main"
            }
        }
    }

    sourceSets {

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
                implementation(libs.ktor.client.okHttp)
                implementation(libs.splash.screen)
                implementation(libs.aws.s3)
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.materialIconsExtended)
                implementation(compose.material3)
                implementation(compose.components.resources)

                implementation(libs.kotlinx.dateTime)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                implementation(libs.bundles.ktor.common)

                implementation(libs.paging.common)
                implementation(libs.image.loader)

                // Coil for efficient image loading and caching
                implementation(libs.coil.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)

                // FileKit for platform-specific file picking
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)

                implementation(libs.file.picker)
                implementation(libs.uuid)
                implementation(libs.material3.adaptive)
                implementation(libs.material3.adaptive.layout)
                implementation(libs.material3.adaptive.navigation)
                implementation(libs.navigation.compose)
                implementation(libs.lifecycle.viewmodel)
                implementation(libs.savedstate)
                implementation(libs.savedstate.compose)
                implementation(projects.thirdparty.androidx.paging.compose)

                implementation(libs.room.runtime)
                implementation(libs.room.paging)
                implementation(libs.sqlite.bundled)

                // Store5 for offline-first caching
                implementation(libs.store5)

                // DataStore for preferences
                implementation(libs.datastore)
                implementation(libs.datastore.preferences)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.koin.core)
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okHttp)
                implementation(libs.aws.s3)
                // Library for OpenStreetMap - now using JOSM repository
                implementation(libs.jmapviewer)

                implementation(project(":tallyModule"))
            }
        }


        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
                // Note: Using Room database now, no longer need SQLDelight
            }
        }

        // macOS source sets
        val macosX64Main by getting
        val macosArm64Main by getting
        val macosMain by creating {
            dependsOn(commonMain)
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        // Linux source sets
        val linuxX64Main by getting {
            dependsOn(commonMain)
            dependencies {
                // Linux can use curl or okHttp - using okHttp for consistency with desktop
                implementation(libs.ktor.client.okHttp)
            }
        }

        // Windows source sets
        val mingwX64Main by getting {
            dependsOn(commonMain)
            dependencies {
                // Windows can use WinHttp or okHttp - using okHttp for consistency
                implementation(libs.ktor.client.okHttp)
            }
        }
    }
}

android {

    compileSdk = libs.versions.android.compileSdk.get().toInt()
    namespace = "com.ampairs.app"

//    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
//    sourceSets["main"].res.srcDirs("src/androidMain/res")
//    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/versions/*"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }

    defaultConfig {
        applicationId = "com.ampairs.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Environment configuration
        buildConfigField("String", "API_BASE_URL", "\"http://10.50.51.6:8080\"")
        buildConfigField("String", "ENVIRONMENT", "\"dev\"")
    }

    signingConfigs {
        val release by creating {
            storeFile = file("$rootDir/ampairs.jks")
            storePassword = "SKFNNFJ234329898g723g47823gr8"
            keyPassword = "SKFNNFJ234329898g723g47823gr8"
            keyAlias = "ampairs"
        }
    }
    buildTypes {
        val debug by getting {
            buildConfigField("String", "API_BASE_URL", "\"http://10.50.51.6:8080\"")
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
            signingConfig = signingConfigs["release"]
        }
        val release by getting {
            buildConfigField("String", "API_BASE_URL", "\"https://api.ampairs.com\"")
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs["release"]
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Ampairs"
            packageVersion = "1.0.0"
            description = "Empowering Retail, One byte at a time"
            copyright = "Copyright 2023 Ampairs. All rights reserved."
            vendor = "Ampairs"
            modules("java.sql")
            windows {
                dirChooser = true
                upgradeUuid = "FEEF6607-E845-4EF5-B62B-B7F48D654796"
                shortcut = true
                menu = true
                iconFile.set(rootProject.file("resources/icon.ico"))
                menuGroup = packageName
            }
            macOS {
                bundleID = "com.ampairs.app"
                packageName = rootProject.name
                iconFile.set(rootProject.file("resources/icon.icns"))
            }
            linux {
                iconFile.set(rootProject.file("resources/icon.png"))
            }
        }

        buildTypes.release {
            proguard {
                obfuscate.set(true)
                optimize.set(true)
                configurationFiles.from("compose-desktop.pro")
            }
        }
    }
}

// Native binary build tasks
tasks.register("buildNativeMacOS") {
    group = "build"
    description = "Build native macOS executables for Intel and Apple Silicon"
    dependsOn("linkDebugExecutableMacosX64", "linkDebugExecutableMacosArm64")
}

tasks.register("buildNativeLinux") {
    group = "build"
    description = "Build native Linux executable"
    dependsOn("linkDebugExecutableLinuxX64")
}

tasks.register("buildNativeWindows") {
    group = "build"
    description = "Build native Windows executable"
    dependsOn("linkDebugExecutableMingwX64")
}

tasks.register("buildAllNative") {
    group = "build"
    description = "Build all native platform executables (macOS, Linux, Windows)"
    dependsOn("buildNativeMacOS", "buildNativeLinux", "buildNativeWindows")
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspCommonMainMetadata", libs.room.compiler)

    // Android
    add("kspAndroid", libs.room.compiler)

    // Desktop JVM
    add("kspDesktop", libs.room.compiler)

    // iOS targets
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)

    // macOS native targets
    add("kspMacosX64", libs.room.compiler)
    add("kspMacosArm64", libs.room.compiler)

    // Linux native target
    add("kspLinuxX64", libs.room.compiler)

    // Windows native target
    add("kspMingwX64", libs.room.compiler)
}

// Fix KSP and Compose resource generation dependency issues
tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().configureEach {
    dependsOn(tasks.matching { it.name.startsWith("generateComposeResClass") })
    dependsOn(tasks.matching { it.name.startsWith("generateResourceAccessorsFor") })
    dependsOn(tasks.matching { it.name.startsWith("generateActualResourceCollectorsFor") })
    dependsOn(tasks.matching { it.name.startsWith("generateExpectResourceCollectorsFor") })
}