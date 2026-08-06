🌤️ Danger-Khan Weather Station
An end-to-end, real-time IoT weather monitoring system powered by the ESP8266 microchip, DHT11 sensor, built-in OLED display, and HiveMQ Cloud MQTT Broker. Telemetry data is broadcast live to a responsive, HTTPS-compliant web dashboard hosted on GitHub Pages.
🌐 Live Web Dashboard
View the real-time weather metrics live in your browser:
👉 Danger-Khan Weather Station Web HMI
✨ Features
 * Local Telemetry Display: Real-time updates on the built-in 0.96" SSD1306 OLED screen, operational even when offline.
 * Non-Blocking Architecture: Continuous local sensor sampling and screen rendering decoupled from Wi-Fi reconnection cycles.
 * Secure WebSockets (WSS): Encrypted telemetry streaming on Port 8884 with /mqtt path compliance for web browsers.
 * Dual-Node Support (Publish/Subscribe): Modular topic structure allowing multiple independent hardware nodes (e.g., main weather sensor node + tipping-bucket rain gauge node).
 * Responsive Dark-Mode Web Dashboard: Clean UI built with vanilla HTML/CSS and Paho MQTT WebSockets engine.
📐 System Architecture
[ Sensor Node 1: ESP8266 ]
   ├─ DHT11 (Temp/Humid) ──────► HiveMQ MQTT Cloud Broker ◄───── [ GitHub Pages Web HMI ]
   └─ OLED Display (Local)        (broker.hivemq.com)               (WSS Port 8884 /mqtt)
                                          ▲
[ Rain Node 2: ESP8266 ]                 │
   └─ Tipping Bucket Gauge ──────────────┘

🔌 Hardware Pin Mapping
Node 1: Weather Station (ESP8266 + Integrated OLED + DHT11)
| Module | Pin Function | NodeMCU Board Pin | ESP8266 GPIO Pin | Notes |
|---|---|---|---|---|
| OLED Display | SDA | D6 | GPIO 14 | Hardwired on-board |
| OLED Display | SCL | D5 | GPIO 12 | Hardwired on-board |
| DHT11 Sensor | Data | D4 | GPIO 2 | Pull-up resistor configured |
| Power | VCC / GND | 3V3 / G | — | System ground common |
Node 2: Rain Meter (Optional Secondary Node)
| Module | Pin Function | NodeMCU Board Pin | ESP8266 GPIO Pin | Notes |
|---|---|---|---|---|
| Reed Switch | Signal | D5 | GPIO 14 | Configured with INPUT_PULLUP & Interrupt |
| Reed Switch | Ground | GND | — | Pin pulled LOW on magnet tip |
🛠️ Software & Library Requirements
Arduino IDE Libraries
Install the following libraries via the Arduino Library Manager (Ctrl + Shift + I):
 * ESP8266 and ESP32 OLED driver by ThingPulse (<SSD1306Wire.h>)
 * DHT sensor library by Adafruit (<DHT.h>)
 * PubSubClient by Nick O'Leary (<PubSubClient.h>)
Web Dependencies
 * Paho MQTT JavaScript Client (mqttws31.min.js) loaded via CDN.
📡 MQTT Topic & Payload Specifications
 * Broker: broker.hivemq.com
 * TCP Port (Hardware): 1883
 * WSS Port (Web Client): 8884 (Path: /mqtt)
1. Main Telemetry Topic
Topic: Danger-Khan/Weather-Station/telemetry
Update Frequency: Every 3 seconds
Payload Example:
{
  "temperature": 28,
  "humidity": 65
}

2. Rain Gauge Topic
Topic: Danger-Khan/Weather-Station/rain
Update Frequency: Every 5 seconds
Payload Example:
{
  "rainfall_mm": 1.12,
  "tips": 4,
  "is_raining": true
}

🚀 Installation & Setup
1. Hardware Firmware Flash
 * Clone this repository:
   git clone https://github.com/Danger-Khan/Weather-Station.git

 * Open ESP8266_LST_Sensor.ino in the Arduino IDE.
 * Configure your local Wi-Fi credentials:
   const char* ssid     = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

 * Select board NodeMCU 1.0 (ESP-12E Module) and flash the code to your microchip.
2. GitHub Pages Deployment
 * Upload index.html to the root directory of your GitHub repository.
 * Go to Settings \rightarrow Pages in your GitHub repository.
 * Set the source branch to main (or master) and save.
 * Your dashboard will be live at https://<username>.github.io/Weather-Station/.
