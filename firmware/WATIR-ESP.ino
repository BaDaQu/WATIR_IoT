#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <ESPAsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <AsyncJson.h>

const char * ssid = "WIFI"; 
const char * password = "1234567890"; 

AsyncWebServer server(80);

void setup() {
  Serial.begin(9600);
  
  WiFi.begin(ssid, password);
  Serial.print("Łączenie z WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nPołączono!");
  Serial.print("IP: "); Serial.println(WiFi.localIP());


  server.on("/api/status", HTTP_GET, [](AsyncWebServerRequest *request){

    AsyncResponseStream *response = request->beginResponseStream("application/json");
    
   
    StaticJsonDocument<512> doc;
    doc["device_id"] = "WATIR_01";
    
    JsonObject sensors = doc.createNestedObject("sensors");
    sensors["temp"] = 23.5;
    sensors["humidity"] = 45;
    sensors["soil_moisture"] = 30;
    sensors["water_level_cm"] = 8;
    
    JsonObject statusObj = doc.createNestedObject("status");
    statusObj["water_error"] = false;
    statusObj["pump_active"] = false;

    JsonObject servos = doc.createNestedObject("servos");
    servos["pan"] = 90;
    servos["tilt"] = 45;

    serializeJson(doc, *response);
    request->send(response);
  });


  server.addHandler(new AsyncCallbackJsonWebHandler("/api/move", [](AsyncWebServerRequest *request, JsonVariant &json) {
    StaticJsonDocument<200> responseDoc;
    JsonObject jsonObj = json.as<JsonObject>();

    if(jsonObj.containsKey("pan") && jsonObj.containsKey("tilt")) {
       int pan = jsonObj["pan"];
       int tilt = jsonObj["tilt"];
       
       Serial.printf("Ruch: Pan=%d, Tilt=%d\n", pan, tilt);
       
       responseDoc["status"] = "success";
       responseDoc["message"] = "OK";
    } else {
       responseDoc["status"] = "error";
       responseDoc["message"] = "Brak danych";
    }

    AsyncResponseStream *response = request->beginResponseStream("application/json");
    serializeJson(responseDoc, *response);
    request->send(response);
  }));

  server.begin();
  Serial.println("Serwer działa.");
}

void loop() {}