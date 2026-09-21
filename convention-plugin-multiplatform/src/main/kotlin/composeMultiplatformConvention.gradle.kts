import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

val libs = the<LibrariesForLibs>()

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("io.insert-koin.compiler.plugin")
    id("org.jetbrains.compose")
    kotlin("multiplatform")
    kotlin("plugin.compose")
}

kotlin {
    android {
        compilerOptions { jvmTarget = JvmTarget.fromTarget(libs.versions.javaVersion.get()) }
        compileSdk {
            version =
                release(
                    libs.versions.build.android.compileSdk
                        .get()
                        .toInt(),
                ) {
                    minorApiLevel =
                        libs.versions.build.android.compileSdkMinor
                            .get()
                            .toInt()
                }
        }
        minSdk =
            libs.versions.build.android.minSdk
                .get()
                .toInt()
        androidResources.enable = true
        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = true
            enableCoverage = true
        }
        withDeviceTestBuilder {
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            animationsDisabled = true
            managedDevices.localDevices.create("managedVirtualDevice") {
                device = "Pixel 2"
                apiLevel = 35
            }
        }
        packaging.resources.excludes.add("META-INF/**")
    }

    jvm {
        compilerOptions { jvmTarget = JvmTarget.fromTarget(libs.versions.javaVersion.get()) }
    }

    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.paging.compose)
            implementation(libs.jetbrains.compose.animation)
            implementation(libs.jetbrains.compose.animation.graphics)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.ui.tooling.preview)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
            implementation(libs.jetbrains.savedstate.compose)
            implementation(libs.jetbrains.window.core)
            implementation(libs.koin.annotations)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.markdown.renderer)
            implementation(project.dependencies.platform(libs.koin.bom))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.jetbrains.compose.ui.test)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.jetbrains.compose.ui.tooling)
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.androidx.uitest.junit4)
                implementation(libs.androidx.uitest.testManifest)
            }
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        iosMain.dependencies {
        }

        webMain.dependencies {
        }
    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries { framework { baseName = "ComposeApp" } }
        }
}

// Koin compiler plugin 1.2.x full-graph compile-safety validation drops commonMain
// @ComponentScan/@Module hints from Kotlin/Native klibs, producing false KOIN-D002
// "Missing definition" errors on iOS targets while JVM/Android compile fine.
// https://github.com/InsertKoinIO/koin-compiler-plugin/issues/105
// https://github.com/InsertKoinIO/koin-compiler-plugin/issues/106
koinCompiler {
    compileSafety = false
}

tasks.withType<Test>().matching { it.name.contains("AndroidHostTest") }.configureEach {
    exclude("**/*CommonTest*")
    systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"
    // Robolectric reflectively pokes JDK internals (e.g. jdk.internal.access.SharedSecrets
    // for ApplicationSharedMemory on SDK 37+); modern JDKs (17+) hide those by default.
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.util.random=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
    )
}

tasks.withType<KotlinJsTest>().matching { it.name == "jsBrowserTest" }.configureEach {
    filter.excludeTestsMatching("*CommonTest*")
}

tasks.withType<AbstractTestTask>().configureEach {
    failOnNoDiscoveredTests = false
}
