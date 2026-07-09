// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
// Wersja: 2.0 — WiFi wbudowane (bez osobnego ESP)
// ============================================
//
// Arduino R4 WiFi przejmuje rolę zarówno starego
// Arduino UNO jak i ESP8266 — jeden układ obsługuje:
// - WiFi + HTTP Server (/api/move, /api/config, /api/water)
// - Czujniki (BME280, HC-SR04, wilgotność gleby)
// - Serwa (ramię robota + joystick fizyczny)
// - Pompę wodną (sterownik L298N, ENA=3, IN1=5, IN2=4)
// - Wyświetlacz LCD I2C
// - Cykliczną telemetrię do backendu Node.js
//

#include <WiFiS3.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include "Arduino_LED_Matrix.h"
#include "ServoControl.h"
#include "Sensors.h"
#include "Watering.h"
#include "DisplayMenu.h"

// =============================================
// KONFIGURACJA WiFi
// =============================================
const char* ssid     = "WATIR";
const char* password = "WATIRINO";

// =============================================
// KONFIGURACJA BACKENDU (adres serwera API)
// Zmień na adres IP swojego serwera Node.js
// =============================================
const char* backendHost    = "10.101.29.4";
const int   backendPort    = 3000;
const char* telemetryPath  = "/api/telemetry";
const char* deviceId       = "WATIR_01";

// =============================================
// OBIEKTY GLOBALNE
// =============================================
LiquidCrystal_I2C lcd(0x27, 16, 2);
WiFiServer serwer(80);
ArduinoLEDMatrix matrycaLED;

