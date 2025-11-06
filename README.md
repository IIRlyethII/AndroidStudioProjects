# 🌬️ Air Quality Monitor TI3042

## 📱 Sistema IoT Completo de Monitoreo de Calidad de Aire

### 🎯 **Proyecto Académico TI3042** - Sistema integral de monitoreo ambiental con ESP32 y Android

---

## ✨ **Características Principales**

### 📱 **Aplicación Android**
- 🔐 **Autenticación persistente** - Login una sola vez (30 días)
- 🎨 **Material Design 3** - Interfaz moderna y profesional  
- 📊 **Dashboard en tiempo real** - Métricas y gráficos en vivo
- 🔧 **Sistema de control avanzado** - Control remoto del ESP32
- 🌟 **Arquitectura Clean** - Modular, escalable y mantenible
- 🔥 **Firebase completo** - Auth, Firestore, Cloud Functions

### 🛠️ **Hardware ESP32**
- 🌬️ **Sensores de calidad de aire** - PPM, temperatura, humedad
- 📡 **Comunicación Bluetooth** - Protocolo JSON personalizado  
- ⚡ **Control automático** - Ventilador y alarmas inteligentes
- 🎛️ **Modo simulación** - Testing sin hardware físico
- 🔄 **Multi-dispositivo** - Soporte para varios ESP32

---

## 🏗️ **Arquitectura Técnica**

### **🔧 Stack Tecnológico**
```
📱 FRONTEND
├── Kotlin + Android SDK 36
├── Jetpack Components (Navigation, Lifecycle, ViewModel)  
├── Material Design 3 + ViewBinding
├── Coroutines + Flow
└── Multi-module Architecture

🔥 BACKEND  
├── Firebase Authentication
├── Cloud Firestore  
├── Firebase Storage
├── Cloud Functions
└── Real-time Database

🛠️ HARDWARE
├── ESP32 DevKit V1
├── Sensor MQ-135 (Calidad aire)
├── DHT22 (Temperatura/Humedad)
├── Módulo Bluetooth HC-05
└── Sistema de ventilación
```

### **🏛️ Clean Architecture Multi-Module**
```
📦 AirQualityMonitor/
├── 🎯 app/                    # Main application
├── 🎨 core/
│   ├── common/               # Shared utilities
│   └── ui/                   # UI components  
├── 📊 data/                  # Data layer
├── 🧠 domain/                # Business logic
├── 🎭 feature/
│   ├── auth/                 # Authentication
│   ├── dashboard/            # Main dashboard
│   ├── control/              # Device control
│   └── monitoring/           # Analytics & reports
└── 🛠️ ESP32_Firmware/         # Hardware code
```

---

## 🚀 **Instalación y Configuración**

### **📋 Prerrequisitos**
- 📱 Android Studio Hedgehog+ (2023.1.1+)
- ☕ JDK 17 o superior  
- 🔥 Cuenta Firebase configurada
- 🛠️ Arduino IDE (para ESP32)
- 📡 ESP32 DevKit V1

### **⚙️ Configuración Firebase**

1. **Crear proyecto Firebase:**
   ```bash
   # Ir a https://console.firebase.google.com/
   # Crear proyecto "AirQualityMonitor"
   # Habilitar Authentication (Email/Password)
   # Crear base de datos Firestore
   ```

2. **Configurar aplicación Android:**
   ```bash
   # Descargar google-services.json
   # Colocar en /app/google-services.json  
   # Configurar SHA-1 fingerprint
   ```

3. **Reglas Firestore de seguridad:**
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
       match /sensor_data/{document} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

### **📱 Instalación Android**

```bash
# 1. Clonar repositorio
git clone https://github.com/IIRlyethII/AndroidStudioProjects.git
cd AndroidStudioProjects/AirQualityMonitor

# 2. Abrir en Android Studio
# File > Open > Seleccionar carpeta del proyecto

# 3. Configurar Firebase  
# Colocar google-services.json en /app/

# 4. Sync y Build
./gradlew build

# 5. Ejecutar en dispositivo/emulador
./gradlew installDebug
```

### **🛠️ Configuración ESP32**

```bash
# 1. Instalar Arduino IDE + ESP32 Board
# 2. Abrir ESP32_Firmware/ESP32_AirMonitor_TI3042.ino
# 3. Configurar bibliotecas:
#    - WiFi
#    - BluetoothSerial  
#    - ArduinoJson
#    - DHT sensor library

# 4. Conectar sensores según diagrama
# 5. Subir código al ESP32
```

---

## 📖 **Uso de la Aplicación**

### **🔐 Primer Uso (Registro)**
1. ✅ Abrir app → Crear cuenta con email/contraseña
2. ✅ Login automático (válido 30 días)
3. ✅ Configurar conexión Bluetooth ESP32

### **📊 Dashboard Principal** 
- 🌡️ **Métricas en tiempo real:** Temperatura, humedad, PPM
- 📈 **Gráficos históricos:** Tendencias de calidad de aire
- ⚠️ **Alertas inteligentes:** Notificaciones de niveles críticos
- 🔄 **Auto-refresh:** Datos actualizados cada 5 segundos

### **🎛️ Sistema de Control**
- 💨 **Control de ventilador:** ON/OFF manual o automático
- 🔔 **Gestión de alarmas:** Configurar umbrales personalizados  
- 📡 **Multi-dispositivo:** Gestionar varios ESP32 simultáneamente
- ⚙️ **Configuración avanzada:** Calibración de sensores

