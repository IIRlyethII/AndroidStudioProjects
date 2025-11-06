package com.ti3042.airmonitor.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper para manejo de permisos Bluetooth
 */
object PermissionHelper {
    
    const val BLUETOOTH_PERMISSION_REQUEST = 1001
    
    /**
     * Permisos requeridos según la versión de Android
     */
    private val bluetoothPermissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    /**
     * Verifica si todos los permisos Bluetooth están concedidos
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return bluetoothPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Solicita los permisos Bluetooth necesarios
     */
    fun requestBluetoothPermissions(activity: Activity) {
        val missingPermissions = bluetoothPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                BLUETOOTH_PERMISSION_REQUEST
            )
        }
    }

    /**
     * Verifica si se debe mostrar la razón del permiso
     */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return bluetoothPermissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    /**
     * Maneja el resultado de la solicitud de permisos
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST) {
            val allPermissionsGranted = grantResults.all { result ->
                result == PackageManager.PERMISSION_GRANTED
            }
            
            if (allPermissionsGranted) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }
}

