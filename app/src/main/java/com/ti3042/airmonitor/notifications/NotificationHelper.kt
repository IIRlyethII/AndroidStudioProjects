package com.ti3042.airmonitor.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ti3042.airmonitor.MainActivity
import com.ti3042.airmonitor.R

/**
 * 📱 NotificationHelper - Sistema de Notificaciones TI3042
 * 
 * Maneja notificaciones push del sistema para alertas de calidad del aire
 * Compatible con Android 8.0+ (Canales) y Android 13+ (Permisos)
 */
class NotificationHelper private constructor(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        
        // 🔔 Canales de Notificación
        private const val CHANNEL_AIR_QUALITY = "air_quality_alerts"
        private const val CHANNEL_SYSTEM_STATUS = "system_status"
        private const val CHANNEL_CONNECTION = "connection_status"
        
        // 🆔 IDs de Notificación
        private const val NOTIFICATION_AIR_QUALITY = 1001
        private const val NOTIFICATION_SYSTEM_STATUS = 1002
        private const val NOTIFICATION_CONNECTION = 1003
        
        // Singleton
        @Volatile
        private var INSTANCE: NotificationHelper? = null
        
        fun getInstance(context: Context): NotificationHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannels()
    }
    
    /**
     * 📢 Crear canales de notificación (Android 8.0+)
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // 🌬️ Canal para Calidad del Aire
                val airQualityChannel = NotificationChannel(
                    CHANNEL_AIR_QUALITY,
                    "Alertas de Calidad del Aire",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones sobre niveles críticos de PPM y calidad del aire"
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                }
                
                // 🔧 Canal para Estado del Sistema
                val systemChannel = NotificationChannel(
                    CHANNEL_SYSTEM_STATUS,
                    "Estado del Sistema",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Información sobre ventilador, sensores y componentes"
                    enableLights(true)
                    lightColor = android.graphics.Color.BLUE
                }
                
                // 📡 Canal para Conexión
                val connectionChannel = NotificationChannel(
                    CHANNEL_CONNECTION,
                    "Estado de Conexión",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Estado de conexión ESP32 y Bluetooth"
                    enableLights(false)
                    setSound(null, null)
                }
                
                // Registrar canales
                notificationManager.createNotificationChannel(airQualityChannel)
                notificationManager.createNotificationChannel(systemChannel)
                notificationManager.createNotificationChannel(connectionChannel)
                
                Log.d(TAG, "✅ Canales de notificación creados")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creando canales: ${e.message}")
            }
        }
    }
    
    /**
     * 🚨 Mostrar notificación de calidad del aire
     */
    fun showAirQualityAlert(ppm: Int, level: String, temperature: Float, humidity: Int) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "⚠️ Sin permisos de notificación")
            return
        }
        
        try {
            val (title, message, icon, priority) = getAirQualityNotificationData(ppm, level)
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_AIR_QUALITY)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$message\n\n🌡️ Temperatura: ${temperature}°C\n💧 Humedad: ${humidity}%\n\n📊 Monitoreo TI3042"))
                .setPriority(priority)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(getNotificationColor(level))
                .addAction(
                    R.drawable.ic_launcher_foreground, 
                    "Ver Dashboard", 
                    pendingIntent
                )
                .setGroup("AIR_QUALITY_GROUP")
                .build()
            
            if (ActivityCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                notificationManager.notify(NOTIFICATION_AIR_QUALITY, notification)
                Log.d(TAG, "📱 Notificación calidad aire enviada - PPM: $ppm")
            } else {
                Log.w(TAG, "⚠️ Sin permisos para notificaciones")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error mostrando notificación aire: ${e.message}")
        }
    }
    
    /**
     * 🔧 Mostrar notificación de estado del sistema
     */
    fun showSystemStatusUpdate(fanStatus: Boolean, buzzerActive: Boolean, uptime: String) {
        if (!hasNotificationPermission()) return
        
        try {
            val title = "🔧 Sistema TI3042"
            val message = "Ventilador: ${if (fanStatus) "ON" else "OFF"} • Buzzer: ${if (buzzerActive) "ON" else "OFF"}"
            
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM_STATUS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$message\n\n⏱️ Tiempo activo: $uptime\n📡 Monitoreo IoT Activo"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(android.graphics.Color.parseColor("#673AB7"))
                .setGroup("SYSTEM_STATUS_GROUP")
                .build()
            
            if (ActivityCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                notificationManager.notify(NOTIFICATION_SYSTEM_STATUS, notification)
                Log.d(TAG, "📱 Notificación sistema enviada")
            } else {
                Log.w(TAG, "⚠️ Sin permisos para notificaciones de sistema")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error notificación sistema: ${e.message}")
        }
    }
    
    /**
     * 📡 Mostrar notificación de conexión
     */
    fun showConnectionStatus(isConnected: Boolean, deviceName: String = "ESP32") {
        if (!hasNotificationPermission()) return
        
        try {
            val (title, message, icon) = if (isConnected) {
                Triple("📡 Conectado", "Recibiendo datos de $deviceName", R.drawable.ic_launcher_foreground)
            } else {
                Triple("📡 Desconectado", "Sin conexión con $deviceName", R.drawable.ic_launcher_foreground)
            }
            
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_CONNECTION)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(if (isConnected) android.graphics.Color.GREEN else android.graphics.Color.RED)
                .setOngoing(isConnected) // Mantener visible si está conectado
                .build()
            
            if (ActivityCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                notificationManager.notify(NOTIFICATION_CONNECTION, notification)
                Log.d(TAG, "📱 Notificación conexión enviada: $isConnected")
            } else {
                Log.w(TAG, "⚠️ Sin permisos para notificaciones de conexión")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error notificación conexión: ${e.message}")
        }
    }
    
    /**
     * 🎨 Obtener datos de notificación según PPM y nivel
     */
    private fun getAirQualityNotificationData(ppm: Int, level: String): NotificationData {
        return when {
            ppm >= 400 -> NotificationData(
                title = "🚨 ¡ALERTA CRÍTICA!",
                message = "Calidad del aire PELIGROSA - $ppm PPM",
                icon = R.drawable.ic_launcher_foreground,
                priority = NotificationCompat.PRIORITY_MAX
            )
            ppm >= 300 -> NotificationData(
                title = "⚠️ Alerta Alta",
                message = "Calidad del aire MALA - $ppm PPM",
                icon = R.drawable.ic_launcher_foreground,
                priority = NotificationCompat.PRIORITY_HIGH
            )
            ppm >= 250 -> NotificationData(
                title = "⚠️ Precaución",
                message = "Calidad del aire MODERADA - $ppm PPM",
                icon = R.drawable.ic_launcher_foreground,
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
            ppm >= 200 -> NotificationData(
                title = "ℹ️ Información",
                message = "Calidad del aire ACEPTABLE - $ppm PPM",
                icon = R.drawable.ic_launcher_foreground,
                priority = NotificationCompat.PRIORITY_LOW
            )
            else -> NotificationData(
                title = "✅ Excelente",
                message = "Calidad del aire BUENA - $ppm PPM",
                icon = R.drawable.ic_launcher_foreground,
                priority = NotificationCompat.PRIORITY_LOW
            )
        }
    }
    
    /**
     * 🎨 Obtener color de notificación según nivel
     */
    private fun getNotificationColor(level: String): Int {
        return when (level.uppercase()) {
            "BUENA", "GOOD" -> android.graphics.Color.GREEN
            "MODERADA", "MODERATE" -> android.graphics.Color.parseColor("#FF9800")
            "MALA", "POOR" -> android.graphics.Color.parseColor("#FF5722")
            "CRÍTICA", "CRITICAL" -> android.graphics.Color.RED
            else -> android.graphics.Color.parseColor("#673AB7")
        }
    }
    
    /**
     * 🔒 Verificar permisos de notificación (Android 13+)
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permisos automáticos en versiones anteriores
        }
    }
    
    /**
     * 🗑️ Limpiar notificaciones específicas
     */
    fun clearAirQualityNotifications() {
        notificationManager.cancel(NOTIFICATION_AIR_QUALITY)
    }
    
    fun clearSystemNotifications() {
        notificationManager.cancel(NOTIFICATION_SYSTEM_STATUS)
    }
    
    fun clearConnectionNotifications() {
        notificationManager.cancel(NOTIFICATION_CONNECTION)
    }
    
    fun clearAllNotifications() {
        notificationManager.cancelAll()
    }
    
    /**
     * 📊 Data class para notificaciones
     */
    private data class NotificationData(
        val title: String,
        val message: String,
        val icon: Int,
        val priority: Int
    )
}