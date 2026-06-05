#include "ServoControl.h"
#include <Servo.h>
#include <EEPROM.h>

// Piny dla ramienia robotycznego
const int pinSerwo1 = 11; 
const int pinSerwo2 = 10; 
const int pinJoyX = A3;
const int pinJoyY = A2;

Servo serwoX; // Serwo do ruchu lewo/prawo
Servo serwoY; // Serwo do ruchu góra/dół

int aktualnaPozycjaX = 90;
int aktualnaPozycjaY = 90;

// Martwa strefa (margines błędu dla joysticka)
const int martwaStrefaMin = 400; 
const int martwaStrefaMax = 600; 

// Adresy komórek pamięci EEPROM dla dwóch roślin
const int ADRES_G1_X = 0;
const int ADRES_G1_Y = 1;
const int ADRES_G2_X = 2;
const int ADRES_G2_Y = 3;

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
void zapiszPozycje(int roslina) {
    if (roslina == 1) {
        EEPROM.update(ADRES_G1_X, aktualnaPozycjaX);
        EEPROM.update(ADRES_G1_Y, aktualnaPozycjaY);
    } else {
        EEPROM.update(ADRES_G2_X, aktualnaPozycjaX);
        EEPROM.update(ADRES_G2_Y, aktualnaPozycjaY);
    }
}

// Odczyt zapisanych pozycji z pamięci i nakierowanie dyszy
void ustawNadRoslina(int roslina) {
    uzyjSerw(true);
    if (roslina == 1) {
        aktualnaPozycjaX = EEPROM.read(ADRES_G1_X);
        aktualnaPozycjaY = EEPROM.read(ADRES_G1_Y);
    } else {
        aktualnaPozycjaX = EEPROM.read(ADRES_G2_X);
        aktualnaPozycjaY = EEPROM.read(ADRES_G2_Y);
    }
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

// Odświeżanie pozycji ramienia (obsługa WiFi i fizycznego joysticka)
void aktualizujSerwa(String komendaWiFi, bool blokadaSerw) {
    if (blokadaSerw) return;

    // Sterowanie bezprzewodowe (z aplikacji)
    if (komendaWiFi == "gora")  aktualnaPozycjaY = constrain(aktualnaPozycjaY + 15, 0, 180);
    else if (komendaWiFi == "dol")   aktualnaPozycjaY = constrain(aktualnaPozycjaY - 15, 0, 180);
    else if (komendaWiFi == "lewo")  aktualnaPozycjaX = constrain(aktualnaPozycjaX + 15, 0, 180);
    else if (komendaWiFi == "prawo") aktualnaPozycjaX = constrain(aktualnaPozycjaX - 15, 0, 180);

    // Sterowanie manualne (fizyczny joystick na obudowie)
    static unsigned long ostatniKrok = 0;
    if (millis() - ostatniKrok >= 25) { 
        ostatniKrok = millis();

        int ox = analogRead(pinJoyX);
        int oy = analogRead(pinJoyY);

        // Obliczamy dominującą oś ruchu, by ignorować fałszywe skosy
        int odchylenieX = abs(ox - 512);
        int odchylenieY = abs(oy - 512);

        if (odchylenieX > odchylenieY) {
            // Reaguj tylko na oś X (lewo/prawo)
            if (ox > martwaStrefaMax) aktualnaPozycjaX -= 2;
            else if (ox < martwaStrefaMin) aktualnaPozycjaX += 2;
        } 
        else if (odchylenieY > odchylenieX) {
            // Reaguj tylko na oś Y (góra/dół)
            if (oy > martwaStrefaMax) aktualnaPozycjaY += 2;
            else if (oy < martwaStrefaMin) aktualnaPozycjaY -= 2;
        }

        // Zabezpieczenie limitów ruchu (0 - 180 stopni)
        aktualnaPozycjaX = constrain(aktualnaPozycjaX, 0, 180);
        aktualnaPozycjaY = constrain(aktualnaPozycjaY, 0, 180);

        if (serwoX.attached()) serwoX.write(aktualnaPozycjaX);
        if (serwoY.attached()) serwoY.write(aktualnaPozycjaY);
    }
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