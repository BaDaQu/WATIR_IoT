// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
// ============================================
//
// Moduł: Wyświetlacz
// Implementacja funkcji obsługujących ekran LCD I2C.
//

#include "DisplayMenu.h"
#include <LiquidCrystal_I2C.h>
#include "ServoControl.h"

extern LiquidCrystal_I2C lcd;
extern bool trybAutomatyczny;

// Uruchomienie ekranu LCD
void konfigurujWyswietlacz() {
  delay(100); // Odczekaj, aby sterownik HD44780 zdążył przetrawić reset sprzętowy
  
  // Wymuszenie twardego resetu magistrali I2C
  Wire.end();
  delay(10);
  Wire.begin();
  
  lcd.init();
  delay(50);
  lcd.init(); 
  lcd.backlight();
  lcd.clear();
}

#include <WiFiS3.h>
#include <EEPROM.h>
#include "Config.h"

// Funkcja pomocnicza: oczekiwanie na puszczenie joysticka
static void waitRelease() {
  waitForJoystickRelease();
}

// Funkcja wprowadzania tekstu/liczby znak po znaku
static void enterString(const char* prompt, char* buffer, int maxLen, const char* charset, bool hideChars = false) {
  lcd.clear();
  lcd.print(prompt);
  
  int len = strlen(buffer);
  int cursorIdx = len;
  if (cursorIdx >= maxLen) cursorIdx = maxLen - 1;
  if (cursorIdx < 0) cursorIdx = 0;
  
  int charSetLen = strlen(charset);

  while (true) {
    lcd.setCursor(0, 1);
    for (int i = 0; i < 16; i++) {
      if (i < maxLen) {
        if (i < (int)strlen(buffer)) {
          lcd.print(hideChars ? '*' : buffer[i]);
        } else if (i == cursorIdx) {
          lcd.print('_'); // Pusty znak pod kursorem
        } else {
          lcd.print(' ');
        }
      } else {
        lcd.print(' '); // Wyczyszczenie reszty linii, aby uniknąć migotania i śmieci
      }
    }
    
    // Migający kursor na aktualnej pozycji
    lcd.setCursor(cursorIdx, 1);
    lcd.cursor();
    lcd.blink();

    JoyDir act = getJoystickAction();
    if (act != JOY_NONE) {
      if (act == JOY_CLICK) {
        waitRelease();
        break;
      }
      else if (act == JOY_LEFT) {
        if (cursorIdx > 0) cursorIdx--;
        waitRelease();
      }
      else if (act == JOY_RIGHT) {
        if (cursorIdx < maxLen - 1) cursorIdx++;
        // Uzupełnij spacjami/pierwszym znakiem jeśli wychodzimy poza strlen
        if (cursorIdx > (int)strlen(buffer)) {
          buffer[cursorIdx-1] = charset[0];
          buffer[cursorIdx] = '\0';
        }
        waitRelease();
      }
      else if (act == JOY_UP || act == JOY_DOWN) {
        // Zmiana aktualnego znaku
        char current = buffer[cursorIdx];
        if (current == '\0') current = charset[0]; // Inicjalizacja
        
        int idx = 0;
        for (int i = 0; i < charSetLen; i++) {
          if (charset[i] == current) {
            idx = i;
            break;
          }
        }
        
        if (act == JOY_UP) {
          idx = (idx + 1) % charSetLen;
        } else {
          idx = (idx - 1 + charSetLen) % charSetLen;
        }
        
        buffer[cursorIdx] = charset[idx];
        if (cursorIdx == (int)strlen(buffer)) {
          buffer[cursorIdx+1] = '\0'; // Przesuń terminator null
        }
        delay(300); // zwiększony delay dla spokojniejszego przewijania znaków
      }
    } else {
      delay(20); // Zabezpieczenie przed zapchaniem magistrali I2C (zapobiega zawieszaniu LCD)
    }
  }
  lcd.noBlink();
  lcd.noCursor();
  
  // Usunięcie końcowych spacji
  int end = strlen(buffer) - 1;
  while (end >= 0 && buffer[end] == ' ') {
    buffer[end] = '\0';
    end--;
  }
}

