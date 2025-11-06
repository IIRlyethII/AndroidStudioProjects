# 🌬️ Air Quality Monitor TI3042 - GUÍA COMPLETA

## 🎯 **PROYECTO COMPLETO IoT**
**ESP32 + Android + Firebase** - Sistema completo de monitoreo de calidad del aire  
**Estado**: ✅ **FUNCIONAL** con simulación + hardware real preparado  

---

## 📱 **¿QUÚ TIENES AHORA?**

### ✅ **Android App Completa**
- **🏗️ Arquitectura Profesional**: Clean Architecture + MVVM + Hilt
- **🎨 UI Moderna**: Material Design 3 + Jetpack Compose
- **📊 Dashboard Completo**: Datos en tiempo real, gráficos, controles
- **🧪 Testing Activity**: Interfaz dedicada para probar ESP32
- **🔄 Dos Modos**: Simulación (Mock) y Hardware Real

### ✅ **ESP32 Firmware Completo** 
- **📁 Ubicación**: `C:\Users\rlyet\ESP32_AirQualityMonitor_TI3042\`
- **🔌 Comunicación Bluetooth**: Protocolo JSON bidireccional
- **🧪 Múltiples Versiones**: Arduino IDE, PlatformIO, Simulación Wokwi
- **📟 Sensores**: MQ-135, DHT22, OLED, relays, buzzer

### ✅ **Sistema de Comunicación**
- **RealBluetoothService**: Servicio Android para ESP32 real ✅ IMPLEMENTADO
- **MockBluetoothService**: Simulación completa funcional 
- **Protocolo JSON**: Comandos estructurados bidireccionales
- **Reconexión Automática**: Sistema robusto de conexión

---

## 🚀 **CÓMO PROBAR TODO**

### **OPCIÓN 1: Solo Simulación (Inmediato - Sin Hardware)**
```bash
1. Abrir app Android
2. Ir a "ESP32 Testing Lab"
3. Modo SIMULACIÓN (azul) -> Conectar
4. Ver datos cambiando automáticamente
5. Probar comandos: LED ON/OFF, Estado, etc.
```

### **OPCIÓN 2: Hardware Real (Con ESP32)**
```bash
1. Programar ESP32 con código simplificado (LEDs)
2. Emparejar Bluetooth en Android
3. En Testing Lab: Cambiar a modo REAL (verde)
4. Seleccionar ESP32 -> Conectar
5. Comandos controlan LEDs físicos del ESP32
```

---

## 📊 **COMANDOS DISPONIBLES**

| Comando | Android → ESP32 | ESP32 Responde |
|---------|----------------|-----------------|
| `GET_STATUS` | Solicita estado | JSON con todos los sensores |
| `SET_LED_ON` | Encender LED | LED físico se enciende |
| `SET_LED_OFF` | Apagar LED | LED físico se apaga |
| `GET_SENSORS` | Leer sensores | Temperatura, humedad, calidad aire |

---

## 🔧 **INSTALACIÓN RÁPIDA**

### **Para Android (Ya está listo):**
```bash
cd C:\Users\rlyet\AndroidStudioProjects\AirQualityMonitor\
.\gradlew installDebug
# Abre "ESP32 Testing Lab" en el teléfono
```

### **Para ESP32 (Si tienes hardware):**
```bash
1. Abrir VS Code
2. Abrir carpeta: C:\Users\rlyet\ESP32_AirQualityMonitor_TI3042\
3. Usar PlatformIO o Arduino IDE
4. Subir código al ESP32
5. Emparejar Bluetooth con Android
```

---

## 🎓 **PARA LA PRESENTACIÓN**

### **🥇 Demo Recomendado (5 minutos)**
1. **Mostrar app funcionando** (1 min) - Dashboard, datos cambiando
2. **Cambiar entre modos** (1 min) - Simulación vs Real
3. **Explicar arquitectura** (2 min) - Clean Architecture, IoT, Bluetooth
4. **Testing en vivo** (1 min) - Comandos, logs, respuestas

### **📊 Puntos Clave**
- ✨ **Sistema IoT completo** - No solo app bonita
- 🏗️ **Arquitectura profesional** - Código empresarial 
- 🔄 **Dual mode** - Simulación + hardware real
- 🧪 **Testing incluido** - Interfaz para demostrar todo
- 📱 **UI moderna** - Material Design 3 actualizado

---

## 🐛 **Troubleshooting Rápido**

### **App no compila:**
```bash
.\gradlew clean build
```

### **ESP32 no conecta:**
```bash
1. Verificar emparejamiento Bluetooth
2. Reiniciar ESP32
3. Verificar permisos en Android
```

### **No aparece Testing Lab:**
```bash
# Verificar AndroidManifest.xml tiene:
<activity android:name=".testing.ESP32TestingActivity" android:exported="true">
```

---

## 📁 **ARCHIVOS CLAVE**

### **Android:**
```
app/src/main/java/com/ti3042/airmonitor/
├── bluetooth/RealBluetoothService.kt    # ✅ NUEVO - Hardware real
├── testing/ESP32TestingActivity.kt      # ✅ NUEVO - Testing UI
└── bluetooth/BluetoothManager.kt        # ✅ Actualizado - Manager
```

### **ESP32:**
```
C:\Users\rlyet\ESP32_AirQualityMonitor_TI3042\
├── Wokwi_Simulation/ESP32_AirMonitor_Simulation_LEDs.ino  # Versión LEDs
├── Arduino_Firmware/                                      # Versión sensores
└── PlatformIO_Project/                                    # Versión pro
```

---

## 🏆 **ESTADO FINAL**

### **✅ LO QUE FUNCIONA 100%:**
- App Android completa con UI moderna
- Sistema de simulación realista
- Interfaz de testing dedicada
- Comunicación Bluetooth preparada
- Código ESP32 en múltiples versiones
- Documentación completa

### **⚡ LO QUE NECESITA HARDWARE:**
- Testing con ESP32 real (opcional para demostrar)
- Sensores físicos MQ-135, DHT22 (mejora la demo)

---

## 🎉 **RESUMEN EJECUTIVO**

🤖 **Android App** con arquitectura empresarial  
🔌 **ESP32 Firmware** con comunicación Bluetooth  
🧪 **Testing Environment** para demostrar todo  
📚 **Documentación** completa del proyecto  


---
