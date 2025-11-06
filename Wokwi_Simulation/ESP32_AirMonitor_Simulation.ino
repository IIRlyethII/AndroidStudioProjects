/*
 * 🧪 SIMULACIÓN WOKWI - ESP32 AIR QUALITY MONITOR
 * TI3042 - Versión para testing sin hardware
 * 
 * Esta versión está optimizada para funcionar en el simulador Wokwi
 * Permite probar el código sin tener el hardware físico
 * 
 * 🚀 PARA USAR:
 * 1. Copia este código en Wokwi
 * 2. Usa el diagram.json para el circuito
 * 3. Ejecuta la simulación
 * 4. Prueba comandos via Serial Monitor
 */

#include <WiFi.h>
#include <BluetoothSerial.h>
#include <ArduinoJson.h>
#include <Wire.h>
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

// ⏰ INTERVALOS DE TIEMPO
#define SENSOR_READ_INTERVAL 3000    // 3 segundos en simulación
#define DATA_SEND_INTERVAL 8000      // 8 segundos en simulación
#define OLED_UPDATE_INTERVAL 2000    // 2 segundos en simulación

// 🧪 DATOS SIMULADOS PARA WOKWI
int simulatedPPM = 450;
float simulatedTemp = 23.5;
float simulatedHumidity = 65.0;
bool increasing = true;

// 📱 BLUETOOTH
BluetoothSerial SerialBT;
const String DEVICE_NAME = "ESP32_AirMonitor_WOKWI";

// 🔧 OBJETOS DE SENSORES
DHT dht(DHT_PIN, DHT_TYPE);
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// 📊 ESTRUCTURA DE DATOS
struct SensorData {
    float temperature = 23.5;
    float humidity = 65.0;
    int ppm = 450;
    String airLevel = "good";
    bool fanStatus = false;
    bool buzzerActive = false;
    bool autoMode = true;
    unsigned long uptime = 0;
    bool bluetoothConnected = false;
} currentData;

struct Thresholds {
    int warning = 1000;
    int critical = 2000;
} thresholds;

// ⏰ CONTROL DE TIEMPO
unsigned long lastSensorRead = 0;
unsigned long lastDataSend = 0;
unsigned long lastOledUpdate = 0;
unsigned long startTime = 0;

void setup() {
    Serial.begin(115200);
    Serial.println("🧪 ESP32 Air Monitor - WOKWI SIMULATION");
    Serial.println("TI3042 - Testing Version");
    
    startTime = millis();
    
    // 📌 CONFIGURAR PINES
    pinMode(RELAY_FAN_PIN, OUTPUT);
    pinMode(BUZZER_PIN, OUTPUT);
    pinMode(LED_STATUS_PIN, OUTPUT);
    
    digitalWrite(RELAY_FAN_PIN, LOW);
    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(LED_STATUS_PIN, HIGH);
    
    // 📟 INICIALIZAR DISPLAY
    if (!display.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS)) {
        Serial.println("❌ OLED no encontrado en simulación");
    } else {
        Serial.println("📟 OLED simulado inicializado");
        showWelcomeScreen();
    }
    
    // 🌡️ INICIALIZAR DHT22 (simulado)
    dht.begin();
    Serial.println("🌡️ DHT22 simulado inicializado");
    
    // 📱 INICIALIZAR BLUETOOTH (simulado)
    SerialBT.begin(DEVICE_NAME);
    Serial.println("📱 Bluetooth simulado: " + DEVICE_NAME);
    
    Serial.println("✅ Simulación Wokwi iniciada correctamente");
    Serial.println("💡 Comandos disponibles:");
    Serial.println("   fan_on, fan_off, buzzer_on, buzzer_off, auto_on, auto_off");
    
    delay(2000);
}

void loop() {
    unsigned long currentTime = millis();
    
    // 📊 SIMULAR LECTURAS DE SENSORES
    if (currentTime - lastSensorRead >= SENSOR_READ_INTERVAL) {
        simulateSensorReadings();
        processAirQuality();
        controlDevices();
        lastSensorRead = currentTime;
    }
    
    // 📱 SIMULAR ENVÍO DE DATOS
    if (currentTime - lastDataSend >= DATA_SEND_INTERVAL) {
        sendSimulatedData();
        lastDataSend = currentTime;
    }
    
    // 📟 ACTUALIZAR DISPLAY
    if (currentTime - lastOledUpdate >= OLED_UPDATE_INTERVAL) {
        updateSimulatedDisplay();
        lastOledUpdate = currentTime;
    }
    
    // 📱 PROCESAR COMANDOS SERIE
    processSerialCommands();
    
    // 💡 ACTUALIZAR LED
    updateStatusLED();
    
    delay(100);
}

void showWelcomeScreen() {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    display.println("ESP32 AirMonitor");
    display.println("WOKWI Simulation");
    display.println("TI3042 - 2024");
    display.println("");
    display.println("Iniciando...");
    display.display();
}

