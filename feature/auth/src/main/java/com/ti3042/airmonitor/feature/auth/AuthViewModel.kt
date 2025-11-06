package com.ti3042.airmonitor.feature.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ti3042.airmonitor.domain.model.User
import com.ti3042.airmonitor.domain.usecase.*
import kotlinx.coroutines.launch

/**
 * 🧠 ViewModel para la pantalla de autenticación
 * Implementa patrón MVVM usando Clean Architecture con Use Cases
 * 
 * **Módulo**: :feature:auth
 * **Propósito**: Gestionar estado y lógica de UI para autenticación
 */
class AuthViewModel(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {
    
    private val tag = "AuthViewModel"
    
    // Estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Estado de autenticación exitosa
    private val _authSuccess = MutableLiveData<User>()
    val authSuccess: LiveData<User> = _authSuccess
    
    // Mensajes de error
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // Mensajes de éxito
    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage
    
    // Estado de validación de formulario
    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError
    
    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError
    
    init {
        checkCurrentUser()
    }
    
    /**
     * 🔍 Verifica si hay un usuario autenticado al iniciar
     */
    private fun checkCurrentUser() {
        viewModelScope.launch {
            try {
                val result = getCurrentUserUseCase()
                result.fold(
                    onSuccess = { user ->
                        user?.let { 
                            _authSuccess.value = it
                            Log.d(tag, "✅ User already authenticated: ${it.email}")
                        }
                    },
                    onFailure = { 
                        Log.d(tag, "ℹ️ No user currently authenticated")
                    }
                )
            } catch (e: Exception) {
                Log.w(tag, "Error checking current user", e)
            }
        }
    }
    
    /**
     * 🔐 Inicia sesión con email y contraseña
     */
    fun signIn(email: String, password: String) {
        if (!validateInput(email.trim(), password.trim())) {
            return
        }
        
        _isLoading.value = true
        Log.d(tag, "🔐 Starting sign in process")
        
        viewModelScope.launch {
            try {
                val result = signInUseCase(email.trim(), password.trim())
                
                result.fold(
                    onSuccess = { user ->
                        _isLoading.value = false
                        _authSuccess.value = user
                        _successMessage.value = "Bienvenido ${user.getDisplayNameOrEmail()}"
                        Log.d(tag, "✅ Sign in successful")
                    },
                    onFailure = { exception ->
                        _isLoading.value = false
                        _errorMessage.value = exception.message ?: "Error de autenticación"
                        Log.w(tag, "❌ Sign in failed: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Error inesperado: ${e.message}"
                Log.e(tag, "💥 Unexpected error during sign in", e)
            }
        }
    }
    
    /**
     * 📝 Registra un nuevo usuario con email y contraseña
     */
    fun signUp(email: String, password: String) {
        if (!validateInput(email.trim(), password.trim())) {
            return
        }
        
        _isLoading.value = true
        Log.d(tag, "📝 Starting sign up process")
        
        viewModelScope.launch {
            try {
                val result = signUpUseCase(email.trim(), password.trim())
                
                result.fold(
                    onSuccess = { user ->
                        _isLoading.value = false
                        _authSuccess.value = user
                        _successMessage.value = "Cuenta creada exitosamente para ${user.email}"
                        Log.d(tag, "✅ Sign up successful")
                    },
                    onFailure = { exception ->
                        _isLoading.value = false
                        _errorMessage.value = exception.message ?: "Error de registro"
                        Log.w(tag, "❌ Sign up failed: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Error inesperado: ${e.message}"
                Log.e(tag, "💥 Unexpected error during sign up", e)
            }
        }
    }
    
    /**
     * 📧 Envía email de recuperación de contraseña
     */
    fun sendPasswordReset(email: String) {
        if (email.isEmpty()) {
            _emailError.value = "Ingrese su email para recuperar la contraseña"
            return
        }
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val result = forgotPasswordUseCase(email.trim())
                
                result.fold(
                    onSuccess = {
                        _isLoading.value = false
                        _successMessage.value = "Email de recuperación enviado a $email"
                        Log.d(tag, "✅ Password reset email sent")
                    },
                    onFailure = { exception ->
                        _isLoading.value = false
                        _errorMessage.value = exception.message ?: "Error al enviar email"
                        Log.w(tag, "❌ Password reset failed: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Error inesperado: ${e.message}"
                Log.e(tag, "💥 Unexpected error during password reset", e)
            }
        }
    }
    
    /**
     * 🚪 Cerrar sesión
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                signOutUseCase()
                _successMessage.value = "Sesión cerrada exitosamente"
                Log.d(tag, "✅ Sign out successful")
            } catch (e: Exception) {
                _errorMessage.value = "Error al cerrar sesión: ${e.message}"
                Log.w(tag, "❌ Sign out failed", e)
            }
        }
    }
    
    /**
     * ✅ Valida los datos de entrada del formulario
     */
    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true
        
        // Validar email
        when {
            email.isEmpty() -> {
                _emailError.value = "Email es requerido"
                isValid = false
            }
            !isValidEmail(email) -> {
                _emailError.value = "Ingrese un email válido"
                isValid = false
            }
            else -> {
                _emailError.value = null
            }
        }
        
        // Validar contraseña
        when {
            password.isEmpty() -> {
                _passwordError.value = "Contraseña es requerida"
                isValid = false
            }
            password.length < 6 -> {
                _passwordError.value = "Contraseña debe tener al menos 6 caracteres"
                isValid = false
            }
            else -> {
                _passwordError.value = null
            }
        }
        
        return isValid
    }
    
    /**
     * ✉️ Valida formato de email
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
    
    /**
     * 🧹 Limpia los mensajes de error
     */
    fun clearErrors() {
        _emailError.value = null
        _passwordError.value = null
        _errorMessage.value = ""
        _successMessage.value = ""
    }
}