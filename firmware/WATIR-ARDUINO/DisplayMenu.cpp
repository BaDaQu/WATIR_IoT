// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
// Wersja: 2.0 — WiFi wbudowane (bez osobnego ESP)
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
  lcd.init(); // Podwójny init pomaga powrócić z trybu 4-bit na niektórych tanich wyświetlaczach
  lcd.backlight();
  lcd.clear();
}

// Zakończono konfigurację (pozostałe funkcje usunięte)