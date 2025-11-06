# ⚡ TESTING RÁPIDO - Air Quality Monitor TI3042

## 🎯 **TESTING EN 3 PASOS**

### **Paso 1: Simulación (2 minutos)**
```bash
1. Compilar: .\gradlew installDebug
2. Abrir "ESP32 Testing Lab" 
3. Modo SIMULACIÓN -> Conectar
4. ✅ Ver datos cambiando automáticamente
```

### **Paso 2: Hardware Real (5 minutos)**
```bash
1. Programar ESP32 con LEDs
2. Emparejar Bluetooth
3. Cambiar a modo REAL
4. ✅ Comandos controlan ESP32 físico
```

### **Paso 3: Demo Completo (3 minutos)**
```bash
1. Mostrar interfaz Android
2. Explicar arquitectura 
3. Demostrar comunicación
4. ✅ Sistema IoT completo
```

---

## 🚀 Pasos de Testing

### **1. Preparación del Entorno**

#### Android Studio:
```bash
# Verificar que el proyecto compila
./gradlew clean build

# Ejecutar tests unitarios
./gradlew test

# Ejecutar tests instrumentados (opcional)
./gradlew connectedAndroidTest
```

#### VS Code + ESP32:
```bash
# Navegar al directorio ESP32
cd C:\Users\rlyet\ESP32_AirQualityMonitor_TI3042\

# Verificar estructura
dir
```

### **2. Testing con Simulación (Recomendado para empezar)**

#### Paso 1: Abrir ESP32Testing Activity
1. Compilar e instalar la app Android
2. Abrir "ESP32 Testing Lab" desde el launcher
3. Verificar que está en modo "SIMULACIÓN" (azul)

#### Paso 2: Conectar y probar
1. Presionar "🔗 Conectar" 
2. Verificar que conecta automáticamente
3. Observar datos simulados cada 3 segundos
4. Probar comandos: "📊 Estado", "💡 LED ON", "🔲 LED OFF"

### **3. Testing con Hardware Real**

#### Preparación ESP32:
1. Abrir VS Code en: `C:\Users\rlyet\ESP32_AirQualityMonitor_TI3042\`
2. Cargar código simplificado:
```cpp
// Usar: Wokwi_Simulation/ESP32_AirMonitor_Simulation_LEDs.ino
// Este código usa LEDs en lugar de sensores reales
```

#### Paso 1: Programar ESP32
```bash
# Conectar ESP32 por USB
# En VS Code con PlatformIO:
pio run -t upload

# O en Arduino IDE:
# File -> Open -> ESP32_AirMonitor_Simulation_LEDs.ino
# Tools -> Board -> ESP32 Dev Module
# Tools -> Port -> COMx (donde x es tu puerto)
# Upload
```

#### Paso 2: Emparejar Bluetooth
1. En Android: Configuración -> Bluetooth
2. Buscar "ESP32_AirMonitor"
3. Emparejar dispositivo

#### Paso 3: Testing en Android
1. En ESP32Testing Activity, presionar "🔄 Cambiar Modo"
2. Verificar que cambia a modo "REAL" (verde)
3. Seleccionar el ESP32 de la lista de dispositivos
4. Presionar "Conectar"
5. Probar comandos y observar LEDs en el ESP32

---

## 📊 Comandos de Testing Disponibles

| Comando | Descripción | Respuesta ESP32 |
|---------|-------------|-----------------|
| `GET_STATUS` | Obtiene estado actual | JSON con temperatura, humedad, etc. |
| `SET_LED_ON` | Enciende LED indicador | LED físico se enciende |
| `SET_LED_OFF` | Apaga LED indicador | LED físico se apaga |
| `GET_SENSORS` | Lee todos los sensores | JSON completo de sensores |
| `RESET` | Reinicia ESP32 | Dispositivo se reinicia |

---

## 🔍 Protocolo de Comunicación

### **Formato JSON Android → ESP32:**
```json
{
  "command": "GET_STATUS",
  "timestamp": 1703001234567
}
```

### **Formato JSON ESP32 → Android:**
```json
{
  "status": "OK",
  "data": {
    "temperature": 23.5,
    "humidity": 45.2,
    "airQuality": 850,
    "co2": 420,
    "pm25": 15.3,
    "pm10": 20.1
  },
  "timestamp": 1703001234567,
  "device": "ESP32_AirMonitor"
}
```

---

## 🐛 Troubleshooting

### **Problema: No encuentra dispositivos Bluetooth**
```bash
# Solución:
1. Verificar permisos en Android (Ubicación + Bluetooth)
2. Asegurar que ESP32 está emparejado
3. Reiniciar Bluetooth en Android
4. Verificar que ESP32 está transmitiendo
```

### **Problema: Se conecta pero no recibe datos**
```bash
# Verificar:
1. ESP32 está enviando datos (Serial Monitor)
2. Formato JSON es correcto
3. No hay interferencia Bluetooth
4. Reiniciar conexión
```

### **Problema: ESP32 no responde a comandos**
```bash
# Verificar:
1. Código ESP32 procesa comandos correctamente
2. Buffer serial no está lleno
3. JSON parsing funciona en ESP32
4. Comandos tienen formato correcto
```

### **Problema: App Android se cierra**
```bash
# Verificar logs:
adb logcat -s "BluetoothManager" "RealBluetoothService" "ESP32Testing"

