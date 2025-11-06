plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    //id("kotlin-kapt") // Comentado temporalmente
}

android {
    namespace = "com.ti3042.airmonitor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ti3042.airmonitor"
        minSdk = 23
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
        viewBinding = true
        // compose = true  // Temporalmente deshabilitado
    }
    
    // composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.8"
    // }
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // Módulos propios
    implementation(project(":core:common"))
    implementation(project(":core:ui"))        // ✨ NUEVO - UI compartida
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:dashboard"))  // ✨ NUEVO - Dashboard modularizado
    implementation(project(":feature:control"))    // ✨ NUEVO - Control del sistema
    implementation(project(":feature:monitoring")) // ✨ NUEVO - Monitoreo avanzado

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    
    // Jetpack Compose BOM (temporalmente deshabilitado)
    // implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    // implementation("androidx.compose.ui:ui")
    // implementation("androidx.compose.ui:ui-tooling-preview")
    // implementation("androidx.compose.material3:material3")
    // implementation("androidx.compose.runtime:runtime-livedata")
    // implementation("androidx.activity:activity-compose:1.8.2")
    // debugImplementation("androidx.compose.ui:ui-tooling")
    // debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Security dependencies for ISO 27400 compliance
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // SwipeRefreshLayout for gesture control
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    //dependencias fire base
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions")
    
    // Navigation Component para dashboard profesional
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    
    // Charts para gráficos
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}