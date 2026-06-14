#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <ESPAsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <AsyncJson.h>
#include <ESP8266HTTPClient.h>
#include <WiFiClient.h>

const char* ssid = "WATIR";
const char* password = "WATIRINO";

// Adres serwera deweloperskiego (Baza danych w Dockerze)
const char* api_url = "http://10.84.74.4:3000/api/telemetry";

AsyncWebServer server(80);

unsigned long previousMillis = 0;
const long interval = 30000; // Pomiary co 30 sekund

void sendTelemetryToBackend() {
  if(WiFi.status() == WL_CONNECTED) {
    WiFiClient client;
    HTTPClient http;

    Serial.println("[HTTP] Rozpoczynam wysyłanie logów...");
    http.begin(client, api_url);
    http.addHeader("Content-Type", "application/json");

    // Budowanie JSONa telemetrycznego
    StaticJsonDocument<512> doc;
    doc["device_id"] = "WATIR_01";

    JsonObject sensors = doc.createNestedObject("sensors");
    sensors["temp"] = 25.1;                 // Tymczasowe dane testowe (zastąpić odczytami z Seriala)
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
  Serial.begin(9600); // Szybkość komunikacji szeregowej z Arduino Uno

  WiFi.begin(ssid, password);
  Serial.print("Łączenie z WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nPołączono! IP: ");
  Serial.println(WiFi.localIP());

  // --- ENDPOINT POST OBSŁUGUJĄCY RUCH RAMIENIEM (Dla aplikacji przez proxy Node.js) ---
  AsyncCallbackJsonWebHandler* handler = new AsyncCallbackJsonWebHandler("/api/move", [](AsyncWebServerRequest *request, JsonVariant &json) {

    JsonObject jsonObj = json.as<JsonObject>();

    // Sprawdzamy czy payload zawiera wymagane klucze "axis" i "value"
    if (jsonObj.containsKey("axis") && jsonObj.containsKey("value")) {

      String axis = jsonObj["axis"].as<String>();
      int val = jsonObj["value"].as<int>();

      // Weryfikacja osi i przesyłanie komendy do Arduino Uno po Serialu
      if (axis == "X" || axis == "Y") {
        Serial.println(axis + ":" + String(val)); // Wysyła np. "X:150\n" lub "Y:90\n" po porcie szeregowym
        request->send(200, "application/json", "{\"status\":\"success\", \"message\":\"Serwo zaktualizowane\"}");
      } else {
        request->send(400, "application/json", "{\"status\":\"error\", \"message\":\"Bledna os (wymagane X lub Y)\"}");
      }

    } else {
      request->send(400, "application/json", "{\"status\":\"error\", \"message\":\"Brak parametru axis lub value w JSON\"}");
    }
  });

  server.addHandler(handler);
  server.begin();
}

void loop() {
  unsigned long currentMillis = millis();

  // Cykliczny timer wysyłający status środowiskowy do chmury
  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;
    sendTelemetryToBackend();
  }
}