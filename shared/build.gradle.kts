import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("com.squareup.sqldelight")
}

kotlin {
    // Note: androidTarget() / android() APIs vary by Kotlin plugin version.
    // To keep this script compatible across environments we avoid calling androidTarget()/android()
    // directly and we don't reference `androidMain` here. Platform-specific drivers can be added
    // in the Android module instead.

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
        val commonMain by getting {
            // Use the Gradle `layout.buildDirectory` API instead of the deprecated `buildDir` property
            kotlin.srcDir(layout.buildDirectory.dir("generated/sqldelight/code/PickleTrackDatabase").get().asFile)
        }
        val commonTest by getting
        // Create an intermediate iosMain source set so native-driver is visible to metadata compilation
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("com.squareup.sqldelight:native-driver:1.5.5")
            }
        }

        commonMain.dependencies {
            // SQLDelight common runtime
            implementation("com.squareup.sqldelight:runtime:1.5.5")
            // Coroutines common artifact for Kotlin multiplatform
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            // SQLDelight coroutines extensions for Flow interop
            implementation("com.squareup.sqldelight:coroutines-extensions:1.5.5")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

// Configure Kotlin compile tasks using kotlinOptions (compatible across plugin versions)
tasks.withType(KotlinCompile::class.java).configureEach {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xexpect-actual-classes")
        jvmTarget = "11"
    }
}

// SQLDelight configuration: generate PickleTrackDatabase in com.example.shared.db
sqldelight {
    database("PickleTrackDatabase") {
        packageName = "com.example.shared.db"
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
