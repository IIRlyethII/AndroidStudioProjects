package com.ti3042.airmonitor.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.ti3042.airmonitor.data.database.AirQualityDatabase
import com.ti3042.airmonitor.data.database.dao.*
import com.ti3042.airmonitor.data.database.repository.SensorDataRepository
import com.ti3042.airmonitor.services.firebase.FirebaseService
import com.ti3042.airmonitor.services.firebase.FirebaseStorageService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAirQualityDatabase(
        @ApplicationContext context: Context
    ): AirQualityDatabase {
        return Room.databaseBuilder(
            context,
            AirQualityDatabase::class.java,
            AirQualityDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    fun provideSensorReadingDao(database: AirQualityDatabase): SensorReadingDao {
        return database.sensorReadingDao()
    }
    
    @Provides
    fun provideDeviceConfigurationDao(database: AirQualityDatabase): DeviceConfigurationDao {
        return database.deviceConfigurationDao()
    }
    
    @Provides
    fun provideCalibrationRecordDao(database: AirQualityDatabase): CalibrationRecordDao {
        return database.calibrationRecordDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
}