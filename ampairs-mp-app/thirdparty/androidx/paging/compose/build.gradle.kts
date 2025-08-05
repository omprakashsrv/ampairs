// Copyright 2024, Christopher Banes
// SPDX-License-Identifier: Apache-2.0


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain {
            dependencies {
                api(libs.paging.common)
                api(compose.runtime)
            }
        }
    }
}

android {
    namespace = "androidx.paging.compose"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    kotlin {
        jvmToolchain(17)
    }
}
