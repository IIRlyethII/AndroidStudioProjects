package com.ti3042.airmonitor.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.ti3042.airmonitor.domain.model.User
import com.ti3042.airmonitor.domain.repository.AuthRepository as DomainAuthRepository
import kotlinx.coroutines.tasks.await

/**
 * 🔐 Repository para manejo de autenticación con Firebase
 * Implementa patrón Repository para separar la lógica de datos de la UI
 * 
 * **Módulo**: :data
 * **Propósito**: Encapsula toda la lógica de autenticación Firebase
 */
class AuthRepository : DomainAuthRepository {
    
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val tag = "AuthRepository"
    
    /**
     * Obtiene el usuario actual autenticado (implementación de la interfaz del dominio)
     */
    override suspend fun getCurrentUser(): User? {
        return firebaseAuth.currentUser?.toUser()
    }
    
    /**
     * Verifica si hay un usuario autenticado (implementación de la interfaz del dominio)
     */
    override suspend fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }
    
    /**
     * Obtiene el FirebaseUser actual (método legacy para compatibilidad)
     */
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
    
    /**
     * Inicia sesión con email y contraseña (implementación de la interfaz del dominio)
     */
    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<User> {
        return try {
            Log.d(tag, "🔐 Attempting login for: $email")
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                Log.d(tag, "✅ Login successful for: ${user.email}")
                Result.success(user.toUser())
            } else {
                Log.w(tag, "❌ Login failed: User is null")
                Result.failure(Exception("Usuario nulo después del login"))
            }
        } catch (e: Exception) {
            Log.w(tag, "❌ Login failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Registra un nuevo usuario (implementación de la interfaz del dominio)
     */
    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<User> {
        return try {
            Log.d(tag, "📝 Attempting registration for: $email")
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                Log.d(tag, "✅ Registration successful for: ${user.email}")
                Result.success(user.toUser())
            } else {
                Log.w(tag, "❌ Registration failed: User is null")
                Result.failure(Exception("Usuario nulo después del registro"))
            }
        } catch (e: Exception) {
            Log.w(tag, "❌ Registration failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Cierra la sesión (implementación de la interfaz del dominio)
     */
    override suspend fun signOut(): Result<Unit> {
        return try {
            Log.d(tag, "🚪 Signing out user")
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(tag, "❌ Sign out failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Envía email de recuperación (implementación de la interfaz del dominio)
     */
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            Log.d(tag, "📧 Sending password reset email to: $email")
            firebaseAuth.sendPasswordResetEmail(email).await()
            Log.d(tag, "✅ Password reset email sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(tag, "❌ Failed to send password reset email: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * 🔄 Mapea FirebaseUser a User del dominio
     */
    private fun FirebaseUser.toUser(): User {
        return User(
            id = this.uid,
            email = this.email ?: "",
            displayName = this.displayName ?: "",
            isEmailVerified = this.isEmailVerified,
            createdAt = this.metadata?.creationTimestamp ?: System.currentTimeMillis()
        )
    }
}