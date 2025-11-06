plugins {
    alias(libs.plugins.kotlin.android)
    id("com.android.library")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ti3042.airmonitor.data"
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
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // Core modules
    implementation(project(":core:common"))
    
    // Domain layer
    implementation(project(":domain"))
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // Android Core
    implementation("androidx.core:core-ktx:1.13.1")
    
    // Firebase BOM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-functions")
    
    // Room Database (comentado temporalmente)
    //implementation("androidx.room:room-runtime:2.6.1")
    //implementation("androidx.room:room-ktx:2.6.1")
    //kapt("androidx.room:room-compiler:2.6.1")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Security dependencies for ISO 27400 compliance
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}