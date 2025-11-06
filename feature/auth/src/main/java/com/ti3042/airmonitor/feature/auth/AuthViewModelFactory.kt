package com.ti3042.airmonitor.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ti3042.airmonitor.domain.usecase.*

/**
 * 🏭 Factory temporal para AuthViewModel
 * TODO: Reemplazar con Hilt cuando esté configurado
 * 
 * **Módulo**: :feature:auth
 * **Propósito**: Crear instancias de AuthViewModel con dependencias
 */
class AuthViewModelFactory : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            // Crear instancias de use cases manualmente
            // TODO: Usar inyección de dependencias con Hilt
            val authRepository = com.ti3042.airmonitor.data.repository.AuthRepository()
            
            val signInUseCase = SignInUseCase(authRepository)
            val signUpUseCase = SignUpUseCase(authRepository)
            val getCurrentUserUseCase = GetCurrentUserUseCase(authRepository)
            val signOutUseCase = SignOutUseCase(authRepository)
            val forgotPasswordUseCase = ForgotPasswordUseCase(authRepository)
            
            return AuthViewModel(
                signInUseCase = signInUseCase,
                signUpUseCase = signUpUseCase,
                getCurrentUserUseCase = getCurrentUserUseCase,
                signOutUseCase = signOutUseCase,
                forgotPasswordUseCase = forgotPasswordUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}