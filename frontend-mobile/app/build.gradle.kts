plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.greetingcard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.greetingcard"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "target"
    productFlavors {
        create("emulator") {
            dimension = "target"
            buildConfigField("String", "BACKEND_URL", "\"http://10.0.2.2:8080/\"")
        }
        create("device") {
            dimension = "target"
            val deviceUrl = rootProject.file("local.properties")
                .takeIf { it.exists() }
                ?.readLines()
                ?.firstOrNull { it.startsWith("deviceBackendUrl=") }
                ?.substringAfter("=")
                ?: "http://localhost:8080/" // Set deviceBackendUrl in local.properties for real device builds
            buildConfigField("String", "BACKEND_URL", "\"$deviceUrl\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // DataStore
    implementation(libs.datastore.preferences)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle ViewModel + Runtime Compose
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Coroutines
    implementation(libs.coroutines.android)

    // Coil (image loading)
    implementation(libs.coil.compose)

    // WorkManager
    implementation(libs.workmanager)
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
