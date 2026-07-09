// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
// Wersja: 2.0 — WiFi wbudowane (bez osobnego ESP)
// ============================================
//
// Moduł: Serwomechanizmy
// Plik nagłówkowy dla funkcji obsługujących ramię robota 
// (serwa Pan/Tilt) oraz wejście z analogowego joysticka.
//

#ifndef SERVO_CONTROL_H
#define SERVO_CONTROL_H

#include <Arduino.h>

void konfigurujSerwa();
void aktualizujSerwa(bool blokadaSerw);
extern String komendaWiFiCiagla;
int pobierzKierunekJoysticka();
int pobierzPozycjeSerwaX();
int pobierzPozycjeSerwaY();
void uzyjSerw(bool wlacz);
void ustawNadRoslina(int roslina, int pPan, int pTilt);
void powrotDoBazy();
void ustawPozycjeSerwaWiFi(int x, int y);

#endif