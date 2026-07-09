// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
// Wersja: 2.0 — WiFi wbudowane (bez osobnego ESP)
// ============================================
//
// Moduł: Czujniki
// Plik nagłówkowy dla funkcji obsługujących czujniki:
// - BME280 (temperatura, wilgotność powietrza)
// - HC-SR04 (dystans do lustra wody)
// - Czujniki wilgotności gleby analogowe
//

#ifndef SENSORS_H
#define SENSORS_H
#include <Arduino.h>

bool konfigurujCzujniki();
int zmierzDystans();
int zmierzWilgotnoscGleby(int pin);
float zmierzTemperature();
float zmierzWilgotnoscPowietrza();

extern bool bmeOK;

// Flagi i zmienne globalne dla trybu Hardware-in-the-Loop (HIL)
// Pozwalają na emulację odczytów czujników przez port Serial.
extern bool mockActive;
extern int mockG1;
extern int mockG2;
extern float mockTemp;
extern int mockDist;

#endif