# Común:
1. Permisos faltantes
2. Bluetooth deshabilitado
3. Memoria insuficiente
```

---

## 📱 Interfaz de Testing

### **Indicadores de Estado:**
- 🔵 **Azul**: Modo Simulación
- 🟢 **Verde**: Modo Real
- 🔗 **Conectado**: Dispositivo enlazado
- 🔌 **Desconectado**: Sin conexión

### **Botones Principales:**
- **🔄 Cambiar Modo**: Alterna entre simulación/real
- **🔗 Conectar**: Establece conexión
- **📊 Estado**: Solicita datos del ESP32
- **💡 LED ON/OFF**: Controla LED físico

### **Log en Tiempo Real:**
- Muestra todos los eventos de comunicación
- Timestamps para debugging
- Mensajes de error detallados

---

## 🎯 Objetivos de Testing

### **✅ Testing Básico:**
1. App compila sin errores
2. Modo simulación funciona
3. UI responde correctamente
4. Logs muestran actividad

### **✅ Testing Intermedio:**
1. ESP32 se programa correctamente
2. Bluetooth empareja sin problemas
3. Conexión se establece
4. Comandos básicos funcionan

### **✅ Testing Avanzado:**
1. Comunicación bidireccional estable
2. Reconexión automática funciona
3. Manejo de errores robusto
4. Datos se sincronizan con Firebase

---

## 📚 Archivos Importantes

### **Android:**
```
app/src/main/java/com/ti3042/airmonitor/
├── bluetooth/
│   ├── BluetoothManager.kt          # Manager principal
│   ├── RealBluetoothService.kt      # Servicio real
│   └── MockBluetoothService.kt      # Servicio simulación
├── testing/
│   └── ESP32TestingActivity.kt      # Interfaz de testing
└── data/
    └── SensorData.kt                # Modelo de datos
```

### **ESP32:**
```
ESP32_AirQualityMonitor_TI3042/
├── Arduino_Firmware/               # Versión completa con sensores
├── PlatformIO_Project/             # Versión profesional
├── Wokwi_Simulation/              # Versión simplificada con LEDs
└── Documentation/                  # Guías y esquemas
```

---

## 🎓 Para la Presentación

### **Demostración Recomendada:**
1. **Mostrar código**: Arquitectura Android + ESP32
2. **Simulación**: Funcionamiento sin hardware
3. **Hardware real**: ESP32 con LEDs respondiendo
4. **Logs en tiempo real**: Debugging visible
5. **Cambio de modos**: Flexibilidad del sistema

### **Puntos Clave:**
- ✨ Sistema completo IoT
- 🏗️ Arquitectura profesional
- 🔄 Comunicación bidireccional
- 🧪 Testing comprehensivo
- 📱 UI moderna Material Design 3
- 🔥 Integración Firebase

---