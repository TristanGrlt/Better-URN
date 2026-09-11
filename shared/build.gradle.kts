import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.0.0"
}

fun getGitTagName(): String {
    return runCatching {
        val output = providers.exec {
            commandLine("git", "describe", "--tags", "--exact-match")
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim()

        if (output.isNotBlank()) {
            output.removePrefix("v")
        } else {
            val latestOutput = providers.exec {
                commandLine("git", "describe", "--tags", "--abbrev=0")
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
            if (latestOutput.isNotBlank()) {
                "${latestOutput.removePrefix("v")}-dev"
            } else {
                "1.0.0-dev"
            }
        }
    }.getOrDefault("1.0.0-dev")
}

fun getGitCommitCount(): Int {
    return runCatching {
        val output = providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim()
        output.toIntOrNull() ?: 1
    }.getOrDefault(1)
}

fun getGitCommitHash(): String {
    return runCatching {
        val output = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim()
        output.ifBlank { "dev" }
    }.getOrDefault("dev")
}

buildkonfig {
    packageName = "org.better.urn"
    objectName = "BuildKonfig"

    defaultConfigs {
        buildConfigField(STRING, "APP_NAME", "Better URN")
        buildConfigField(STRING, "VERSION_NAME", getGitTagName())
        buildConfigField(INT, "BUILD_NUMBER", getGitCommitCount().toString())
        buildConfigField(STRING, "GIT_HASH", getGitCommitHash())
    }
}

kotlin {
    jvm()
    
    android {
       namespace = "org.better.urn.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.cameraCore)
            implementation(libs.androidx.camera2)
            implementation(libs.androidx.cameraLifecycle)
            implementation(libs.androidx.cameraView)
        }
        jvmMain.dependencies {
            val osName = System.getProperty("os.name").lowercase()
            val javafxClassifier = when {
                osName.contains("win") -> "win"
                osName.contains("mac") -> "mac"
                else -> "linux"
            }
            implementation("org.openjfx:javafx-controls:21.0.2:$javafxClassifier")
            implementation("org.openjfx:javafx-media:21.0.2:$javafxClassifier")
            implementation("org.openjfx:javafx-graphics:21.0.2:$javafxClassifier")
            implementation("org.openjfx:javafx-base:21.0.2:$javafxClassifier")
            implementation("org.openjfx:javafx-swing:21.0.2:$javafxClassifier")
            implementation("org.apache.pdfbox:pdfbox:3.0.8")
            implementation(libs.kotlinx.datetime)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(compose.materialIconsExtended)

            implementation("io.ktor:ktor-client-core:2.3.11")
            implementation("io.ktor:ktor-client-cio:2.3.11")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
            api(libs.kotlinx.datetime)
            implementation("media.kamel:kamel-image:0.9.4")
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
            implementation(libs.zxing.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
            implementation(libs.kotlinx.datetime)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

tasks.withType<Test>().configureEach {
    val testStorageDir = layout.buildDirectory.dir("tmp/test-storage").get().asFile.absolutePath
    val testCacheDir = layout.buildDirectory.dir("tmp/test-cache").get().asFile.absolutePath
    systemProperty("betterurn.storage.dir", testStorageDir)
    systemProperty("betterurn.cache.dir", testCacheDir)
}