---

## 🧪 **Testing y Desarrollo**

### **🔬 Modo Simulación**
```bash
# Testing sin hardware físico
# Usa datos mock realistas
# Simula conexión Bluetooth
# Perfecto para desarrollo UI
```

### **🛠️ Wokwi Simulation** 
```bash
# Simulador online completo
# ESP32 + sensores virtuales
# Testing de firmware
# Debugging visual
```

### **📊 Testing Avanzado**
- ✅ Unit Tests (JUnit + Mockito)
- ✅ UI Tests (Espresso)  
- ✅ Integration Tests (Firebase Local)
- ✅ Hardware Tests (ESP32 real)

---

## 📚 **Documentación Técnica**

### **📋 Recursos Incluidos**
- 📖 [**TESTING_GUIDE.md**](TESTING_GUIDE.md) - Guía completa de testing
- 🛠️ [**ESP32 Installation Guide**](ESP32_Firmware/INSTALLATION_GUIDE.md)
- 📡 [**Protocol Specification**](ESP32_Firmware/PROTOCOL_SPECIFICATION.md)
- 🔥 **Firebase Setup Guide** (integrado)

### **🎯 Casos de Uso Principales**
1. **Monitoreo Residencial** - Calidad de aire en hogares
2. **Entornos Industriales** - Control de emisiones
3. **Espacios Educativos** - Laboratorios y aulas
4. **Investigación Científica** - Recolección de datos ambientales

---

## 🤝 **Contribuir al Proyecto**

### **🔄 Workflow de Desarrollo**
```bash
# 1. Fork del repositorio
git fork https://github.com/IIRlyethII/AndroidStudioProjects.git

# 2. Crear rama feature
git checkout -b feature/nueva-funcionalidad  

# 3. Desarrollo y testing
./gradlew test

# 4. Pull Request
git push origin feature/nueva-funcionalidad
```

### **📝 Estándares de Código**
- ✅ **Kotlin Style Guide** - Convenciones oficiales
- ✅ **Clean Architecture** - Separación de responsabilidades
- ✅ **MVVM Pattern** - ViewModels + LiveData/Flow  
- ✅ **Documentation** - KDoc para funciones públicas

---

## 📊 **Estado del Proyecto**

### **✅ Completado (v1.0)**
- [x] 🔐 Sistema de autenticación completo
- [x] 📱 Dashboard funcional con métricas
- [x] 🔥 Integración Firebase completa
- [x] 🛠️ Firmware ESP32 estable  
- [x] 📡 Comunicación Bluetooth robusta
- [x] 🎨 UI/UX profesional Material Design 3
- [x] 📚 Documentación técnica completa

### **🚧 En Desarrollo (v1.1)**  
- [ ] 📈 Analytics avanzado con ML
- [ ] 🌐 Soporte WiFi P2P  
- [ ] 📱 Notificaciones push inteligentes
- [ ] 🔄 Sincronización multi-dispositivo
- [ ] 📊 Exportación de reportes PDF

### **💡 Futuro (v2.0)**
- [ ] 🤖 IA para predicción de calidad de aire
- [ ] 🌍 Integración con APIs meteorológicas
- [ ] 📱 Companion app para smartwatches
- [ ] ☁️ Dashboard web complementario

---

## 👨‍🎓 **Información Académica**

**🎓 Proyecto:** Sistemas Distribuidos TI3042  
**👤 Autor:** [IIRlyethII](https://github.com/IIRlyethII)  
**📅 Fecha:** Noviembre 2025  
**🏫 Institución:** [Tu Institución Educativa]  

### **🎯 Objetivos de Aprendizaje Alcanzados**
- ✅ Arquitectura de software escalable
- ✅ Integración IoT con dispositivos móviles  
- ✅ Comunicación Bluetooth y protocolos personalizados
- ✅ Base de datos en la nube y sincronización
- ✅ Desarrollo Android moderno con Jetpack
- ✅ Testing automatizado y CI/CD

---

## 📞 **Contacto y Soporte**

### **🐛 Reportar Bugs**
- 📝 [Issues en GitHub](https://github.com/IIRlyethII/AndroidStudioProjects/issues)
- 📧 Email: [tu-email@ejemplo.com]
- 💬 Discord: [Tu Discord#1234]

### **🤝 Colaboraciones**
¿Interesado en contribuir? ¡Todas las contribuciones son bienvenidas!
- 🔀 Pull Requests
- 🐛 Bug Reports  
- 💡 Feature Requests
- 📚 Mejoras de documentación

---

## 📄 **Licencia**

```
MIT License

Copyright (c) 2025 IIRlyethII

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

### 🌟 **¡Gracias por usar Air Quality Monitor TI3042!** 🌟

[![GitHub stars](https://img.shields.io/github/stars/IIRlyethII/AndroidStudioProjects?style=social)](https://github.com/IIRlyethII/AndroidStudioProjects/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/IIRlyethII/AndroidStudioProjects?style=social)](https://github.com/IIRlyethII/AndroidStudioProjects/network)
[![GitHub issues](https://img.shields.io/github/issues/IIRlyethII/AndroidStudioProjects)](https://github.com/IIRlyethII/AndroidStudioProjects/issues)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**🚀 Sistema IoT completo para monitoreo de calidad de aire con ESP32 y Android**

</div>