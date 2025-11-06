plugins {
    alias(libs.plugins.kotlin.android)
    id("com.android.library")
}

android {
    namespace = "com.ti3042.airmonitor.core.common"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    // Kotlin Coroutines (para NavigationHelper)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // Android Core (mínimo para Context y otras utilidades Android)
    implementation("androidx.core:core-ktx:1.13.1")
    
    // Navigation Component (requerido para NavigationHelper)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    
    // Fragment (requerido para Navigation)
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}