package com.ti3042.airmonitor

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

//@HiltAndroidApp
class AirMonitorApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Configure Firestore settings for better performance
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        
        FirebaseFirestore.getInstance().firestoreSettings = settings
        
        // Enable Firestore logging for debugging
        FirebaseFirestore.setLoggingEnabled(true)
    }
}