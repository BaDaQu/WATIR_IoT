#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <ESPAsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <AsyncJson.h>
#include <ESP8266HTTPClient.h> 
#include <WiFiClient.h>       

const char* ssid = "ssid";
const char* password = "wifi_password";

//(Zmieńcie 192.168.X.X na właściwe IP z komputera Windows)
const char* api_url = "http:// your_ip:3000/api/telemetry"; 

AsyncWebServer server(80);


unsigned long previousMillis = 0;
const long interval = 30000; 


void sendTelemetryToBackend() {
  if(WiFi.status() == WL_CONNECTED) {
    WiFiClient client;
    HTTPClient http;
    
    Serial.println("[HTTP] Rozpoczynam wysyłanie logów...");
    http.begin(client, api_url);
    http.addHeader("Content-Type", "application/json");

    // Budowanie JSONa
    StaticJsonDocument<512> doc;
    doc["device_id"] = "WATIR_01";
    
    JsonObject sensors = doc.createNestedObject("sensors");
    sensors["temp"] = 25.1;                 // Tymczasowe dane testowe
    sensors["humidity"] = 42;
    sensors["soil_moisture"] = 35;
    sensors["water_level_cm"] = 10;
    
    JsonObject status = doc.createNestedObject("status");
    status["water_error"] = false;
    status["pump_active"] = false;

    JsonObject servos = doc.createNestedObject("servos");
    servos["pan"] = 90;
    servos["tilt"] = 45;

    String requestBody;
    serializeJson(doc, requestBody);

 
    int httpResponseCode = http.POST(requestBody);

    if (httpResponseCode > 0) {
      Serial.printf("[HTTP] Odpowiedź serwera: %d\n", httpResponseCode);
      String response = http.getString();
      Serial.println(response);
    } else {
      Serial.printf("[HTTP] Błąd połączenia: %s\n", http.errorToString(httpResponseCode).c_str());
    }
    
    http.end(); 
  } else {
    Serial.println("Błąd: Rozłączono z WiFi!");
  }
}

void setup() {
  Serial.begin(9600);
  
  WiFi.begin(ssid, password);
  Serial.print("Łączenie z WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nPołączono! IP: ");
  Serial.println(WiFi.localIP());


}

void loop() {
  unsigned long currentMillis = millis();

  
  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;
    
 
    sendTelemetryToBackend();
  }
}