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

// Główny ekran wyboru trybu przy uruchomieniu robota
void pokazMenu() {
  int kursor = 0;
  bool wybrany = false;
  
  lcd.clear();
  
  while (!wybrany) {
    lcd.setCursor(0, 0); 
    lcd.print("WYBIERZ TRYB:");
    
    // Odczyt kierunku, aby przesuwać strzałkę wyboru
    int kierunek = pobierzKierunekJoysticka();
    if (kierunek != 0) { 
        kursor = (kierunek > 0) ? 1 : 0; 
        delay(200); 
    }
    
    // Wizualizacja strzałki na ekranie
    lcd.setCursor(0, 1);
    if (kursor == 0) lcd.print("> AUTO   MANUAL ");
    else             lcd.print("  AUTO > MANUAL ");
    
    // Potwierdzenie przyciskiem z joysticka
    if (digitalRead(2) == LOW) { 
        trybAutomatyczny = (kursor == 0); 
        wybrany = true; 
        delay(500); 
    }
    delay(50);
  }
  
  // Zakończenie konfiguracji
  lcd.clear(); 
  lcd.print("STARTUJEMY..."); 
  delay(1000); 
  lcd.clear();
}