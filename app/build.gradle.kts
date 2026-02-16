plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt") // 1. AGREGADO: Necesario para que Room funcione
}

android {
    namespace = "com.jucar.heyplanty"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jucar.heyplanty"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    aaptOptions {
        noCompress.add("tflite")
    }
}

dependencies {
    val work_version = "2.9.0"
    implementation("androidx.work:work-runtime-ktx:$work_version")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // --- DEPENDENCIAS PARA NAVEGACIÓN Y CÁMARA ---
    val navVersion = "2.7.7"
    val cameraVersion = "1.3.4"
    implementation("androidx.navigation:navigation-compose:$navVersion")
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")
    // ------------------------------------------

    // --- SECCIÓN DE INTELIGENCIA ARTIFICIAL (TENSORFLOW LITE) ---
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    // Opcional: Para aceleración por GPU, descomentar si es necesario más adelante
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.9.0")
    // -----------------------------------------------------------

    // --- SECCIÓN DE ROOM AGREGADA ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion") // Generador de código
    // --------------------------------

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}