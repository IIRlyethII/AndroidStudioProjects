/*
 * 🌟 ESP32 AIR QUALITY MONITOR - PLATFORMIO VERSION
 * TI3042 - Proyecto Integrador
 * 
 * Este es tu firmware ESP32 adaptado para PlatformIO
 * Mejoras sobre versión Arduino IDE:
 * ✅ Gestión automática de librerías
 * ✅ IntelliSense completo
 * ✅ Debugging avanzado
 * ✅ Compilación optimizada
 * ✅ Gestión de dependencias
 */

#include <Arduino.h>
#include <WiFi.h>
#include <BluetoothSerial.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <Adafruit_ADS1X15.h>
#include <Adafruit_SSD1306.h>
#include <Adafruit_GFX.h>
#include <DHT.h>

// 🔧 CONFIGURACIÓN DE PINES
#define DHT_PIN 4
#define DHT_TYPE DHT22
#define RELAY_FAN_PIN 2
#define BUZZER_PIN 5
#define LED_STATUS_PIN 18

// 📟 CONFIGURACIÓN OLED
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
#define SCREEN_ADDRESS 0x3C

// 📊 CONFIGURACIÓN DE SENSORES
#define MQ135_CHANNEL 0
#define CALIBRATION_SAMPLES 50
#define SAMPLES_INTERVAL 100

// ⏰ INTERVALOS DE TIEMPO
#define SENSOR_READ_INTERVAL 2000
#define DATA_SEND_INTERVAL 5000
#define OLED_UPDATE_INTERVAL 1000

// 🌐 CONFIGURACIÓN WIFI
const char* WIFI_SSID = "TU_WIFI_SSID";
const char* WIFI_PASSWORD = "TU_WIFI_PASSWORD";

// 📱 BLUETOOTH
BluetoothSerial SerialBT;
const String DEVICE_NAME = "ESP32_AirMonitor_TI3042";

// 🔧 OBJETOS DE SENSORES
DHT dht(DHT_PIN, DHT_TYPE);
Adafruit_ADS1115 ads;
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// 📊 ESTRUCTURAS DE DATOS
struct SensorData {
    float temperature = 0.0;
    float humidity = 0.0;
    int ppm = 0;
    String airLevel = "good";
    bool fanStatus = false;
    bool buzzerActive = false;
    bool autoMode = true;
    unsigned long uptime = 0;
    int batteryLevel = 100;
    int wifiSignal = 0;
    bool bluetoothConnected = false;
};

struct Thresholds {
    int warning = 1000;
    int critical = 2000;
};

// 📊 VARIABLES GLOBALES
SensorData currentData;
Thresholds thresholds;
float r0 = 10.0;  // Resistencia en aire limpio
float rl = 10.0;  // Resistencia de carga

// ⏰ CONTROL DE TIEMPO
unsigned long lastSensorRead = 0;
unsigned long lastDataSend = 0;
unsigned long lastOledUpdate = 0;
unsigned long startTime = 0;

// 🚀 CONFIGURACIÓN INICIAL
void setup() {
    Serial.begin(115200);
    Serial.println("🚀 Iniciando ESP32 Air Monitor TI3042 - PlatformIO Version");
    
    startTime = millis();
    
    // 📌 CONFIGURAR PINES
    setupPins();
    
    // 📟 INICIALIZAR DISPLAY
    setupDisplay();
    
    // 📊 INICIALIZAR SENSORES
    setupSensors();
    
    // 📱 INICIALIZAR BLUETOOTH
    setupBluetooth();
    
    // 🎯 CALIBRAR SENSOR MQ-135
    calibrateMQ135();
    
    Serial.println("✅ Sistema PlatformIO inicializado correctamente");
    displayMessage("Sistema Listo", "PlatformIO OK");
    
    delay(2000);
}

// 🔄 LOOP PRINCIPAL
void loop() {
    unsigned long currentTime = millis();
    
    // 📊 LEER SENSORES
    if (currentTime - lastSensorRead >= SENSOR_READ_INTERVAL) {
        readSensors();
        processAirQuality();
        controlDevices();
        lastSensorRead = currentTime;
    }
    
    // 📱 ENVIAR DATOS VIA BLUETOOTH
    if (currentTime - lastDataSend >= DATA_SEND_INTERVAL) {
        sendDataToBluetooth();
        lastDataSend = currentTime;
    }
    
    // 📟 ACTUALIZAR DISPLAY
    if (currentTime - lastOledUpdate >= OLED_UPDATE_INTERVAL) {
        updateDisplay();
        lastOledUpdate = currentTime;
    }
    
    // 📱 PROCESAR COMANDOS BLUETOOTH
    processBluetoothCommands();
    
    // 💡 ACTUALIZAR LED DE ESTADO
    updateStatusLED();
    
    delay(50);
}

