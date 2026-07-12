// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
// ============================================
//
// Moduł: Serwomechanizmy
// Plik nagłówkowy dla funkcji obsługujących ramię robota 
// (serwa Pan/Tilt) oraz wejście z analogowego joysticka.
//

#ifndef SERVO_CONTROL_H
#define SERVO_CONTROL_H

#include <Arduino.h>

enum JoyDir {
    JOY_NONE,
    JOY_UP,
    JOY_DOWN,
    JOY_LEFT,
    JOY_RIGHT,
    JOY_CLICK
};

void konfigurujSerwa();
void aktualizujSerwa(bool blokadaSerw);
extern String komendaWiFiCiagla;
int pobierzKierunekJoysticka(); // Stara funkcja, ew. do refaktoryzacji
JoyDir getJoystickAction();
void waitForJoystickRelease();
void calibratePlantPosition(int &pan, int &tilt, int roslina);

int pobierzPozycjeSerwaX();
int pobierzPozycjeSerwaY();
void uzyjSerw(bool wlacz);
void ustawNadRoslina(int roslina, int pPan, int pTilt);
void powrotDoBazy();
void ustawPozycjeSerwaWiFi(int x, int y);

#endif