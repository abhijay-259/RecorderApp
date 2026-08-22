plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

android {
    namespace = "com.example.recorderapp"
    compileSdk = 37 // Simplified syntax

    defaultConfig {
        applicationId = "com.example.recorderapp"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// 🔑 FIXED: Room migration configuration uncommented so Room can output its change-logs.
ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
}

dependencies {
    val room_version = "2.8.4"

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Ktor core engine for networking
    implementation("io.ktor:ktor-client-android:3.0.1")
    // Content negotiation plugin to automatically handle data translations
    implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
    // JSON tool to convert Kotlin objects into JSON text blocks automatically
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")

    // viewmodel connecting to compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // Room Database
    implementation("androidx.room:room-runtime:${room_version}")
    ksp("androidx.room:room-compiler:$room_version")

    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.ui.test.junit4))
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
