#if defined(ESP8266)
  #include <ESP8266WiFi.h>
#elif defined(ESP32)
  #include <WiFi.h>
#endif

#include <PubSubClient.h>
#include <DHT.h>
#include <SSD1306Wire.h>  // ThingPulse Driver

// ---------------------------------------------------------------------
// HARDWARE PIN DEFINITIONS
// GPIO 2  = Pin D4 (DHT11 Data Pin)
// GPIO 14 = Pin D6 (Built-in OLED SDA)
// GPIO 12 = Pin D5 (Built-in OLED SCL)
// ---------------------------------------------------------------------
#define DHTPIN  2     
#define DHTTYPE DHT11

DHT dht(DHTPIN, DHTTYPE);

// ThingPulse Driver: SSD1306Wire(Address, SDA_Pin, SCL_Pin)
SSD1306Wire display(0x3c, 14, 12); 

// ---------------------------------------------------------------------
// NETWORK & MQTT CONFIGURATION
// ---------------------------------------------------------------------
const char* ssid        = "Khan";
const char* password    = "DangerKhan0125@.";
const char* mqtt_server = "broker.hivemq.com";
const int   mqtt_port   = 1883;
const char* mqtt_topic  = "Danger-Khan/Weather-Station/telemetry";

WiFiClient espClient;
PubSubClient client(espClient);

// Global Variables
float temperature   = 0.0;
float humidity      = 0.0;
bool  wifiConnected = false;
bool  apiConnected  = false;

// Non-blocking Timers
unsigned long lastDHTRead   = 0;
unsigned long lastMQTTPub   = 0;
unsigned long lastMQTTRetry = 0;

// ---------------------------------------------------------------------
// DISPLAY RENDERER
// ---------------------------------------------------------------------
void updateDisplay() {
    display.clear();

    // --- TEMPERATURE SECTION ---
    display.setFont(ArialMT_Plain_10);
    display.drawString(0, 0, "TEMP:");
    display.setFont(ArialMT_Plain_16);
    if (isnan(temperature)) {
        display.drawString(45, 0, "--.- C");
    } else {
        display.drawString(45, 0, String(temperature, 1) + " C");
    }

    // --- HUMIDITY SECTION ---
    display.setFont(ArialMT_Plain_10);
    display.drawString(0, 20, "HUM :");
    display.setFont(ArialMT_Plain_16);
    if (isnan(humidity)) {
        display.drawString(45, 20, "-- %");
    } else {
        display.drawString(45, 20, String((int)humidity) + " %");
    }

    // --- HUMIDITY BAR (0 - 100%) ---
    int fillWidth = map(constrain((int)humidity, 0, 100), 0, 100, 0, 122);
    display.drawRect(0, 38, 124, 7);
    if (fillWidth > 0) {
        display.fillRect(1, 39, fillWidth, 5);
    }

    // --- BOTTOM STATUS BAR (2-pixel height solid line) ---
    display.drawLine(0, 48, 127, 48);
    display.drawLine(0, 49, 127, 49);

    // --- STATUS INDICATORS ---
    display.setFont(ArialMT_Plain_10);
    String statusStr = "WF: " + String(wifiConnected ? "OK" : "NC") + 
                       "    API: " + String(apiConnected ? "OK" : "NC");
    display.drawString(0, 51, statusStr);

    display.display();
}

// ---------------------------------------------------------------------
// NETWORK & MQTT CONTROLLER
// ---------------------------------------------------------------------
void handleNetwork() {
    wifiConnected = (WiFi.status() == WL_CONNECTED);

    if (wifiConnected) {
        if (!client.connected()) {
            apiConnected = false;
            unsigned long now = millis();
            if (now - lastMQTTRetry > 5000) { 
                lastMQTTRetry = now;
                String clientId = "DangerKhan-ESP8266-" + String(ESP.getChipId(), HEX);
                if (client.connect(clientId.c_str())) {
                    apiConnected = true;
                }
            }
        } else {
            apiConnected = true;
            client.loop();
        }
    } else {
        apiConnected = false;
    }
}

// ---------------------------------------------------------------------
// SETUP & LOOP
// ---------------------------------------------------------------------
void setup() {
    Serial.begin(115200);

    // Initialize Integrated OLED Display
    display.init();
    display.clear();
    display.flipScreenVertically();
    display.setFont(ArialMT_Plain_10);
    display.drawString(0, 20, "Connecting Wi-Fi...");
    display.display();

    // Start Hardware
    dht.begin();
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, password);
    client.setServer(mqtt_server, mqtt_port);
}

void loop() {
    unsigned long currentMillis = millis();

    // 1. Read DHT11 Sensor Independently Every 2 Seconds
    if (currentMillis - lastDHTRead >= 2000) {
        lastDHTRead = currentMillis;

        float t = dht.readTemperature();
        float h = dht.readHumidity();

        if (!isnan(t) && !isnan(h)) {
            temperature = t;
            humidity    = h;
        }

        // Screen updates locally even without Wi-Fi
        updateDisplay();
    }

    // 2. Network Management
    handleNetwork();

    // 3. Broadcast Data to Cloud Broker Every 3 Seconds if API Connected
    if (apiConnected && (currentMillis - lastMQTTPub >= 3000)) {
        lastMQTTPub = currentMillis;

        if (!isnan(temperature) && !isnan(humidity)) {
            String payload = "{\"temperature\":" + String((int)temperature) + 
                             ",\"humidity\":" + String((int)humidity) + "}";
            client.publish(mqtt_topic, payload.c_str());
        }
    }
}
