/*
 * 🌟 FIRMWARE ESP32 - MONITOR DE CALIDAD DEL AIRE
 * TI3042 - Proyecto Integrador
 * 
 * FUNCIONALIDADES:
 * ✅ Lectura de sensor MQ-135 (CO2/gases)
 * ✅ Lectura de sensor DHT22 (temperatura/humedad)  
 * ✅ Comunicación Bluetooth con Android
 * ✅ Control de ventilador y buzzer
 * ✅ Display OLED para información local
 * ✅ JSON protocol para intercambio de datos
 * 
 * HARDWARE REQUERIDO:
 * - ESP32 DevKit V1
 * - MQ-135 (Gas sensor)
 * - DHT22 (Temp/Humidity)
 * - ADS1115 (ADC 16-bit)
 * - SSD1306 OLED 128x64
 * - Relay module
 * - Buzzer activo
 * - Resistencias y conexiones
 */

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
#define MQ135_CHANNEL 0  // Canal ADS1115 para MQ-135
#define CALIBRATION_SAMPLES 50
#define SAMPLES_INTERVAL 100

// ⏰ INTERVALOS DE TIEMPO
#define SENSOR_READ_INTERVAL 2000    // 2 segundos
#define DATA_SEND_INTERVAL 5000      // 5 segundos
#define OLED_UPDATE_INTERVAL 1000    // 1 segundo

// 🌐 CONFIGURACIÓN WIFI (Opcional)
const char* WIFI_SSID = "TU_WIFI_SSID";
const char* WIFI_PASSWORD = "TU_WIFI_PASSWORD";

// 📱 BLUETOOTH
BluetoothSerial SerialBT;
const String DEVICE_NAME = "ESP32_AirMonitor_TI3042";

// 🔧 OBJETOS DE SENSORES
DHT dht(DHT_PIN, DHT_TYPE);
Adafruit_ADS1115 ads;
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// 📊 VARIABLES GLOBALES
struct SensorData {
    float temperature = 0.0;
    float humidity = 0.0;
    int ppm = 0;
    String airLevel = "good";
    bool fanStatus = false;
    bool buzzerActive = false;
    bool autoMode = true;
    unsigned long uptime = 0;
    int batteryLevel = 100;  // Simulado
    int wifiSignal = 0;
    bool bluetoothConnected = false;
};

struct Thresholds {
    int warning = 1000;   // PPM para warning
    int critical = 2000;  // PPM para crítico
};

SensorData currentData;
Thresholds thresholds;

// ⏰ CONTROL DE TIEMPO
unsigned long lastSensorRead = 0;
unsigned long lastDataSend = 0;
unsigned long lastOledUpdate = 0;
unsigned long startTime = 0;

// 🎛️ CALIBRACIÓN MQ-135
float r0 = 10.0;  // Resistencia en aire limpio (calibrar)
float rl = 10.0;  // Resistencia de carga

void setup() {
    Serial.begin(115200);
    Serial.println("🚀 Iniciando ESP32 Air Monitor TI3042");
    
    startTime = millis();
    
    // 📌 CONFIGURAR PINES
    setupPins();
    
    // 📟 INICIALIZAR DISPLAY
    setupDisplay();
    
    // 📊 INICIALIZAR SENSORES
    setupSensors();
    
    // 📱 INICIALIZAR BLUETOOTH
    setupBluetooth();
    
    // 🌐 INICIALIZAR WIFI (OPCIONAL)
    // setupWiFi();
    
    // 🎯 CALIBRAR SENSOR MQ-135
    calibrateMQ135();
    
    Serial.println("✅ Sistema inicializado correctamente");
    displayMessage("Sistema Listo", "TI3042 Monitor");
    
    delay(2000);
}

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
    
    delay(50);  // Pequeña pausa para estabilidad
}

// 📌 CONFIGURAR PINES
void setupPins() {
    pinMode(RELAY_FAN_PIN, OUTPUT);
    pinMode(BUZZER_PIN, OUTPUT);
    pinMode(LED_STATUS_PIN, OUTPUT);
    
    digitalWrite(RELAY_FAN_PIN, LOW);
    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(LED_STATUS_PIN, HIGH);
    
    Serial.println("📌 Pines configurados");
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
    display.println("Iniciando...");
    display.display();
    
    Serial.println("📟 Display OLED configurado");
}

// 📊 CONFIGURAR SENSORES
void setupSensors() {
    // Inicializar DHT22
    dht.begin();
    Serial.println("🌡️ DHT22 inicializado");
    
    // Inicializar ADS1115
    if (!ads.begin()) {
        Serial.println("❌ Error: ADS1115 no encontrado");
    } else {
        Serial.println("📊 ADS1115 inicializado");
    }
    
    // Configurar ganancia del ADS1115
    ads.setGain(GAIN_TWOTHIRDS);  // +/- 6.144V
}

// 📱 CONFIGURAR BLUETOOTH
void setupBluetooth() {
    SerialBT.begin(DEVICE_NAME);
    Serial.println("📱 Bluetooth iniciado: " + DEVICE_NAME);
    Serial.println("📱 Listo para emparejamiento...");
    
    currentData.bluetoothConnected = false;
}

// 🌐 CONFIGURAR WIFI (OPCIONAL)
void setupWiFi() {
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("🌐 Conectando a WiFi");
    
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
        delay(500);
        Serial.print(".");
        attempts++;
    }
    
    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\n✅ WiFi conectado!");
        Serial.println("📡 IP: " + WiFi.localIP().toString());
        currentData.wifiSignal = WiFi.RSSI();
    } else {
        Serial.println("\n❌ WiFi no conectado");
        currentData.wifiSignal = 0;
    }
}

// 🎯 CALIBRAR SENSOR MQ-135
void calibrateMQ135() {
    Serial.println("🎯 Calibrando MQ-135...");
    displayMessage("Calibrando", "Sensor MQ-135");
    
    float sum = 0.0;
    for (int i = 0; i < CALIBRATION_SAMPLES; i++) {
        int16_t adc = ads.readADC_SingleEnded(MQ135_CHANNEL);
        float voltage = ads.computeVolts(adc);
        float rs = ((5.0 * rl) / voltage) - rl;
        sum += rs;
        
        delay(SAMPLES_INTERVAL);
        
        if (i % 10 == 0) {
            Serial.print("📊 Muestra " + String(i) + "/" + String(CALIBRATION_SAMPLES));
        }
    }
    
    r0 = sum / CALIBRATION_SAMPLES;
    Serial.println("✅ Calibración completada. R0 = " + String(r0));
}