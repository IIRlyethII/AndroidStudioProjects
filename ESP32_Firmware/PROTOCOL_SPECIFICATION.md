# 📋 **PROTOCOLO DE COMUNICACIÓN ESP32 ↔ ANDROID**
## TI3042 - Air Quality Monitor

---

## 🎯 **ESPECIFICACIÓN DEL PROTOCOLO**

### **📱 FORMATO BASE JSON**
```json
{
    "type": "sensor_data | command | response | error",
    "timestamp": 1699123456789,
    "device_id": "ESP32_TI3042", 
    "version": "1.0.0",
    "data": { /* payload específico */ }
}
```

---

## 📊 **1. DATOS DEL SENSOR (ESP32 → ANDROID)**

### **📡 Mensaje completo cada 5 segundos:**
```json
{
    "device": "ESP32_TI3042",
    "version": "1.0.0",
    "timestamp": 1699123456789,
    "air_quality": {
        "ppm": 1250,
        "level": "moderate",
        "temperature": 23.5,
        "humidity": 65,
        "gas_composition": {
            "oxygen": 20.9,
            "co2": 1000.0,
            "smoke": 125.0,
            "vapor": 62.5,
            "others": 62.5
        }
    },
    "system": {
        "fan_status": true,
        "buzzer_active": false,
        "auto_mode": true,
        "uptime": 3600,
        "battery_level": 100,
        "wifi_signal": -45,
        "bluetooth_connected": true
    },
    "thresholds": {
        "warning": 1000,
        "critical": 2000
    }
}
```

### **📊 Campos explicados:**
- **ppm**: Partes por millón de CO2/gases detectados
- **level**: "good" (<1000), "moderate" (1000-1999), "poor" (≥2000)
- **temperature**: Grados Celsius con 1 decimal
- **humidity**: Porcentaje de humedad relativa
- **gas_composition**: Desglose estimado de gases
- **uptime**: Segundos desde el arranque del ESP32

---

## 🎛️ **2. COMANDOS DE CONTROL (ANDROID → ESP32)**

### **🌀 Control del Ventilador:**
```json
{
    "action": "control",
    "timestamp": 1699123456789,
    "fan": {
        "enable": true
    }
}
```

### **🔔 Control del Buzzer:**
```json
{
    "action": "control", 
    "timestamp": 1699123456789,
    "buzzer": {
        "enable": false
    }
}
```

### **🤖 Cambiar Modo Automático:**
```json
{
    "action": "control",
    "timestamp": 1699123456789,
    "auto_mode": true
}
```

### **⚙️ Configurar Umbrales:**
```json
{
    "action": "configure",
    "timestamp": 1699123456789,
    "thresholds": {
        "warning": 1200,
        "critical": 2500
    }
}
```

### **📊 Solicitar Estado:**
```json
{
    "action": "status",
    "timestamp": 1699123456789
}
```

---

## 📤 **3. RESPUESTAS DEL ESP32**

### **✅ Respuesta Exitosa:**
```json
{
    "type": "response",
    "success": true,
    "message": "Control executed successfully",
    "timestamp": 1699123456789
}
```

### **❌ Respuesta de Error:**
```json
{
    "type": "response", 
    "success": false,
    "message": "Invalid JSON format",
    "timestamp": 1699123456789
}
```

---

## 🔄 **4. FLUJO DE COMUNICACIÓN**

### **📱 Conexión Inicial:**
```mermaid
Android  ──(conectar BT)──>  ESP32
ESP32    ──(confirmar)────>  Android
ESP32    ──(datos iniciales)─> Android  
```

### **📊 Intercambio Continuo:**
```
ESP32: Envía datos cada 5s
Android: Procesa y actualiza UI
Android: Envía comandos cuando sea necesario
ESP32: Ejecuta y responde confirmación
```

---

## 🛠️ **5. IMPLEMENTACIÓN EN ANDROID**

### **📱 Actualizar RealBluetoothService:**
```kotlin
class RealBluetoothService : BluetoothService {
    
    override fun processReceivedData(jsonData: String) {
        try {
            val sensorData = JsonParser.parseJsonToSensorData(jsonData)
            sensorData?.let { 
                callback?.onDataReceived(it)
                Log.d(TAG, "📊 Datos recibidos: PPM=${it.airQuality.ppm}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parseando datos: ${e.message}")
        }
    }
    
    override fun sendCommand(command: ControlCommand): Boolean {
        return try {
            val jsonCommand = JsonParser.commandToJson(command)
            bluetoothSocket?.outputStream?.write(jsonCommand.toByteArray())
            Log.d(TAG, "📤 Comando enviado: $jsonCommand")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando comando: ${e.message}")
            false
        }
    }
}
```

---

## 🔍 **6. DEBUGGING Y TESTING**

### **📊 Logs ESP32:**
```cpp
Serial.println("📱 Datos enviados via Bluetooth");
Serial.println("📤 Respuesta enviada: " + message);
Serial.println("🎛️ Comando ejecutado: " + action);
```

### **📱 Logs Android:**
```kotlin
Log.d(TAG, "📊 Datos recibidos: PPM=${sensorData.airQuality.ppm}")
Log.d(TAG, "📤 Enviando comando: ${command.action}")
Log.d(TAG, "🔗 Estado Bluetooth: ${isConnected()}")
```

---

## ⚠️ **7. MANEJO DE ERRORES**

### **🔄 Reconexión Automática:**
```kotlin
private fun handleConnectionLost() {
    Log.w(TAG, "🔄 Conexión perdida, intentando reconectar...")
    reconnectAttempts++
    
    if (reconnectAttempts <= MAX_RECONNECT_ATTEMPTS) {
        Handler().postDelayed({
            connect(lastDeviceAddress)
        }, RECONNECT_DELAY)
    }
}
```

### **📊 Validación de Datos:**
```cpp
// En ESP32
if (isnan(temperature) || isnan(humidity)) {
    sendErrorResponse("Sensor DHT22 error");
    return;
}

if (ppm < 0 || ppm > 5000) {
    sendErrorResponse("PPM out of range");
    return;
}
```

---

## 🎯 **RESULTADO ESPERADO**

Después de implementar este protocolo:

✅ **Comunicación bidireccional** fluida y confiable  
✅ **Control remoto** completo desde Android  
✅ **Monitoreo en tiempo real** con datos precisos  
✅ **Manejo de errores** robusto y recuperación automática  
✅ **Logs detallados** para debugging eficiente  

**🚀 Tu proyecto pasará de simulación a sistema IoT real funcionando!**