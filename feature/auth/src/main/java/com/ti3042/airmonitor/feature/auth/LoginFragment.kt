package com.ti3042.airmonitor.feature.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.ti3042.airmonitor.core.common.navigation.NavigationHelper
import com.ti3042.airmonitor.feature.auth.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

/**
 * 🔐 Fragment de Login usando Clean Architecture
 * Maneja la UI de autenticación en el módulo :feature:auth
 * 
 * **Módulo**: :feature:auth
 * **Propósito**: UI para inicio de sesión y registro de usuarios
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory() }
    
    // NavigationHelper es un object, no necesita instanciación
    
    private val tag = "LoginFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
        
        Log.d(tag, "🎯 LoginFragment initialized")
    }

    /**
     * 🖱️ Configura los listeners de botones
     */
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.textInputEmail.editText?.text.toString()
            val password = binding.textInputPassword.editText?.text.toString()
            
            authViewModel.clearErrors()
            authViewModel.signIn(email, password)
            
            Log.d(tag, "🔐 Login attempt for: $email")
        }
        
        binding.btnRegister.setOnClickListener {
            val email = binding.textInputEmail.editText?.text.toString()
            val password = binding.textInputPassword.editText?.text.toString()
            
            authViewModel.clearErrors()
            authViewModel.signUp(email, password)
            
            Log.d(tag, "📝 Register attempt for: $email")
        }
        
        binding.textForgotPassword.setOnClickListener {
            val email = binding.textInputEmail.editText?.text.toString()
            
            if (email.isNotEmpty()) {
                authViewModel.sendPasswordReset(email)
            } else {
                showSnackbar("Ingrese su email para recuperar la contraseña", isError = true)
            }
        }
    }

    /**
     * 👀 Observa los cambios en el ViewModel
     */
    private fun observeViewModel() {
        // Observar estado de carga
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }
        
        // Observar autenticación exitosa
        authViewModel.authSuccess.observe(viewLifecycleOwner) { user ->
            Log.d(tag, "✅ Authentication successful for: ${user.email}")
            navigateToMain()
        }
        
        // Observar errores de email
        authViewModel.emailError.observe(viewLifecycleOwner) { error ->
            binding.textInputEmail.error = error
        }
        
        // Observar errores de contraseña
        authViewModel.passwordError.observe(viewLifecycleOwner) { error ->
            binding.textInputPassword.error = error
        }
        
        // Observar mensajes de error generales
        authViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                showSnackbar(message, isError = true)
            }
        }
        
        // Observar mensajes de éxito
        authViewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                showSnackbar(message, isError = false)
            }
        }
    }

    /**
     * 🔄 Muestra/oculta el indicador de carga
     */
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false
            binding.btnRegister.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            binding.btnRegister.isEnabled = true
        }
    }

    /**
     * 📱 Navega a la pantalla principal
     */
    private fun navigateToMain() {
        try {
            // Usar navegación simple por ahora (TODO: mejorar cuando esté integrado en :app)
            val intent = android.content.Intent().apply {
                setClassName(requireContext(), "com.ti3042.airmonitor.MainActivity")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            Log.d(tag, "🎯 Navigation to main successful")
        } catch (e: Exception) {
            Log.e(tag, "❌ Navigation to main failed", e)
            showSnackbar("Error al navegar: ${e.message}", isError = true)
        }
    }

    /**
     * 📢 Muestra mensajes usando Snackbar
     */
    private fun showSnackbar(message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        
        if (isError) {
            snackbar.setBackgroundTint(
                resources.getColor(android.R.color.holo_red_light, null)
            )
        } else {
            snackbar.setBackgroundTint(
                resources.getColor(android.R.color.holo_green_light, null)
            )
        }
        
        snackbar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}