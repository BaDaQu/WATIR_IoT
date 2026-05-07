#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <SoftwareSerial.h>
#include "ServoControl.h"
#include "Sensors.h"
#include "Watering.h"
#include "DisplayMenu.h"

// Obiekty globalne
LiquidCrystal_I2C lcd(0x27, 16, 2);
SoftwareSerial portESP(3, 5); // RX na pinie 3, TX na pinie 5

const int pinBuzzer = 9;
bool trybAutomatyczny = false;
bool systemZablokowany = false; 
unsigned long ostatniOdczyt = 0;
unsigned long ostatnieKlikniecie = 0;

void setup() {
  Serial.begin(9600);
  portESP.begin(9600); // Komunikacja z modułem WiFi (ESP8266)
  portESP.setTimeout(10);
  
  Wire.begin();
  pinMode(2, INPUT_PULLUP); // Przycisk wbudowany w joystick
  
  // Wywołanie spolonizowanych funkcji konfiguracyjnych
  konfigurujPodlewanie();
  konfigurujWyswietlacz();
  konfigurujSerwa();
  konfigurujCzujniki();
  
  pokazMenu(); // Uruchamia ekran powitalny i wybór trybu
}

void loop() {
  // --- 1. OBSŁUGA PRZYCISKU JOYSTICKA ---
  if (digitalRead(2) == LOW) {
    unsigned long czasTeraz = millis();
    
    // Podwójne/szybkie kliknięcie powraca do menu
    if (czasTeraz - ostatnieKlikniecie < 400) { 
        lcd.clear(); 
        pokazMenu(); 
    }
    // Pojedyncze kliknięcie wymusza ręczne podlanie (jeśli jest woda)
    else if (zmierzDystans() < 12) { 
        podlej(); 
    }
    
    ostatnieKlikniecie = czasTeraz; 
    delay(250); // Prosty debouncing (niweluje drgania styków)
  }

  // --- 2. ODBIÓR KOMEND Z ESP8266 (WiFi) ---
  String komenda = "";
  if (trybAutomatyczny && portESP.available()) { 
    komenda = portESP.readStringUntil('\n'); 
    komenda.trim(); 
    
    if (komenda == "podlej") { 
        podlej(); 
        komenda = ""; 
    }
  } 

  // --- 3. RUCH RAMIENIA ROBOTA ---
  aktualizujSerwa(komenda, systemZablokowany);

  // --- 4. ODCZYT CZUJNIKÓW I WYSYŁKA DANYCH (co 1 sekundę) ---
  if (millis() - ostatniOdczyt >= 1000) {
    ostatniOdczyt = millis();
    
    int dystansWody = zmierzDystans();
    int gleba1 = zmierzWilgotnoscGleby1();
    int gleba2 = zmierzWilgotnoscGleby2();
    
    // Zabezpieczenie przed spaleniem pompy (Fail-Safe)
    if (dystansWody >= 12) {
      systemZablokowany = true;
      lcd.setCursor(0, 0); 
      lcd.print("!!! BRAK WODY !!!");
    } else {
      systemZablokowany = false;
      
      // Linia 1 na LCD: Temperatura i Wilgotność powietrza
      lcd.setCursor(0, 0); 
      lcd.print("T:"); lcd.print(zmierzTemperature(), 1); 
      lcd.print("C H:"); lcd.print(zmierzWilgotnoscPowietrza(), 0); lcd.print("%    ");
      
      // Linia 2 na LCD: Obie rośliny i poziom wody
      lcd.setCursor(0, 1); 
      lcd.print("G1:"); lcd.print(gleba1);
      lcd.setCursor(6, 1); 
      lcd.print("G2:"); lcd.print(gleba2);
      lcd.setCursor(12, 1); 
      lcd.print("W:"); lcd.print(dystansWody);
      
      // Czyszczenie starego zera, gdy wynik wody spadnie poniżej 10cm
      if(dystansWody < 10) lcd.print(" "); 

      // --- LOGIKA AUTOMATYCZNA I WYSYŁKA ---
      if (trybAutomatyczny) {
        // Autonomiczna decyzja o podlaniu
        logikaPodlewania(dystansWody, gleba1, gleba2); 
        
        // Usypiamy serwa na czas transmisji, by uniknąć spadków napięcia
        uzyjSerw(false); 
        
        // Formatowanie i wysyłanie paczki danych do ESP8266
        portESP.print("D:"); 
        portESP.print(zmierzTemperature(), 1); portESP.print(","); 
        portESP.print(zmierzWilgotnoscPowietrza(), 0); portESP.print(",");
        portESP.print(gleba1); portESP.print(",");
        portESP.print(gleba2); portESP.print(","); 
        portESP.print(dystansWody); portESP.print(",");
        portESP.print(systemZablokowany ? 1 : 0); portESP.print(","); 
        portESP.print(pobierzPozycjeSerwaX()); portESP.print(",");
        portESP.println(pobierzPozycjeSerwaY());
        
        // Zapis do EEPROM (jeśli użytkownik kliknął to w panelu WWW)
        if (komenda == "save1") {
            zapiszPozycje(1);
            lcd.clear(); lcd.print("Zapisano Poz. 1");
            delay(1000);
        } 
        else if (komenda == "save2") {
            zapiszPozycje(2);
            lcd.clear(); lcd.print("Zapisano Poz. 2");
            delay(1000);
        }
        
        // Budzimy serwa po zakończeniu transmisji
        uzyjSerw(true); 
      }
    }
  }
}