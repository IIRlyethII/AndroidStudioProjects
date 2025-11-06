// 📊 LEER TODOS LOS SENSORES
void readSensors() {
    // 🌡️ LEER DHT22
    currentData.temperature = dht.readTemperature();
    currentData.humidity = dht.readHumidity();
    
    // Validar lecturas DHT22
    if (isnan(currentData.temperature) || isnan(currentData.humidity)) {
        Serial.println("❌ Error leyendo DHT22");
        currentData.temperature = 0.0;
        currentData.humidity = 0.0;
    }
    
    // 💨 LEER MQ-135
    currentData.ppm = readMQ135PPM();
    
    // ⏰ ACTUALIZAR UPTIME
    currentData.uptime = (millis() - startTime) / 1000;
    
    // 📶 ACTUALIZAR CONEXIONES
    currentData.bluetoothConnected = SerialBT.hasClient();
    if (WiFi.status() == WL_CONNECTED) {
        currentData.wifiSignal = WiFi.RSSI();
    }
    
    Serial.println("📊 Sensores leídos - PPM: " + String(currentData.ppm) + 
                   ", Temp: " + String(currentData.temperature) + 
                   "°C, Hum: " + String(currentData.humidity) + "%");
}

// 💨 LEER PPM DEL MQ-135
int readMQ135PPM() {
    // Leer múltiples muestras para estabilidad
    float sum = 0.0;
    int samples = 5;
    
    for (int i = 0; i < samples; i++) {
        int16_t adc = ads.readADC_SingleEnded(MQ135_CHANNEL);
        float voltage = ads.computeVolts(adc);
        
        if (voltage <= 0.1) {
            voltage = 0.1;  // Evitar división por cero
        }
        
        float rs = ((5.0 * rl) / voltage) - rl;
        float ratio = rs / r0;
        
        // Fórmula aproximada para CO2 en PPM (MQ-135)
        // PPM = 116.6020682 * pow(ratio, -2.769034857)
        float ppm = 116.6020682 * pow(ratio, -2.769034857);
        
        sum += ppm;
        delay(50);
    }
    
    int avgPPM = (int)(sum / samples);
    
    // Filtrar valores extremos
    if (avgPPM < 0) avgPPM = 0;
    if (avgPPM > 5000) avgPPM = 5000;
    
    return avgPPM;
}

// 🧠 PROCESAR CALIDAD DEL AIRE
void processAirQuality() {
    // Determinar nivel de calidad del aire
    if (currentData.ppm < thresholds.warning) {
        currentData.airLevel = "good";
    } else if (currentData.ppm < thresholds.critical) {
        currentData.airLevel = "moderate";
    } else {
        currentData.airLevel = "poor";
    }
    
    Serial.println("🧠 Calidad del aire: " + currentData.airLevel + 
                   " (PPM: " + String(currentData.ppm) + ")");
}

// 🎛️ CONTROLAR DISPOSITIVOS
void controlDevices() {
    if (currentData.autoMode) {
        // Control automático del ventilador
        bool shouldActivateFan = (currentData.ppm >= thresholds.warning);
        
        if (shouldActivateFan != currentData.fanStatus) {
            currentData.fanStatus = shouldActivateFan;
            digitalWrite(RELAY_FAN_PIN, currentData.fanStatus ? HIGH : LOW);
            Serial.println("🌀 Ventilador: " + String(currentData.fanStatus ? "ON" : "OFF"));
        }
        
        // Control automático del buzzer
        bool shouldActivateBuzzer = (currentData.ppm >= thresholds.critical);
        
        if (shouldActivateBuzzer != currentData.buzzerActive) {
            currentData.buzzerActive = shouldActivateBuzzer;
            
            if (currentData.buzzerActive) {
                // Patrón de beep para alerta crítica
                for (int i = 0; i < 3; i++) {
                    digitalWrite(BUZZER_PIN, HIGH);
                    delay(100);
                    digitalWrite(BUZZER_PIN, LOW);
                    delay(100);
                }
                Serial.println("🔔 Alerta crítica activada!");
            }
        }
    }
}

