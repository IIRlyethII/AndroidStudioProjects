# 🌬️ Air Quality Monitor TI3042 - GUÍA COMPLETA

## 🎯 **PROYECTO COMPLETO IoT**
**ESP32 + Android + Firebase** - Sistema completo de monitoreo de calidad del aire  
**Estado**: ✅ **FUNCIONAL** con simulación + hardware real preparado  

---

## 📱 **¿QUÉ TIENES AHORA?**

### ✅ **Revisar la carpeta con documentacion adjunta**
- **📁 Ubicación**: `AndroidStudioProjects/.Documentos De Evaluacion`
- **🧩 Firmware simplificado.zip**: Comprimido con los codigos del ESP32 simplficacos
- **📝 SISTEMA_AUTH_PERSISTENTE.md**: Informacion de la autenticacion al ingresar una cuenta

### ✅ **Android App Completa**
- **🏗️ Arquitectura Profesional**: Clean Architecture + MVVM + Hilt
- **🎨 UI Moderna**: Material Design 3 + Jetpack Compose
- **📊 Dashboard Completo**: Datos en tiempo real, gráficos, controles
- **🧪 Testing Activity**: Interfaz dedicada para probar ESP32
- **🔄 Dos Modos**: Simulación (Mock) y Hardware Real

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
