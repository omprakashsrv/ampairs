// Copyright 2024, Christopher Banes
// SPDX-License-Identifier: Apache-2.0


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.base)
                api(libs.kotlin.inject.runtime)
            }
        }

        val mokoImplMain by creating {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.moko.permissions.core)
            }
        }

        androidMain {
            dependsOn(mokoImplMain)

            dependencies {
                api(libs.androidx.activity)
            }
        }

        iosMain {
            dependsOn(mokoImplMain)
        }
    }
}

android {
    namespace = "app.tivi.core.permissions"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    kotlin {
        jvmToolchain(17)
    }
}
