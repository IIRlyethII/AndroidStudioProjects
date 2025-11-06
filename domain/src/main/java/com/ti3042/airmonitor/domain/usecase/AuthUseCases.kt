package com.ti3042.airmonitor.domain.usecase

import com.ti3042.airmonitor.domain.model.User
import com.ti3042.airmonitor.domain.repository.AuthRepository

/**
 * 🔐 Use Case: Iniciar sesión
 * Encapsula toda la lógica de negocio para el login
 * 
 * **Módulo**: :domain
 * **Propósito**: Lógica de negocio para autenticación de usuarios
 */
class SignInUseCase(
    private val authRepository: AuthRepository
) {
    
    suspend operator fun invoke(email: String, password: String): Result<User> {
        // Validaciones de negocio
        if (email.isBlank()) {
            return Result.failure(ValidationException("Email no puede estar vacío"))
        }
        
        if (!isValidEmail(email)) {
            return Result.failure(ValidationException("Email no tiene formato válido"))
        }
        
        if (password.isBlank()) {
            return Result.failure(ValidationException("Contraseña no puede estar vacía"))
        }
        
        if (password.length < 6) {
            return Result.failure(ValidationException("Contraseña debe tener al menos 6 caracteres"))
        }
        
        // Ejecutar login
        return try {
            val result = authRepository.signInWithEmailAndPassword(email.trim(), password)
            
            result.fold(
                onSuccess = { user ->
                    if (user.isValidForAppUsage()) {
                        Result.success(user)
                    } else {
                        Result.failure(BusinessException("Usuario no está habilitado para usar la aplicación"))
                    }
                },
                onFailure = { error ->
                    Result.failure(mapAuthError(error))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
    
    private fun mapAuthError(error: Throwable): Throwable {
        return when {
            error.message?.contains("password is invalid") == true -> 
                AuthException("Contraseña incorrecta")
            error.message?.contains("no user record") == true -> 
                AuthException("No existe una cuenta con este email")
            error.message?.contains("network error") == true -> 
                NetworkException("Error de conexión. Verifique su internet")
            else -> AuthException("Error de autenticación: ${error.message}")
        }
    }
}

/**
 * 📝 Use Case: Registrar usuario
 * Encapsula la lógica para crear nuevas cuentas
 */
class SignUpUseCase(
    private val authRepository: AuthRepository
) {
    
    suspend operator fun invoke(email: String, password: String): Result<User> {
        // Validaciones de negocio más estrictas para registro
        if (email.isBlank()) {
            return Result.failure(ValidationException("Email es requerido"))
        }
        
        if (!isValidEmail(email)) {
            return Result.failure(ValidationException("Formato de email inválido"))
        }
        
        if (password.isBlank()) {
            return Result.failure(ValidationException("Contraseña es requerida"))
        }
        
        if (password.length < 8) {
            return Result.failure(ValidationException("Contraseña debe tener al menos 8 caracteres"))
        }
        
        if (!hasValidPasswordComplexity(password)) {
            return Result.failure(ValidationException("Contraseña debe contener al menos una mayúscula y un número"))
        }
        
        // Ejecutar registro
        return try {
            authRepository.signUpWithEmailAndPassword(email.trim(), password)
        } catch (e: Exception) {
            Result.failure(mapRegistrationError(e))
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
    
    private fun hasValidPasswordComplexity(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasNumber = password.any { it.isDigit() }
        return hasUppercase && hasNumber
    }
    
    private fun mapRegistrationError(error: Throwable): Throwable {
        return when {
            error.message?.contains("email address is already in use") == true -> 
                AuthException("Ya existe una cuenta con este email")
            error.message?.contains("weak-password") == true -> 
                ValidationException("La contraseña es muy débil")
            error.message?.contains("network error") == true -> 
                NetworkException("Error de conexión. Verifique su internet")
            else -> AuthException("Error de registro: ${error.message}")
        }
    }
}

/**
 * 🔑 Use Case: Obtener usuario actual
 * Verifica y obtiene el usuario autenticado
 */
class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    
    suspend operator fun invoke(): Result<User?> {
        return try {
            val user = authRepository.getCurrentUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 🚪 Use Case: Cerrar sesión
 * Lógica para logout seguro
 */
class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    
    suspend operator fun invoke(): Result<Unit> {
        return try {
            authRepository.signOut()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 📧 Use Case: Recuperar contraseña
 * Lógica para envío de email de recuperación
 */
class ForgotPasswordUseCase(
    private val authRepository: AuthRepository
) {
    
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(ValidationException("Email es requerido"))
        }
        
        if (!isValidEmail(email)) {
            return Result.failure(ValidationException("Formato de email inválido"))
        }
        
        return try {
            authRepository.sendPasswordResetEmail(email.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
}

/**
 * ❌ Excepciones de dominio
 */
class ValidationException(message: String) : Exception(message)
class AuthException(message: String) : Exception(message)
class BusinessException(message: String) : Exception(message)
class NetworkException(message: String) : Exception(message)