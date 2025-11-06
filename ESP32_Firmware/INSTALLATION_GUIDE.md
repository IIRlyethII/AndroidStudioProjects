# 🔧 **GUÍA DE INSTALACIÓN ESP32**
## TI3042 - Air Quality Monitor

---

## 📋 **COMPONENTES REQUERIDOS**

### **🔧 Hardware:**
- ✅ **ESP32 DevKit V1** - Microcontrolador principal
- ✅ **MQ-135** - Sensor de gases (CO2, NH3, NOx, smoke)
- ✅ **DHT22** - Sensor temperatura y humedad
- ✅ **ADS1115** - Convertidor ADC 16-bit I2C
- ✅ **SSD1306 OLED** - Display 128x64 I2C
- ✅ **Módulo Relay** - Control ventilador 5V
- ✅ **Buzzer Activo 5V** - Alertas sonoras
- ✅ **LED RGB** - Indicador de estado
- ✅ **Resistencias** - 10kΩ, 220Ω, 1kΩ
- ✅ **Protoboard** - Para conexiones
- ✅ **Cables jumper** - Macho-macho, macho-hembra
- ✅ **Fuente 5V 2A** - Alimentación externa

### **💻 Software:**
- ✅ **Arduino IDE 1.8.19+** o **Arduino IDE 2.x**
- ✅ **ESP32 Board Package**
- ✅ **Librerías requeridas** (ver lista abajo)

---

## ⚙️ **CONFIGURACIÓN ARDUINO IDE**

### **1️⃣ Instalar ESP32 Board Manager:**
```
1. Abrir Arduino IDE
2. File → Preferences
3. En "Additional Board Manager URLs" agregar:
   https://dl.espressif.com/dl/package_esp32_index.json
4. Tools → Board → Boards Manager
5. Buscar "ESP32" e instalar "ESP32 by Espressif Systems"
```

### **2️⃣ Instalar Librerías Requeridas:**
```
Sketch → Include Library → Manage Libraries → Buscar e instalar:

📊 ArduinoJson by Benoit Blanchon (v6.21.3+)
📡 Adafruit ADS1X15 by Adafruit (v2.4.0+)  
📟 Adafruit SSD1306 by Adafruit (v2.5.7+)
🎨 Adafruit GFX Library by Adafruit (v1.11.3+)
🌡️ DHT sensor library by Adafruit (v1.4.4+)
🔧 Adafruit Unified Sensor by Adafruit (v1.1.9+)
```

### **3️⃣ Configurar Board y Puerto:**
```
Tools → Board → ESP32 Dev Module
Tools → Flash Size → 4MB (32Mb)
Tools → Partition Scheme → Default 4MB with spiffs
Tools → Upload Speed → 921600
Tools → Port → COM3 (o el puerto correspondiente)
```

---

## 🔌 **DIAGRAMA DE CONEXIONES**

### **📊 ESP32 Pinout:**
```
ESP32 DevKit V1 Connections:

🔋 ALIMENTACIÓN:
VIN  ← 5V (Fuente externa)
GND  ← GND (Común)
3V3  ← Sensores 3.3V

📡 I2C (SDA=21, SCL=22):
GPIO21 (SDA) ← ADS1115 SDA, OLED SDA
GPIO22 (SCL) ← ADS1115 SCL, OLED SCL

📊 SENSORES ANALÓGICOS:
ADS1115 A0   ← MQ-135 A0

📊 SENSORES DIGITALES:
GPIO4        ← DHT22 DATA

🎛️ CONTROL DISPOSITIVOS:
GPIO2        ← Relay IN (Ventilador)
GPIO5        ← Buzzer +
GPIO18       ← LED Status

⚡ ALIMENTACIÓN COMÚN:
5V  → Relay VCC, Buzzer VCC, MQ-135 VCC
3V3 → DHT22 VCC, ADS1115 VCC, OLED VCC  
GND → Todos los GND
```

### **🔧 Conexiones Detalladas:**

#### **MQ-135 (Sensor de Gases):**
```
MQ-135 VCC  → 5V
MQ-135 GND  → GND
MQ-135 A0   → ADS1115 A0
MQ-135 D0   → No conectar
```

#### **DHT22 (Temperatura/Humedad):**
```
DHT22 VCC   → 3V3
DHT22 GND   → GND  
DHT22 DATA  → GPIO4 (con resistencia pull-up 10kΩ a 3V3)
```

#### **ADS1115 (Convertidor ADC):**
```
ADS1115 VDD → 3V3
ADS1115 GND → GND
ADS1115 SCL → GPIO22 (SCL)
ADS1115 SDA → GPIO21 (SDA)
ADS1115 A0  → MQ-135 A0
```