// 📱 ENVIAR DATOS VIA BLUETOOTH
void sendDataToBluetooth() {
    if (!SerialBT.hasClient()) {
        return;
    }
    
    // Crear JSON con datos del sensor
    StaticJsonDocument<512> doc;
    
    // Información del dispositivo
    doc["device"] = "ESP32_TI3042";
    doc["version"] = "1.0.0";
    doc["timestamp"] = millis();
    
    // Calidad del aire
    JsonObject airQuality = doc.createNestedObject("air_quality");
    airQuality["ppm"] = currentData.ppm;
    airQuality["level"] = currentData.airLevel;
    airQuality["temperature"] = currentData.temperature;
    airQuality["humidity"] = currentData.humidity;
    
    // Composición de gases (aproximada)
    JsonObject gasComposition = airQuality.createNestedObject("gas_composition");
    gasComposition["oxygen"] = 20.9;
    gasComposition["co2"] = currentData.ppm * 0.8;
    gasComposition["smoke"] = currentData.ppm * 0.1;
    gasComposition["vapor"] = currentData.ppm * 0.05;
    gasComposition["others"] = currentData.ppm * 0.05;
    
    // Estado del sistema
    JsonObject system = doc.createNestedObject("system");
    system["fan_status"] = currentData.fanStatus;
    system["buzzer_active"] = currentData.buzzerActive;
    system["auto_mode"] = currentData.autoMode;
    system["uptime"] = currentData.uptime;
    system["battery_level"] = currentData.batteryLevel;
    system["wifi_signal"] = currentData.wifiSignal;
    system["bluetooth_connected"] = currentData.bluetoothConnected;
    
    // Umbrales configurados
    JsonObject thresholdsObj = doc.createNestedObject("thresholds");
    thresholdsObj["warning"] = thresholds.warning;
    thresholdsObj["critical"] = thresholds.critical;
    
    // Convertir a string y enviar
    String jsonString;
    serializeJson(doc, jsonString);
    
    SerialBT.println(jsonString);
    Serial.println("📱 Datos enviados via Bluetooth: " + jsonString.substring(0, 100) + "...");
}

// 📱 PROCESAR COMANDOS BLUETOOTH
void processBluetoothCommands() {
    if (SerialBT.available()) {
        String command = SerialBT.readString();
        command.trim();
        
        Serial.println("📱 Comando recibido: " + command);
        
        // Parsear JSON
        StaticJsonDocument<256> doc;
        DeserializationError error = deserializeJson(doc, command);
        
        if (error) {
            Serial.println("❌ Error parseando comando JSON");
            sendErrorResponse("Invalid JSON format");
            return;
        }
        
        String action = doc["action"];
        
        if (action == "control") {
            handleControlCommand(doc);
        } else if (action == "configure") {
            handleConfigCommand(doc);
        } else if (action == "status") {
            handleStatusCommand();
        } else {
            sendErrorResponse("Unknown action: " + action);
        }
    }
}

// 🎛️ MANEJAR COMANDOS DE CONTROL
void handleControlCommand(JsonDocument& doc) {
    bool responseOk = true;
    String responseMsg = "Control executed successfully";
    
    // Control manual del ventilador
    if (doc.containsKey("fan")) {
        JsonObject fanControl = doc["fan"];
        if (fanControl.containsKey("enable")) {
            currentData.fanStatus = fanControl["enable"];
            digitalWrite(RELAY_FAN_PIN, currentData.fanStatus ? HIGH : LOW);
            Serial.println("🌀 Ventilador controlado manualmente: " + String(currentData.fanStatus ? "ON" : "OFF"));
        }
    }
    
    // Control manual del buzzer
    if (doc.containsKey("buzzer")) {
        JsonObject buzzerControl = doc["buzzer"];
        if (buzzerControl.containsKey("enable")) {
            currentData.buzzerActive = buzzerControl["enable"];
            digitalWrite(BUZZER_PIN, currentData.buzzerActive ? HIGH : LOW);
            Serial.println("🔔 Buzzer controlado manualmente: " + String(currentData.buzzerActive ? "ON" : "OFF"));
        }
    }
    
    // Control de modo automático
    if (doc.containsKey("auto_mode")) {
        currentData.autoMode = doc["auto_mode"];
        Serial.println("🤖 Modo automático: " + String(currentData.autoMode ? "ON" : "OFF"));
    }
    
    sendResponse(responseOk, responseMsg);
}

