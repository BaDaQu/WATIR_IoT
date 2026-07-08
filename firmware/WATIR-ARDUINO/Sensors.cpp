// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
// Wersja: 2.0 — WiFi wbudowane (bez osobnego ESP)
// ============================================
//
// Moduł: Czujniki
// Implementacja funkcji obsługujących czujniki:
// - BME280 (temperatura, wilgotność powietrza)
// - HC-SR04 (dystans do lustra wody)
// - Czujniki wilgotności gleby analogowe
//

#include "Sensors.h"
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>
#include <SPI.h>

// Piny dla czujników środowiskowych
const int pinGleba1 = A1;  
const int pinGleba2 = A0; 
const int pinTrig = 8;
const int pinEcho = 7;

// Piny SPI dla BME280: SCK=13, MISO=12, MOSI=11, CS=10
#define BME_CS 10
Adafruit_BME280 bme(BME_CS); // Użycie sprzętowego SPI
bool bmeOK = false; // Status dla panelu diagnostycznego

// Inicjalizacja czujników przy starcie
bool konfigurujCzujniki() {
  pinMode(pinTrig, OUTPUT);
  pinMode(pinEcho, INPUT);
  
  // Próba inicjalizacji BME280 po SPI
  bmeOK = bme.begin(); // Dla SPI adres nie ma znaczenia
  return bmeOK;
}

// Pomiar odległości do lustra wody (w centymetrach)
int zmierzDystans() {
  digitalWrite(pinTrig, LOW); delayMicroseconds(2);
  digitalWrite(pinTrig, HIGH); delayMicroseconds(10);
  digitalWrite(pinTrig, LOW);
  
  long czasTrwania = pulseIn(pinEcho, HIGH, 25000);
  if (czasTrwania == 0) return 999; // Zwróć błąd jeśli brak echa
  return czasTrwania * 0.034 / 2;
}

// Odczyt z czujnika gleby nr 1 (wartość w procentach: 0-100%)
int zmierzWilgotnoscGleby1() {
  return constrain(map(analogRead(pinGleba1), 0, 1023, 0, 100), 0, 100);
}

// Odczyt z czujnika gleby nr 2 (wartość w procentach: 0-100%)
int zmierzWilgotnoscGleby2() {
  return constrain(map(analogRead(pinGleba2), 0, 1023, 0, 100), 0, 100);
}

// Odczyt temperatury z BME280
float zmierzTemperature() { 
  return bme.readTemperature();
}

// Odczyt wilgotności powietrza z BME280
float zmierzWilgotnoscPowietrza() { 
  return bme.readHumidity(); 
}