#### **SSD1306 OLED (Display):**
```
OLED VCC → 3V3
OLED GND → GND
OLED SCL → GPIO22 (SCL) 
OLED SDA → GPIO21 (SDA)
```

#### **Módulo Relay (Control Ventilador):**
```
Relay VCC → 5V
Relay GND → GND
Relay IN  → GPIO2
Relay COM → Ventilador Terminal 1
Relay NO  → 5V (para ventilador)
```

#### **Buzzer Activo:**
```
Buzzer + → GPIO5
Buzzer - → GND
```

#### **LED Status:**
```
LED Anodo  → GPIO18 (con resistencia 220Ω)
LED Catodo → GND
```

---

## 🚀 **PROCESO DE INSTALACIÓN**

### **1️⃣ Preparar Hardware:**
```bash
1. Montar circuito en protoboard según diagrama
2. Verificar todas las conexiones 3 veces
3. Conectar fuente 5V externa (NO usar USB del PC)
4. Verificar voltajes con multímetro:
   - 5V en VIN del ESP32
   - 3.3V en pin 3V3 del ESP32
   - Continuidad en todas las conexiones GND
```

### **2️⃣ Cargar Firmware:**
```bash
1. Conectar ESP32 via USB al PC (solo para programación)
2. Abrir "ESP32_AirMonitor_TI3042.ino" en Arduino IDE
3. Verificar que todas las librerías estén instaladas
4. Seleccionar board "ESP32 Dev Module"
5. Seleccionar puerto correcto
6. Compilar (Ctrl+R) - Verificar que no haya errores
7. Subir código (Ctrl+U)
8. Abrir Serial Monitor (115200 baudios)
```

### **3️⃣ Verificar Funcionamiento:**
```bash
📊 EN SERIAL MONITOR DEBERÍAS VER:
🚀 Iniciando ESP32 Air Monitor TI3042
📌 Pines configurados
📟 Display OLED configurado  
🌡️ DHT22 inicializado
📊 ADS1115 inicializado
📱 Bluetooth iniciado: ESP32_AirMonitor_TI3042
🎯 Calibrando MQ-135...
✅ Calibración completada. R0 = XX.XX
✅ Sistema inicializado correctamente
📊 Sensores leídos - PPM: XXX, Temp: XX.X°C, Hum: XX%
```

### **4️⃣ Verificar Display OLED:**
```
📟 EN PANTALLA OLED DEBERÍAS VER:
ESP32 AirMonitor
================
PPM: 450
Nivel: good
T:23.5C H:65%
FAN:OFF BUZ:OFF
BT:OK WiFi:--
Uptime: 120s
```

---

## 🔧 **CALIBRACIÓN INICIAL**

### **📊 Calibrar MQ-135:**
```cpp
1. Dejar el sensor en aire limpio por 24 horas
2. El firmware calibrará automáticamente al inicio
3. Anotar el valor R0 que aparece en Serial Monitor
4. Si es necesario, ajustar manualmente en el código:
   
   float r0 = 10.0;  // ← Cambiar por el valor calibrado
```

### **⚙️ Ajustar Umbrales:**
```cpp
struct Thresholds {
    int warning = 1000;   // ← PPM para alerta amarilla
    int critical = 2000;  // ← PPM para alerta roja  
};
```

---

## 🐛 **TROUBLESHOOTING**

### **❌ Error: Board not found**
```bash
✅ Solución:
1. Instalar drivers ESP32: https://bit.ly/3QwjpPu
2. Verificar cable USB (debe transmitir datos, no solo cargar)
3. Presionar botón BOOT en ESP32 mientras subes código
```

### **❌ Error: ADS1115 no encontrado**
```bash
✅ Solución:
1. Verificar conexiones I2C (SDA=21, SCL=22)
2. Verificar alimentación 3.3V del ADS1115
3. Probar con I2C Scanner para detectar dirección
```

### **❌ Error: OLED no responde**
```bash
✅ Solución:
1. Verificar dirección I2C (0x3C o 0x3D)
2. Cambiar en código si es necesario:
   #define SCREEN_ADDRESS 0x3D  // ← Probar 0x3D
```

### **❌ Lecturas DHT22 NaN**
```bash
✅ Solución:
1. Verificar resistencia pull-up 10kΩ en DATA pin
2. Verificar alimentación 3.3V (NO 5V)
3. Esperar 2 segundos entre lecturas
```

---

## 🎯 **SIGUIENTE PASO**

Una vez funcionando el ESP32:

1. **📱 Emparejar con Android** usando "ESP32_AirMonitor_TI3042"
2. **🔧 Actualizar RealBluetoothService** en Android
3. **🧪 Probar comunicación** bidireccional
4. **📊 Verificar datos** en tiempo real en la app

**🎉 ¡Tu sistema IoT estará completamente funcional!**