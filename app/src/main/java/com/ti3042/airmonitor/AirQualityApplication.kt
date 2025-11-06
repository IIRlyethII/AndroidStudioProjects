package com.ti3042.airmonitor

import android.app.Application
import android.util.Log

/**
 * 🚀 Application principal - Ensamblador de módulos
 * Punto de entrada principal de la aplicación multi-módulo
 * 
 * **Módulo**: :app
 * **Propósito**: Coordinar la inicialización de todos los módulos
 */
class AirQualityApplication : Application() {
    
    companion object {
        const val TAG = "AirQualityApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "🚀 Inicializando AirQuality Monitor")
        
        // Inicialización de módulos
        initializeModules()
        
        Log.d(TAG, "✅ Aplicación inicializada exitosamente")
    }
    
    /**
     * 🔧 Inicializa todos los módulos de la aplicación
     */
    private fun initializeModules() {
        Log.d(TAG, "🔧 Inicializando módulos...")
        
        // TODO: Aquí se pueden agregar inicializaciones específicas de módulos
        // Por ejemplo: Hilt, Room, configuraciones globales, etc.
        
        initializeCoreModules()
        initializeDataLayer()
        initializeFeatureModules()
    }
    
    /**
     * 🏗️ Inicializar módulos core (común, navegación, etc.)
     */
    private fun initializeCoreModules() {
        Log.d(TAG, "🏗️ Core modules initialized")
        // :core:common - Ya disponible
        // :core:ui - Para futuros componentes UI compartidos
        // :core:navigation - Para navegación avanzada
    }
    
    /**
     * 💾 Inicializar capa de datos
     */
    private fun initializeDataLayer() {
        Log.d(TAG, "💾 Data layer initialized")
        // :data - Repositorios y fuentes de datos
        // :domain - Lógica de negocio y use cases
    }
    
    /**
     * 🎯 Inicializar módulos de características
     */
    private fun initializeFeatureModules() {
        Log.d(TAG, "🎯 Feature modules initialized")
        // :feature:auth - Autenticación
        // :feature:dashboard - Panel principal (futuro)
        // :feature:monitoring - Monitoreo de sensores (futuro)
    }
    
    /**
     * 🧹 Limpieza de recursos al terminar
     */
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "🧹 Cleaning up resources")
    }
}