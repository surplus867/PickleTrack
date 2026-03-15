import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("com.squareup.sqldelight")
}

kotlin {
    // Note: the project's Kotlin/Gradle plugin version doesn't expose the same jvmToolchain DSL everywhere.
    // Avoid using `jvmToolchain { languageVersion.set(...) }` here to stay compatible.

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
            kotlin.srcDir(layout.buildDirectory.dir("generated/sqldelight/code/PickleTrackDatabase").get().asFile)
        }
        val commonTest by getting
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("com.squareup.sqldelight:native-driver:1.5.5")
            }
        }

        commonMain.dependencies {
            implementation("com.squareup.sqldelight:runtime:1.5.5")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            implementation("com.squareup.sqldelight:coroutines-extensions:1.5.5")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
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

// Configure Kotlin compile tasks via reflection at execution time to avoid script-time deprecated API diagnostics
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
    doFirst {
        try {
            val taskObj = this
            // Try modern compilerOptions getter
            val compilerOptionsGetter = taskObj.javaClass.methods.firstOrNull { it.name == "getCompilerOptions" }
            if (compilerOptionsGetter != null) {
                val compilerOptions = compilerOptionsGetter.invoke(taskObj) ?: return@doFirst
                // Try to set freeCompilerArgs
                runCatching {
                    val freeArgsGetter = compilerOptions.javaClass.methods.firstOrNull { it.name == "getFreeCompilerArgs" }
                    val freeArgsSetter = compilerOptions.javaClass.methods.firstOrNull { it.name == "setFreeCompilerArgs" && it.parameterCount == 1 }
                    val current = freeArgsGetter?.invoke(compilerOptions) as? MutableList<String>
                    val args = current ?: ArrayList<String>()
                    if (!args.contains("-Xexpect-actual-classes")) args.add("-Xexpect-actual-classes")
                    freeArgsSetter?.invoke(compilerOptions, args)
                }
                // Try to set jvmTarget
                runCatching {
                    val setJvm = compilerOptions.javaClass.methods.firstOrNull { it.name == "setJvmTarget" && it.parameterCount == 1 }
                    if (setJvm != null) setJvm.invoke(compilerOptions, "11")
                }
            } else {
                // Fallback to kotlinOptions via reflection
                val kotlinOptionsGetter = taskObj.javaClass.methods.firstOrNull { it.name == "getKotlinOptions" }
                if (kotlinOptionsGetter != null) {
                    val kotlinOptions = kotlinOptionsGetter.invoke(taskObj) ?: return@doFirst
                    runCatching {
                        val freeArgsGetter = kotlinOptions.javaClass.methods.firstOrNull { it.name == "getFreeCompilerArgs" }
                        val freeArgsSetter = kotlinOptions.javaClass.methods.firstOrNull { it.name == "setFreeCompilerArgs" && it.parameterCount == 1 }
                        val current = freeArgsGetter?.invoke(kotlinOptions) as? MutableList<String>
                        val args = current ?: ArrayList<String>()
                        if (!args.contains("-Xexpect-actual-classes")) args.add("-Xexpect-actual-classes")
                        freeArgsSetter?.invoke(kotlinOptions, args)
                    }
                    runCatching {
                        val setJvm = kotlinOptions.javaClass.methods.firstOrNull { it.name == "setJvmTarget" && it.parameterCount == 1 }
                        setJvm?.invoke(kotlinOptions, "11")
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to configure Kotlin compiler options reflectively: ${e.message}")
        }
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
