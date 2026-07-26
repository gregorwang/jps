import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun releaseSecret(name: String): String? = providers.gradleProperty(name).orNull
    ?: providers.environmentVariable(name).orNull

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val releaseStoreFilePath = releaseSecret("AJL_RELEASE_STORE_FILE")
val releaseStorePassword = releaseSecret("AJL_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSecret("AJL_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSecret("AJL_RELEASE_KEY_PASSWORD")
val releaseSigningReady = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }
val publicAssetsCleared = releaseSecret("AJL_PUBLIC_ASSETS_CLEARED") == "true"
val appUpdateBaseUrl = releaseSecret("AJL_APP_UPDATE_BASE_URL")
    ?: "https://anime-japanese-lab-android-updates.ishallnotwant123.workers.dev"

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.animejapaneselab.nativeapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.animejapaneselab.nativeapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"

        buildConfigField("String", "APP_UPDATE_BASE_URL", buildConfigString(appUpdateBaseUrl))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Personal-use targets: current Windows emulator plus modern Android phones/tablets.
        ndk {
            abiFilters += listOf("x86_64", "arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = file(checkNotNull(releaseStoreFilePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "ALLOW_INTERNAL_REFERENCE_ASSETS", "true")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "ALLOW_INTERNAL_REFERENCE_ASSETS", "false")
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        create("localSlim") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "ALLOW_INTERNAL_REFERENCE_ASSETS", "true")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            res.srcDir("../local-fusion-assets/res")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.configureEach {
    if (name == "packageRelease" || name == "bundleRelease") {
        doFirst {
            check(releaseSigningReady) {
                "Release signing is not configured. Set AJL_RELEASE_STORE_FILE, AJL_RELEASE_STORE_PASSWORD, AJL_RELEASE_KEY_ALIAS and AJL_RELEASE_KEY_PASSWORD."
            }
            check(publicAssetsCleared) {
                "Public release is blocked because personal/internal reference assets are still packaged. Replace them with redistribution-cleared assets, then set AJL_PUBLIC_ASSETS_CLEARED=true."
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")

    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("app.rive:rive-android:11.7.1")
    implementation("com.airbnb.android:lottie-compose:6.7.1")

    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
