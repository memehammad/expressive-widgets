import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI sets GITHUB_RUN_NUMBER so every build installs as an update over the last one.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "dev.memehammad.widgets"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.memehammad.widgets"
        minSdk = 31
        targetSdk = 35
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }

    // Uses app/release.jks when present (local, or decoded from the SIGNING_KEY secret in CI);
    // otherwise falls back to the debug key so the build still produces an installable APK.
    val releaseKey = file("release.jks")
    signingConfigs {
        create("release") {
            storeFile = releaseKey
            storePassword = "clashtimers"
            keyAlias = "clashtimers"
            keyPassword = "clashtimers"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseKey.exists()) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Optional system toggles and battery health: a shell running via Shizuku.
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
