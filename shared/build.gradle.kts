import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    // SQLDelight Gradle plugin removed to avoid script compilation issues in this AGP/Kotlin setup.
}

kotlin {
    // Configure the Android target with compiler options.
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val androidMain by getting

        commonMain.dependencies {
            // SQLDelight common runtime (library only; SQLDelight Gradle plugin not applied)
            implementation("com.squareup.sqldelight:runtime:1.5.5")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            // Android-specific SQLDelight driver
            implementation("com.squareup.sqldelight:android-driver:1.5.5")
        }

        // iOS target-specific SQLDelight native driver
        val iosArm64Main by getting {
            dependencies {
                implementation("com.squareup.sqldelight:native-driver:1.5.5")
            }
        }
        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation("com.squareup.sqldelight:native-driver:1.5.5")
            }
        }
    }
}

android {
    namespace = "com.example.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
