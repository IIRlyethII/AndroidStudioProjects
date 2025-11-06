package com.ti3042.airmonitor.domain.repository

import com.ti3042.airmonitor.domain.model.User
import com.ti3042.airmonitor.domain.model.AirQuality

/**
 * 🔐 Interfaz de repositorio de autenticación (Domain)
 * Define el contrato sin depender de implementaciones específicas
 * 
 * **Módulo**: :domain
 * **Propósito**: Contrato para operaciones de autenticación
 */
interface AuthRepository {
    
    /**
     * Obtener usuario actual autenticado
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Verificar si hay usuario autenticado
     */
    suspend fun isUserAuthenticated(): Boolean
    
    /**
     * Iniciar sesión con email y contraseña
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<User>
    
    /**
     * Registrar nuevo usuario
     */
    suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<User>
    
    /**
     * Cerrar sesión
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Enviar email de recuperación de contraseña
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
}

/**
 * 🌬️ Interfaz de repositorio de calidad de aire
 * Define el contrato para datos de sensores
 */
interface AirQualityRepository {
    
    /**
     * Obtener datos en tiempo real
     */
    fun getRealtimeData(deviceId: String): kotlinx.coroutines.flow.Flow<Result<AirQuality>>
    
    /**
     * Obtener datos históricos
     */
    suspend fun getHistoricalData(deviceId: String, startTime: Long, endTime: Long): Result<List<AirQuality>>
    
    /**
     * Configurar alertas personalizadas
     */
    suspend fun configureAlerts(
        userId: String,
        gasType: com.ti3042.airmonitor.domain.model.GasType,
        warningThreshold: Double,
        dangerThreshold: Double
    ): Result<Unit>
    
    /**
     * Obtener última lectura de calidad de aire
     */
    suspend fun getLatestReading(): Result<AirQuality>
    
    /**
     * Guardar nueva lectura
     */
    suspend fun saveReading(airQuality: AirQuality): Result<Unit>
}

/**
 * 👤 Interfaz de repositorio de usuario
 * Define el contrato para datos de perfil de usuario
 */
interface UserRepository {
    
    /**
     * Obtener perfil de usuario
     */
    suspend fun getUserProfile(userId: String): Result<User>
    
    /**
     * Actualizar perfil de usuario
     */
    suspend fun updateUserProfile(user: User): Result<Unit>
    
    /**
     * Eliminar cuenta de usuario
     */
    suspend fun deleteUserAccount(userId: String): Result<Unit>
}