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
  lcd.init();
  lcd.backlight();
}

// Zakończono konfigurację (pozostałe funkcje usunięte)