void simulateSensorReadings() {
    // 🧪 SIMULAR DATOS REALISTAS
    
    // Simular variación de PPM
    if (increasing) {
        simulatedPPM += random(5, 25);
        if (simulatedPPM > 2500) increasing = false;
    } else {
        simulatedPPM -= random(5, 25);
        if (simulatedPPM < 400) increasing = true;
    }
    
    // Simular variación de temperatura
    simulatedTemp += (random(-10, 10) / 10.0);
    if (simulatedTemp < 18.0) simulatedTemp = 18.0;
    if (simulatedTemp > 35.0) simulatedTemp = 35.0;
    
    // Simular variación de humedad
    simulatedHumidity += (random(-5, 5) / 2.0);
    if (simulatedHumidity < 30.0) simulatedHumidity = 30.0;
    if (simulatedHumidity > 90.0) simulatedHumidity = 90.0;
    
    // Actualizar estructura de datos
    currentData.ppm = simulatedPPM;
    currentData.temperature = simulatedTemp;
    currentData.humidity = simulatedHumidity;
    currentData.uptime = (millis() - startTime) / 1000;
    
    Serial.println("🧪 SIMULACIÓN - PPM: " + String(currentData.ppm) + 
                   ", Temp: " + String(currentData.temperature, 1) + "°C" +
                   ", Hum: " + String(currentData.humidity, 1) + "%");
}

void processAirQuality() {
    if (currentData.ppm < thresholds.warning) {
        currentData.airLevel = "good";
    } else if (currentData.ppm < thresholds.critical) {
        currentData.airLevel = "moderate";
    } else {
        currentData.airLevel = "poor";
    }
}

void controlDevices() {
    if (currentData.autoMode) {
        // Control automático del ventilador
        bool shouldActivateFan = (currentData.ppm >= thresholds.warning);
        
        if (shouldActivateFan != currentData.fanStatus) {
            currentData.fanStatus = shouldActivateFan;
            digitalWrite(RELAY_FAN_PIN, currentData.fanStatus ? HIGH : LOW);
            Serial.println("🌀 SIMULACIÓN - Ventilador: " + String(currentData.fanStatus ? "ON" : "OFF"));
        }
        
        // Control automático del buzzer
        bool shouldActivateBuzzer = (currentData.ppm >= thresholds.critical);
        
        if (shouldActivateBuzzer != currentData.buzzerActive) {
            currentData.buzzerActive = shouldActivateBuzzer;
            
            if (currentData.buzzerActive) {
                // Simular beeps
                for (int i = 0; i < 3; i++) {
                    digitalWrite(BUZZER_PIN, HIGH);
                    delay(100);
                    digitalWrite(BUZZER_PIN, LOW);
                    delay(100);
                }
                Serial.println("🔔 SIMULACIÓN - Alerta crítica activada!");
            }
        }
    }
}

void sendSimulatedData() {
    // 📱 CREAR JSON PARA SIMULACIÓN
    StaticJsonDocument<512> doc;
    
    doc["device"] = "ESP32_TI3042_WOKWI";
    doc["version"] = "1.0.0-simulation";
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
    system["simulation"] = true;
    
    String jsonString;
    serializeJson(doc, jsonString);
    
    Serial.println("📱 JSON SIMULADO:");
    Serial.println(jsonString);
    Serial.println("---");
}

void updateSimulatedDisplay() {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    
    display.println("ESP32 WOKWI SIM");
    display.println("===============");
    display.println("PPM: " + String(currentData.ppm));
    display.println("Nivel: " + currentData.airLevel);
    display.println("T:" + String(currentData.temperature, 1) + "C H:" + String(currentData.humidity, 0) + "%");
    
    String deviceStatus = "";
    deviceStatus += currentData.fanStatus ? "FAN:ON " : "FAN:OFF ";
    deviceStatus += currentData.buzzerActive ? "BUZ:ON" : "BUZ:OFF";
    display.println(deviceStatus);
    
    display.println("Mode:" + String(currentData.autoMode ? "AUTO" : "MAN"));
    display.println("Up:" + String(currentData.uptime) + "s");
    
    display.display();
}

void processSerialCommands() {
    if (Serial.available()) {
        String command = Serial.readString();
        command.trim();
        command.toLowerCase();
        
        Serial.println("🎮 Comando recibido: " + command);
        
        if (command == "fan_on") {
            currentData.fanStatus = true;
            digitalWrite(RELAY_FAN_PIN, HIGH);
            Serial.println("✅ Ventilador activado manualmente");
        }
        else if (command == "fan_off") {
            currentData.fanStatus = false;
            digitalWrite(RELAY_FAN_PIN, LOW);
            Serial.println("✅ Ventilador desactivado manualmente");
        }
        else if (command == "buzzer_on") {
            currentData.buzzerActive = true;
            digitalWrite(BUZZER_PIN, HIGH);
            Serial.println("✅ Buzzer activado manualmente");
        }
        else if (command == "buzzer_off") {
            currentData.buzzerActive = false;
            digitalWrite(BUZZER_PIN, LOW);
            Serial.println("✅ Buzzer desactivado manualmente");
        }
        else if (command == "auto_on") {
            currentData.autoMode = true;
            Serial.println("✅ Modo automático activado");
        }
        else if (command == "auto_off") {
            currentData.autoMode = false;
            Serial.println("✅ Modo manual activado");
        }
        else if (command == "status") {
            sendSimulatedData();
        }
        else if (command == "help") {
            Serial.println("💡 Comandos disponibles:");
            Serial.println("   fan_on, fan_off - Control ventilador");
            Serial.println("   buzzer_on, buzzer_off - Control buzzer");
            Serial.println("   auto_on, auto_off - Control modo");
            Serial.println("   status - Ver estado actual");
            Serial.println("   help - Esta ayuda");
        }
        else {
            Serial.println("❓ Comando desconocido. Usa 'help' para ver comandos disponibles.");
        }
    }
}

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