package com.ti3042.airmonitor.core.common.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController

/**
 * 🧭 Utilidades para navegación mejorada usando Navigation Component
 * Implementa mejores prácticas para transiciones y navegación
 * 
 * **Módulo**: :core:common
 * **Propósito**: Funcionalidades de navegación compartidas entre features
 */
object NavigationHelper {
    
    private const val TAG = "NavigationHelper"
    
    /**
     * 🎬 Animaciones personalizadas para transiciones
     */
    object Animations {
        const val SLIDE_IN_RIGHT = android.R.anim.slide_in_left
        const val SLIDE_OUT_LEFT = android.R.anim.slide_out_right
        const val FADE_IN = android.R.anim.fade_in
        const val FADE_OUT = android.R.anim.fade_out
    }
    
    /**
     * 📱 Navegar a una Activity con animación personalizada
     */
    fun navigateToActivity(
        context: Context,
        targetActivity: Class<*>,
        clearStack: Boolean = false,
        extras: Map<String, String>? = null,
        enterAnim: Int = Animations.FADE_IN,
        exitAnim: Int = Animations.FADE_OUT
    ) {
        try {
            val intent = Intent(context, targetActivity).apply {
                if (clearStack) {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                
                // Añadir extras si se proporcionan
                extras?.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }
            
            context.startActivity(intent)
            
            // Aplicar animación si el contexto es una Activity
            if (context is Activity) {
                context.overridePendingTransition(enterAnim, exitAnim)
            }
            
            Log.d(TAG, "✅ Navegación exitosa a: ${targetActivity.simpleName}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error navegando a ${targetActivity.simpleName}: ${e.message}")
        }
    }
    
    /**
     * 🔙 Navegar hacia atrás con Navigation Component
     */
    fun navigateBack(fragment: Fragment): Boolean {
        return try {
            val navController = fragment.findNavController()
            navController.navigateUp() || navController.popBackStack()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error navegando hacia atrás: ${e.message}")
            false
        }
    }
    
    /**
     * 🎯 Navegar a un destino específico con Navigation Component
     */
    fun navigateTo(
        navController: NavController,
        destinationId: Int,
        clearBackStack: Boolean = false
    ): Boolean {
        return try {
            if (clearBackStack) {
                // Limpiar back stack y navegar
                navController.popBackStack(navController.graph.startDestinationId, false)
                navController.navigate(destinationId)
            } else {
                navController.navigate(destinationId)
            }
            
            Log.d(TAG, "✅ Navegación exitosa a destino: $destinationId")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error navegando a destino $destinationId: ${e.message}")
            false
        }
    }
    
    /**
     * 🏠 Volver al inicio del grafo de navegación
     */
    fun navigateToStart(navController: NavController) {
        try {
            navController.popBackStack(navController.graph.startDestinationId, false)
            Log.d(TAG, "✅ Navegación al inicio completada")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error navegando al inicio: ${e.message}")
        }
    }
    
    /**
     * 📊 Logging para analytics de navegación
     */
    fun logNavigation(from: String, to: String, method: String = "navigation_component") {
        try {
            Log.d(TAG, "📊 Navigation: $from → $to (method: $method)")
            // Aquí se puede integrar con Firebase Analytics u otros servicios
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error logging navegación: ${e.message}")
        }
    }
}