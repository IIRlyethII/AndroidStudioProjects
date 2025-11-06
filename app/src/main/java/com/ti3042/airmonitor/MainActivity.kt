package com.ti3042.airmonitor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.ti3042.airmonitor.firebase.FirebaseManager
import com.ti3042.airmonitor.feature.auth.AuthActivity
import com.ti3042.airmonitor.auth.PersistentAuthManager
// import com.ti3042.airmonitor.ui.dashboard.DashboardFragment // Comentado temporalmente
import com.ti3042.airmonitor.ui.dashboard.SimpleDashboardFragment
import com.ti3042.airmonitor.ui.dashboard.DashboardSimpleFragment
import com.ti3042.airmonitor.ui.control.ControlSystemActivity
import com.ti3042.airmonitor.notifications.NotificationHelper

class MainActivity : AppCompatActivity() {
    
    private val tag = "MainActivity"
    private var firebaseManager: FirebaseManager? = null
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authManager: PersistentAuthManager
    
    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "📱 MainActivity onCreate iniciado")
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()
        authManager = PersistentAuthManager(this)
        
        // Check if user is authenticated
        if (firebaseAuth.currentUser == null) {
            Log.d(tag, "❌ Usuario no autenticado, redirecting to login")
            redirectToLogin()
            return
        }
        
        // Log user info from intent extras (from AuthActivity)
        val authSource = intent.getStringExtra("auth_source")
        val userEmail = intent.getStringExtra("user_email")
        if (authSource != null) {
            Log.d(tag, "✅ User authenticated via: $authSource, email: $userEmail")
        }
        
        try {
            setContentView(R.layout.activity_main)
            Log.d(tag, "🎨 Layout principal establecido")
            
            // Inicializar servicios
            initFirebase()
            initNotificationHelper()
            requestNotificationPermissions()
            startFirebaseDataService()
            
            // Usar Navigation Component mejorado
            setupNavigation(savedInstanceState)
            
            Log.d(tag, "🚀 MainActivity iniciada correctamente")
            
        } catch (e: Exception) {
            Log.e(tag, "💥 Error en onCreate: ${e.message}", e)
        }
    }
    
    /**
     * 🧭 Configurar Navigation Component con mejores prácticas
     */
    private fun setupNavigation(savedInstanceState: Bundle?) {
        try {
            if (savedInstanceState == null) {
                Log.d(tag, "📋 Configurando navegación inicial")
                
                // Usar el Fragment correcto según disponibilidad
                val fragmentToShow = try {
                    // Intentar usar DashboardFragment si está disponible
                    DashboardSimpleFragment()
                } catch (e: Exception) {
                    Log.w(tag, "⚠️ Usando DashboardSimpleFragment como fallback")
                    DashboardSimpleFragment()
                }
                
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host, fragmentToShow)
                    .commit()
                    
                Log.d(tag, "✅ Fragment inicial configurado")
                
                // Registrar vista del Dashboard para analytics
                firebaseManager?.logScreenView("Dashboard")
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ Error configurando navegación: ${e.message}")
        }
    }
    
    private fun initFirebase() {
        try {
            firebaseManager = FirebaseManager.getInstance()
            firebaseManager?.initialize(this)
            firebaseManager?.setUserProperties(simulationMode = true)
            Log.d(tag, "Firebase inicializado correctamente")
        } catch (e: Exception) {
            Log.e(tag, "Error inicializando Firebase: ${e.message}")
            // Continuar sin Firebase si falla
        }
    }
    
    /**
     * 📱 Inicializar NotificationHelper
     */
    private fun initNotificationHelper() {
        try {
            notificationHelper = NotificationHelper.getInstance(this)
            Log.d(tag, "✅ NotificationHelper inicializado")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error inicializando NotificationHelper: ${e.message}")
        }
    }
    
    /**
     * 🔒 Solicitar permisos de notificación para Android 13+
     */
    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(tag, "✅ Permisos de notificación concedidos")
                    // Mostrar notificación de bienvenida
                    showWelcomeNotification()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d(tag, "🔔 Explicando necesidad de permisos")
                    // Aquí podrías mostrar un diálogo explicativo
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION
                    )
                }
                else -> {
                    Log.d(tag, "🔔 Solicitando permisos de notificación")
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION
                    )
                }
            }
        } else {
            Log.d(tag, "📱 Android < 13 - Permisos automáticos")
            showWelcomeNotification()
        }
    }
    
    /**
     * 🎉 Mostrar notificación de bienvenida
     */
    private fun showWelcomeNotification() {
        try {
            if (::notificationHelper.isInitialized) {
                notificationHelper.showConnectionStatus(true, "Simulación TI3042")
                Log.d(tag, "📱 Notificación de bienvenida enviada")
            }
        } catch (e: Exception) {
            Log.e(tag, "❌ Error mostrando notificación bienvenida: ${e.message}")
        }
    }
    
    /**
     * 🔒 Manejar respuesta de permisos
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(tag, "✅ Permisos de notificación concedidos")
                    showWelcomeNotification()
                } else {
                    Log.w(tag, "❌ Permisos de notificación denegados")
                    // La app seguirá funcionando sin notificaciones
                }
            }
        }
    }
    
    /**
     * � Abrir el Sistema de Control Avanzado (ahora desde el menú de ajustes)
     */
    private fun openControlSystem() {
        try {
            Log.d(tag, "🔧 Abriendo Sistema de Control")
            
            val intent = Intent(this, ControlSystemActivity::class.java)
            startActivity(intent)
            
            // Log para analytics
            try {
                Log.d(tag, "📊 Sistema de control accedido")
            } catch (e: Exception) {
                Log.e(tag, "Error logging analytics: ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e(tag, "❌ Error abriendo Sistema de Control: ${e.message}")
            android.widget.Toast.makeText(
                this,
                "❌ Error accediendo al sistema de control",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_control_system -> {
                openControlSystem()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_profile -> {
                showUserProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun redirectToLogin() {
        Log.d(tag, "🔐 Redirecting to login with enhanced navigation")
        
        val extras = mapOf(
            "redirect_reason" to "authentication_required",
            "source_activity" to "MainActivity",
            "redirect_timestamp" to System.currentTimeMillis().toString()
        )
        
        // Usar NavigationHelper del módulo :core:common
        com.ti3042.airmonitor.core.common.navigation.NavigationHelper.navigateToActivity(
            context = this,
            targetActivity = AuthActivity::class.java,
            clearStack = true,
            extras = extras ?: emptyMap(),
            enterAnim = com.ti3042.airmonitor.core.common.navigation.NavigationHelper.Animations.SLIDE_IN_RIGHT,
            exitAnim = com.ti3042.airmonitor.core.common.navigation.NavigationHelper.Animations.SLIDE_OUT_LEFT
        )
        
        // Log navigation for analytics
        com.ti3042.airmonitor.core.common.navigation.NavigationHelper.logNavigation(
            from = "MainActivity",
            to = "AuthActivity",
            method = "authentication_required"
        )
        
        finish()
    }
    
    private fun logout() {
        Log.d(tag, "� CERRAR SESIÓN - Usuario solicita logout manual")
        
        // Usar el nuevo gestor persistente para logout completo
        authManager.signOut()
        
        // Clear any cached data if needed
        notificationHelper.clearConnectionNotifications()
        
        // Mostrar confirmación
        android.widget.Toast.makeText(
            this, 
            "✅ Sesión cerrada. Deberás hacer login nuevamente.", 
            android.widget.Toast.LENGTH_LONG
        ).show()
        
        // Redirect to login
        redirectToLogin()
    }
    
    private fun showUserProfile() {
        val user = firebaseAuth.currentUser
        val email = user?.email ?: "Usuario"
        android.widget.Toast.makeText(this, "Usuario: $email", android.widget.Toast.LENGTH_LONG).show()
    }
    
    override fun onResume() {
        super.onResume()
        // Verificar autenticación en resume
        if (firebaseAuth.currentUser == null) {
            redirectToLogin()
            return
        }
        Log.d(tag, "App resumida - Usuario: ${firebaseAuth.currentUser?.email}")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(tag, "App pausada")
    }
    
    /**
     * � Iniciar servicio simplificado de Firebase
     */
    private fun startFirebaseDataService() {
        try {
            val serviceIntent = Intent(this, com.ti3042.airmonitor.services.FirebaseDataService::class.java)
            startService(serviceIntent)
            Log.d(tag, "� FirebaseDataService iniciado correctamente")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error iniciando FirebaseDataService: ${e.message}")
        }
    }
}