// 📌 CONFIGURAR PINES
void setupPins() {
    pinMode(RELAY_FAN_PIN, OUTPUT);
    pinMode(BUZZER_PIN, OUTPUT);
    pinMode(LED_STATUS_PIN, OUTPUT);
    
    digitalWrite(RELAY_FAN_PIN, LOW);
    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(LED_STATUS_PIN, HIGH);
    
    Serial.println("📌 Pines configurados con PlatformIO");
}

// 📟 CONFIGURAR DISPLAY OLED
void setupDisplay() {
    if (!display.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS)) {
        Serial.println("❌ Error: OLED no encontrado");
        return;
    }
    
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    display.println("ESP32 AirMonitor");
    display.println("TI3042 - 2024");
    display.println("PlatformIO Ready");
    display.display();
    
    Serial.println("📟 Display OLED configurado (PlatformIO)");
}

// 📊 CONFIGURAR SENSORES
void setupSensors() {
    dht.begin();
    Serial.println("🌡️ DHT22 inicializado (PlatformIO)");
    
    if (!ads.begin()) {
        Serial.println("❌ Error: ADS1115 no encontrado");
    } else {
        Serial.println("📊 ADS1115 inicializado (PlatformIO)");
    }
    
    ads.setGain(GAIN_TWOTHIRDS);
}

// 📱 CONFIGURAR BLUETOOTH
void setupBluetooth() {
    SerialBT.begin(DEVICE_NAME);
    Serial.println("📱 Bluetooth PlatformIO iniciado: " + DEVICE_NAME);
    Serial.println("📱 Listo para emparejamiento...");
    
    currentData.bluetoothConnected = false;
}

// 🎯 CALIBRAR SENSOR MQ-135
void calibrateMQ135() {
    Serial.println("🎯 Calibrando MQ-135 con PlatformIO...");
    displayMessage("Calibrando", "PlatformIO");
    
    float sum = 0.0;
    for (int i = 0; i < CALIBRATION_SAMPLES; i++) {
        int16_t adc = ads.readADC_SingleEnded(MQ135_CHANNEL);
        float voltage = ads.computeVolts(adc);
        float rs = ((5.0 * rl) / voltage) - rl;
        sum += rs;
        
        delay(SAMPLES_INTERVAL);
    }
    
    r0 = sum / CALIBRATION_SAMPLES;
    Serial.println("✅ Calibración PlatformIO completada. R0 = " + String(r0));
}

// 📊 LEER SENSORES
void readSensors() {
    currentData.temperature = dht.readTemperature();
    currentData.humidity = dht.readHumidity();
    
    if (isnan(currentData.temperature) || isnan(currentData.humidity)) {
        Serial.println("❌ Error leyendo DHT22");
        currentData.temperature = 0.0;
        currentData.humidity = 0.0;
    }
    
    currentData.ppm = readMQ135PPM();
    currentData.uptime = (millis() - startTime) / 1000;
    currentData.bluetoothConnected = SerialBT.hasClient();
    
    Serial.println("📊 Sensores PlatformIO - PPM: " + String(currentData.ppm) + 
                   ", Temp: " + String(currentData.temperature) + "°C");
}

// 💨 LEER PPM DEL MQ-135
int readMQ135PPM() {
    float sum = 0.0;
    int samples = 5;
    
    for (int i = 0; i < samples; i++) {
        int16_t adc = ads.readADC_SingleEnded(MQ135_CHANNEL);
        float voltage = ads.computeVolts(adc);
        
        if (voltage <= 0.1) voltage = 0.1;
        
        float rs = ((5.0 * rl) / voltage) - rl;
        float ratio = rs / r0;
        float ppm = 116.6020682 * pow(ratio, -2.769034857);
        
        sum += ppm;
        delay(50);
    }
    
    int avgPPM = (int)(sum / samples);
    if (avgPPM < 0) avgPPM = 0;
    if (avgPPM > 5000) avgPPM = 5000;
    
    return avgPPM;
}

// 🧠 PROCESAR CALIDAD DEL AIRE
void processAirQuality() {
    if (currentData.ppm < thresholds.warning) {
        currentData.airLevel = "good";
    } else if (currentData.ppm < thresholds.critical) {
        currentData.airLevel = "moderate";
    } else {
        currentData.airLevel = "poor";
    }
}