// ⚙️ MANEJAR COMANDOS DE CONFIGURACIÓN
void handleConfigCommand(JsonDocument& doc) {
    bool responseOk = true;
    String responseMsg = "Configuration updated successfully";
    
    // Configurar umbrales
    if (doc.containsKey("thresholds")) {
        JsonObject thresholdsObj = doc["thresholds"];
        
        if (thresholdsObj.containsKey("warning")) {
            thresholds.warning = thresholdsObj["warning"];
            Serial.println("⚠️ Umbral warning: " + String(thresholds.warning));
        }
        
        if (thresholdsObj.containsKey("critical")) {
            thresholds.critical = thresholdsObj["critical"];
            Serial.println("🚨 Umbral critical: " + String(thresholds.critical));
        }
    }
    
    sendResponse(responseOk, responseMsg);
}

// 📊 MANEJAR COMANDO DE STATUS
void handleStatusCommand() {
    // Enviar estado completo inmediatamente
    sendDataToBluetooth();
}

// 📤 ENVIAR RESPUESTA
void sendResponse(bool success, String message) {
    StaticJsonDocument<128> doc;
    doc["type"] = "response";
    doc["success"] = success;
    doc["message"] = message;
    doc["timestamp"] = millis();
    
    String jsonString;
    serializeJson(doc, jsonString);
    
    SerialBT.println(jsonString);
    Serial.println("📤 Respuesta enviada: " + message);
}

// ❌ ENVIAR ERROR
void sendErrorResponse(String error) {
    sendResponse(false, error);
}

// 📟 ACTUALIZAR DISPLAY OLED
void updateDisplay() {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    
    // Línea 1: Título
    display.println("ESP32 AirMonitor");
    display.println("================");
    
    // Línea 3: PPM y nivel
    display.println("PPM: " + String(currentData.ppm));
    display.println("Nivel: " + currentData.airLevel);
    
    // Línea 5: Temperatura y humedad
    display.println("T:" + String(currentData.temperature, 1) + "C H:" + String(currentData.humidity, 0) + "%");
    
    // Línea 6: Estado de dispositivos
    String deviceStatus = "";
    deviceStatus += currentData.fanStatus ? "FAN:ON " : "FAN:OFF ";
    deviceStatus += currentData.buzzerActive ? "BUZ:ON" : "BUZ:OFF";
    display.println(deviceStatus);
    
    // Línea 7: Conectividad
    String connectivity = "";
    connectivity += currentData.bluetoothConnected ? "BT:OK " : "BT:-- ";
    connectivity += (WiFi.status() == WL_CONNECTED) ? "WiFi:OK" : "WiFi:--";
    display.println(connectivity);
    
    // Línea 8: Uptime
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
    
    // Patrón de parpadeo según calidad del aire
    int blinkInterval = 1000;  // Normal
    
    if (currentData.airLevel == "moderate") {
        blinkInterval = 500;   // Más rápido para warning
    } else if (currentData.airLevel == "poor") {
        blinkInterval = 200;   // Muy rápido para crítico
    }
    
    if (currentTime - lastBlink >= blinkInterval) {
        ledState = !ledState;
        digitalWrite(LED_STATUS_PIN, ledState ? HIGH : LOW);
        lastBlink = currentTime;
    }
}