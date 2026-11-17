plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose) // Compose compiler plugin: turns @Composable functions into UI code
    alias(libs.plugins.ksp) // Runs the Room annotation processor
}

android {
    namespace = "kz.kbtu.library"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "kz.kbtu.library"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose) // NavHost, NavController, routes
    implementation(libs.kotlinx.coroutines.android)

    // Room: local SQLite database (entities, DAOs, Flow queries, coroutines)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core) // Icons.Default.Search, Add, Clear...

    // Local JVM unit tests (app/src/test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests (app/src/androidTest): run on a device or emulator, use a real in-memory Room database
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Only in debug builds: Android Studio @Preview support
    debugImplementation(libs.androidx.compose.ui.tooling)
}