// 🎛️ CONTROLAR DISPOSITIVOS
void controlDevices() {
    if (currentData.autoMode) {
        bool shouldActivateFan = (currentData.ppm >= thresholds.warning);
        
        if (shouldActivateFan != currentData.fanStatus) {
            currentData.fanStatus = shouldActivateFan;
            digitalWrite(RELAY_FAN_PIN, currentData.fanStatus ? HIGH : LOW);
            Serial.println("🌀 PlatformIO - Ventilador: " + String(currentData.fanStatus ? "ON" : "OFF"));
        }
        
        bool shouldActivateBuzzer = (currentData.ppm >= thresholds.critical);
        
        if (shouldActivateBuzzer != currentData.buzzerActive) {
            currentData.buzzerActive = shouldActivateBuzzer;
            
            if (currentData.buzzerActive) {
                for (int i = 0; i < 3; i++) {
                    digitalWrite(BUZZER_PIN, HIGH);
                    delay(100);
                    digitalWrite(BUZZER_PIN, LOW);
                    delay(100);
                }
                Serial.println("🔔 PlatformIO - Alerta crítica!");
            }
        }
    }
}

// 📱 ENVIAR DATOS VIA BLUETOOTH
void sendDataToBluetooth() {
    if (!SerialBT.hasClient()) return;
    
    StaticJsonDocument<512> doc;
    
    doc["device"] = "ESP32_TI3042_PlatformIO";
    doc["version"] = "1.0.0";
    doc["timestamp"] = millis();
    
    JsonObject airQuality = doc.createNestedObject("air_quality");
    airQuality["ppm"] = currentData.ppm;
    airQuality["level"] = currentData.airLevel;
    airQuality["temperature"] = currentData.temperature;
    airQuality["humidity"] = currentData.humidity;
    
    JsonObject system = doc.createNestedObject("system");
    system["fan_status"] = currentData.fanStatus;
    system["buzzer_active"] = currentData.buzzerActive;
    system["auto_mode"] = currentData.autoMode;
    system["uptime"] = currentData.uptime;
    system["bluetooth_connected"] = currentData.bluetoothConnected;
    
    String jsonString;
    serializeJson(doc, jsonString);
    
    SerialBT.println(jsonString);
    Serial.println("📱 PlatformIO - Datos enviados via Bluetooth");
}

// 📱 PROCESAR COMANDOS BLUETOOTH
void processBluetoothCommands() {
    if (SerialBT.available()) {
        String command = SerialBT.readString();
        command.trim();
        
        Serial.println("📱 PlatformIO - Comando recibido: " + command);
        
        StaticJsonDocument<256> doc;
        DeserializationError error = deserializeJson(doc, command);
        
        if (error) {
            Serial.println("❌ Error parseando JSON en PlatformIO");
            return;
        }
        
        String action = doc["action"];
        
        if (action == "control") {
            if (doc.containsKey("fan")) {
                currentData.fanStatus = doc["fan"]["enable"];
                digitalWrite(RELAY_FAN_PIN, currentData.fanStatus ? HIGH : LOW);
            }
            
            if (doc.containsKey("buzzer")) {
                currentData.buzzerActive = doc["buzzer"]["enable"];
                digitalWrite(BUZZER_PIN, currentData.buzzerActive ? HIGH : LOW);
            }
            
            if (doc.containsKey("auto_mode")) {
                currentData.autoMode = doc["auto_mode"];
            }
        }
    }
}

// 📟 ACTUALIZAR DISPLAY OLED
void updateDisplay() {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    
    display.println("ESP32 PlatformIO");
    display.println("================");
    display.println("PPM: " + String(currentData.ppm));
    display.println("Nivel: " + currentData.airLevel);
    display.println("T:" + String(currentData.temperature, 1) + "C H:" + String(currentData.humidity, 0) + "%");
    
    String deviceStatus = "";
    deviceStatus += currentData.fanStatus ? "FAN:ON " : "FAN:OFF ";
    deviceStatus += currentData.buzzerActive ? "BUZ:ON" : "BUZ:OFF";
    display.println(deviceStatus);
    
    display.println("BT:" + String(currentData.bluetoothConnected ? "OK" : "--"));
    display.println("Uptime: " + String(currentData.uptime) + "s");
    
    display.display();
}

// 📟 MOSTRAR MENSAJE EN DISPLAY
void displayMessage(String line1, String line2) {
    display.clearDisplay();
    display.setTextSize(2);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 20);
    display.println(line1);
    display.setCursor(0, 40);
    display.println(line2);
    display.display();
}

// 💡 ACTUALIZAR LED DE ESTADO
void updateStatusLED() {
    static unsigned long lastBlink = 0;
    static bool ledState = false;
    
    unsigned long currentTime = millis();
    int blinkInterval = 1000;
    
    if (currentData.airLevel == "moderate") {
        blinkInterval = 500;
    } else if (currentData.airLevel == "poor") {
        blinkInterval = 200;
    }
    
    if (currentTime - lastBlink >= blinkInterval) {
        ledState = !ledState;
        digitalWrite(LED_STATUS_PIN, ledState ? HIGH : LOW);
        lastBlink = currentTime;
    }
}