// Funkcja wprowadzania liczby
static int enterNumber(const char* prompt, int currentVal, int maxLen = 3) {
  char buf[10];
  snprintf(buf, sizeof(buf), "%d", currentVal);
  const char* digits = "0123456789 ";
  enterString(prompt, buf, maxLen, digits, false);
  return atoi(buf);
}

void runLcdConfigMenu() {
  lcd.clear();
  lcd.print("1. Skanuj WiFi");
  lcd.setCursor(0, 1);
  lcd.print("2. Uzyj EEPROM");
  
  int choice = 1; // 1 = WiFi, 2 = EEPROM
  bool redraw = true;
  while(true) {
    if (redraw) {
      lcd.setCursor(0, choice - 1);
      lcd.print(">");
      lcd.setCursor(0, 2 - choice);
      lcd.print(" ");
      redraw = false;
    }
    
    JoyDir act = getJoystickAction();
    if (act == JOY_UP || act == JOY_DOWN) {
      choice = (choice == 1) ? 2 : 1;
      redraw = true;
      waitRelease();
    } else if (act == JOY_CLICK) {
      waitRelease();
      break;
    } else {
      delay(20); // Zabezpieczenie przed zapchaniem magistrali I2C
    }
  }
  
  if (choice == 1) {
    bool wifiConnected = false;
    bool skipScan = false;
    while (!wifiConnected) {
      if (!skipScan) {
        // --- SKANOWANIE WIFI ---
        lcd.clear();
        lcd.print("Skanowanie...");
        int numNetworks = WiFi.scanNetworks();
        if (numNetworks == 0) {
          lcd.clear();
          lcd.print("Brak sieci!");
          delay(2000);
          trybAutomatyczny = true;
          return; // Przejście do działania offline
        }
        
        int netIdx = 0;
        bool redraw = true;
        while(true) {
          if (redraw) {
            lcd.clear();
            lcd.print("Wybierz WiFi:");
            lcd.setCursor(0, 1);
            lcd.print("< ");
            String ssidName = WiFi.SSID(netIdx);
            lcd.print(ssidName.substring(0, 12));
            lcd.print(" >");
            redraw = false;
          }
          
          JoyDir act = getJoystickAction();
          if (act == JOY_RIGHT) {
            netIdx = (netIdx + 1) % numNetworks;
            redraw = true;
            waitRelease();
          } else if (act == JOY_LEFT) {
            netIdx = (netIdx - 1 + numNetworks) % numNetworks;
            redraw = true;
            waitRelease();
          } else if (act == JOY_CLICK) {
            strncpy(watirConfig.wifi_ssid, WiFi.SSID(netIdx), 31);
            watirConfig.wifi_ssid[31] = '\0';
            waitRelease();
            break;
          } else {
            delay(20); // Zabezpieczenie I2C
          }
        }
      }
      skipScan = false; // Reset flagi po ewentualnym pominięciu
      
      // --- HASŁO WIFI ---
      const char* fullCharset = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_+=";
      enterString("Haslo WiFi:", watirConfig.wifi_pass, 16, fullCharset, false); // Pokaż znaki dla ułatwienia (lub hideChars=true)
      
      // --- PRÓBA POŁĄCZENIA ---
      lcd.clear();
      lcd.print("Laczenie...");
      WiFi.begin(watirConfig.wifi_ssid, watirConfig.wifi_pass);
      
      int proba = 0;
      while (WiFi.status() != WL_CONNECTED && proba < 30) { // 15 sekund
        delay(500);
        lcd.print(".");
        proba++;
      }
      
      if (WiFi.status() == WL_CONNECTED) {
        lcd.clear();
        lcd.print("WiFi OK!");
        delay(1500);
        wifiConnected = true;
      } else {
        lcd.clear();
        lcd.print("Blad WiFi!");
        delay(1500);
        
        // --- ZAPYTANIE O PONOWIENIE ---
        int retryChoice = 1; // 1 = Tak, 2 = Nie
        bool redrawRetry = true;
        bool retryDecided = false;
        
        while(!retryDecided) {
          if (redrawRetry) {
            lcd.clear();
            lcd.print("Sprobowac znow?");
            lcd.setCursor(0, 1);
            if (retryChoice == 1) lcd.print(">Tak  Nie");
            else                  lcd.print(" Tak >Nie");
            redrawRetry = false;
          }
          
          JoyDir act = getJoystickAction();
          if (act == JOY_LEFT || act == JOY_RIGHT) {
            retryChoice = (retryChoice == 1) ? 2 : 1;
            redrawRetry = true;
            waitRelease();
          } else if (act == JOY_CLICK) {
            waitRelease();
            retryDecided = true;
          } else {
            delay(20); // Zabezpieczenie I2C
          }
        }
        
        if (retryChoice == 1) {
          skipScan = true; // Pomija ponowne skanowanie i przechodzi do wpisywania hasła
        } else {
          lcd.clear();
          lcd.print("Tryb OFFLINE");
          delay(1500);
          extern bool isOfflineMode;
          isOfflineMode = true;
          break; // Przerywa pętlę WiFi i idzie dalej
        }
      }
    }
  } else {
    // Łączenie z zapisanym EEPROM
    if (strlen(watirConfig.wifi_ssid) > 0) {
      lcd.clear();
      lcd.print("Laczenie z:");
      lcd.setCursor(0, 1);
      lcd.print(watirConfig.wifi_ssid);
      WiFi.begin(watirConfig.wifi_ssid, watirConfig.wifi_pass);
      int proba = 0;
      while (WiFi.status() != WL_CONNECTED && proba < 30) { delay(500); proba++; }
    }
  }

  // Jeśli udało się połączyć z WiFi (skan lub EEPROM), przerywamy menu.
  // Resztę konfiguracji użytkownik przeprowadzi z poziomu aplikacji.
  if (WiFi.status() == WL_CONNECTED) {
    lcd.clear();
    lcd.print("Siec gotowa.");
    delay(1500);
    EEPROM.put(0, watirConfig); // Zapisz nowe/stare dane WiFi, żeby pamiętał
    trybAutomatyczny = true;
    return; 
  }

  // --- PARAMETRY PODLEWANIA (Tylko dla trybu OFFLINE) ---
  lcd.clear();
  lcd.print("Konfig. upraw");
  delay(1500);

  watirConfig.p1_moisture = constrain(enterNumber("Gleba 1 (%) min:", watirConfig.p1_moisture, 2), 1, 99);
  strcpy(watirConfig.p1_name, "Offline 1");
  watirConfig.p2_moisture = constrain(enterNumber("Gleba 2 (%) min:", watirConfig.p2_moisture, 2), 1, 99);
  strcpy(watirConfig.p2_name, "Offline 2");
  
  watirConfig.p1_pump_power = constrain(enterNumber("Moc Pompy R1(%):", watirConfig.p1_pump_power, 2), 1, 99);
  watirConfig.p2_pump_power = constrain(enterNumber("Moc Pompy R2(%):", watirConfig.p2_pump_power, 2), 1, 99);
  
  watirConfig.min_air_humidity_force = constrain(enterNumber("Wilg.powietrza<:", watirConfig.min_air_humidity_force, 2), 1, 99);
  watirConfig.max_temp_force = constrain(enterNumber("Temp.pow. (C) >:", watirConfig.max_temp_force, 2), 1, 99);

  // --- POZYCJE SERW ---
  lcd.clear();
  lcd.print("Ustaw Pozycje 1");
  delay(1000);
  calibratePlantPosition(watirConfig.p1_pan, watirConfig.p1_tilt, 1);
  
  lcd.clear();
  lcd.print("Ustaw Pozycje 2");
  delay(1000);
  calibratePlantPosition(watirConfig.p2_pan, watirConfig.p2_tilt, 2);

  // Konfiguracja offline jest tymczasowa (nie zapisujemy jej do EEPROM),
  // więc reset płytki ją zresetuje.
  lcd.clear();
  lcd.print("Gotowe!");
  delay(2000);
  
  trybAutomatyczny = true;
}
