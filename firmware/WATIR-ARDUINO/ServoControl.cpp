// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
// ============================================
//
// Moduł: Serwomechanizmy
// Implementacja funkcji obsługujących ramię robota 
// (serwa Pan/Tilt) oraz wejście z analogowego joysticka.
//

#include "ServoControl.h"
#include <Servo.h>
// (Usunięto EEPROM.h - cała konfiguracja zapisywana globalnie w WATIR-ARDUINO.ino)

// Piny dla ramienia robotycznego
const int pinSerwo1 = 9; 
const int pinSerwo2 = 6; 
const int pinJoyX = A3;
const int pinJoyY = A2;
const int pinJoyBtn = 2; 

Servo serwoX; // Serwo do ruchu lewo/prawo
Servo serwoY; // Serwo do ruchu góra/dół

int aktualnaPozycjaX = 90;
int aktualnaPozycjaY = 90;

// Martwa strefa (margines błędu dla joysticka)
const int martwaStrefaMin = 400; 
const int martwaStrefaMax = 600; 

// Krok serwa w stopniach na tick (większy = szybszy ruch)
const int krokSerwa = 5;

// Zmienne pozycji zostały przeniesione do głównego obiektu konfiguracji w WATIR-ARDUINO.ino,

// Włączanie lub usypianie serw (oszczędność prądu i redukcja drgań)
void uzyjSerw(bool wlacz) {
    if (wlacz) {
        if (!serwoX.attached()) serwoX.attach(pinSerwo1);
        if (!serwoY.attached()) serwoY.attach(pinSerwo2);
        serwoX.write(aktualnaPozycjaX);
        serwoY.write(aktualnaPozycjaY);
    } else {
        serwoX.detach();
        serwoY.detach();
    }
}

// Inicjalizacja serw na środek (90 stopni)
void konfigurujSerwa() {
    uzyjSerw(true);
    aktualnaPozycjaX = 90;
    aktualnaPozycjaY = 90;
}

// Zapis aktualnych kątów ramienia do nieulotnej pamięci
// Zapis odbywa się teraz z poziomu WATIR-ARDUINO.ino, po zaktualizowaniu struktury watirConfig

void ustawNadRoslina(int roslina, int pPan, int pTilt) {
    uzyjSerw(true);
    aktualnaPozycjaX = pPan;
    aktualnaPozycjaY = pTilt;

    serwoX.write(aktualnaPozycjaX);
    serwoY.write(aktualnaPozycjaY);
    delay(1000); // Odczekaj, aż ramię fizycznie dojedzie na pozycję
}

// Odczyt skrajnych wychyleń joysticka do menu
int pobierzKierunekJoysticka() {
    int x = analogRead(pinJoyX);
    if (x > 800) return 1;  
    if (x < 200) return -1; 
    return 0;               
}

// Aktywny kierunek z aplikacji WiFi (ustawiany przez handleMove)
// Dopóki nie przyjdzie "stop", firmware sam porusza serwem
// w każdym obrocie pętli — identycznie jak fizyczny joystick.
String komendaWiFiCiagla = "";

// Odświeżanie pozycji ramienia (obsługa WiFi i fizycznego joysticka)
void aktualizujSerwa(bool blokadaSerw) {
    if (blokadaSerw) return;

    // Wspólny timer dla obu źródeł sterowania (25ms między krokami)
    static unsigned long ostatniKrok = 0;
    if (millis() - ostatniKrok < 25) return;
    ostatniKrok = millis();

    // --- Sterowanie bezprzewodowe (ciągły kierunek z aplikacji) ---
    if (komendaWiFiCiagla == "lewo")       aktualnaPozycjaX += krokSerwa;
    else if (komendaWiFiCiagla == "prawo")  aktualnaPozycjaX -= krokSerwa;
    else if (komendaWiFiCiagla == "gora")   aktualnaPozycjaY -= krokSerwa;
    else if (komendaWiFiCiagla == "dol")    aktualnaPozycjaY += krokSerwa;

    // --- Sterowanie manualne (fizyczny joystick na obudowie) ---
    int ox = analogRead(pinJoyX);
    int oy = analogRead(pinJoyY);

    // Obliczamy dominującą oś ruchu, by ignorować fałszywe skosy
    int odchylenieX = abs(ox - 512);
    int odchylenieY = abs(oy - 512);

    if (odchylenieX > odchylenieY) {
        // Reaguj tylko na oś X (lewo/prawo)
        if (ox > martwaStrefaMax) aktualnaPozycjaX -= krokSerwa;
        else if (ox < martwaStrefaMin) aktualnaPozycjaX += krokSerwa;
    } 
    else if (odchylenieY > odchylenieX) {
        // Reaguj tylko na oś Y (góra/dół)
        if (oy > martwaStrefaMax) aktualnaPozycjaY += krokSerwa;
        else if (oy < martwaStrefaMin) aktualnaPozycjaY -= krokSerwa;
    }

    // Zabezpieczenie limitów ruchu (0 - 180 stopni)
    aktualnaPozycjaX = constrain(aktualnaPozycjaX, 0, 180);
    aktualnaPozycjaY = constrain(aktualnaPozycjaY, 0, 180);

    if (serwoX.attached()) serwoX.write(aktualnaPozycjaX);
    if (serwoY.attached()) serwoY.write(aktualnaPozycjaY);
}
void powrotDoBazy() {
    aktualnaPozycjaX = 90;
    aktualnaPozycjaY = 90;
    serwoX.write(90);
    serwoY.write(90);
    delay(1000);
}