byte animacjaRosliny[8][8][12] = {
  // Klatka 0: Pusta gleba
  {
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
  },
  // Klatka 1: Kropla spada 1
  {
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
  },
  // Klatka 2: Kropla spada 2
  {
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
  },
  // Klatka 3: Rozprysk
  {
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0 },
    { 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0 },
    { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
  },
  // Klatka 4: Mały kiełek
  {
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
  },
  // Klatka 5: Liście
  {
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
  },
  // Klatka 6: Duża roślina
  {
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0 },
    { 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
  },
  // Klatka 7: Duża roślina (oczekiwanie)
  {
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0 },
    { 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
    { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
  }
};

// =============================================
// ZMIENNE STANU SYSTEMU
// =============================================
bool trybAutomatyczny  = false;   // Ustawiany przez menu na LCD
bool systemZablokowany = false;   // Fail-Safe — brak wody
bool pompAktywna       = false;   // Flaga dla telemetrii

#include <EEPROM.h>

// =============================================
// KONFIGURACJA PROFILU ROŚLINY
// (zapisywana i odczytywana z EEPROM)
// =============================================
struct WatirConfig {
  uint32_t magic;
  int p1_moisture;
  int p1_sensor;
  int p1_pan;
  int p1_tilt;
  int p2_moisture;
  int p2_sensor;
  int p2_pan;
  int p2_tilt;
  int min_temp_block;
  int max_temp_force;
  int pump_power;
  bool auto_watering;
  unsigned long check_interval_ms;
};

WatirConfig watirConfig;

// =============================================
// TIMERY
// =============================================
unsigned long ostatniOdczyt                = 0;
unsigned long ostatnieKlikniecie           = 0;
unsigned long ostatniaTelemetria           = 0;
unsigned long ostatnieSprawdzeniePodlewania = 0;
const unsigned long TELEMETRY_INTERVAL     = 30000;  // Co 30 sekund

// =============================================
// PIN PRZYCISKU JOYSTICKA
// =============================================
const int pinPrzycisk = 2;

// =============================================
// DEKLARACJE FUNKCJI
// =============================================
void obsluzKlientaHTTP();
void obsluzKlientaSerial();
void handleMove(WiFiClient& client, String& body);
void handleConfig(WiFiClient& client, String& body);
void handleWater(WiFiClient& client);
void handlePump(WiFiClient& client, String& body);
void wyslijTelemetrie();
void wyslijOdpowiedzJSON(WiFiClient& client, int kod, String json);

// =============================================
// PANEL DIAGNOSTYCZNY
// =============================================
void aktualizujPanelDiagnostyczny() {
  bool bladWiFi = (WiFi.status() != WL_CONNECTED);
  bool bladBME = !bmeOK;
  bool brakWody = systemZablokowany;
  bool trybReczny = !trybAutomatyczny;

  bool jestBlad = bladWiFi || bladBME || brakWody || trybReczny;

  if (!jestBlad) {
    // Wszystko OK - odtwarzaj animację
    static unsigned long czasOstatniejKlatki = 0;
    static int aktualnaKlatka = 0;
    
    if (millis() - czasOstatniejKlatki > 300) { 
      czasOstatniejKlatki = millis();
      aktualnaKlatka = (aktualnaKlatka + 1) % 8;
      matrycaLED.renderBitmap(animacjaRosliny[aktualnaKlatka], 8, 12);
    }
  } else {
    // Wystąpił błąd - zatrzymaj animację i pokaż podświetlone sektory
    byte panel[8][12] = {0};
    
    if (bladWiFi) { // Górny-Lewy (WiFi)
      for(int r=0; r<4; r++) for(int c=0; c<6; c++) panel[r][c] = 1;
    }
    if (bladBME) { // Górny-Prawy (BME280)
      for(int r=0; r<4; r++) for(int c=6; c<12; c++) panel[r][c] = 1;
    }
    if (brakWody) { // Dolny-Lewy (Brak wody)
      for(int r=4; r<8; r++) for(int c=0; c<6; c++) panel[r][c] = 1;
    }
    if (trybReczny) { // Dolny-Prawy (Tryb ręczny)
      for(int r=4; r<8; r++) for(int c=6; c<12; c++) panel[r][c] = 1;
    }
    
    matrycaLED.renderBitmap(panel, 8, 12);
  }
}

// =============================================
// SETUP
// =============================================
void setup() {
  Serial.begin(115200);

  // Czekaj na Serial max 3 sekundy (R4 WiFi używa natywnego USB)
  unsigned long serialTimeout = millis() + 3000;
  while (!Serial && millis() < serialTimeout) { ; }

  pinMode(pinPrzycisk, INPUT_PULLUP);

  // --- Inicjalizacja magistrali I2C (piny SDA i SCL oraz A4 i A5) ---
  Wire.begin();

  // --- Inicjalizacja matrycy LED na płytce R4 ---
  matrycaLED.begin();
  matrycaLED.renderBitmap(animacjaRosliny[0], 8, 12);

  // --- Inicjalizacja modułów sprzętowych ---
  konfigurujPodlewanie();
  konfigurujWyswietlacz();
  konfigurujSerwa();

  EEPROM.get(0, watirConfig);
  if (watirConfig.magic != 0x12345678) {
    watirConfig.magic = 0x12345678;
    watirConfig.p1_moisture = 35;
    watirConfig.p1_sensor = A1;
    watirConfig.p1_pan = 90;
    watirConfig.p1_tilt = 90;
    watirConfig.p2_moisture = 35;
    watirConfig.p2_sensor = A0;
    watirConfig.p2_pan = 90;
    watirConfig.p2_tilt = 90;
    watirConfig.min_temp_block = 5;
    watirConfig.max_temp_force = 35;
    watirConfig.pump_power = 70;
    watirConfig.auto_watering = true;
    watirConfig.check_interval_ms = 10000;
    EEPROM.put(0, watirConfig);
  }
  ustawMocPompy(watirConfig.pump_power);

  // --- Łączenie z WiFi (do 60 sekund) ---
  lcd.clear();
  lcd.print("Laczenie WiFi...");
  Serial.print("[WiFi] Laczenie z siecia: ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  int proba = 0;
  // 120 prób co 500 ms = 60 sekund
  while (WiFi.status() != WL_CONNECTED && proba < 120) {
    delay(500);
    Serial.print(".");
    proba++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    // Wait for IP address to be assigned by DHCP
    while (WiFi.localIP()[0] == 0) {
      delay(100);
    }
    Serial.println();
    Serial.print("[WiFi] Polaczono! IP: ");
    Serial.println(WiFi.localIP());

    trybAutomatyczny = true;

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("WiFi OK!");
    lcd.setCursor(0, 1);
    lcd.print("TRYB: AUTO");
    delay(2000);
  } else {
    Serial.println();
    Serial.println("[WiFi] Blad polaczenia! Przejscie w tryb reczny.");

    trybAutomatyczny = true; // Zabezpieczenie przed zablokowaniem automatyki w przypadku braku połączenia WiFi

    lcd.clear();
    lcd.print("Brak WiFi!");
    lcd.setCursor(0, 1);
    lcd.print("TRYB: MANUAL");
    delay(2000);
  }

  // --- Inicjalizacja czujników po WiFi ---
  // Inicjalizacja czujników SPI (np. BME280) po WiFi.begin(), 
  // aby uniknąć problemów z rekonfiguracją magistrali przez bibliotekę WiFi.
  konfigurujCzujniki();

  // --- Start serwera HTTP ---
  serwer.begin();
  Serial.println("[HTTP] Serwer nasluchuje na porcie 80");

  lcd.clear();
}

// =============================================
// LOOP — GŁÓWNA PĘTLA PROGRAMU
// =============================================
void loop() {
  
  // --- OBSŁUGA MATRYCY LED I PANELU DIAGNOSTYCZNEGO ---
  aktualizujPanelDiagnostyczny();

  // --- 1. OBSŁUGA PRZYCISKU JOYSTICKA ---
  if (digitalRead(pinPrzycisk) == LOW) {
    unsigned long czasTeraz = millis();

    // Podwójne kliknięcie — powrót do menu
    if (czasTeraz - ostatnieKlikniecie < 400) {
      lcd.clear();
     
    }
    // Pojedyncze kliknięcie — ręczne podlanie (jeśli jest woda)
    else if (zmierzDystans() < 12) {
      pompAktywna = true;
      podlej();
      pompAktywna = false;
    }

    ostatnieKlikniecie = czasTeraz;
    delay(250); // Debouncing
  }

  // --- 2. OBSŁUGA ŻĄDAŃ HTTP (Z APLIKACJI / BACKENDU) ---
  obsluzKlientaHTTP();

  // --- 2B. OBSŁUGA KONFIGURACJI PRZEZ SERIAL (OFFLINE) ---
  obsluzKlientaSerial();

  // --- 3. RUCH RAMIENIA — JOYSTICK FIZYCZNY + KIERUNEK WiFi ---
  // Serwa nigdy nie blokujemy — blokada "brak wody" dotyczy tylko pompy
  aktualizujSerwa(false);

  // --- 4. TELEMETRIA SERIAL (na żywo, co 200 ms) ---
  static unsigned long ostatniaTelemetriaSerial = 0;
  if (millis() - ostatniaTelemetriaSerial >= 200) {
    ostatniaTelemetriaSerial = millis();
    Serial.print("{\"telemetry\": {\"pan\": ");
    Serial.print(pobierzPozycjeSerwaX());
    Serial.print(", \"tilt\": ");
    Serial.print(pobierzPozycjeSerwaY());
    Serial.println("}}");
  }

  // --- 5. ODCZYT CZUJNIKÓW I AKTUALIZACJA LCD (co 1 sekundę) ---
  if (millis() - ostatniOdczyt >= 1000) {
    ostatniOdczyt = millis();

    int dystansWody = zmierzDystans();
    int gleba1 = zmierzWilgotnoscGleby(watirConfig.p1_sensor);
    int gleba2 = zmierzWilgotnoscGleby(watirConfig.p2_sensor);

    // Fail-Safe — brak wody
    if (dystansWody >= 12) {
      systemZablokowany = true;
      lcd.setCursor(0, 0);
      lcd.print("!!! BRAK WODY !!!");
    } else {
      systemZablokowany = false;

      // LCD linia 1: Temperatura i wilgotność powietrza
      lcd.setCursor(0, 0);
      lcd.print("T:"); lcd.print(zmierzTemperature(), 1);
      lcd.print("C H:"); lcd.print(zmierzWilgotnoscPowietrza(), 0); lcd.print("%    ");

      // LCD linia 2: Wilgotność gleby i poziom wody
      lcd.setCursor(0, 1);
      lcd.print("G1:"); lcd.print(gleba1);
      lcd.setCursor(6, 1);
      lcd.print("G2:"); lcd.print(gleba2);
      lcd.setCursor(12, 1);
      lcd.print("W:"); lcd.print(dystansWody);
      if (dystansWody < 10) lcd.print(" ");
    }
  }

  // --- 5. TRYB AUTOMATYCZNY — PODLEWANIE Z PROFILU ---
  if (watirConfig.auto_watering && !systemZablokowany) {
    if (millis() - ostatnieSprawdzeniePodlewania >= watirConfig.check_interval_ms) {
      ostatnieSprawdzeniePodlewania = millis();

      int dystansWody = zmierzDystans();
      float temp = zmierzTemperature();
      int gleba1 = zmierzWilgotnoscGleby(watirConfig.p1_sensor);
      int gleba2 = zmierzWilgotnoscGleby(watirConfig.p2_sensor);

      if (dystansWody < 12) {
        if (temp < watirConfig.min_temp_block) {
          Serial.println("[AUTO] Za zimno! Blokada podlewania.");
        } else {
          bool force = (temp > watirConfig.max_temp_force);
          
          if (force || gleba1 < watirConfig.p1_moisture) {
            Serial.println("[AUTO] Podlewam Rosline 1!");
            ustawNadRoslina(1, watirConfig.p1_pan, watirConfig.p1_tilt);
            pompAktywna = true; podlej(); pompAktywna = false;
            delay(1000);
          }
          
          if (force || gleba2 < watirConfig.p2_moisture) {
            Serial.println("[AUTO] Podlewam Rosline 2!");
            ustawNadRoslina(2, watirConfig.p2_pan, watirConfig.p2_tilt);
            pompAktywna = true; podlej(); pompAktywna = false;
            delay(1000);
          }
          
          powrotDoBazy();
        }
      }
    }
  }

  // --- 6. CYKLICZNA TELEMETRIA DO BACKENDU (co 30 sekund) ---
  if (WiFi.status() == WL_CONNECTED) {
    if (millis() - ostatniaTelemetria >= TELEMETRY_INTERVAL) {
      ostatniaTelemetria = millis();

      // Usypiamy serwa na czas transmisji (oszczędność prądu)
      if (trybAutomatyczny) uzyjSerw(false);

      wyslijTelemetrie();

      // Budzimy serwa po transmisji
      if (trybAutomatyczny) uzyjSerw(true);
    }
  }
}

// =============================================
// OBSŁUGA HTTP — PARSOWANIE ŻĄDAŃ
// Ręczne parsowanie, bo na R4 WiFi nie ma
// AsyncWebServer — używamy wbudowanego WiFiServer
// =============================================
void obsluzKlientaHTTP() {
  WiFiClient client = serwer.available();
  if (!client) return;

  // Czekaj na dane z timeoutem
  unsigned long timeout = millis() + 3000;
  while (!client.available() && millis() < timeout) {
    delay(1);
  }
  if (!client.available()) {
    client.stop();
    return;
  }

  // Odczytaj linię żądania: "POST /api/move HTTP/1.1"
  String linia = client.readStringUntil('\n');
  linia.trim();

  String metoda  = "";
  String sciezka = "";
  int sp1 = linia.indexOf(' ');
  int sp2 = linia.indexOf(' ', sp1 + 1);
  if (sp1 > 0 && sp2 > sp1) {
    metoda  = linia.substring(0, sp1);
    sciezka = linia.substring(sp1 + 1, sp2);
  }

  // Odczytaj nagłówki — szukamy Content-Length
  int contentLength = 0;
  while (client.available()) {
    String naglowek = client.readStringUntil('\n');
    naglowek.trim();
    if (naglowek.length() == 0) break; // Pusta linia = koniec nagłówków

    String naglowekLower = naglowek;
    naglowekLower.toLowerCase();
    if (naglowekLower.startsWith("content-length:")) {
      String wartosc = naglowek.substring(15);
      wartosc.trim();
      contentLength = wartosc.toInt();
    }
  }

  // Odczytaj ciało żądania (body)
  String body = "";
  if (contentLength > 0 && metoda == "POST") {
    body.reserve(contentLength);
    unsigned long bodyTimeout = millis() + 5000;
    while ((int)body.length() < contentLength && millis() < bodyTimeout) {
      if (client.available()) {
        body += (char)client.read();
      } else {
        delay(1);
      }
    }
  }

  // --- ROUTING ENDPOINTÓW ---
  if (metoda == "POST" && sciezka == "/api/move") {
    handleMove(client, body);
  }
  else if (metoda == "POST" && sciezka == "/api/config") {
    handleConfig(client, body);
  }
  else if (metoda == "POST" && sciezka == "/api/water") {
    handleWater(client);
  }
  else if (metoda == "POST" && sciezka == "/api/pump") {
    handlePump(client, body);
  }
  else {
    wyslijOdpowiedzJSON(client, 404,
      "{\"status\":\"error\",\"message\":\"Nieznany endpoint\"}");
  }

  delay(1);
  client.stop();
}

// =============================================
// OBSŁUGA SERIAL — KONFIGURACJA OFFLINE
// Oczekuje JSON-a o strukturze takiej samej jak /api/config:
// {"config": {"moisture_threshold": 35, ...}}
// =============================================
void obsluzKlientaSerial() {
  if (Serial.available() > 0) {
    String linia = Serial.readStringUntil('\n');
    linia.trim();

    if (linia.length() > 0 && linia.startsWith("{")) {
      StaticJsonDocument<256> doc;
      DeserializationError err = deserializeJson(doc, linia);

      if (err) {
        Serial.println("{\"status\":\"error\",\"message\":\"Nieprawidlowy JSON\"}");
        return;
      }

      JsonObject mockObj = doc["mock"];
      if (!mockObj.isNull()) {
        if (mockObj.containsKey("active")) mockActive = mockObj["active"].as<bool>();
        if (mockObj.containsKey("g1")) mockG1 = mockObj["g1"].as<int>();
        if (mockObj.containsKey("g2")) mockG2 = mockObj["g2"].as<int>();
        if (mockObj.containsKey("temp")) mockTemp = mockObj["temp"].as<float>();
        if (mockObj.containsKey("water_level")) mockDist = mockObj["water_level"].as<int>();
        
        Serial.println("{\"status\":\"success\",\"message\":\"Mock aktywowany/zaktualizowany\"}");
        return;
      }

      if (doc.containsKey("debug")) {
        Serial.print("{\"DEBUG\": {\"p1_m\": "); Serial.print(watirConfig.p1_moisture);
        Serial.print(", \"p1_sensor\": "); Serial.print(watirConfig.p1_sensor);
        Serial.print(", \"g1\": "); Serial.print(zmierzWilgotnoscGleby(watirConfig.p1_sensor));
        Serial.print(", \"auto_w\": "); Serial.print(watirConfig.auto_watering);
        Serial.print(", \"zab\": "); Serial.print(systemZablokowany);
        Serial.print(", \"mock\": "); Serial.print(mockActive);
        Serial.println("}}");
        return;
      }

      JsonObject config = doc["config"];
      if (config.isNull()) {
        Serial.println("{\"status\":\"error\",\"message\":\"Brak obiektu config lub mock\"}");
        return;
      }

      // Aktualizuj parametry profilu globalnego
      if (config.containsKey("auto_watering")) watirConfig.auto_watering = config["auto_watering"].as<bool>();
      if (config.containsKey("check_interval_ms")) watirConfig.check_interval_ms = config["check_interval_ms"].as<unsigned long>();
      if (config.containsKey("pump_power")) {
        watirConfig.pump_power = config["pump_power"].as<int>();
        ustawMocPompy(watirConfig.pump_power);
      }
      if (config.containsKey("min_temp_block")) watirConfig.min_temp_block = config["min_temp_block"].as<int>();
      if (config.containsKey("max_temp_force")) watirConfig.max_temp_force = config["max_temp_force"].as<int>();

      if (config.containsKey("target_plant")) {
        int tPlant = config["target_plant"].as<int>();
        if (tPlant == 1) {
          if (config.containsKey("moisture_threshold")) watirConfig.p1_moisture = config["moisture_threshold"].as<int>();
          if (config.containsKey("sensor")) watirConfig.p1_sensor = config["sensor"].as<int>();
          if (config.containsKey("pan")) watirConfig.p1_pan = constrain(config["pan"].as<int>(), 0, 180);
          if (config.containsKey("tilt")) watirConfig.p1_tilt = constrain(config["tilt"].as<int>(), 0, 180);
          ustawPozycjeSerwaWiFi(watirConfig.p1_pan, watirConfig.p1_tilt);
        } else if (tPlant == 2) {
          if (config.containsKey("moisture_threshold")) watirConfig.p2_moisture = config["moisture_threshold"].as<int>();
          if (config.containsKey("sensor")) watirConfig.p2_sensor = config["sensor"].as<int>();
          if (config.containsKey("pan")) watirConfig.p2_pan = constrain(config["pan"].as<int>(), 0, 180);
          if (config.containsKey("tilt")) watirConfig.p2_tilt = constrain(config["tilt"].as<int>(), 0, 180);
          ustawPozycjeSerwaWiFi(watirConfig.p2_pan, watirConfig.p2_tilt);
        }
      }
      
      EEPROM.put(0, watirConfig);

      // Informacja na LCD
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("Serial OK!");
      lcd.setCursor(0, 1);
      lcd.print("Zapisano EEPROM.");
      
      // Wyślij potwierdzenie po Serialu (dla GUI w Pythonie)
      Serial.println("{\"status\":\"success\",\"message\":\"Zapisano do EEPROM\"}");
    }
  }
}

// =============================================
// HANDLER: POST /api/move
// Tryb ciągły (joystick w aplikacji):
//   Payload: {"direction":"lewo"}
//   Kierunki: "lewo", "prawo", "gora", "dol", "stop"
//   Firmware sam porusza serwem w pętli loop() —
//   identycznie jak fizyczny joystick.
//
// Tryb absolutny (ustawienie konkretnego kąta):
//   Payload: {"axis":"X","value":120}
// =============================================
void handleMove(WiFiClient& client, String& body) {
  StaticJsonDocument<128> doc;
  DeserializationError err = deserializeJson(doc, body);

  if (err) {
    wyslijOdpowiedzJSON(client, 400,
      "{\"status\":\"error\",\"message\":\"Nieprawidlowy JSON\"}");
    return;
  }

  // --- Tryb ciągły: {"direction":"lewo"} ---
  if (doc.containsKey("direction")) {
    String dir = doc["direction"].as<String>();
    if (dir == "lewo" || dir == "prawo" || dir == "gora" || dir == "dol") {
      komendaWiFiCiagla = dir;
      wyslijOdpowiedzJSON(client, 200,
        "{\"status\":\"success\",\"message\":\"Kierunek ustawiony\"}");
    } else if (dir == "stop") {
      komendaWiFiCiagla = "";
      wyslijOdpowiedzJSON(client, 200,
        "{\"status\":\"success\",\"message\":\"Ruch zatrzymany\"}");
    } else {
      wyslijOdpowiedzJSON(client, 400,
        "{\"status\":\"error\",\"message\":\"Nieznany kierunek\"}");
    }
    return;
  }

  // --- Tryb absolutny: {"axis":"X","value":120} ---
  if (!doc.containsKey("axis") || !doc.containsKey("value")) {
    wyslijOdpowiedzJSON(client, 400,
      "{\"status\":\"error\",\"message\":\"Brak parametru axis/value lub direction\"}");
    return;
  }

  String axis = doc["axis"].as<String>();
  int value   = constrain(doc["value"].as<int>(), 0, 180);

  if (axis == "X") {
    ustawPozycjeSerwaWiFi(value, pobierzPozycjeSerwaY());
    wyslijOdpowiedzJSON(client, 200,
      "{\"status\":\"success\",\"message\":\"Serwo X zaktualizowane\"}");
  }
  else if (axis == "Y") {
    ustawPozycjeSerwaWiFi(pobierzPozycjeSerwaX(), value);
    wyslijOdpowiedzJSON(client, 200,
      "{\"status\":\"success\",\"message\":\"Serwo Y zaktualizowane\"}");
  }
  else {
    wyslijOdpowiedzJSON(client, 400,
      "{\"status\":\"error\",\"message\":\"Bledna os (wymagane X lub Y)\"}");
  }
}

// =============================================
// HANDLER: POST /api/config
// Payload: {"config":{
//   "moisture_threshold":35,
//   "auto_watering":true,
//   "check_interval_ms":10000,
//   "pump_power":100,
//   "pan":90,
//   "tilt":90
// }}
//
// Identyczny format jak backend
// POST /api/plants/:id/apply → wysyła do ESP.
// Aktualizuje parametry profilu rośliny
// i natychmiast ustawia serwa na pozycję z profilu.
// =============================================
void handleConfig(WiFiClient& client, String& body) {
  StaticJsonDocument<256> doc;
  DeserializationError err = deserializeJson(doc, body);

  if (err) {
    wyslijOdpowiedzJSON(client, 400,
      "{\"status\":\"error\",\"message\":\"Nieprawidlowy JSON\"}");
    return;
  }

  // Wyciągnij obiekt "config" z payloadu
  JsonObject config = doc["config"];
  if (config.isNull()) {
    wyslijOdpowiedzJSON(client, 400,
      "{\"status\":\"error\",\"message\":\"Brak obiektu config w JSON\"}");
    return;
  }

  // (Uproszczone z racji przejścia na Serial Config)
  if (config.containsKey("pump_power")) {
    watirConfig.pump_power = config["pump_power"].as<int>();
    ustawMocPompy(watirConfig.pump_power);
    EEPROM.put(0, watirConfig);
  }
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("HTTP Config OK!");
  delay(1000);
  lcd.clear();

  wyslijOdpowiedzJSON(client, 200,
    "{\"status\":\"success\",\"message\":\"Konfiguracja zaaplikowana\"}");
}

// =============================================
// HANDLER: POST /api/water
// Payload: {} (puste ciało)
// Ręczne podlewanie z aplikacji mobilnej
// =============================================
void handleWater(WiFiClient& client) {
  if (systemZablokowany || zmierzDystans() >= 12) {
    wyslijOdpowiedzJSON(client, 400,
      "{\"status\":\"error\",\"message\":\"Brak wody - podlewanie zablokowane\"}");
    return;
  }

  Serial.println("[WATER] Reczne podlewanie z aplikacji");
  pompAktywna = true;
  podlej();
  pompAktywna = false;

  wyslijOdpowiedzJSON(client, 200,
    "{\"status\":\"success\",\"message\":\"Podlano pomyslnie\"}");
}

// =============================================
// HANDLER: POST /api/pump
// Pełne sterowanie pompą z aplikacji.
//
// Payload:
//   {"power": 60}                → ustaw moc na 60%
//   {"power": 60, "duration": 5000} → podlej 5s przy 60%
//   {"action": "status"}          → zwróć aktualny stan pompy
//
// Odpowiedź zawsze zawiera aktualny stan:
//   {"status":"success", "pump_power":60, "pump_active":false}
// =============================================
void handlePump(WiFiClient& client, String& body) {
  StaticJsonDocument<128> doc;
  DeserializationError err = deserializeJson(doc, body);

  // Jeśli brak JSON lub pusty body → zwróć status pompy
  if (err || body.length() == 0) {
    String resp = "{\"status\":\"success\",\"pump_power\":";
    resp += pobierzMocPompy();
    resp += ",\"pump_active\":";
    resp += pompAktywna ? "true" : "false";
    resp += "}";
    wyslijOdpowiedzJSON(client, 200, resp);
    return;
  }

  // --- Akcja "status" ---
  if (doc.containsKey("action")) {
    String action = doc["action"].as<String>();
    if (action == "status") {
      String resp = "{\"status\":\"success\",\"pump_power\":";
      resp += pobierzMocPompy();
      resp += ",\"pump_active\":";
      resp += pompAktywna ? "true" : "false";
      resp += "}";
      wyslijOdpowiedzJSON(client, 200, resp);
      return;
    }
  }

  // --- Ustawienie mocy ---
  if (doc.containsKey("power")) {
    int power = constrain(doc["power"].as<int>(), 0, 100);
    ustawMocPompy(power);
  }

  // --- Podlewanie z czasem trwania ---
  if (doc.containsKey("duration")) {
    int durationMs = constrain(doc["duration"].as<int>(), 500, 30000);

    if (systemZablokowany || zmierzDystans() >= 12) {
      wyslijOdpowiedzJSON(client, 400,
        "{\"status\":\"error\",\"message\":\"Brak wody - podlewanie zablokowane\"}");
      return;
    }

    Serial.print("[PUMP] Podlewanie: "); Serial.print(durationMs);
    Serial.print("ms przy "); Serial.print(pobierzMocPompy()); Serial.println("%");

    pompAktywna = true;
    // Ręczne sterowanie — nie używamy podlej(), bo mamy własny czas
    ustawPompe(true);
    delay(durationMs);
    ustawPompe(false);
    pompAktywna = false;
  }

  // Odpowiedź z aktualnym stanem
  String resp = "{\"status\":\"success\",\"pump_power\":";
  resp += pobierzMocPompy();
  resp += ",\"pump_active\":";
  resp += pompAktywna ? "true" : "false";
  resp += "}";
  wyslijOdpowiedzJSON(client, 200, resp);
}

// =============================================
// TELEMETRIA — WYSYŁANIE DANYCH DO BACKENDU
// JSON identyczny z formatem starego ESP8266:
// {
//   "device_id": "WATIR_01",
//   "sensors": { "temp", "humidity", "soil_moisture", "water_level_cm" },
//   "status":  { "water_error", "pump_active" },
//   "servos":  { "pan", "tilt" }
// }
// =============================================
void wyslijTelemetrie() {
  WiFiClient client;

  Serial.println("[HTTP] Wysylam telemetrie...");

  if (!client.connect(backendHost, backendPort)) {
    Serial.println("[HTTP] Blad polaczenia z backendem!");
    return;
  }

  // Budowanie JSONa telemetrycznego
  StaticJsonDocument<512> doc;
  doc["device_id"] = deviceId;

  JsonObject sensors = doc.createNestedObject("sensors");
  sensors["temp"]           = zmierzTemperature();
  sensors["humidity"]       = (int)zmierzWilgotnoscPowietrza();
  sensors["soil_moisture_1"] = zmierzWilgotnoscGleby(watirConfig.p1_sensor);
  sensors["soil_moisture_2"] = zmierzWilgotnoscGleby(watirConfig.p2_sensor);
  sensors["water_level_cm"] = zmierzDystans();

  JsonObject status = doc.createNestedObject("status");
  status["water_error"]  = systemZablokowany;
  status["pump_active"]  = pompAktywna;
  status["pump_power"]   = pobierzMocPompy();

  JsonObject servos = doc.createNestedObject("servos");
  servos["pan"]  = pobierzPozycjeSerwaX();
  servos["tilt"] = pobierzPozycjeSerwaY();

  String requestBody;
  serializeJson(doc, requestBody);

  // Wysyłanie HTTP POST ręcznie (brak biblioteki HTTPClient na R4)
  client.println("POST " + String(telemetryPath) + " HTTP/1.1");
  client.println("Host: " + String(backendHost) + ":" + String(backendPort));
  client.println("Content-Type: application/json");
  client.println("Content-Length: " + String(requestBody.length()));
  client.println("Connection: close");
  client.println();
  client.print(requestBody);

  // Odczyt odpowiedzi z timeoutem
  unsigned long timeout = millis() + 5000;
  while (!client.available() && millis() < timeout) {
    delay(10);
  }

  if (client.available()) {
    String statusLine = client.readStringUntil('\n');
    Serial.print("[HTTP] Odpowiedz: ");
    Serial.println(statusLine);
  } else {
    Serial.println("[HTTP] Brak odpowiedzi z backendu (timeout)");
  }

  client.stop();
}

// =============================================
// HELPER — WYSYŁANIE ODPOWIEDZI HTTP JSON
// =============================================
void wyslijOdpowiedzJSON(WiFiClient& client, int kod, String json) {
  String statusTekst;
  switch (kod) {
    case 200: statusTekst = "OK";                    break;
    case 400: statusTekst = "Bad Request";           break;
    case 404: statusTekst = "Not Found";             break;
    default:  statusTekst = "Internal Server Error"; break;
  }

  client.println("HTTP/1.1 " + String(kod) + " " + statusTekst);
  client.println("Content-Type: application/json");
  client.println("Access-Control-Allow-Origin: *");
  client.println("Connection: close");
  client.println("Content-Length: " + String(json.length()));
  client.println();
  client.print(json);
}
