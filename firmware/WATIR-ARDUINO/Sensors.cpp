// ============================================
// WATIR IoT — Firmware dla Arduino UNO R4 WiFi
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
const int pinGleba1 = A0;  
const int pinGleba2 = A1; 
const int pinTrig = 8;
const int pinEcho = 7;

// Piny SPI dla BME280: SCK=13, MISO=12, MOSI=11, CS=10
#define BME_CS 10
// Konfiguracja sprzętowego SPI dla BME280
Adafruit_BME280 bme(BME_CS); 
bool bmeOK = false; // Status dla panelu diagnostycznego

// Stan symulacji HIL (Hardware-in-the-Loop)
bool mockActive = false;
int mockG1 = 50;
int mockG2 = 50;
float mockTemp = 20.0;
int mockDist = 5;

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
  if (mockActive) return mockDist;

  digitalWrite(pinTrig, LOW); delayMicroseconds(2);
  digitalWrite(pinTrig, HIGH); delayMicroseconds(10);
  digitalWrite(pinTrig, LOW);
  
  long czasTrwania = pulseIn(pinEcho, HIGH, 25000);
  if (czasTrwania == 0) return 999; // Zwróć błąd jeśli brak echa
  return czasTrwania * 0.034 / 2;
}

// Odczyt z wybranego czujnika gleby (wartość w procentach: 0-100%)
int zmierzWilgotnoscGleby(int pin) {
  if (mockActive) {
    if (pin == 14 || pin == A0) return mockG1; // A0 to teraz G1
    if (pin == 15 || pin == A1) return mockG2; // A1 to teraz G2
    return 50;
  }
  return constrain(map(analogRead(pin), 0, 1023, 0, 100), 0, 100);
}

// Odczyt temperatury z BME280
float zmierzTemperature() {
  if (mockActive) return mockTemp;
  if (!bmeOK) return -99.0;
  return bme.readTemperature();
}

// Odczyt wilgotności powietrza z BME280
float zmierzWilgotnoscPowietrza() { 
  return bme.readHumidity(); 
}