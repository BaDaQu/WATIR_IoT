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
int zmierzWilgotnoscGleby1(); 
int zmierzWilgotnoscGleby2(); 
float zmierzTemperature();
float zmierzWilgotnoscPowietrza();

extern bool bmeOK;

#endif