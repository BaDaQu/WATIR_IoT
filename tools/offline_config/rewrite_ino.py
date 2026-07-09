import re

with open('firmware/WATIR-ARDUINO/WATIR-ARDUINO.ino', 'r') as f:
    content = f.read()

# 1. Replace globals
old_globals = """// =============================================
// KONFIGURACJA PROFILU ROŚLINY
// (aktualizowana przez POST /api/config)
// =============================================
int           profilProgWilgotnosci  = 35;      // moisture_threshold (%)
bool          profilAutoWatering     = true;     // auto_watering
unsigned long profilCheckIntervalMs  = 10000;    // check_interval_ms
int           profilPan              = 90;       // pan  (pozycja serwa X)
int           profilTilt             = 90;       // tilt (pozycja serwa Y)"""

new_globals = """#include <EEPROM.h>

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

WatirConfig watirConfig;"""
content = content.replace(old_globals, new_globals)

# 2. Add EEPROM load in setup
old_setup = """  // --- Inicjalizacja modułów sprzętowych ---
  konfigurujPodlewanie();
  konfigurujWyswietlacz();
  konfigurujSerwa();"""
new_setup = """  // --- Inicjalizacja modułów sprzętowych ---
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
  ustawMocPompy(watirConfig.pump_power);"""
content = content.replace(old_setup, new_setup)

# 3. Update Odczyt gleby w loop
old_read = """    int gleba1 = zmierzWilgotnoscGleby1();
    int gleba2 = zmierzWilgotnoscGleby2();"""
new_read = """    int gleba1 = zmierzWilgotnoscGleby(watirConfig.p1_sensor);
    int gleba2 = zmierzWilgotnoscGleby(watirConfig.p2_sensor);"""
content = content.replace(old_read, new_read)


# 4. Update Auto Watering logic
old_auto = """  // --- 5. TRYB AUTOMATYCZNY — PODLEWANIE Z PROFILU ---
  if (trybAutomatyczny && profilAutoWatering && !systemZablokowany) {
    if (millis() - ostatnieSprawdzeniePodlewania >= profilCheckIntervalMs) {
      ostatnieSprawdzeniePodlewania = millis();

      int dystansWody = zmierzDystans();
      int gleba1 = zmierzWilgotnoscGleby1();
      int gleba2 = zmierzWilgotnoscGleby2();

      if (dystansWody < 12) {
        if (gleba1 < profilProgWilgotnosci || gleba2 < profilProgWilgotnosci) {
          Serial.println("[AUTO] Wilgotnosc ponizej progu — podlewam!");

          // Przesuń ramię na pozycję z profilu rośliny
          uzyjSerw(true);
          ustawPozycjeSerwaWiFi(profilPan, profilTilt);
          delay(1000); // Czekaj aż ramię dojedzie na pozycję

          // Podlej
          pompAktywna = true;
          podlej();
          pompAktywna = false;

          delay(1000);
          powrotDoBazy();
        }
      }
    }
  }"""
new_auto = """  // --- 5. TRYB AUTOMATYCZNY — PODLEWANIE Z PROFILU ---
  if (trybAutomatyczny && watirConfig.auto_watering && !systemZablokowany) {
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
  }"""
content = content.replace(old_auto, new_auto)

# 5. Update obsluzKlientaSerial
old_serial = """      // Aktualizuj parametry profilu
      if (config.containsKey("moisture_threshold"))
        profilProgWilgotnosci = config["moisture_threshold"].as<int>();

      if (config.containsKey("auto_watering"))
        profilAutoWatering = config["auto_watering"].as<bool>();

      if (config.containsKey("check_interval_ms"))
        profilCheckIntervalMs = config["check_interval_ms"].as<unsigned long>();

      if (config.containsKey("pump_power"))
        ustawMocPompy(config["pump_power"].as<int>());

      if (config.containsKey("target_plant") && config.containsKey("pan") && config.containsKey("tilt")) {
        int tPlant = config["target_plant"].as<int>();
        int tPan = constrain(config["pan"].as<int>(), 0, 180);
        int tTilt = constrain(config["tilt"].as<int>(), 0, 180);
        
        // Zapisujemy w EEPROM jako Roślina 1 lub 2
        ustawPozycjeSerwaWiFi(tPan, tTilt);
        zapiszPozycje(tPlant);
        
        Serial.println("{\\"status\\":\\"success\\",\\"message\\":\\"Zapisano pozycje serw dla Rosliny " + String(tPlant) + "\\"}");
        return; // Zakończ, jeśli to była tylko aktualizacja pozycji serw
      }

      // Informacja na LCD
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("Serial OK!");
      lcd.setCursor(0, 1);
      lcd.print("P:"); lcd.print(profilPan);
      lcd.print(" T:");  lcd.print(profilTilt);
      lcd.print(" W:");  lcd.print(profilProgWilgotnosci);
      
      // Wyślij potwierdzenie po Serialu (dla GUI w Pythonie)
      Serial.println("{\\"status\\":\\"success\\",\\"message\\":\\"Konfiguracja Serial zaaplikowana\\"}");"""

new_serial = """      // Aktualizuj parametry profilu globalnego
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
      Serial.println("{\\"status\\":\\"success\\",\\"message\\":\\"Zapisano do EEPROM\\"}");"""
content = content.replace(old_serial, new_serial)

# 6. handleConfig
old_handleConfig = """  // Aktualizuj parametry profilu
  if (config.containsKey("moisture_threshold"))
    profilProgWilgotnosci = config["moisture_threshold"].as<int>();

  if (config.containsKey("auto_watering"))
    profilAutoWatering = config["auto_watering"].as<bool>();

  if (config.containsKey("check_interval_ms"))
    profilCheckIntervalMs = config["check_interval_ms"].as<unsigned long>();

  if (config.containsKey("pump_power"))
    ustawMocPompy(config["pump_power"].as<int>());

  // Logowanie na Serial Monitor
  Serial.println("[CONFIG] Profil zaaplikowany:");
  Serial.print("  moisture_threshold: "); Serial.println(profilProgWilgotnosci);
  Serial.print("  auto_watering:      "); Serial.println(profilAutoWatering ? "TAK" : "NIE");
  Serial.print("  check_interval_ms:  "); Serial.println(profilCheckIntervalMs);
  Serial.print("  pump_power:         "); Serial.print(pobierzMocPompy()); Serial.println("%");

  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Profil OK!");
  lcd.setCursor(0, 1);
  lcd.print(" W:");  lcd.print(profilProgWilgotnosci);
  lcd.print(" P:");  lcd.print(pobierzMocPompy());
  delay(1500);
  lcd.clear();"""

new_handleConfig = """  // (Uproszczone z racji przejścia na Serial Config)
  if (config.containsKey("pump_power")) {
    watirConfig.pump_power = config["pump_power"].as<int>();
    ustawMocPompy(watirConfig.pump_power);
    EEPROM.put(0, watirConfig);
  }
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("HTTP Config OK!");
  delay(1000);
  lcd.clear();"""
content = content.replace(old_handleConfig, new_handleConfig)

with open('firmware/WATIR-ARDUINO/WATIR-ARDUINO.ino', 'w') as f:
    f.write(content)