// Funkcje pomocnicze zwracające kąty
int pobierzPozycjeSerwaX() { return aktualnaPozycjaX; }
int pobierzPozycjeSerwaY() { return aktualnaPozycjaY; }

// Bezpośrednie ustawienie pozycji serw z WiFi (z /api/move i /api/config)
void ustawPozycjeSerwaWiFi(int x, int y) {
    uzyjSerw(true); // Upewnij się, że serwa są podłączone
    aktualnaPozycjaX = constrain(x, 0, 180);
    aktualnaPozycjaY = constrain(y, 0, 180);
    serwoX.write(aktualnaPozycjaX);
    serwoY.write(aktualnaPozycjaY);
}

// --- NOWE FUNKCJE DO MENU LCD ---

JoyDir getJoystickAction() {
    if (digitalRead(pinJoyBtn) == LOW) {
        delay(50); // debounce
        if (digitalRead(pinJoyBtn) == LOW) {
            return JOY_CLICK;
        }
    }
    int ox = analogRead(pinJoyX);
    int oy = analogRead(pinJoyY);
    
    int odchylenieX = abs(ox - 512);
    int odchylenieY = abs(oy - 512);
    
    // Ignorowanie skosów: wygrywa silniejsze odchylenie
    if (odchylenieX > odchylenieY) {
        if (ox > martwaStrefaMax) return JOY_LEFT;  // Odwrócona oś X
        if (ox < martwaStrefaMin) return JOY_RIGHT;
    } else if (odchylenieY > odchylenieX) {
        if (oy > martwaStrefaMax) return JOY_UP;    // Odwrócona oś Y
        if (oy < martwaStrefaMin) return JOY_DOWN;
    }
    return JOY_NONE;
}

void waitForJoystickRelease() {
    while(getJoystickAction() != JOY_NONE) {
        delay(10);
    }
    delay(50);
}

#include <LiquidCrystal_I2C.h>
extern LiquidCrystal_I2C lcd;

void calibratePlantPosition(int &pan, int &tilt, int roslina) {
    uzyjSerw(true);
    aktualnaPozycjaX = pan;
    aktualnaPozycjaY = tilt;
    serwoX.write(aktualnaPozycjaX);
    serwoY.write(aktualnaPozycjaY);
    
    lcd.clear();
    lcd.print("Kalibracja R"); lcd.print(roslina);
    lcd.setCursor(0, 1);
    lcd.print("Ustaw i Kliknij");

    // Poczekaj chwilę, żeby użytkownik puścił joystick po wejściu do tego menu
    waitForJoystickRelease();

    while(true) {
        JoyDir act = getJoystickAction();
        if (act == JOY_CLICK) {
            waitForJoystickRelease();
            pan = aktualnaPozycjaX;
            tilt = aktualnaPozycjaY;
            
            lcd.clear();
            lcd.print("Zapisano poz.");
            delay(1000);
            break;
        }
        
        if (act != JOY_NONE) {
            if (act == JOY_LEFT) aktualnaPozycjaX += krokSerwa;
            else if (act == JOY_RIGHT) aktualnaPozycjaX -= krokSerwa;
            else if (act == JOY_UP) aktualnaPozycjaY -= krokSerwa;
            else if (act == JOY_DOWN) aktualnaPozycjaY += krokSerwa;
            
            aktualnaPozycjaX = constrain(aktualnaPozycjaX, 0, 180);
            aktualnaPozycjaY = constrain(aktualnaPozycjaY, 0, 180);
            
            serwoX.write(aktualnaPozycjaX);
            serwoY.write(aktualnaPozycjaY);
            delay(50); // Mniejsze opóźnienie dla płynności
        }
    }
}
